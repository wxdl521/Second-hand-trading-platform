package com.syxs.module.admin.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminUserDTO {

    private Long id;
    private String name;
    private String phone;
    private String password;
    private String avatar;
    private String city;
    private String bio;
    private String role;
    private String kycLevel;
    private Integer carbonPoints;
    private String accountStatus;
    private String kycReviewStatus;
    private String registerAt;
    private String lastActiveAt;
}
