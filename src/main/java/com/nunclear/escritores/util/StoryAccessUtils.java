package com.nunclear.escritores.util;

import com.nunclear.escritores.entity.AppUser;
import com.nunclear.escritores.entity.Story;
import com.nunclear.escritores.exception.ResourceNotFoundException;
import com.nunclear.escritores.exception.UnauthorizedException;
import com.nunclear.escritores.repository.AppUserRepository;
import com.nunclear.escritores.repository.StoryRepository;

/**
 * Utility methods for validating whether the current user can read or edit a
 * particular {@link Story}.  These helpers consolidate logic for common
 * access checks that previously existed in many service classes.  Centralizing
 * this functionality reduces code duplication and ensures consistent behavior
 * across the application.
 */
public final class StoryAccessUtils {

    /** Message used when a story cannot be found or read. */
    public static final String STORY_NOT_FOUND = "Historia no encontrada";

    private StoryAccessUtils() {
        // utility class; prevent instantiation
    }

    /**
     * Returns the requested story if the currently authenticated user is allowed to
     * edit it.  A user may edit a story if they are the owner or have a moderator/admin
     * access level.  If the story does not exist, a {@link ResourceNotFoundException}
     * is thrown.  If the user lacks edit permissions, an {@link UnauthorizedException}
     * is thrown.
     *
     * @param storyId the identifier of the story to fetch
     * @param storyRepository repository used to retrieve stories
     * @param appUserRepository repository used to lookup the current user
     * @return the story that the current user can edit
     */
    public static Story getEditableStory(Integer storyId,
                                         StoryRepository storyRepository,
                                         AppUserRepository appUserRepository) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException(STORY_NOT_FOUND));

        AppUser currentUser = AuthUtils.getAuthenticatedUser(appUserRepository);
        boolean isOwner = story.getOwnerUserId().equals(currentUser.getId());
        boolean isModeratorOrAdmin = AuthUtils.isModeratorOrAdmin(currentUser);

        if (!isOwner && !isModeratorOrAdmin) {
            throw new UnauthorizedException("No tienes permisos sobre esta historia");
        }

        return story;
    }

    /**
     * Validates that the current user has read access to the given story.  A story
     * is publicly readable if its visibility state is "public", its publication
     * state is "published", and it has not been archived.  Otherwise, the current
     * user must be the owner or have a moderator/admin access level.  If access is
     * denied, this method throws a {@link ResourceNotFoundException}.
     *
     * @param story the story to validate
     * @param appUserRepository repository used to lookup the current user
     */
    public static void validateReadAccess(Story story, AppUserRepository appUserRepository) {
        if (!canReadStory(story, appUserRepository)) {
            throw new ResourceNotFoundException(STORY_NOT_FOUND);
        }
    }

    /**
     * Returns whether a story is publicly readable without requiring an authenticated user.
     */
    public static boolean isPublicReadable(Story story) {
        return "public".equalsIgnoreCase(story.getVisibilityState())
                && "published".equalsIgnoreCase(story.getPublicationState())
                && story.getArchivedAt() == null;
    }

    /**
     * Determines whether the given story is readable by the current user.  Publicly
     * readable stories always return {@code true}.  Private stories return
     * {@code true} only if the current user is the owner or has a moderator/admin
     * access level.  If no user is authenticated, this method returns {@code false}.
     *
     * @param story the story to check
     * @param appUserRepository repository used to lookup the current user
     * @return {@code true} if the story can be read by the current user, {@code false} otherwise
     */
    public static boolean canReadStory(Story story, AppUserRepository appUserRepository) {
        if (isPublicReadable(story)) {
            return true;
        }

        AppUser currentUser = AuthUtils.tryGetAuthenticatedUser(appUserRepository);
        if (currentUser == null) {
            return false;
        }

        return story.getOwnerUserId().equals(currentUser.getId()) || AuthUtils.isModeratorOrAdmin(currentUser);
    }
}