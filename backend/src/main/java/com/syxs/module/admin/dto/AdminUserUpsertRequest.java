package com.syxs.module.admin.dto;

import lombok.Data;

@Data
public class AdminUserUpsertRequest {

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
