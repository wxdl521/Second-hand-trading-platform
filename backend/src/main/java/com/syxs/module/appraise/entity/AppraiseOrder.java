package com.syxs.module.appraise.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Entity
@Table(name = "appraise_order")
public class AppraiseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String goodsTitle;

    private String mode;

    private String bookingTime;

    private String note;

    private String status;

    private String userPhone;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
