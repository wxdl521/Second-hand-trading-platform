package com.syxs.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ApiVersionPathRewriteFilter extends OncePerRequestFilter {

    private static final String API_V1_PREFIX = "/api/v1";
    private static final String REWRITE_FLAG = ApiVersionPathRewriteFilter.class.getName() + ".REWRITTEN";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (uri == null
            || !uri.startsWith(API_V1_PREFIX)
            || Boolean.TRUE.equals(request.getAttribute(REWRITE_FLAG))) {
            filterChain.doFilter(request, response);
            return;
        }

        String rewrittenPath = "/api" + uri.substring(API_V1_PREFIX.length());
        if ("/api".equals(rewrittenPath)) {
            rewrittenPath = "/api/";
        }
        request.setAttribute(REWRITE_FLAG, Boolean.TRUE);
        RequestDispatcher dispatcher = request.getRequestDispatcher(rewrittenPath);
        dispatcher.forward(request, response);
    }
}
