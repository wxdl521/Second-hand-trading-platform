package com.syxs.module.user.entity;

import com.syxs.common.enums.KycLevel;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nickname;

    private String phone;

    private String password;

    private String avatar;

    private String city;

    private String bio;

    private String role;

    @Enumerated(EnumType.STRING)
    private KycLevel kycLevel;

    private Integer carbonPoints;

    private String accountStatus;

    private String kycReviewStatus;

    private LocalDateTime createdAt;

    private LocalDateTime lastActiveAt;
}
