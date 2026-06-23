package com.nunclear.escritores.service;

import com.nunclear.escritores.dto.request.ReplaceMediaRequest;
import com.nunclear.escritores.dto.request.UploadMediaRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.entity.AppUser;
import com.nunclear.escritores.entity.Chapter;
import com.nunclear.escritores.entity.Media;
import com.nunclear.escritores.entity.Story;
import com.nunclear.escritores.enums.AccessLevel;
import com.nunclear.escritores.exception.ResourceNotFoundException;
import com.nunclear.escritores.exception.UnauthorizedException;
import com.nunclear.escritores.repository.AppUserRepository;
import com.nunclear.escritores.repository.ChapterRepository;
import com.nunclear.escritores.repository.MediaRepository;
import com.nunclear.escritores.repository.StoryRepository;
import com.nunclear.escritores.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock
    private MediaRepository mediaRepository;
    @Mock
    private ChapterRepository chapterRepository;
    @Mock
    private StoryRepository storyRepository;
    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private MediaService mediaService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void uploadMedia_deberiaCrearArchivo() {
        Chapter chapter = mock(Chapter.class);
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        Media saved = mock(Media.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(chapterRepository.findById(3)).thenReturn(Optional.of(chapter));
        when(chapter.getId()).thenReturn(3);
        when(chapter.getStoryId()).thenReturn(10);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        when(mediaRepository.save(any(Media.class))).thenReturn(saved);
        when(saved.getId()).thenReturn(100);
        when(saved.getFilename()).thenReturn("imagen_1234abcd.png");
        when(saved.getOriginalFilename()).thenReturn("imagen.png");
        when(saved.getMediaKind()).thenReturn("image");
        when(saved.getChapterId()).thenReturn(3);

        UploadMediaResponse response = mediaService.uploadMedia(
                new UploadMediaRequest("imagen.png", "IMAGE", "desc", 3, "/files/imagen.png")
        );

        assertEquals(100, response.id());
        assertEquals("imagen_1234abcd.png", response.filename());
        assertEquals("imagen.png", response.originalFilename());
        assertEquals("image", response.mediaKind());
        assertEquals(3, response.chapterId());

        ArgumentCaptor<Media> captor = ArgumentCaptor.forClass(Media.class);
        verify(mediaRepository).save(captor.capture());

        Media media = captor.getValue();
        assertEquals("imagen.png", media.getOriginalFilename());
        assertEquals("image", media.getMediaKind());
        assertEquals("desc", media.getDescription());
        assertEquals(3, media.getChapterId());
        assertEquals("/files/imagen.png", media.getStoragePath());
        assertNotNull(media.getFilename());
        assertTrue(media.getFilename().endsWith(".png"));
        assertTrue(media.getFilename().startsWith("imagen_"));
    }

    @Test
    void getMediaById_deberiaRetornarArchivo_siCapituloEsPublico() {
        Media media = mock(Media.class);
        Chapter chapter = mock(Chapter.class);
        Story story = mock(Story.class);

        when(mediaRepository.findById(5)).thenReturn(Optional.of(media));
        when(media.getId()).thenReturn(5);
        when(media.getFilename()).thenReturn("imagen_xxxx.png");
        when(media.getOriginalFilename()).thenReturn("imagen.png");
        when(media.getMediaKind()).thenReturn("image");
        when(media.getDescription()).thenReturn("desc");
        when(media.getChapterId()).thenReturn(3);
        when(media.getStoragePath()).thenReturn("/files/imagen.png");

        when(chapterRepository.findById(3)).thenReturn(Optional.of(chapter));
        //when(chapter.getChapterId()).thenReturn(null); // not used
        when(chapter.getStoryId()).thenReturn(10);
        when(chapter.getArchivedAt()).thenReturn(null);
        when(chapter.getPublicationState()).thenReturn("published");

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getVisibilityState()).thenReturn("public");
        when(story.getPublicationState()).thenReturn("published");
        when(story.getArchivedAt()).thenReturn(null);

        MediaDetailResponse response = mediaService.getMediaById(5);

        assertEquals(5, response.id());
        assertEquals("imagen_xxxx.png", response.filename());
        assertEquals("imagen.png", response.originalFilename());
        assertEquals("image", response.mediaKind());
        assertEquals("/files/imagen.png", response.storagePath());
    }

    @Test
    void getMediaById_deberiaLanzarNotFound_siNoEsVisible() {
        Media media = mock(Media.class);
        Chapter chapter = mock(Chapter.class);
        Story story = mock(Story.class);

        when(mediaRepository.findById(5)).thenReturn(Optional.of(media));
        when(media.getChapterId()).thenReturn(3);

        when(chapterRepository.findById(3)).thenReturn(Optional.of(chapter));
        when(chapter.getStoryId()).thenReturn(10);
        when(chapter.getArchivedAt()).thenReturn(null);
        when(chapter.getPublicationState()).thenReturn("draft");

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getVisibilityState()).thenReturn("private");
        when(story.getPublicationState()).thenReturn("draft");
        when(story.getArchivedAt()).thenReturn(null);

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> mediaService.getMediaById(5)
        );

        assertEquals("Archivo no encontrado", ex.getMessage());
    }

    @Test
    void getMediaByChapter_deberiaRetornarPagina() {
        Chapter chapter = mock(Chapter.class);
        Story story = mock(Story.class);
        Media media = mock(Media.class);

        when(chapterRepository.findById(3)).thenReturn(Optional.of(chapter));
        when(chapter.getStoryId()).thenReturn(10);
        when(chapter.getArchivedAt()).thenReturn(null);
        when(chapter.getPublicationState()).thenReturn("published");

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getVisibilityState()).thenReturn("public");
        when(story.getPublicationState()).thenReturn("published");
        when(story.getArchivedAt()).thenReturn(null);

        Page<Media> page = new PageImpl<>(List.of(media), PageRequest.of(0, 20), 1);
        when(mediaRepository.findByChapterId(eq(3), any(Pageable.class))).thenReturn(page);

        when(media.getId()).thenReturn(1);
        when(media.getFilename()).thenReturn("img.png");
        when(media.getMediaKind()).thenReturn("image");

        PageResponse<MediaListItemResponse> response = mediaService.getMediaByChapter(3, 0, 20, null);

        assertEquals(1, response.content().size());
        assertEquals("img.png", response.content().get(0).filename());
        assertEquals("image", response.content().get(0).mediaKind());
    }

    @Test
    void replaceMedia_deberiaActualizarArchivo() {
        Media media = mock(Media.class);
        Chapter chapter = mock(Chapter.class);
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        Media saved = mock(Media.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(mediaRepository.findById(5)).thenReturn(Optional.of(media));
        when(media.getChapterId()).thenReturn(3);

        when(chapterRepository.findById(3)).thenReturn(Optional.of(chapter));
        when(chapter.getStoryId()).thenReturn(10);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        when(mediaRepository.save(media)).thenReturn(saved);
        when(saved.getId()).thenReturn(5);
        when(saved.getFilename()).thenReturn("nuevo_1234abcd.jpg");
        when(saved.getDescription()).thenReturn("nueva desc");

        ReplaceMediaResponse response = mediaService.replaceMedia(
                5, new ReplaceMediaRequest("nuevo.jpg", "nueva desc", "/files/nuevo.jpg")
        );

        assertEquals(5, response.id());
        assertEquals("nuevo_1234abcd.jpg", response.filename());
        assertEquals("nueva desc", response.description());

        verify(media).setOriginalFilename("nuevo.jpg");
        verify(media).setDescription("nueva desc");
        verify(media).setStoragePath("/files/nuevo.jpg");
        verify(media).setFilename(argThat(name ->
                name.startsWith("nuevo_") && name.endsWith(".jpg")));
    }

    @Test
    void downloadMedia_deberiaRetornarRuta_siEsVisible() {
        Media media = mock(Media.class);
        Chapter chapter = mock(Chapter.class);
        Story story = mock(Story.class);

        when(mediaRepository.findById(5)).thenReturn(Optional.of(media));
        when(media.getChapterId()).thenReturn(3);
        when(media.getStoragePath()).thenReturn("/files/doc.pdf");

        when(chapterRepository.findById(3)).thenReturn(Optional.of(chapter));
        when(chapter.getStoryId()).thenReturn(10);
        when(chapter.getArchivedAt()).thenReturn(null);
        when(chapter.getPublicationState()).thenReturn("published");

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getVisibilityState()).thenReturn("public");
        when(story.getPublicationState()).thenReturn("published");
        when(story.getArchivedAt()).thenReturn(null);

        MediaDownloadResponse response = mediaService.downloadMedia(5);

        assertEquals("/files/doc.pdf", response.downloadUrl());
    }

    @Test
    void deleteMedia_deberiaEliminarArchivo() {
        Media media = mock(Media.class);
        Chapter chapter = mock(Chapter.class);
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(mediaRepository.findById(5)).thenReturn(Optional.of(media));
        when(media.getChapterId()).thenReturn(3);

        when(chapterRepository.findById(3)).thenReturn(Optional.of(chapter));
        when(chapter.getStoryId()).thenReturn(10);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        MessageResponse response = mediaService.deleteMedia(5);

        assertEquals("Archivo eliminado correctamente", response.message());
        verify(mediaRepository).delete(media);
    }

    @Test
    void uploadMedia_deberiaLanzarUnauthorized_siPrincipalNoEsCustomUserDetails() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of())
        );

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> mediaService.uploadMedia(new UploadMediaRequest(
                        "a.png", "image", null, 3, "/files/a.png"
                ))
        );

        assertEquals("No autenticado", ex.getMessage());
    }

    private void mockAuthenticated(CustomUserDetails principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
    }
}