package com.syxs.module.goods.service.impl;

import com.syxs.common.enums.GoodsStatus;
import com.syxs.common.exception.BusinessException;
import com.syxs.common.utils.CarbonCalcUtil;
import com.syxs.module.goods.dto.GoodsCreateDTO;
import com.syxs.module.goods.dto.GoodsCategoryVO;
import com.syxs.module.goods.dto.GoodsDetailVO;
import com.syxs.module.goods.dto.GoodsListVO;
import com.syxs.module.goods.entity.Favorite;
import com.syxs.module.goods.entity.Goods;
import com.syxs.module.goods.entity.GoodsImage;
import com.syxs.module.goods.repository.FavoriteRepository;
import com.syxs.module.goods.repository.GoodsImageRepository;
import com.syxs.module.goods.repository.GoodsRepository;
import com.syxs.module.goods.service.GoodsService;
import com.syxs.module.user.entity.User;
import com.syxs.module.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GoodsServiceImpl implements GoodsService {

    private static final String DEFAULT_CITY = "Shanghai";
    private static final String DEFAULT_LEVEL = "L1";

    private final GoodsRepository goodsRepository;
    private final GoodsImageRepository goodsImageRepository;
    private final UserRepository userRepository;
    private final FavoriteRepository favoriteRepository;

    public GoodsServiceImpl(GoodsRepository goodsRepository,
                            GoodsImageRepository goodsImageRepository,
                            UserRepository userRepository,
                            FavoriteRepository favoriteRepository) {
        this.goodsRepository = goodsRepository;
        this.goodsImageRepository = goodsImageRepository;
        this.userRepository = userRepository;
        this.favoriteRepository = favoriteRepository;
    }

    @Override
    public List<GoodsListVO> listGoods(String keyword, String category, BigDecimal maxPrice, String sortMode) {
        return goodsRepository.searchCatalog(normalizeKeyword(keyword), normalizeCategory(category), maxPrice).stream()
            .sorted(buildComparator(sortMode))
            .map(this::toGoodsListVO)
            .toList();
    }

    @Override
    public List<GoodsCategoryVO> listCategories() {
        return goodsRepository.listCategoryStats().stream()
            .map(item -> GoodsCategoryVO.builder()
                .name(item.getName())
                .count(item.getCount() == null ? 0L : item.getCount())
                .build())
            .toList();
    }

    @Override
    public GoodsDetailVO getDetail(Long id) {
        return toGoodsDetailVO(findGoods(id));
    }

    @Override
    @Transactional
    public GoodsDetailVO createGoods(GoodsCreateDTO request, String operatorPhone) {
        User currentUser = findUser(operatorPhone);
        Goods goods = new Goods();
        applyGoodsPayload(goods, request, currentUser);
        goods.setStatus(GoodsStatus.DRAFT);
        goods.setAuditStatus("草稿");
        goods.setReviewNote("已保存为草稿，可补充资料后再提交审核。");
        goods.setViewCount(0);
        goods.setFavorCount(0);
        goods.setMockCertified(false);
        goods.setTransferType(goods.getTransferType() == null ? "自卖" : goods.getTransferType());
        goods.setCreatedAt(LocalDateTime.now());
        Goods saved = goodsRepository.save(goods);
        syncImages(saved.getId(), request.getImageUrls());
        return toGoodsDetailVO(saved);
    }

    @Override
    @Transactional
    public GoodsDetailVO updateGoods(Long id, GoodsCreateDTO request, String operatorPhone) {
        User operator = findUser(operatorPhone);
        Goods goods = findGoods(id);
        assertSeller(goods, operator);
        if (goods.getStatus() == GoodsStatus.SOLD) {
            throw new BusinessException("Sold goods cannot be edited");
        }
        applyGoodsPayload(goods, request, operator);
        Goods saved = goodsRepository.save(goods);
        syncImages(saved.getId(), request.getImageUrls());
        return toGoodsDetailVO(saved);
    }

    @Override
    @Transactional
    public GoodsDetailVO publishGoods(Long id, String operatorPhone) {
        User operator = findUser(operatorPhone);
        Goods goods = findGoods(id);
        assertSeller(goods, operator);
        if (goods.getStatus() == GoodsStatus.SOLD) {
            throw new BusinessException("Sold goods cannot be published again");
        }
        goods.setStatus(GoodsStatus.PENDING_APPRAISAL);
        goods.setAuditStatus("待审核");
        goods.setReviewNote("商品已提交审核，请等待运营复核。");
        goods.setMockCertified(false);
        return toGoodsDetailVO(goodsRepository.save(goods));
    }

    @Override
    @Transactional
    public void deleteGoods(Long id, String operatorPhone) {
        User operator = findUser(operatorPhone);
        Goods goods = findGoods(id);
        assertSeller(goods, operator);
        goods.setStatus(GoodsStatus.OFF_SHELF);
        goods.setReviewNote("商品已下架。");
        goodsRepository.save(goods);
    }

    @Override
    public List<GoodsListVO> listMyGoods(String operatorPhone, String status) {
        User operator = findUser(operatorPhone);
        GoodsStatus targetStatus = parseStatus(status);
        List<Goods> goods = targetStatus == null
            ? goodsRepository.findAllBySellerPhoneOrderByCreatedAtDesc(operator.getPhone())
            : goodsRepository.findAllBySellerPhoneAndStatusOrderByCreatedAtDesc(operator.getPhone(), targetStatus);
        return goods.stream()
            .map(this::toGoodsListVO)
            .toList();
    }

    @Override
    @Transactional
    public void favoriteGoods(Long id, String operatorPhone) {
        User operator = findUser(operatorPhone);
        Goods goods = findGoods(id);
        if (favoriteRepository.findByUserIdAndGoodsId(operator.getId(), goods.getId()).isEmpty()) {
            Favorite favorite = new Favorite();
            favorite.setUserId(operator.getId());
            favorite.setGoodsId(goods.getId());
            favorite.setCreatedAt(LocalDateTime.now());
            favoriteRepository.save(favorite);
        }
        refreshFavoriteCount(goods);
    }

    @Override
    @Transactional
    public void unfavoriteGoods(Long id, String operatorPhone) {
        User operator = findUser(operatorPhone);
        Goods goods = findGoods(id);
        favoriteRepository.deleteByUserIdAndGoodsId(operator.getId(), goods.getId());
        refreshFavoriteCount(goods);
    }

    @Override
    public List<GoodsListVO> listFavoriteGoods(String operatorPhone) {
        User operator = findUser(operatorPhone);
        return favoriteRepository.findAllByUserIdOrderByCreatedAtDesc(operator.getId()).stream()
            .map(Favorite::getGoodsId)
            .map(goodsRepository::findById)
            .flatMap(optional -> optional.stream())
            .filter(goods -> goods.getStatus() != GoodsStatus.OFF_SHELF)
            .map(this::toGoodsListVO)
            .toList();
    }

    private GoodsListVO toGoodsListVO(Goods goods) {
        return GoodsListVO.builder()
            .id(goods.getId())
            .title(goods.getTitle())
            .category(goods.getCategory())
            .brand(goods.getBrand())
            .conditionLevel(goods.getConditionLevel())
            .coverUrl(goods.getCoverUrl())
            .salePrice(goods.getSalePrice())
            .aiPrice(goods.getAiPrice())
            .carbonSavedKg(goods.getCarbonSavedKg())
            .story(goods.getStory())
            .sellerName(goods.getSellerName())
            .sellerLevel(goods.getSellerLevel())
            .city(goods.getCity())
            .status(goods.getStatus() == null ? GoodsStatus.DRAFT.name() : goods.getStatus().name())
            .auditStatus(goods.getAuditStatus())
            .reviewNote(goods.getReviewNote())
            .favorCount(goods.getFavorCount() == null ? 0 : goods.getFavorCount())
            .mockCertified(resolveMockCertified(goods))
            .createdAt(goods.getCreatedAt())
            .build();
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String normalized = keyword.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeCategory(String category) {
        if (category == null) {
            return null;
        }
        String normalized = category.trim();
        if (normalized.isEmpty() || "全部".equals(normalized)) {
            return null;
        }
        return normalized;
    }

    private Comparator<Goods> buildComparator(String sortMode) {
        Comparator<Goods> latestComparator = Comparator
            .comparing(Goods::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(Goods::getId, Comparator.nullsLast(Comparator.reverseOrder()));

        if ("priceAsc".equals(sortMode)) {
            return Comparator
                .comparing(Goods::getSalePrice, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(latestComparator);
        }
        if ("priceDesc".equals(sortMode)) {
            return Comparator
                .comparing(Goods::getSalePrice, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(latestComparator);
        }
        return latestComparator;
    }

    private List<String> splitTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return List.of(tags.split(",")).stream()
            .map(String::trim)
            .filter(tag -> !tag.isEmpty())
            .toList();
    }

    private Goods findGoods(Long id) {
        return goodsRepository.findById(id)
            .orElseThrow(() -> new BusinessException("Goods not found"));
    }

    private User findUser(String phone) {
        return userRepository.findByPhone(phone)
            .orElseThrow(() -> new BusinessException("User not found"));
    }

    private void assertSeller(Goods goods, User operator) {
        if (operator.getRole() != null && "ADMIN".equalsIgnoreCase(operator.getRole())) {
            return;
        }
        if (goods.getSellerPhone() == null || !goods.getSellerPhone().equals(operator.getPhone())) {
            throw new BusinessException("Only the seller can perform this action");
        }
    }

    private void applyGoodsPayload(Goods goods, GoodsCreateDTO request, User currentUser) {
        List<String> imageUrls = normalizeImages(request.getImageUrls());
        String coverUrl = imageUrls.isEmpty() ? "/uploads/demo-created.svg" : imageUrls.get(0);

        goods.setTitle(request.getTitle());
        goods.setCategory(request.getCategory());
        goods.setBrand(request.getBrand());
        goods.setConditionLevel(request.getConditionLevel());
        goods.setSalePrice(request.getSalePrice());
        goods.setOriginalPrice(request.getSalePrice().multiply(new BigDecimal("1.65")));
        if (goods.getAiPrice() == null) {
            goods.setAiPrice(request.getSalePrice().multiply(new BigDecimal("0.98")));
        }
        goods.setCarbonSavedKg(CarbonCalcUtil.estimateCarbonSaved(request.getSalePrice().doubleValue()));
        goods.setCity(currentUser.getCity() == null || currentUser.getCity().isBlank() ? DEFAULT_CITY : currentUser.getCity());
        goods.setSellerName(currentUser.getNickname());
        goods.setSellerPhone(currentUser.getPhone());
        goods.setSellerLevel(currentUser.getKycLevel() == null ? DEFAULT_LEVEL : currentUser.getKycLevel().name());
        goods.setCoverUrl(coverUrl);
        goods.setStory(request.getStory() == null || request.getStory().isBlank()
            ? request.getTitle() + " is ready for the second-hand marketplace."
            : request.getStory());
        goods.setTags(request.getTags() == null || request.getTags().isBlank()
            ? "well-kept,verified-ready,story-friendly"
            : request.getTags());
        goods.setDescription(request.getDescription());
        goods.setTransferType(goods.getTransferType() == null || goods.getTransferType().isBlank() ? "自卖" : goods.getTransferType());
    }

    private void syncImages(Long goodsId, List<String> imageUrls) {
        List<String> gallery = normalizeImages(imageUrls);
        if (gallery.isEmpty()) {
            gallery = List.of("/uploads/demo-created.svg");
        }
        goodsImageRepository.deleteByGoodsId(goodsId);
        for (int index = 0; index < gallery.size(); index++) {
            GoodsImage goodsImage = new GoodsImage();
            goodsImage.setGoodsId(goodsId);
            goodsImage.setImageUrl(gallery.get(index));
            goodsImage.setSortNo(index + 1);
            goodsImage.setIsCover(index == 0);
            goodsImageRepository.save(goodsImage);
        }
    }

    private void refreshFavoriteCount(Goods goods) {
        goods.setFavorCount((int) favoriteRepository.countByGoodsId(goods.getId()));
        goodsRepository.save(goods);
    }

    private GoodsDetailVO toGoodsDetailVO(Goods goods) {
        List<String> gallery = goodsImageRepository.findByGoodsIdOrderBySortNoAsc(goods.getId()).stream()
            .map(GoodsImage::getImageUrl)
            .toList();
        return GoodsDetailVO.builder()
            .id(goods.getId())
            .title(goods.getTitle())
            .category(goods.getCategory())
            .brand(goods.getBrand())
            .conditionLevel(goods.getConditionLevel())
            .salePrice(goods.getSalePrice())
            .originalPrice(goods.getOriginalPrice())
            .aiPrice(goods.getAiPrice())
            .description(goods.getDescription())
            .story(goods.getStory())
            .sellerName(goods.getSellerName())
            .sellerLevel(goods.getSellerLevel())
            .city(goods.getCity())
            .status(goods.getStatus() == null ? GoodsStatus.DRAFT.name() : goods.getStatus().name())
            .auditStatus(goods.getAuditStatus())
            .reviewNote(goods.getReviewNote())
            .favorCount(goods.getFavorCount() == null ? 0 : goods.getFavorCount())
            .mockCertified(resolveMockCertified(goods))
            .tags(splitTags(goods.getTags()))
            .gallery(gallery.isEmpty() ? List.of(goods.getCoverUrl()) : gallery)
            .build();
    }

    private boolean resolveMockCertified(Goods goods) {
        return Boolean.TRUE.equals(goods.getMockCertified()) || "审核通过".equals(goods.getAuditStatus());
    }

    private List<String> normalizeImages(List<String> imageUrls) {
        if (imageUrls == null) {
            return List.of();
        }
        return imageUrls.stream()
            .filter(url -> url != null && !url.isBlank())
            .map(String::trim)
            .distinct()
            .toList();
    }

    private GoodsStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return GoodsStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("Unsupported goods status");
        }
    }
}
