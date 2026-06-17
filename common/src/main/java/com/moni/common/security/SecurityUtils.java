package com.moni.common.security;

import com.moni.common.error.CommonErrorCode;
import com.moni.common.error.exception.CustomException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

public class SecurityUtils {

    private SecurityUtils() {
    }

    private static HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new CustomException(CommonErrorCode.UNAUTHORIZED);
        }
        return attributes.getRequest();
    }

    public static UUID getCurrentUserId() {
        String userId = getCurrentRequest().getHeader("X-User-Id");
        if (userId == null || userId.isBlank()) {
            throw new CustomException(CommonErrorCode.UNAUTHORIZED);
        }
        return UUID.fromString(userId);
    }

    public static String getCurrentUserEmail() {
        String email = getCurrentRequest().getHeader("X-User-Email");
        if (email == null || email.isBlank()) {
            throw new CustomException(CommonErrorCode.UNAUTHORIZED);
        }
        return email;
    }

    public static String getCurrentUserRole() {
        String role = getCurrentRequest().getHeader("X-User-Role");
        return (role != null) ? role : "";
    }

    public static boolean isAdmin() {
        return "ADMIN".equals(getCurrentUserRole());
    }

    public static boolean hasRole(String role) {
        return role.equals(getCurrentUserRole());
    }
}
