package com.nunclear.escritores.util;

import com.nunclear.escritores.entity.AppUser;
import com.nunclear.escritores.exception.UnauthorizedException;
import com.nunclear.escritores.repository.AppUserRepository;
import com.nunclear.escritores.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility methods for retrieving the currently authenticated user from the
 * {@link SecurityContextHolder}.  This class centralizes common logic that
 * was previously duplicated across multiple service classes.  By using
 * these helpers, services avoid repeatedly checking for null authentication,
 * casting principals and looking up {@link AppUser} instances.  The methods
 * throw {@link UnauthorizedException} when appropriate to signal unauthenticated
 * access attempts.
 */
public final class AuthUtils {
    private AuthUtils() {
        // utility class; prevent instantiation
    }

    /**
     * Returns the currently authenticated {@link AppUser}.  If no user is
     * authenticated, or if the principal is not of type {@link CustomUserDetails},
     * this method throws an {@link UnauthorizedException}.  The provided
     * {@link AppUserRepository} is used to fetch the entity by its ID.
     *
     * @param appUserRepository repository used to lookup users by ID
     * @return the authenticated user
     * @throws UnauthorizedException if there is no authenticated user
     */
    public static AppUser getAuthenticatedUser(AppUserRepository appUserRepository) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new UnauthorizedException("No autenticado");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetails userDetails)) {
            throw new UnauthorizedException("No autenticado");
        }

        return appUserRepository.findById(userDetails.getId())
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));
    }

    /**
     * Attempts to return the currently authenticated {@link AppUser}.  If no user
     * is authenticated, or if the principal is not of type {@link CustomUserDetails},
     * this method returns {@code null} instead of throwing an exception.
     *
     * @param appUserRepository repository used to lookup users by ID
     * @return the authenticated user, or {@code null} if none
     */
    public static AppUser tryGetAuthenticatedUser(AppUserRepository appUserRepository) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetails userDetails)) {
            return null;
        }

        return appUserRepository.findById(userDetails.getId()).orElse(null);
    }

    /**
     * Determines whether the given user has a moderator or administrator access level.
     *
     * @param user the user to check; may be {@code null}
     * @return {@code true} if the user has a moderator or admin access level, {@code false} otherwise
     */
    public static boolean isModeratorOrAdmin(AppUser user) {
        if (user == null) {
            return false;
        }
        return user.getAccessLevel().isModeratorOrAdmin();
    }
}