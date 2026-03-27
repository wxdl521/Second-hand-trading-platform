package com.syxs.module.user.service;

import com.syxs.common.exception.BusinessException;
import com.syxs.common.utils.JwtUtil;
import com.syxs.module.user.dto.TokenPairDTO;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class AuthSessionService {

    private final JwtUtil jwtUtil;
    private final ConcurrentHashMap<String, String> refreshTokenStore = new ConcurrentHashMap<>();
    private final Set<String> blacklistedAccessTokens = ConcurrentHashMap.newKeySet();

    public AuthSessionService(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    public TokenPairDTO issueTokens(String phone) {
        String accessToken = jwtUtil.generateToken(phone);
        String refreshToken = jwtUtil.generateRefreshToken(phone);
        refreshTokenStore.put(refreshToken, phone);
        return new TokenPairDTO(accessToken, refreshToken);
    }

    public TokenPairDTO refresh(String refreshToken) {
        String storedPhone = refreshTokenStore.get(refreshToken);
        if (storedPhone == null) {
            throw new BusinessException("Invalid refresh token");
        }

        String parsedPhone = jwtUtil.parseSubject(refreshToken);
        if (!storedPhone.equals(parsedPhone)) {
            throw new BusinessException("Invalid refresh token");
        }

        refreshTokenStore.remove(refreshToken);
        return issueTokens(parsedPhone);
    }

    public void logout(String accessToken, String refreshToken) {
        if (accessToken != null && !accessToken.isBlank()) {
            blacklistedAccessTokens.add(accessToken);
        }

        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenStore.remove(refreshToken);
            return;
        }

        if (accessToken != null && !accessToken.isBlank()) {
            try {
                String phone = jwtUtil.parseSubject(accessToken);
                refreshTokenStore.entrySet().removeIf(entry -> entry.getValue().equals(phone));
            } catch (Exception ignored) {
                // ignore logout cleanup for invalid or expired access tokens
            }
        }
    }

    public boolean isBlacklisted(String accessToken) {
        return accessToken != null && blacklistedAccessTokens.contains(accessToken);
    }
}
