package com.syxs.module.user.controller;

import com.syxs.common.exception.BusinessException;
import com.syxs.common.result.R;
import com.syxs.common.support.CurrentUserResolver;
import com.syxs.module.user.dto.ChangePasswordRequest;
import com.syxs.module.user.dto.KycSubmitRequest;
import com.syxs.module.user.dto.UserProfileDTO;
import com.syxs.module.user.dto.UserProfileUpdateRequest;
import com.syxs.module.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final CurrentUserResolver currentUserResolver;

    public UserController(UserService userService, CurrentUserResolver currentUserResolver) {
        this.userService = userService;
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping({"/profile", "/me"})
    public R<UserProfileDTO> profile(HttpServletRequest request) {
        return R.ok(userService.getCurrentProfile(resolvePhone(request)));
    }

    @PutMapping({"/profile", "/me"})
    public R<UserProfileDTO> updateProfile(HttpServletRequest request,
                                           @Valid @RequestBody UserProfileUpdateRequest body) {
        return R.ok(userService.updateProfile(resolvePhone(request), body));
    }

    @PostMapping("/password/change")
    public R<Void> changePassword(HttpServletRequest request,
                                  @Valid @RequestBody ChangePasswordRequest body) {
        userService.changePassword(resolvePhone(request), body);
        return R.ok(null);
    }

    @PostMapping("/kyc/submit")
    public R<UserProfileDTO> submitKyc(HttpServletRequest request,
                                       @Valid @RequestBody KycSubmitRequest body) {
        return R.ok(userService.submitKyc(resolvePhone(request), body));
    }

    private String resolvePhone(HttpServletRequest request) {
        return currentUserResolver.resolvePhone(request);
    }
}
