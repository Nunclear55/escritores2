package com.nunclear.escritores.service;

import com.nunclear.escritores.util.AuthUtils;

import com.nunclear.escritores.dto.request.CreateFavoriteRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.entity.AppUser;
import com.nunclear.escritores.entity.Story;
import com.nunclear.escritores.entity.StoryFavorite;
import com.nunclear.escritores.exception.BadRequestException;
import com.nunclear.escritores.exception.ResourceNotFoundException;
import com.nunclear.escritores.repository.AppUserRepository;
import com.nunclear.escritores.repository.StoryRepository;
import com.nunclear.escritores.repository.FavoriteStoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import com.nunclear.escritores.util.StoryAccessUtils;
import com.nunclear.escritores.util.PaginationUtils;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    // Mala práctica corregida:
    // string mágico repetido.
    // Tipo: duplicación de literales / baja mantenibilidad.

    private final FavoriteStoryRepository userFavoriteStoryRepository;
    private final StoryRepository storyRepository;
    private final AppUserRepository appUserRepository;

    public FavoriteResponse createFavorite(CreateFavoriteRequest request) {
        AppUser currentUser = AuthUtils.getAuthenticatedUser(appUserRepository);

        Story story = storyRepository.findById(request.storyId())
                .orElseThrow(() -> new ResourceNotFoundException(StoryAccessUtils.STORY_NOT_FOUND));

        StoryAccessUtils.validateReadAccess(story, appUserRepository);

        if (userFavoriteStoryRepository.existsByUserIdAndStoryId(currentUser.getId(), story.getId())) {
            throw new BadRequestException("La historia ya está en favoritos");
        }

        StoryFavorite favorite = new StoryFavorite();
        favorite.setUserId(currentUser.getId());
        favorite.setStoryId(story.getId());

        StoryFavorite saved = userFavoriteStoryRepository.save(favorite);

        return new FavoriteResponse(
                saved.getId(),
                saved.getStoryId(),
                saved.getUserId()
        );
    }

    public MessageResponse removeFavorite(Integer storyId) {
        AppUser currentUser = AuthUtils.getAuthenticatedUser(appUserRepository);

        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException(StoryAccessUtils.STORY_NOT_FOUND));

        if (!userFavoriteStoryRepository.existsByUserIdAndStoryId(currentUser.getId(), story.getId())) {
            throw new ResourceNotFoundException("La historia no está en favoritos");
        }

        userFavoriteStoryRepository.deleteByUserIdAndStoryId(currentUser.getId(), story.getId());
        return new MessageResponse("Historia eliminada de favoritos");
    }

    public PageResponse<FavoriteListItemResponse> getMyFavorites(int page, int size, String sort) {
        AppUser currentUser = AuthUtils.getAuthenticatedUser(appUserRepository);

        Pageable pageable = PaginationUtils.buildPageable(page, size, sort == null || sort.isBlank() ? "createdAt,desc" : sort, "createdAt", "storyId");
        Page<StoryFavorite> result = userFavoriteStoryRepository.findByUserId(currentUser.getId(), pageable);

        return new PageResponse<>(
                result.getContent().stream()
                        .map(favorite -> {
                            Story story = storyRepository.findById(favorite.getStoryId()).orElse(null);
                            return new FavoriteListItemResponse(
                                    favorite.getStoryId(),
                                    story != null ? story.getTitle() : null
                            );
                        })
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public FavoriteCheckResponse isFavorite(Integer storyId) {
        AppUser currentUser = AuthUtils.getAuthenticatedUser(appUserRepository);

        boolean favorite = userFavoriteStoryRepository.existsByUserIdAndStoryId(currentUser.getId(), storyId);
        return new FavoriteCheckResponse(favorite);
    }

    public FavoriteCountResponse countFavorites(Integer storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException(StoryAccessUtils.STORY_NOT_FOUND));

        StoryAccessUtils.validateReadAccess(story, appUserRepository);

        long count = userFavoriteStoryRepository.countByStoryId(storyId);
        return new FavoriteCountResponse(count);
    }

}