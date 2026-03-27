package com.syxs.module.admin.controller;

import com.syxs.common.result.R;
import com.syxs.module.admin.dto.AdminLoginRequest;
import com.syxs.module.admin.service.AdminService;
import com.syxs.module.user.dto.UserProfileDTO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AdminService adminService;

    public AdminAuthController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/login")
    public R<UserProfileDTO> login(@Valid @RequestBody AdminLoginRequest request) {
        return R.ok(adminService.login(request));
    }
}
