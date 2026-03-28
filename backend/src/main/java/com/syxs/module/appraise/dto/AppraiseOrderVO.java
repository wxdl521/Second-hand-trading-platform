package com.syxs.module.appraise.dto;

import com.syxs.module.appraise.entity.AppraiseOrder;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AppraiseOrderVO {

    private Long id;
    private String goodsTitle;
    private String mode;
    private String bookingTime;
    private String note;
    private String status;
    private String userPhone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AppraiseOrderVO from(AppraiseOrder order) {
        return AppraiseOrderVO.builder()
            .id(order.getId())
            .goodsTitle(order.getGoodsTitle())
            .mode(order.getMode())
            .bookingTime(order.getBookingTime())
            .note(order.getNote())
            .status(order.getStatus())
            .userPhone(order.getUserPhone())
            .createdAt(order.getCreatedAt())
            .updatedAt(order.getUpdatedAt())
            .build();
    }
}
