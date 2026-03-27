package com.syxs.module.user.service;

import com.syxs.module.user.dto.ChangePasswordRequest;
import com.syxs.module.user.dto.KycSubmitRequest;
import com.syxs.module.user.dto.LoginRequest;
import com.syxs.module.user.dto.OtpSendRequest;
import com.syxs.module.user.dto.RefreshTokenRequest;
import com.syxs.module.user.dto.RegisterRequest;
import com.syxs.module.user.dto.TokenPairDTO;
import com.syxs.module.user.dto.UserProfileDTO;
import com.syxs.module.user.dto.UserProfileUpdateRequest;

public interface UserService {

    void sendOtp(OtpSendRequest request);

    UserProfileDTO login(LoginRequest request);

    UserProfileDTO register(RegisterRequest request);

    UserProfileDTO getCurrentProfile(String phone);

    UserProfileDTO updateProfile(String phone, UserProfileUpdateRequest request);

    void changePassword(String phone, ChangePasswordRequest request);

    UserProfileDTO submitKyc(String phone, KycSubmitRequest request);

    TokenPairDTO refreshToken(RefreshTokenRequest request);

    void logout(String accessToken, String refreshToken);
}
