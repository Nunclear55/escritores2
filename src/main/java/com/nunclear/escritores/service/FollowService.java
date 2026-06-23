package com.nunclear.escritores.service;

import com.nunclear.escritores.util.AuthUtils;

import com.nunclear.escritores.dto.request.CreateFollowRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.entity.AppUser;
import com.nunclear.escritores.entity.UserFollow;
import com.nunclear.escritores.exception.BadRequestException;
import com.nunclear.escritores.exception.ResourceNotFoundException;
import com.nunclear.escritores.repository.AppUserRepository;
import com.nunclear.escritores.repository.UserFollowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import com.nunclear.escritores.util.PaginationUtils;

@Service
@RequiredArgsConstructor
public class FollowService {

    private static final String USER_NOT_FOUND = "Usuario no encontrado";

    private final UserFollowRepository userFollowRepository;
    private final AppUserRepository appUserRepository;

    public FollowResponse createFollow(CreateFollowRequest request) {
        AppUser currentUser = AuthUtils.getAuthenticatedUser(appUserRepository);

        AppUser followedUser = appUserRepository.findById(request.followedUserId())
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));

        if (followedUser.getDeletedAt() != null) {
            throw new ResourceNotFoundException(USER_NOT_FOUND);
        }

        if (currentUser.getId().equals(followedUser.getId())) {
            throw new BadRequestException("No puedes seguirte a ti mismo");
        }

        if (userFollowRepository.existsByFollowerUserIdAndFollowedUserId(currentUser.getId(), followedUser.getId())) {
            throw new BadRequestException("Ya sigues a este autor");
        }

        UserFollow follow = new UserFollow();
        follow.setFollowerUserId(currentUser.getId());
        follow.setFollowedUserId(followedUser.getId());

        UserFollow saved = userFollowRepository.save(follow);

        return new FollowResponse(
                saved.getId(),
                saved.getFollowerUserId(),
                saved.getFollowedUserId()
        );
    }

    public MessageResponse unfollow(Integer followedUserId) {
        AppUser currentUser = AuthUtils.getAuthenticatedUser(appUserRepository);

        AppUser followedUser = appUserRepository.findById(followedUserId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));

        if (!userFollowRepository.existsByFollowerUserIdAndFollowedUserId(currentUser.getId(), followedUser.getId())) {
            throw new ResourceNotFoundException("No sigues a este autor");
        }

        userFollowRepository.deleteByFollowerUserIdAndFollowedUserId(currentUser.getId(), followedUser.getId());

        return new MessageResponse("Dejaste de seguir al autor");
    }

    public PageResponse<FollowUserItemResponse> getMyFollowing(int page, int size, String sort) {
        AppUser currentUser = AuthUtils.getAuthenticatedUser(appUserRepository);

        Pageable pageable = PaginationUtils.buildPageable(page, size, sort == null || sort.isBlank() ? "createdAt,desc" : sort, "createdAt", "followerUserId", "followedUserId");
        Page<UserFollow> result = userFollowRepository.findByFollowerUserId(currentUser.getId(), pageable);

        return new PageResponse<>(
                result.getContent().stream()
                        .map(follow -> {
                            AppUser user = appUserRepository.findById(follow.getFollowedUserId()).orElse(null);
                            return new FollowUserItemResponse(
                                    follow.getFollowedUserId(),
                                    user != null ? user.getDisplayName() : null
                            );
                        })
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public PageResponse<FollowUserItemResponse> getFollowers(Integer userId, int page, int size, String sort) {
        AppUser targetUser = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));

        if (targetUser.getDeletedAt() != null) {
            throw new ResourceNotFoundException(USER_NOT_FOUND);
        }

        Pageable pageable = PaginationUtils.buildPageable(page, size, sort == null || sort.isBlank() ? "createdAt,desc" : sort, "createdAt", "followerUserId", "followedUserId");
        Page<UserFollow> result = userFollowRepository.findByFollowedUserId(userId, pageable);

        return new PageResponse<>(
                result.getContent().stream()
                        .map(follow -> {
                            AppUser user = appUserRepository.findById(follow.getFollowerUserId()).orElse(null);
                            return new FollowUserItemResponse(
                                    follow.getFollowerUserId(),
                                    user != null ? user.getDisplayName() : null
                            );
                        })
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public FollowCheckResponse isFollowing(Integer userId) {
        AppUser currentUser = AuthUtils.getAuthenticatedUser(appUserRepository);

        boolean following = userFollowRepository.existsByFollowerUserIdAndFollowedUserId(currentUser.getId(), userId);

        return new FollowCheckResponse(following);
    }

    public FollowCountResponse countFollowers(Integer userId) {
        AppUser targetUser = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));

        if (targetUser.getDeletedAt() != null) {
            throw new ResourceNotFoundException(USER_NOT_FOUND);
        }

        long count = userFollowRepository.countByFollowedUserId(userId);
        return new FollowCountResponse(count);
    }

}