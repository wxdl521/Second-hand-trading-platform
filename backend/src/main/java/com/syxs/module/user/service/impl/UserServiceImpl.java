package com.syxs.module.user.service.impl;

import com.syxs.common.enums.KycLevel;
import com.syxs.common.exception.BusinessException;
import com.syxs.config.OtpConfig;
import com.syxs.module.user.dto.ChangePasswordRequest;
import com.syxs.module.user.dto.KycSubmitRequest;
import com.syxs.module.user.dto.LoginRequest;
import com.syxs.module.user.dto.OtpSendRequest;
import com.syxs.module.user.dto.RefreshTokenRequest;
import com.syxs.module.user.dto.RegisterRequest;
import com.syxs.module.user.dto.TokenPairDTO;
import com.syxs.module.user.dto.UserProfileDTO;
import com.syxs.module.user.dto.UserProfileUpdateRequest;
import com.syxs.module.user.entity.OtpCode;
import com.syxs.module.user.entity.User;
import com.syxs.module.user.repository.OtpCodeRepository;
import com.syxs.module.user.repository.UserRepository;
import com.syxs.module.user.service.AuthSessionService;
import com.syxs.module.user.service.UserService;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final OtpCodeRepository otpCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthSessionService authSessionService;
    private final OtpConfig otpConfig;
    private final SecureRandom random = new SecureRandom();

    public UserServiceImpl(UserRepository userRepository,
                           OtpCodeRepository otpCodeRepository,
                           PasswordEncoder passwordEncoder,
                           AuthSessionService authSessionService,
                           OtpConfig otpConfig) {
        this.userRepository = userRepository;
        this.otpCodeRepository = otpCodeRepository;
        this.passwordEncoder = passwordEncoder;
        this.authSessionService = authSessionService;
        this.otpConfig = otpConfig;
    }

    @Override
    public void sendOtp(OtpSendRequest request) {
        String code = String.format("%06d", random.nextInt(1_000_000));
        OtpCode otp = new OtpCode();
        otp.setPhone(request.getPhone());
        otp.setCode(code);
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(otpConfig.getExpireMinutes()));
        otp.setUsed(Boolean.FALSE);
        otp.setCreatedAt(LocalDateTime.now());
        otpCodeRepository.save(otp);

        log.info("========================================");
        log.info("OTP for {} : {} (expires {} min)", maskPhone(request.getPhone()), code, otpConfig.getExpireMinutes());
        log.info("========================================");
    }

    @Override
    public UserProfileDTO login(LoginRequest request) {
        validateOtp(request.getPhone(), request.getOtpCode());
        User user = findUserByPhone(request.getPhone(), "User does not exist, please register first");
        touchLastActive(user);
        TokenPairDTO tokenPair = authSessionService.issueTokens(user.getPhone());
        return toProfile(user, tokenPair);
    }

    @Override
    public UserProfileDTO register(RegisterRequest request) {
        userRepository.findByPhone(request.getPhone()).ifPresent(existing -> {
            throw new BusinessException("Phone already registered");
        });
        validateOtp(request.getPhone(), request.getOtpCode());

        User user = new User();
        user.setNickname(request.getNickname());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setAvatar("/uploads/default-avatar.svg");
        user.setCity("未设置");
        user.setBio("欢迎来到尚有新生。");
        user.setRole("USER");
        user.setKycLevel(KycLevel.L1);
        user.setCarbonPoints(520);
        user.setCreatedAt(LocalDateTime.now());
        user.setLastActiveAt(user.getCreatedAt());
        User saved = userRepository.save(user);
        TokenPairDTO tokenPair = authSessionService.issueTokens(saved.getPhone());
        return toProfile(saved, tokenPair);
    }

    @Override
    public UserProfileDTO getCurrentProfile(String phone) {
        User user = findUserByPhone(phone, "User not found");
        return toProfile(user, null);
    }

    @Override
    public UserProfileDTO updateProfile(String phone, UserProfileUpdateRequest request) {
        User user = findUserByPhone(phone, "User not found");
        user.setNickname(request.getNickname());
        user.setCity(request.getCity());
        user.setBio(request.getBio());
        if (request.getAvatar() != null && !request.getAvatar().isBlank()) {
            user.setAvatar(request.getAvatar());
        }
        touchLastActive(user);
        User saved = userRepository.save(user);
        return toProfile(saved, null);
    }

    @Override
    public void changePassword(String phone, ChangePasswordRequest request) {
        User user = findUserByPhone(phone, "User not found");
        if (hasText(user.getPassword())) {
            if (!hasText(request.getCurrentPassword())) {
                throw new BusinessException("Current password is required");
            }
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new BusinessException("Current password is incorrect");
            }
            if (passwordEncoder.matches(request.getNextPassword(), user.getPassword())) {
                throw new BusinessException("New password must be different from the current password");
            }
        }
        user.setPassword(passwordEncoder.encode(request.getNextPassword()));
        touchLastActive(user);
        userRepository.save(user);
    }

    @Override
    public UserProfileDTO submitKyc(String phone, KycSubmitRequest request) {
        User user = findUserByPhone(phone, "User not found");
        if (!hasText(request.getRealName()) || !hasText(request.getIdNumber())) {
            throw new BusinessException("Real name and ID number are required");
        }
        user.setKycLevel(KycLevel.L3);
        user.setKycReviewStatus(null);
        touchLastActive(user);
        User saved = userRepository.save(user);
        return toProfile(saved, null);
    }

    @Override
    public TokenPairDTO refreshToken(RefreshTokenRequest request) {
        return authSessionService.refresh(request.getRefreshToken());
    }

    @Override
    public void logout(String accessToken, String refreshToken) {
        authSessionService.logout(accessToken, refreshToken);
    }

    private void validateOtp(String phone, String otpCode) {
        OtpCode latest = otpCodeRepository.findTopByPhoneOrderByCreatedAtDesc(phone)
            .orElseThrow(() -> new BusinessException("OTP code does not exist, please request a new one"));
        if (Boolean.TRUE.equals(latest.getUsed())) {
            throw new BusinessException("OTP code has already been used");
        }
        if (latest.getExpiresAt() == null || latest.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("OTP code has expired");
        }
        if (!latest.getCode().equals(otpCode)) {
            throw new BusinessException("OTP code is invalid");
        }
        latest.setUsed(Boolean.TRUE);
        otpCodeRepository.save(latest);
    }

    private User findUserByPhone(String phone, String errorMessage) {
        return userRepository.findByPhone(phone)
            .orElseThrow(() -> new BusinessException(errorMessage));
    }

    private void touchLastActive(User user) {
        user.setLastActiveAt(LocalDateTime.now());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private UserProfileDTO toProfile(User user, TokenPairDTO tokenPair) {
        return UserProfileDTO.builder()
            .id(user.getId())
            .nickname(user.getNickname())
            .phone(user.getPhone())
            .city(user.getCity())
            .avatar(user.getAvatar())
            .kycLevel(user.getKycLevel())
            .bio(user.getBio())
            .role(user.getRole())
            .carbonPoints(user.getCarbonPoints())
            .token(tokenPair == null ? null : tokenPair.getAccessToken())
            .accessToken(tokenPair == null ? null : tokenPair.getAccessToken())
            .refreshToken(tokenPair == null ? null : tokenPair.getRefreshToken())
            .build();
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
