package com.syxs.common.support;

import com.syxs.common.exception.BusinessException;
import com.syxs.common.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserResolver {

    private final JwtUtil jwtUtil;

    public CurrentUserResolver(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    public String resolvePhone(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String phone = authentication.getName();
            if (phone != null && !phone.isBlank() && !"anonymousUser".equals(phone)) {
                return phone;
            }
        }

        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return jwtUtil.parseSubject(authorization.substring(7));
        }

        throw new BusinessException("Unauthorized");
    }
}
