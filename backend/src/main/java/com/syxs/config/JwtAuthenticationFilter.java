package com.syxs.config;

import com.syxs.common.utils.JwtUtil;
import com.syxs.module.user.entity.User;
import com.syxs.module.user.repository.UserRepository;
import com.syxs.module.user.service.AuthSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final AuthSessionService authSessionService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserRepository userRepository, AuthSessionService authSessionService) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.authSessionService = authSessionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authorization.substring(7);
            if (authSessionService.isBlacklisted(token)) {
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }
            String phone = jwtUtil.parseSubject(token);
            if (phone != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                userRepository.findByPhone(phone).ifPresent(user -> authenticate(request, user));
            }
        } catch (Exception ignored) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, User user) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            user.getPhone(),
            null,
            List.of(new SimpleGrantedAuthority("ROLE_" + (user.getRole() == null ? "USER" : user.getRole())))
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
