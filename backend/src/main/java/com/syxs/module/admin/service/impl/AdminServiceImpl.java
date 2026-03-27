package com.syxs.module.admin.service.impl;

import com.syxs.common.enums.GoodsStatus;
import com.syxs.common.enums.KycLevel;
import com.syxs.common.enums.OrderStatus;
import com.syxs.common.exception.BusinessException;
import com.syxs.common.utils.CarbonCalcUtil;
import com.syxs.module.admin.dto.AdminCategoryDTO;
import com.syxs.module.admin.dto.AdminCategoryUpsertRequest;
import com.syxs.module.admin.dto.AdminChartItemDTO;
import com.syxs.module.admin.dto.AdminDashboardDTO;
import com.syxs.module.admin.dto.AdminGoodsDTO;
import com.syxs.module.admin.dto.AdminGoodsUpsertRequest;
import com.syxs.module.admin.dto.AdminLoginRequest;
import com.syxs.module.admin.dto.AdminOrderDTO;
import com.syxs.module.admin.dto.AdminOrderUpsertRequest;
import com.syxs.module.admin.dto.AdminTrendPointDTO;
import com.syxs.module.admin.dto.AdminUserDTO;
import com.syxs.module.admin.dto.AdminUserUpsertRequest;
import com.syxs.module.admin.entity.AdminCategory;
import com.syxs.module.admin.repository.AdminCategoryRepository;
import com.syxs.module.admin.service.AdminService;
import com.syxs.module.goods.entity.Goods;
import com.syxs.module.goods.entity.GoodsImage;
import com.syxs.module.goods.repository.GoodsImageRepository;
import com.syxs.module.goods.repository.GoodsRepository;
import com.syxs.module.message.service.UserMessageService;
import com.syxs.module.order.entity.Order;
import com.syxs.module.order.repository.OrderRepository;
import com.syxs.module.user.dto.TokenPairDTO;
import com.syxs.module.user.dto.UserProfileDTO;
import com.syxs.module.user.entity.User;
import com.syxs.module.user.repository.UserRepository;
import com.syxs.module.user.service.AuthSessionService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminServiceImpl implements AdminService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String DEFAULT_GOODS_IMAGE = "/uploads/demo-created.svg";
    private static final String DEFAULT_AVATAR = "/uploads/default-avatar.svg";
    private static final String DEFAULT_CATEGORY_IMAGE = "/uploads/demo-created.svg";

    private final AdminCategoryRepository adminCategoryRepository;
    private final UserRepository userRepository;
    private final GoodsRepository goodsRepository;
    private final GoodsImageRepository goodsImageRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthSessionService authSessionService;
    private final UserMessageService userMessageService;

    public AdminServiceImpl(AdminCategoryRepository adminCategoryRepository,
                            UserRepository userRepository,
                            GoodsRepository goodsRepository,
                            GoodsImageRepository goodsImageRepository,
                            OrderRepository orderRepository,
                            PasswordEncoder passwordEncoder,
                            AuthSessionService authSessionService,
                            UserMessageService userMessageService) {
        this.adminCategoryRepository = adminCategoryRepository;
        this.userRepository = userRepository;
        this.goodsRepository = goodsRepository;
        this.goodsImageRepository = goodsImageRepository;
        this.orderRepository = orderRepository;
        this.passwordEncoder = passwordEncoder;
        this.authSessionService = authSessionService;
        this.userMessageService = userMessageService;
    }

    @Override
    public UserProfileDTO login(AdminLoginRequest request) {
        User user = userRepository.findByPhone(request.getPhone())
            .orElseThrow(() -> new BusinessException("管理员账号或密码错误"));

        if (!isAdmin(user)) {
            throw new BusinessException("该账号没有管理员权限");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("管理员账号或密码错误");
        }

        TokenPairDTO tokenPair = authSessionService.issueTokens(user.getPhone());
        return toProfile(user, tokenPair);
    }

    @Override
    public List<AdminUserDTO> listUsers(String operatorPhone) {
        assertAdmin(operatorPhone);
        return userRepository.findAllByOrderByCreatedAtDesc().stream()
            .map(this::toAdminUser)
            .toList();
    }

    @Override
    public List<AdminGoodsDTO> listGoods(String operatorPhone) {
        assertAdmin(operatorPhone);
        return goodsRepository.findAllByOrderByCreatedAtDesc().stream()
            .map(this::toAdminGoods)
            .toList();
    }

    @Override
    public List<AdminCategoryDTO> listCategories(String operatorPhone) {
        assertAdmin(operatorPhone);
        ensureUncategorizedCategory();
        return adminCategoryRepository.findAllByOrderBySortNoAscIdAsc().stream()
            .map(this::toAdminCategory)
            .toList();
    }

    @Override
    @Transactional
    public AdminGoodsDTO createGoods(String operatorPhone, AdminGoodsUpsertRequest request) {
        assertAdmin(operatorPhone);
        Goods goods = new Goods();
        applyGoodsRequest(goods, request);
        Goods saved = goodsRepository.save(goods);
        syncGoodsImages(saved.getId(), request.getHeroImage(), request.getGallery());
        return toAdminGoods(saved);
    }

    @Override
    @Transactional
    public AdminGoodsDTO updateGoods(Long id, String operatorPhone, AdminGoodsUpsertRequest request) {
        assertAdmin(operatorPhone);
        Goods goods = goodsRepository.findById(id)
            .orElseThrow(() -> new BusinessException("商品不存在"));
        applyGoodsRequest(goods, request);
        Goods saved = goodsRepository.save(goods);
        syncGoodsImages(saved.getId(), request.getHeroImage(), request.getGallery());
        return toAdminGoods(saved);
    }

    @Override
    @Transactional
    public void deleteGoods(Long id, String operatorPhone) {
        assertAdmin(operatorPhone);
        Goods goods = goodsRepository.findById(id)
            .orElseThrow(() -> new BusinessException("商品不存在"));
        goodsImageRepository.deleteByGoodsId(id);
        orderRepository.deleteByGoodsId(id);
        goodsRepository.delete(goods);
    }

    @Override
    @Transactional
    public AdminGoodsDTO approveGoods(Long id, String operatorPhone, String reviewNote) {
        assertAdmin(operatorPhone);
        Goods goods = goodsRepository.findById(id)
            .orElseThrow(() -> new BusinessException("商品不存在"));
        goods.setAuditStatus("审核通过");
        goods.setReviewNote(hasText(reviewNote) ? reviewNote.trim() : "审核通过，允许上架流转。");
        goods.setStatus(GoodsStatus.ON_SALE);
        goods.setMockCertified(true);
        Goods approvedGoods = goodsRepository.save(goods);
        userMessageService.createForUserPhone(
            approvedGoods.getSellerPhone(),
            "GOODS_REVIEW",
            "商品审核通过",
            "商品《" + defaultString(approvedGoods.getTitle(), "未命名商品") + "》已审核通过并上架。"
        );
        return toAdminGoods(approvedGoods);
    }

    @Override
    @Transactional
    public AdminGoodsDTO rejectGoods(Long id, String operatorPhone, String reviewNote) {
        assertAdmin(operatorPhone);
        Goods goods = goodsRepository.findById(id)
            .orElseThrow(() -> new BusinessException("商品不存在"));
        goods.setAuditStatus("已驳回");
        goods.setReviewNote(hasText(reviewNote) ? reviewNote.trim() : "资料不完整，请补充后重新提交。");
        goods.setStatus(GoodsStatus.DRAFT);
        goods.setMockCertified(false);
        Goods rejectedGoods = goodsRepository.save(goods);
        userMessageService.createForUserPhone(
            rejectedGoods.getSellerPhone(),
            "GOODS_REVIEW",
            "商品审核未通过",
            "商品《" + defaultString(rejectedGoods.getTitle(), "未命名商品") + "》审核未通过，请补充资料后重新提交。"
        );
        return toAdminGoods(rejectedGoods);
    }

    @Override
    public List<AdminOrderDTO> listOrders(String operatorPhone) {
        assertAdmin(operatorPhone);
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
            .map(this::toAdminOrder)
            .toList();
    }

    @Override
    @Transactional
    public AdminOrderDTO createOrder(String operatorPhone, AdminOrderUpsertRequest request) {
        assertAdmin(operatorPhone);
        Order order = new Order();
        applyOrderRequest(order, request);
        Order saved = orderRepository.save(order);
        syncGoodsStatusForOrder(saved.getGoodsId(), saved.getStatus());
        return toAdminOrder(saved);
    }

    @Override
    @Transactional
    public AdminOrderDTO updateOrder(Long id, String operatorPhone, AdminOrderUpsertRequest request) {
        assertAdmin(operatorPhone);
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new BusinessException("订单不存在"));
        Long previousGoodsId = order.getGoodsId();
        applyOrderRequest(order, request);
        Order saved = orderRepository.save(order);

        if (previousGoodsId != null && !previousGoodsId.equals(saved.getGoodsId())) {
            resetGoodsStatus(previousGoodsId);
        }

        syncGoodsStatusForOrder(saved.getGoodsId(), saved.getStatus());
        return toAdminOrder(saved);
    }

    @Override
    @Transactional
    public void deleteOrder(Long id, String operatorPhone) {
        assertAdmin(operatorPhone);
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new BusinessException("订单不存在"));
        orderRepository.delete(order);
        resetGoodsStatus(order.getGoodsId());
    }

    @Override
    @Transactional
    public AdminCategoryDTO createCategory(String operatorPhone, AdminCategoryUpsertRequest request) {
        assertAdmin(operatorPhone);
        ensureCategoryNameUnique(request.getName(), null);
        AdminCategory category = new AdminCategory();
        applyCategoryRequest(category, request);
        category.setSortNo(resolveNextSortNo());
        return toAdminCategory(adminCategoryRepository.save(category));
    }

    @Override
    @Transactional
    public AdminCategoryDTO updateCategory(Long id, String operatorPhone, AdminCategoryUpsertRequest request) {
        assertAdmin(operatorPhone);
        AdminCategory category = adminCategoryRepository.findById(id)
            .orElseThrow(() -> new BusinessException("分类不存在"));
        ensureCategoryNameUnique(request.getName(), id);
        String oldName = category.getName();
        applyCategoryRequest(category, request);
        AdminCategory saved = adminCategoryRepository.save(category);

        if (!oldName.equals(saved.getName())) {
            goodsRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(item -> oldName.equals(item.getCategory()))
                .forEach(item -> {
                    item.setCategory(saved.getName());
                    goodsRepository.save(item);
                });
        }

        return toAdminCategory(saved);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id, String operatorPhone) {
        assertAdmin(operatorPhone);
        AdminCategory category = adminCategoryRepository.findById(id)
            .orElseThrow(() -> new BusinessException("分类不存在"));
        if ("未分类".equals(category.getName())) {
            throw new BusinessException("默认未分类不可删除");
        }

        AdminCategory uncategorized = ensureUncategorizedCategory();
        goodsRepository.findAllByOrderByCreatedAtDesc().stream()
            .filter(item -> category.getName().equals(item.getCategory()))
            .forEach(item -> {
                item.setCategory(uncategorized.getName());
                goodsRepository.save(item);
            });
        adminCategoryRepository.delete(category);
    }

    @Override
    public AdminDashboardDTO getDashboard(String operatorPhone) {
        assertAdmin(operatorPhone);

        List<Goods> goods = goodsRepository.findAllByOrderByCreatedAtDesc();
        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();
        List<User> users = userRepository.findAllByOrderByCreatedAtDesc();

        BigDecimal totalGmv = orders.stream()
            .map(Order::getAmount)
            .filter(amount -> amount != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalCarbonSaved = goods.stream()
            .map(Goods::getCarbonSavedKg)
            .filter(value -> value != null)
            .mapToInt(Integer::intValue)
            .sum();

        long pendingGoodsCount = goods.stream().filter(item -> "待审核".equals(resolveAuditStatus(item))).count();
        long approvedGoodsCount = goods.stream().filter(item -> "审核通过".equals(resolveAuditStatus(item))).count();
        long rejectedGoodsCount = goods.stream().filter(item -> "已驳回".equals(resolveAuditStatus(item))).count();
        long pendingKycCount = users.stream().filter(item -> "待审核".equals(resolveKycReviewStatus(item))).count();
        long approvedKycCount = users.stream().filter(item -> "已通过".equals(resolveKycReviewStatus(item))).count();
        long rejectedKycCount = users.stream().filter(item -> "已驳回".equals(resolveKycReviewStatus(item))).count();
        long completedOrdersCount = orders.stream().filter(item -> item.getStatus() == OrderStatus.COMPLETED).count();
        long paidOrdersCount = orders.stream().filter(item -> item.getStatus() != OrderStatus.PENDING_PAYMENT).count();
        LocalDate today = LocalDate.now();
        long newUsersCount = users.stream()
            .filter(item -> item.getCreatedAt() != null && !item.getCreatedAt().toLocalDate().isBefore(today.minusDays(6)))
            .count();

        long viewCount = goods.stream().map(Goods::getViewCount).filter(value -> value != null).mapToLong(Integer::longValue).sum();
        long favoriteCount = goods.stream().map(Goods::getFavorCount).filter(value -> value != null).mapToLong(Integer::longValue).sum();

        List<AdminTrendPointDTO> recentGmv = new ArrayList<>();
        for (int offset = 6; offset >= 0; offset--) {
            LocalDate currentDate = today.minusDays(offset);
            BigDecimal dayGmv = orders.stream()
                .filter(item -> item.getCreatedAt() != null && item.getCreatedAt().toLocalDate().isEqual(currentDate))
                .map(Order::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            long dayOrderCount = orders.stream()
                .filter(item -> item.getCreatedAt() != null && item.getCreatedAt().toLocalDate().isEqual(currentDate))
                .count();
            recentGmv.add(AdminTrendPointDTO.builder()
                .label(currentDate.getMonthValue() + "/" + currentDate.getDayOfMonth())
                .gmv(dayGmv)
                .orderCount(dayOrderCount)
                .build());
        }

        return AdminDashboardDTO.builder()
            .totalGmv(totalGmv)
            .newUsersCount((int) newUsersCount)
            .totalCarbonSaved(totalCarbonSaved)
            .pendingGoodsCount((int) pendingGoodsCount)
            .pendingKycCount((int) pendingKycCount)
            .completedOrdersCount((int) completedOrdersCount)
            .auditApprovalRate(calculateRate(approvedGoodsCount, approvedGoodsCount + rejectedGoodsCount))
            .kycPassRate(calculateRate(approvedKycCount, approvedKycCount + rejectedKycCount))
            .orderCompletionRate(calculateRate(completedOrdersCount, orders.size()))
            .recentGmv(recentGmv)
            .orderFunnel(List.of(
                AdminChartItemDTO.builder().label("商品浏览").value(viewCount).build(),
                AdminChartItemDTO.builder().label("商品收藏").value(favoriteCount).build(),
                AdminChartItemDTO.builder().label("提交订单").value((long) orders.size()).build(),
                AdminChartItemDTO.builder().label("完成支付").value(paidOrdersCount).build(),
                AdminChartItemDTO.builder().label("交易完成").value(completedOrdersCount).build()
            ))
            .goodsAudit(List.of(
                AdminChartItemDTO.builder().label("待审核").value(pendingGoodsCount).build(),
                AdminChartItemDTO.builder().label("审核通过").value(approvedGoodsCount).build(),
                AdminChartItemDTO.builder().label("已驳回").value(rejectedGoodsCount).build()
            ))
            .userStatus(List.of(
                AdminChartItemDTO.builder().label("正常账户").value(users.stream().filter(item -> "正常".equals(resolveAccountStatus(item))).count()).build(),
                AdminChartItemDTO.builder().label("已封禁").value(users.stream().filter(item -> "已封禁".equals(resolveAccountStatus(item))).count()).build(),
                AdminChartItemDTO.builder().label("待 KYC 审核").value(pendingKycCount).build()
            ))
            .orderStatus(List.of(
                AdminChartItemDTO.builder().label("待付款").value(orders.stream().filter(item -> item.getStatus() == OrderStatus.PENDING_PAYMENT).count()).build(),
                AdminChartItemDTO.builder().label("待发货").value(orders.stream().filter(item -> item.getStatus() == OrderStatus.PENDING_SHIPMENT).count()).build(),
                AdminChartItemDTO.builder().label("运输中").value(orders.stream().filter(item -> item.getStatus() == OrderStatus.IN_TRANSIT).count()).build(),
                AdminChartItemDTO.builder().label("已完成").value(completedOrdersCount).build()
            ))
            .pendingGoods(goods.stream().filter(item -> "待审核".equals(resolveAuditStatus(item))).limit(5).map(this::toAdminGoods).toList())
            .pendingUsers(users.stream().filter(item -> "待审核".equals(resolveKycReviewStatus(item))).limit(5).map(this::toAdminUser).toList())
            .build();
    }

    @Override
    @Transactional
    public AdminUserDTO createUser(String operatorPhone, AdminUserUpsertRequest request) {
        assertAdmin(operatorPhone);
        ensurePhoneUnique(request.getPhone(), null);
        User user = new User();
        applyUserRequest(user, request, true);
        return toAdminUser(userRepository.save(user));
    }

    @Override
    @Transactional
    public AdminUserDTO updateUser(Long id, String operatorPhone, AdminUserUpsertRequest request) {
        assertAdmin(operatorPhone);
        User user = userRepository.findById(id)
            .orElseThrow(() -> new BusinessException("用户不存在"));
        ensurePhoneUnique(request.getPhone(), id);
        applyUserRequest(user, request, false);
        return toAdminUser(userRepository.save(user));
    }

    @Override
    @Transactional
    public void deleteUser(Long id, String operatorPhone) {
        User operator = assertAdmin(operatorPhone);
        User target = userRepository.findById(id)
            .orElseThrow(() -> new BusinessException("用户不存在"));
        if (operator.getId().equals(target.getId())) {
            throw new BusinessException("不能删除当前登录管理员");
        }
        userRepository.delete(target);
    }

    @Override
    @Transactional
    public AdminUserDTO approveUserKyc(Long id, String operatorPhone) {
        assertAdmin(operatorPhone);
        User user = userRepository.findById(id)
            .orElseThrow(() -> new BusinessException("用户不存在"));
        user.setKycReviewStatus("已通过");
        if (user.getKycLevel() == null || user.getKycLevel() == KycLevel.L1) {
            user.setKycLevel(KycLevel.L2);
        }
        user.setLastActiveAt(LocalDateTime.now());
        User approvedUser = userRepository.save(user);
        userMessageService.createForUserPhone(
            approvedUser.getPhone(),
            "KYC_REVIEW",
            "实名认证已通过",
            "你的实名认证审核已通过，当前等级已更新。"
        );
        return toAdminUser(approvedUser);
    }

    @Override
    @Transactional
    public AdminUserDTO rejectUserKyc(Long id, String operatorPhone) {
        assertAdmin(operatorPhone);
        User user = userRepository.findById(id)
            .orElseThrow(() -> new BusinessException("用户不存在"));
        user.setKycReviewStatus("已驳回");
        user.setLastActiveAt(LocalDateTime.now());
        User rejectedUser = userRepository.save(user);
        userMessageService.createForUserPhone(
            rejectedUser.getPhone(),
            "KYC_REVIEW",
            "实名认证未通过",
            "你的实名认证审核未通过，请补充资料后重新提交。"
        );
        return toAdminUser(rejectedUser);
    }

    @Override
    @Transactional
    public AdminUserDTO banUser(Long id, String operatorPhone) {
        assertAdmin(operatorPhone);
        User user = userRepository.findById(id)
            .orElseThrow(() -> new BusinessException("用户不存在"));
        user.setAccountStatus("已封禁");
        user.setLastActiveAt(LocalDateTime.now());
        return toAdminUser(userRepository.save(user));
    }

    @Override
    @Transactional
    public AdminUserDTO unbanUser(Long id, String operatorPhone) {
        assertAdmin(operatorPhone);
        User user = userRepository.findById(id)
            .orElseThrow(() -> new BusinessException("用户不存在"));
        user.setAccountStatus("正常");
        user.setLastActiveAt(LocalDateTime.now());
        return toAdminUser(userRepository.save(user));
    }

    private void applyGoodsRequest(Goods goods, AdminGoodsUpsertRequest request) {
        if (!hasText(request.getTitle()) || !hasText(request.getCategory()) || !hasText(request.getBrand())) {
            throw new BusinessException("商品标题、分类和品牌不能为空");
        }

        BigDecimal price = request.getPrice() != null ? request.getPrice() : defaultBigDecimal(goods.getSalePrice(), BigDecimal.ZERO);
        String heroImage = firstNonBlank(request.getHeroImage(), firstImage(request.getGallery()), goods.getCoverUrl(), DEFAULT_GOODS_IMAGE);
        String auditStatus = hasText(request.getAuditStatus()) ? request.getAuditStatus().trim() : resolveAuditStatus(goods);

        goods.setTitle(request.getTitle().trim());
        goods.setCategory(request.getCategory().trim());
        goods.setBrand(request.getBrand().trim());
        goods.setConditionLevel(hasText(request.getCondition()) ? request.getCondition().trim() : defaultString(goods.getConditionLevel(), "95 新"));
        goods.setSalePrice(price);
        goods.setOriginalPrice(request.getOriginalPrice() != null
            ? request.getOriginalPrice()
            : defaultBigDecimal(goods.getOriginalPrice(), price.multiply(new BigDecimal("1.60")).setScale(2, RoundingMode.HALF_UP)));
        goods.setAiPrice(request.getAiPrice() != null
            ? request.getAiPrice()
            : defaultBigDecimal(goods.getAiPrice(), price.multiply(new BigDecimal("0.98")).setScale(2, RoundingMode.HALF_UP)));
        goods.setCarbonSavedKg(request.getCarbonSavedKg() != null
            ? request.getCarbonSavedKg()
            : defaultInteger(goods.getCarbonSavedKg(), CarbonCalcUtil.estimateCarbonSaved(price.doubleValue())));
        goods.setCity(hasText(request.getCity()) ? request.getCity().trim() : defaultString(goods.getCity(), "上海"));
        goods.setSellerName(hasText(request.getSellerName()) ? request.getSellerName().trim() : defaultString(goods.getSellerName(), "平台卖家"));
        goods.setSellerLevel(hasText(request.getSellerLevel()) ? request.getSellerLevel().trim() : defaultString(goods.getSellerLevel(), "L2"));
        goods.setCoverUrl(heroImage);
        goods.setStory(hasText(request.getStory()) ? request.getStory().trim() : defaultString(goods.getStory(), "待补充商品故事"));
        goods.setDescription(hasText(request.getDescription()) ? request.getDescription().trim() : defaultString(goods.getDescription(), "待补充商品描述"));
        goods.setTags(joinTags(request.getTags(), goods.getTags()));
        goods.setStatus(toGoodsStatus(request.getStatus(), goods.getStatus()));
        goods.setCreatedAt(parseDateTime(request.getCreatedAt(), goods.getCreatedAt() == null ? LocalDateTime.now() : goods.getCreatedAt()));
        goods.setAuditStatus(auditStatus);
        goods.setReviewNote(hasText(request.getReviewNote())
            ? request.getReviewNote().trim()
            : "已驳回".equals(auditStatus) ? "资料不完整，请补充后重新提交。" : "待审核".equals(auditStatus) ? "新发布商品，待运营审核。" : "审核通过，允许上架流转。");
        goods.setTransferType(hasText(request.getTransferType()) ? request.getTransferType().trim() : defaultString(goods.getTransferType(), "自卖"));
        goods.setViewCount(request.getViewCount() != null ? request.getViewCount() : defaultInteger(goods.getViewCount(), 0));
        goods.setFavorCount(request.getFavorCount() != null ? request.getFavorCount() : defaultInteger(goods.getFavorCount(), 0));
        goods.setMockCertified(request.getMockCertified() != null ? request.getMockCertified() : "审核通过".equals(auditStatus));
    }

    private void applyUserRequest(User user, AdminUserUpsertRequest request, boolean creating) {
        if (!hasText(request.getName()) || !hasText(request.getPhone())) {
            throw new BusinessException("用户姓名和手机号不能为空");
        }

        user.setNickname(request.getName().trim());
        user.setPhone(request.getPhone().trim());
        if (hasText(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword().trim()));
        } else if (creating) {
            user.setPassword(passwordEncoder.encode("Test@123"));
        }
        user.setAvatar(hasText(request.getAvatar()) ? request.getAvatar().trim() : defaultString(user.getAvatar(), DEFAULT_AVATAR));
        user.setCity(hasText(request.getCity()) ? request.getCity().trim() : defaultString(user.getCity(), "上海"));
        user.setBio(hasText(request.getBio()) ? request.getBio().trim() : defaultString(user.getBio(), ""));
        user.setRole(hasText(request.getRole()) ? request.getRole().trim().toUpperCase() : defaultString(user.getRole(), "USER"));
        user.setKycLevel(toKycLevel(request.getKycLevel(), user.getKycLevel()));
        user.setCarbonPoints(request.getCarbonPoints() != null ? request.getCarbonPoints() : defaultInteger(user.getCarbonPoints(), 0));
        user.setAccountStatus(hasText(request.getAccountStatus()) ? request.getAccountStatus().trim() : resolveAccountStatus(user));
        user.setKycReviewStatus(hasText(request.getKycReviewStatus()) ? request.getKycReviewStatus().trim() : resolveKycReviewStatus(user));
        user.setCreatedAt(parseDateTime(request.getRegisterAt(), user.getCreatedAt() == null ? LocalDateTime.now() : user.getCreatedAt()));
        user.setLastActiveAt(parseDateTime(request.getLastActiveAt(), user.getLastActiveAt() == null ? user.getCreatedAt() : user.getLastActiveAt()));
    }

    private void applyOrderRequest(Order order, AdminOrderUpsertRequest request) {
        if (request.getGoodsId() == null || !hasText(request.getBuyerName())) {
            throw new BusinessException("订单商品和买家姓名不能为空");
        }

        Goods goods = goodsRepository.findById(request.getGoodsId())
            .orElseThrow(() -> new BusinessException("关联商品不存在"));

        order.setGoodsId(goods.getId());
        order.setBuyerName(request.getBuyerName().trim());
        order.setAmount(request.getAmount() != null ? request.getAmount() : defaultBigDecimal(goods.getSalePrice(), BigDecimal.ZERO));
        order.setStatus(toOrderStatus(request.getStatus(), order.getStatus()));
        order.setCreatedAt(parseDateTime(request.getCreatedAt(), order.getCreatedAt() == null ? LocalDateTime.now() : order.getCreatedAt()));
    }

    private void syncGoodsStatusForOrder(Long goodsId, OrderStatus orderStatus) {
        if (goodsId == null) {
            return;
        }

        goodsRepository.findById(goodsId).ifPresent(goods -> {
            GoodsStatus nextStatus = orderStatus == OrderStatus.COMPLETED ? GoodsStatus.SOLD : GoodsStatus.RESERVED;
            goods.setStatus(nextStatus);
            goodsRepository.save(goods);
        });
    }

    private void resetGoodsStatus(Long goodsId) {
        if (goodsId == null) {
            return;
        }

        goodsRepository.findById(goodsId).ifPresent(goods -> {
            goods.setStatus(GoodsStatus.ON_SALE);
            goodsRepository.save(goods);
        });
    }

    private void applyCategoryRequest(AdminCategory category, AdminCategoryUpsertRequest request) {
        if (!hasText(request.getName())) {
            throw new BusinessException("分类名称不能为空");
        }

        category.setName(request.getName().trim());
        category.setDescription(defaultString(request.getDescription(), "用于管理后台商品分类与品牌标签。"));
        category.setFeaturedBrands(joinTags(request.getFeaturedBrands(), category.getFeaturedBrands()));
        category.setCoverImage(firstNonBlank(request.getCoverImage(), category.getCoverImage(), DEFAULT_CATEGORY_IMAGE));
        if (category.getSortNo() == null) {
            category.setSortNo(resolveNextSortNo());
        }
    }

    private void syncGoodsImages(Long goodsId, String heroImage, List<String> gallery) {
        LinkedHashSet<String> images = new LinkedHashSet<>();
        if (hasText(heroImage)) {
            images.add(heroImage.trim());
        }
        if (gallery != null) {
            gallery.stream().filter(this::hasText).map(String::trim).forEach(images::add);
        }
        if (images.isEmpty()) {
            images.add(DEFAULT_GOODS_IMAGE);
        }

        goodsImageRepository.deleteByGoodsId(goodsId);
        int index = 0;
        for (String imageUrl : images) {
            GoodsImage goodsImage = new GoodsImage();
            goodsImage.setGoodsId(goodsId);
            goodsImage.setImageUrl(imageUrl);
            goodsImage.setSortNo(index + 1);
            goodsImage.setIsCover(index == 0);
            goodsImageRepository.save(goodsImage);
            index++;
        }
    }

    private void ensurePhoneUnique(String phone, Long currentUserId) {
        if (!hasText(phone)) {
            throw new BusinessException("手机号不能为空");
        }

        userRepository.findByPhone(phone.trim()).ifPresent(existing -> {
            if (currentUserId == null || !existing.getId().equals(currentUserId)) {
                throw new BusinessException("手机号已存在，请更换后再试");
            }
        });
    }

    private User assertAdmin(String operatorPhone) {
        User operator = userRepository.findByPhone(operatorPhone)
            .orElseThrow(() -> new BusinessException("管理员不存在"));

        if (!isAdmin(operator)) {
            throw new BusinessException("无管理员访问权限");
        }
        return operator;
    }

    private boolean isAdmin(User user) {
        return user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole());
    }

    private AdminOrderDTO toAdminOrder(Order order) {
        return AdminOrderDTO.builder()
            .id(order.getId())
            .goodsId(order.getGoodsId())
            .buyerName(order.getBuyerName())
            .amount(defaultBigDecimal(order.getAmount(), BigDecimal.ZERO))
            .status(order.getStatus() == null ? OrderStatus.PENDING_PAYMENT.name() : order.getStatus().name())
            .createdAt(formatDateTime(order.getCreatedAt()))
            .build();
    }

    private AdminCategoryDTO toAdminCategory(AdminCategory category) {
        return AdminCategoryDTO.builder()
            .id(category.getId())
            .name(category.getName())
            .description(defaultString(category.getDescription(), "用于管理后台商品分类与品牌标签。"))
            .featuredBrands(splitTags(category.getFeaturedBrands()))
            .coverImage(firstNonBlank(category.getCoverImage(), DEFAULT_CATEGORY_IMAGE))
            .build();
    }

    private AdminGoodsDTO toAdminGoods(Goods goods) {
        List<String> gallery = goodsImageRepository.findByGoodsIdOrderBySortNoAsc(goods.getId()).stream()
            .map(GoodsImage::getImageUrl)
            .toList();

        return AdminGoodsDTO.builder()
            .id(goods.getId())
            .title(goods.getTitle())
            .category(goods.getCategory())
            .brand(goods.getBrand())
            .condition(defaultString(goods.getConditionLevel(), "95 新"))
            .price(defaultBigDecimal(goods.getSalePrice(), BigDecimal.ZERO))
            .originalPrice(defaultBigDecimal(goods.getOriginalPrice(), BigDecimal.ZERO))
            .carbonSavedKg(defaultInteger(goods.getCarbonSavedKg(), 0))
            .aiPrice(defaultBigDecimal(goods.getAiPrice(), BigDecimal.ZERO))
            .city(defaultString(goods.getCity(), "上海"))
            .sellerName(defaultString(goods.getSellerName(), "平台卖家"))
            .sellerLevel(defaultString(goods.getSellerLevel(), "L2"))
            .story(defaultString(goods.getStory(), "待补充商品故事"))
            .description(defaultString(goods.getDescription(), "待补充商品描述"))
            .status(toGoodsStatusLabel(goods.getStatus()))
            .tags(splitTags(goods.getTags()))
            .heroImage(firstNonBlank(goods.getCoverUrl(), DEFAULT_GOODS_IMAGE))
            .gallery(gallery.isEmpty() ? List.of(firstNonBlank(goods.getCoverUrl(), DEFAULT_GOODS_IMAGE)) : gallery)
            .createdAt(formatDateTime(goods.getCreatedAt()))
            .auditStatus(resolveAuditStatus(goods))
            .reviewNote(defaultString(goods.getReviewNote(), "审核通过，允许正常流转。"))
            .transferType(defaultString(goods.getTransferType(), "自卖"))
            .viewCount(defaultInteger(goods.getViewCount(), 0))
            .favorCount(defaultInteger(goods.getFavorCount(), 0))
            .mockCertified(goods.getMockCertified() != null ? goods.getMockCertified() : "审核通过".equals(resolveAuditStatus(goods)))
            .build();
    }

    private AdminUserDTO toAdminUser(User user) {
        return AdminUserDTO.builder()
            .id(user.getId())
            .name(user.getNickname())
            .phone(user.getPhone())
            .password("")
            .avatar(firstNonBlank(user.getAvatar(), DEFAULT_AVATAR))
            .city(defaultString(user.getCity(), "上海"))
            .bio(defaultString(user.getBio(), ""))
            .role(defaultString(user.getRole(), "USER"))
            .kycLevel(user.getKycLevel() == null ? "L1" : user.getKycLevel().name())
            .carbonPoints(defaultInteger(user.getCarbonPoints(), 0))
            .accountStatus(resolveAccountStatus(user))
            .kycReviewStatus(resolveKycReviewStatus(user))
            .registerAt(formatDateTime(user.getCreatedAt()))
            .lastActiveAt(formatDateTime(user.getLastActiveAt() == null ? user.getCreatedAt() : user.getLastActiveAt()))
            .build();
    }

    private UserProfileDTO toProfile(User user, TokenPairDTO tokenPair) {
        return UserProfileDTO.builder()
            .id(user.getId())
            .nickname(user.getNickname())
            .phone(user.getPhone())
            .city(user.getCity())
            .avatar(user.getAvatar())
            .kycLevel(user.getKycLevel())
            .bio(user.getBio())
            .role(user.getRole())
            .carbonPoints(user.getCarbonPoints())
            .token(tokenPair == null ? null : tokenPair.getAccessToken())
            .accessToken(tokenPair == null ? null : tokenPair.getAccessToken())
            .refreshToken(tokenPair == null ? null : tokenPair.getRefreshToken())
            .build();
    }

    private String resolveAuditStatus(Goods goods) {
        return hasText(goods.getAuditStatus()) ? goods.getAuditStatus().trim() : "审核通过";
    }

    private String resolveAccountStatus(User user) {
        return hasText(user.getAccountStatus()) ? user.getAccountStatus().trim() : "正常";
    }

    private String resolveKycReviewStatus(User user) {
        if (hasText(user.getKycReviewStatus())) {
            return user.getKycReviewStatus().trim();
        }
        return user.getKycLevel() == null || user.getKycLevel() == KycLevel.L1 ? "待审核" : "已通过";
    }

    private GoodsStatus toGoodsStatus(String status, GoodsStatus fallback) {
        if (!hasText(status)) {
            return fallback == null ? GoodsStatus.ON_SALE : fallback;
        }

        return switch (status.trim()) {
            case "已预订", "RESERVED" -> GoodsStatus.RESERVED;
            case "已售出", "SOLD" -> GoodsStatus.SOLD;
            case "草稿", "DRAFT" -> GoodsStatus.DRAFT;
            case "待审核", "PENDING_APPRAISAL" -> GoodsStatus.PENDING_APPRAISAL;
            case "已下架", "OFF_SHELF" -> GoodsStatus.OFF_SHELF;
            case "在售中", "ON_SALE" -> GoodsStatus.ON_SALE;
            default -> GoodsStatus.ON_SALE;
        };
    }

    private String toGoodsStatusLabel(GoodsStatus status) {
        if (status == null) {
            return "在售中";
        }

        return switch (status) {
            case DRAFT -> "草稿";
            case PENDING_APPRAISAL -> "待审核";
            case OFF_SHELF -> "已下架";
            case RESERVED -> "已预订";
            case SOLD -> "已售出";
            default -> "在售中";
        };
    }

    private KycLevel toKycLevel(String value, KycLevel fallback) {
        if (!hasText(value)) {
            return fallback == null ? KycLevel.L1 : fallback;
        }

        try {
            return KycLevel.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException error) {
            return fallback == null ? KycLevel.L1 : fallback;
        }
    }

    private OrderStatus toOrderStatus(String value, OrderStatus fallback) {
        if (!hasText(value)) {
            return fallback == null ? OrderStatus.PENDING_PAYMENT : fallback;
        }

        return switch (value.trim()) {
            case "待发货", "PENDING_SHIPMENT" -> OrderStatus.PENDING_SHIPMENT;
            case "运输中", "IN_TRANSIT" -> OrderStatus.IN_TRANSIT;
            case "已完成", "COMPLETED" -> OrderStatus.COMPLETED;
            default -> OrderStatus.PENDING_PAYMENT;
        };
    }

    private LocalDateTime parseDateTime(String value, LocalDateTime fallback) {
        if (!hasText(value)) {
            return fallback;
        }

        String normalized = value.trim().replace('T', ' ');
        List<DateTimeFormatter> formatters = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ISO_LOCAL_DATE
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                if (formatter == DateTimeFormatter.ISO_LOCAL_DATE) {
                    return LocalDate.parse(normalized, formatter).atTime(LocalTime.of(10, 0));
                }
                return LocalDateTime.parse(normalized, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        return fallback;
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : value.format(DATE_TIME_FORMATTER);
    }

    private List<String> splitTags(String tags) {
        if (!hasText(tags)) {
            return List.of();
        }
        return List.of(tags.split(",")).stream()
            .map(String::trim)
            .filter(this::hasText)
            .toList();
    }

    private String joinTags(List<String> tags, String fallback) {
        if (tags == null || tags.isEmpty()) {
            return defaultString(fallback, "");
        }
        return tags.stream()
            .filter(this::hasText)
            .map(String::trim)
            .distinct()
            .reduce((left, right) -> left + "," + right)
            .orElse(defaultString(fallback, ""));
    }

    private String firstImage(List<String> gallery) {
        if (gallery == null) {
            return null;
        }
        return gallery.stream().filter(this::hasText).map(String::trim).findFirst().orElse(null);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String defaultString(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    private Integer defaultInteger(Integer value, Integer fallback) {
        return value != null ? value : fallback;
    }

    private BigDecimal defaultBigDecimal(BigDecimal value, BigDecimal fallback) {
        return value != null ? value : fallback;
    }

    private void ensureCategoryNameUnique(String name, Long currentCategoryId) {
        if (!hasText(name)) {
            throw new BusinessException("分类名称不能为空");
        }

        adminCategoryRepository.findByName(name.trim()).ifPresent(existing -> {
            if (currentCategoryId == null || !existing.getId().equals(currentCategoryId)) {
                throw new BusinessException("分类名称已存在，请使用其他名称");
            }
        });
    }

    private int resolveNextSortNo() {
        return adminCategoryRepository.findAllByOrderBySortNoAscIdAsc().stream()
            .map(AdminCategory::getSortNo)
            .filter(value -> value != null)
            .max(Integer::compareTo)
            .orElse(0) + 1;
    }

    private AdminCategory ensureUncategorizedCategory() {
        return adminCategoryRepository.findByName("未分类")
            .orElseGet(() -> {
                AdminCategory category = new AdminCategory();
                category.setName("未分类");
                category.setDescription("承接待整理或暂未归类的商品，方便管理员后续调整。");
                category.setFeaturedBrands("");
                category.setCoverImage(DEFAULT_CATEGORY_IMAGE);
                category.setSortNo(resolveNextSortNo());
                return adminCategoryRepository.save(category);
            });
    }

    private int calculateRate(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0;
        }
        return (int) Math.round((numerator * 100.0d) / denominator);
    }
}
