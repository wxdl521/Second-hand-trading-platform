package com.syxs.module.user.controller;

import com.syxs.common.result.R;
import com.syxs.module.user.dto.LoginRequest;
import com.syxs.module.user.dto.LogoutRequest;
import com.syxs.module.user.dto.OtpSendRequest;
import com.syxs.module.user.dto.RefreshTokenRequest;
import com.syxs.module.user.dto.RegisterRequest;
import com.syxs.module.user.dto.TokenPairDTO;
import com.syxs.module.user.dto.UserProfileDTO;
import com.syxs.module.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/otp/send")
    public R<Void> sendOtp(@Valid @RequestBody OtpSendRequest request) {
        userService.sendOtp(request);
        return R.ok(null);
    }

    @PostMapping("/login")
    public R<UserProfileDTO> login(@Valid @RequestBody LoginRequest request) {
        return R.ok(userService.login(request));
    }

    @PostMapping("/register")
    public R<UserProfileDTO> register(@Valid @RequestBody RegisterRequest request) {
        return R.ok(userService.register(request));
    }

    @PostMapping("/refresh")
    public R<TokenPairDTO> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return R.ok(userService.refreshToken(request));
    }

    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request, @RequestBody(required = false) LogoutRequest body) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        String accessToken = authorization != null && authorization.startsWith("Bearer ")
            ? authorization.substring(7)
            : null;
        String refreshToken = body == null ? null : body.getRefreshToken();
        userService.logout(accessToken, refreshToken);
        return R.ok(null);
    }
}
