package com.syxs.module.home.service.impl;

import com.syxs.common.enums.GoodsStatus;
import com.syxs.module.goods.entity.Goods;
import com.syxs.module.goods.repository.GoodsRepository;
import com.syxs.module.home.dto.HomeBannerVO;
import com.syxs.module.home.dto.HomeCategoryCardVO;
import com.syxs.module.home.dto.HomeLandingVO;
import com.syxs.module.home.service.HomeService;
import com.syxs.module.order.entity.Order;
import com.syxs.module.order.repository.OrderRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class HomeServiceImpl implements HomeService {

    private static final String DEFAULT_CATEGORY_SAMPLE = "\u67E5\u770B\u8BE5\u5206\u7C7B\u597D\u7269";
    private static final String DEFAULT_BANNER_TITLE = "\u5E73\u53F0\u7CBE\u9009\u597D\u7269";
    private static final String DEFAULT_BANNER_SUBTITLE = "\u5E73\u53F0\u4E25\u9009";

    private final GoodsRepository goodsRepository;
    private final OrderRepository orderRepository;

    public HomeServiceImpl(GoodsRepository goodsRepository,
                           OrderRepository orderRepository) {
        this.goodsRepository = goodsRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    public HomeLandingVO getLanding() {
        List<Goods> visibleGoods = goodsRepository.findAllByOrderByCreatedAtDesc().stream()
            .filter(this::isVisibleGoods)
            .toList();
        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();
        LocalDate today = LocalDate.now();

        Map<String, String> categorySamples = visibleGoods.stream()
            .filter(item -> item.getCategory() != null && !item.getCategory().isBlank())
            .collect(Collectors.toMap(
                Goods::getCategory,
                item -> firstNonBlank(item.getTitle(), DEFAULT_CATEGORY_SAMPLE),
                (left, right) -> left
            ));

        List<HomeCategoryCardVO> categories = goodsRepository.listCategoryStats().stream()
            .filter(item -> item.getName() != null && !item.getName().isBlank())
            .map(item -> HomeCategoryCardVO.builder()
                .name(item.getName())
                .count(item.getCount() == null ? 0L : item.getCount())
                .sample(categorySamples.getOrDefault(item.getName(), DEFAULT_CATEGORY_SAMPLE))
                .build())
            .toList();

        long orderCount = orders.size();
        int carbonSavedKg = visibleGoods.stream()
            .map(Goods::getCarbonSavedKg)
            .filter(Objects::nonNull)
            .mapToInt(Integer::intValue)
            .sum() + Math.toIntExact(orderCount * 5);

        int todayCarbonKg = visibleGoods.stream()
            .filter(item -> item.getCreatedAt() != null && item.getCreatedAt().toLocalDate().isEqual(today))
            .map(Goods::getCarbonSavedKg)
            .filter(Objects::nonNull)
            .mapToInt(Integer::intValue)
            .sum() + Math.toIntExact(
                orders.stream()
                    .filter(item -> item.getCreatedAt() != null && item.getCreatedAt().toLocalDate().isEqual(today))
                    .count() * 5
            );

        if (todayCarbonKg == 0 && carbonSavedKg > 0) {
            todayCarbonKg = Math.min(12, carbonSavedKg);
        }

        return HomeLandingVO.builder()
            .banners(visibleGoods.stream()
                .filter(item -> item.getCoverUrl() != null && !item.getCoverUrl().isBlank())
                .limit(4)
                .map(this::toBanner)
                .toList())
            .categories(categories)
            .goodsCount((long) visibleGoods.size())
            .orderCount(orderCount)
            .carbonSavedKg(carbonSavedKg)
            .todayCarbonKg(todayCarbonKg)
            .build();
    }

    private HomeBannerVO toBanner(Goods goods) {
        return HomeBannerVO.builder()
            .id("banner-" + goods.getId())
            .goodsId(goods.getId())
            .imageUrl(firstNonBlank(goods.getCoverUrl(), "/uploads/demo-created.svg"))
            .title(firstNonBlank(goods.getTitle(), DEFAULT_BANNER_TITLE))
            .subtitle(buildBannerSubtitle(goods))
            .build();
    }

    private String buildBannerSubtitle(Goods goods) {
        if (goods.getStory() != null && !goods.getStory().isBlank()) {
            return goods.getStory();
        }
        String brand = goods.getBrand() == null ? "" : goods.getBrand().trim();
        String category = goods.getCategory() == null ? "" : goods.getCategory().trim();
        if (!brand.isEmpty() && !category.isEmpty()) {
            return brand + " \u00B7 " + category;
        }
        return firstNonBlank(category, DEFAULT_BANNER_SUBTITLE);
    }

    private boolean isVisibleGoods(Goods goods) {
        return goods.getStatus() != null && goods.getStatus() != GoodsStatus.DRAFT;
    }

    private String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
