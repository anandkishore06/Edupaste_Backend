package com.edupaste.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {
    public static UserDetailsImpl getCurrentUserDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl) {
            return (UserDetailsImpl) auth.getPrincipal();
        }
        throw new RuntimeException("User is not authenticated");
    }

    public static Long getCurrentUserId() {
        return getCurrentUserDetails().getId();
    }

    public static Long getCurrentSchoolId() {
        return getCurrentUserDetails().getSchoolId();
    }

    public static String getCurrentUserRole() {
        return getCurrentUserDetails().getRole();
    }

    public static boolean hasPermission(String permission) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(permission));
    }
}
