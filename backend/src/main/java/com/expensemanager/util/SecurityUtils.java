package com.expensemanager.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * Get the current authenticated user's ID from the request attribute
     * set by JwtAuthenticationFilter.
     */
    public static Long getCurrentUserId() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new IllegalStateException("No request context found");
        }
        HttpServletRequest request = attrs.getRequest();
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            throw new IllegalStateException("User not authenticated");
        }
        return userId;
    }
}
