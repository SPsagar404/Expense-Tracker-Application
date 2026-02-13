package com.expensemanager.security;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility class for common security-related operations.
 */
public class SecurityUtils {

    /**
     * Get the ID of the currently authenticated user from the request attribute.
     * The ID is populated in the JwtAuthenticationFilter.
     *
     * @return Long userId or null if not found
     */
    public static Long getCurrentUserId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            Object userId = request.getAttribute("userId");
            if (userId instanceof Long) {
                return (Long) userId;
            }
        }
        return null;
    }
}
