package com.syxs.module.order.dto;

import com.syxs.common.enums.OrderStatus;
import com.syxs.module.order.entity.Order;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderVO {

    private Long id;
    private Long goodsId;
    private String buyerName;
    private String buyerPhone;
    private String sellerName;
    private String sellerPhone;
    private String shippingCompany;
    private String trackingNo;
    private String servicePhone;
    private String deliveryAddress;
    private BigDecimal amount;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime shippedAt;
    private LocalDateTime completedAt;
    private LocalDateTime refundedAt;

    public static OrderVO from(Order order) {
        return OrderVO.builder()
            .id(order.getId())
            .goodsId(order.getGoodsId())
            .buyerName(order.getBuyerName())
            .buyerPhone(order.getBuyerPhone())
            .sellerName(order.getSellerName())
            .sellerPhone(order.getSellerPhone())
            .shippingCompany(order.getShippingCompany())
            .trackingNo(order.getTrackingNo())
            .servicePhone(order.getServicePhone())
            .deliveryAddress(order.getDeliveryAddress())
            .amount(order.getAmount())
            .status(order.getStatus())
            .createdAt(order.getCreatedAt())
            .paidAt(order.getPaidAt())
            .shippedAt(order.getShippedAt())
            .completedAt(order.getCompletedAt())
            .refundedAt(order.getRefundedAt())
            .build();
    }
}
