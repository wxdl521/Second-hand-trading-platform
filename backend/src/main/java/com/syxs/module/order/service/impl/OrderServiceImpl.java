package com.syxs.module.order.service.impl;

import com.syxs.common.enums.GoodsStatus;
import com.syxs.common.exception.BusinessException;
import com.syxs.config.CarbonConfig;
import com.syxs.module.carbon.entity.CarbonAccount;
import com.syxs.module.carbon.entity.CarbonRecord;
import com.syxs.module.carbon.repository.CarbonAccountRepository;
import com.syxs.module.carbon.repository.CarbonRecordRepository;
import com.syxs.module.goods.entity.Goods;
import com.syxs.module.goods.repository.GoodsRepository;
import com.syxs.common.enums.OrderStatus;
import com.syxs.module.message.service.UserMessageService;
import com.syxs.module.order.entity.Order;
import com.syxs.module.order.repository.OrderRepository;
import com.syxs.module.order.service.OrderService;
import com.syxs.module.user.entity.User;
import com.syxs.module.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderServiceImpl implements OrderService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String DEFAULT_SHIPPING_COMPANY = "顺丰速运";
    private static final String DEFAULT_SERVICE_PHONE = "400-820-2026";
    private static final String DEFAULT_SUPPORT_ADDRESS = "上海市徐汇区绿色循环交付中心";

    private final OrderRepository orderRepository;
    private final GoodsRepository goodsRepository;
    private final UserRepository userRepository;
    private final CarbonAccountRepository carbonAccountRepository;
    private final CarbonRecordRepository carbonRecordRepository;
    private final CarbonConfig carbonConfig;
    private final UserMessageService userMessageService;

    public OrderServiceImpl(OrderRepository orderRepository,
                            GoodsRepository goodsRepository,
                            UserRepository userRepository,
                            CarbonAccountRepository carbonAccountRepository,
                            CarbonRecordRepository carbonRecordRepository,
                            CarbonConfig carbonConfig,
                            UserMessageService userMessageService) {
        this.orderRepository = orderRepository;
        this.goodsRepository = goodsRepository;
        this.userRepository = userRepository;
        this.carbonAccountRepository = carbonAccountRepository;
        this.carbonRecordRepository = carbonRecordRepository;
        this.carbonConfig = carbonConfig;
        this.userMessageService = userMessageService;
    }

    @Override
    public List<Order> listOrders(String operatorPhone) {
        User operator = findUser(operatorPhone);
        if (isAdmin(operator)) {
            return orderRepository.findAllByOrderByCreatedAtDesc();
        }
        Set<Long> orderIds = new LinkedHashSet<>();
        List<Order> orders = new ArrayList<>();
        for (Order order : orderRepository.findAllByBuyerPhoneOrderByCreatedAtDesc(operator.getPhone())) {
            if (orderIds.add(order.getId())) {
                orders.add(order);
            }
        }
        for (Order order : orderRepository.findAllBySellerPhoneOrderByCreatedAtDesc(operator.getPhone())) {
            if (orderIds.add(order.getId())) {
                orders.add(order);
            }
        }
        return orders;
    }

    @Override
    public List<Order> listBuyOrders(String operatorPhone) {
        return orderRepository.findAllByBuyerPhoneOrderByCreatedAtDesc(findUser(operatorPhone).getPhone());
    }

    @Override
    public List<Order> listSellOrders(String operatorPhone) {
        return orderRepository.findAllBySellerPhoneOrderByCreatedAtDesc(findUser(operatorPhone).getPhone());
    }

    @Override
    public Order getOrder(Long id, String operatorPhone) {
        User operator = findUser(operatorPhone);
        Order order = findOrder(id);
        assertAccessible(order, operator);
        return order;
    }

    @Override
    @Transactional
    public Order createOrder(Long goodsId, String operatorPhone) {
        User operator = findUser(operatorPhone);
        Goods goods = findGoods(goodsId);
        if (goods.getStatus() != GoodsStatus.ON_SALE) {
            throw new BusinessException("Goods is not available for ordering");
        }
        if (operator.getPhone().equals(goods.getSellerPhone())) {
            throw new BusinessException("You cannot buy your own goods");
        }
        Order order = new Order();
        order.setGoodsId(goodsId);
        order.setBuyerName(operator.getNickname());
        order.setBuyerPhone(operator.getPhone());
        order.setSellerName(goods.getSellerName());
        order.setSellerPhone(goods.getSellerPhone());
        order.setShippingCompany("平台担保交易");
        order.setServicePhone(DEFAULT_SERVICE_PHONE);
        order.setDeliveryAddress(resolveDeliveryAddress(operator));
        order.setAmount(goods.getSalePrice());
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setCreatedAt(LocalDateTime.now());
        goods.setStatus(GoodsStatus.RESERVED);
        goodsRepository.save(goods);
        Order saved = orderRepository.save(order);
        userMessageService.createForUserPhone(
            operator.getPhone(),
            "ORDER",
            "订单已创建",
            "订单 #" + saved.getId() + " 已创建，请尽快完成支付。"
        );
        userMessageService.createForUserPhone(
            goods.getSellerPhone(),
            "ORDER",
            "收到新的订单",
            "商品《" + goods.getTitle() + "》收到新的下单请求，等待买家支付。"
        );
        return saved;
    }

    @Override
    @Transactional
    public Order payOrder(Long id, String operatorPhone) {
        User operator = findUser(operatorPhone);
        Order order = findOrder(id);
        assertBuyer(order, operator);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException("Only pending payment orders can be paid");
        }
        order.setStatus(OrderStatus.PENDING_SHIPMENT);
        order.setPaidAt(LocalDateTime.now());
        syncGoodsStatus(order.getGoodsId(), GoodsStatus.RESERVED);
        Order saved = orderRepository.save(order);
        userMessageService.createForUserPhone(
            order.getSellerPhone(),
            "ORDER",
            "订单待发货",
            "订单 #" + saved.getId() + " 已完成支付，请尽快安排发货。"
        );
        userMessageService.createForUserPhone(
            order.getBuyerPhone(),
            "ORDER",
            "支付成功",
            "订单 #" + saved.getId() + " 支付成功，等待卖家发货。"
        );
        return saved;
    }

    @Override
    @Transactional
    public Order shipOrder(Long id, String operatorPhone) {
        User operator = findUser(operatorPhone);
        Order order = findOrder(id);
        assertSeller(order, operator);
        if (order.getStatus() != OrderStatus.PENDING_SHIPMENT) {
            throw new BusinessException("Only pending shipment orders can be shipped");
        }
        order.setStatus(OrderStatus.IN_TRANSIT);
        order.setShippingCompany(DEFAULT_SHIPPING_COMPANY);
        order.setTrackingNo(buildTrackingNo(order));
        if (order.getServicePhone() == null || order.getServicePhone().isBlank()) {
            order.setServicePhone(DEFAULT_SERVICE_PHONE);
        }
        order.setShippedAt(LocalDateTime.now());
        syncGoodsStatus(order.getGoodsId(), GoodsStatus.RESERVED);
        Order saved = orderRepository.save(order);
        userMessageService.createForUserPhone(
            order.getBuyerPhone(),
            "ORDER",
            "卖家已发货",
            "订单 #" + saved.getId() + " 已发货，请留意物流并确认收货。"
        );
        return saved;
    }

    @Override
    @Transactional
    public Order confirmOrder(Long id, String operatorPhone) {
        User operator = findUser(operatorPhone);
        Order order = findOrder(id);
        assertBuyer(order, operator);
        if (order.getStatus() != OrderStatus.IN_TRANSIT) {
            throw new BusinessException("Only in-transit orders can be confirmed");
        }
        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());
        syncGoodsStatus(order.getGoodsId(), GoodsStatus.SOLD);
        awardCarbon(operator, findGoods(order.getGoodsId()), order);
        Order saved = orderRepository.save(order);
        userMessageService.createForUserPhone(
            order.getBuyerPhone(),
            "ORDER",
            "订单已完成",
            "订单 #" + saved.getId() + " 已确认收货，碳积分已入账。"
        );
        userMessageService.createForUserPhone(
            order.getSellerPhone(),
            "ORDER",
            "订单已完成",
            "订单 #" + saved.getId() + " 买家已确认收货，本次交易完成。"
        );
        return saved;
    }

    @Override
    @Transactional
    public Order refundOrder(Long id, String operatorPhone) {
        User operator = findUser(operatorPhone);
        Order order = findOrder(id);
        if (!isAdmin(operator) && !operator.getPhone().equals(order.getBuyerPhone())) {
            throw new BusinessException("Only the buyer can request a refund");
        }
        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new BusinessException("Completed orders do not support refund");
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT
            && order.getStatus() != OrderStatus.PENDING_SHIPMENT
            && order.getStatus() != OrderStatus.IN_TRANSIT) {
            throw new BusinessException("This order cannot be refunded in the current status");
        }
        order.setStatus(OrderStatus.REFUNDED);
        order.setRefundedAt(LocalDateTime.now());
        syncGoodsStatus(order.getGoodsId(), GoodsStatus.ON_SALE);
        Order saved = orderRepository.save(order);
        userMessageService.createForUserPhone(
            order.getBuyerPhone(),
            "ORDER",
            "退款已完成",
            "订单 #" + saved.getId() + " 已退款，商品重新回到在售状态。"
        );
        userMessageService.createForUserPhone(
            order.getSellerPhone(),
            "ORDER",
            "订单已退款",
            "订单 #" + saved.getId() + " 已退款处理，商品重新上架。"
        );
        return saved;
    }

    private Order findOrder(Long id) {
        return orderRepository.findById(id)
            .orElseThrow(() -> new BusinessException("Order not found"));
    }

    private Goods findGoods(Long goodsId) {
        return goodsRepository.findById(goodsId)
            .orElseThrow(() -> new BusinessException("Goods not found"));
    }

    private User findUser(String phone) {
        return userRepository.findByPhone(phone)
            .orElseThrow(() -> new BusinessException("User not found"));
    }

    private boolean isAdmin(User operator) {
        return operator.getRole() != null && "ADMIN".equalsIgnoreCase(operator.getRole());
    }

    private boolean canAccess(Order order, User operator) {
        return operator != null
            && (isAdmin(operator)
            || operator.getPhone().equals(order.getBuyerPhone())
            || operator.getPhone().equals(order.getSellerPhone()));
    }

    private void assertAccessible(Order order, User operator) {
        if (!canAccess(order, operator)) {
            throw new BusinessException("You are not allowed to access this order");
        }
    }

    private void assertBuyer(Order order, User operator) {
        if (!operator.getPhone().equals(order.getBuyerPhone())) {
            throw new BusinessException("Only the buyer can perform this action");
        }
    }

    private void assertSeller(Order order, User operator) {
        if (!isAdmin(operator) && !operator.getPhone().equals(order.getSellerPhone())) {
            throw new BusinessException("Only the seller can perform this action");
        }
    }

    private void syncGoodsStatus(Long goodsId, GoodsStatus status) {
        Goods goods = findGoods(goodsId);
        goods.setStatus(status);
        goodsRepository.save(goods);
    }

    private String resolveDeliveryAddress(User buyer) {
        String city = buyer.getCity() == null || buyer.getCity().isBlank() ? "上海市" : buyer.getCity();
        return city + " · " + DEFAULT_SUPPORT_ADDRESS;
    }

    private String buildTrackingNo(Order order) {
        String orderNo = String.format(Locale.ROOT, "%06d", order.getId());
        return "SF" + orderNo + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMdd"));
    }

    private void awardCarbon(User operator, Goods goods, Order order) {
        int points = Math.max(30, calculatePoints(order.getAmount()));
        CarbonAccount account = carbonAccountRepository.findByUserId(operator.getId())
            .orElseGet(() -> {
                CarbonAccount fresh = new CarbonAccount();
                fresh.setUserId(operator.getId());
                fresh.setBalance(0);
                return fresh;
            });
        account.setBalance((account.getBalance() == null ? 0 : account.getBalance()) + points);
        carbonAccountRepository.save(account);

        CarbonRecord record = new CarbonRecord();
        record.setUserId(operator.getId());
        record.setTitle("确认收货获得绿色积分");
        record.setPoints(points);
        record.setType("收入");
        record.setBizDate(LocalDateTime.now().format(DATE_FORMATTER));
        record.setDescription(
            "订单 " + order.getId() + " 完成交易，累计减碳 " + calculateCarbonSaved(goods, order).stripTrailingZeros().toPlainString() + "kg。"
        );
        carbonRecordRepository.save(record);
    }

    private int calculatePoints(BigDecimal amount) {
        BigDecimal base = amount == null ? BigDecimal.ZERO : amount;
        return base.multiply(BigDecimal.valueOf(carbonConfig.getPointsPerYuan()))
            .setScale(0, RoundingMode.HALF_UP)
            .intValue();
    }

    private BigDecimal calculateCarbonSaved(Goods goods, Order order) {
        if (goods.getCarbonSavedKg() != null && goods.getCarbonSavedKg() > 0) {
            return BigDecimal.valueOf(goods.getCarbonSavedKg());
        }
        BigDecimal amount = order.getAmount() == null ? BigDecimal.ZERO : order.getAmount();
        return amount.multiply(BigDecimal.valueOf(carbonConfig.getCo2PerYuan()))
            .setScale(2, RoundingMode.HALF_UP);
    }
}
