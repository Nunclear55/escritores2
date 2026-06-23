package com.nunclear.escritores.service;

import com.nunclear.escritores.dto.request.CreateIdeaRequest;
import com.nunclear.escritores.dto.request.UpdateIdeaRequest;
import com.nunclear.escritores.dto.response.*;
import com.nunclear.escritores.entity.AppUser;
import com.nunclear.escritores.entity.Idea;
import com.nunclear.escritores.entity.Story;
import com.nunclear.escritores.enums.AccessLevel;
import com.nunclear.escritores.exception.ResourceNotFoundException;
import com.nunclear.escritores.exception.UnauthorizedException;
import com.nunclear.escritores.repository.AppUserRepository;
import com.nunclear.escritores.repository.IdeaRepository;
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
class IdeaServiceTest {

    @Mock
    private IdeaRepository ideaRepository;
    @Mock
    private StoryRepository storyRepository;
    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private IdeaService ideaService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createIdea_deberiaCrearIdea() {
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        Idea saved = mock(Idea.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getId()).thenReturn(10);
        when(story.getOwnerUserId()).thenReturn(1);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        when(ideaRepository.save(any(Idea.class))).thenReturn(saved);
        when(saved.getId()).thenReturn(100);
        when(saved.getStoryId()).thenReturn(10);
        when(saved.getTitle()).thenReturn("Idea base");

        CreateIdeaResponse response = ideaService.createIdea(
                new CreateIdeaRequest(10, "Idea base", "contenido")
        );

        assertEquals(100, response.id());
        assertEquals(10, response.storyId());
        assertEquals("Idea base", response.title());

        ArgumentCaptor<Idea> captor = ArgumentCaptor.forClass(Idea.class);
        verify(ideaRepository).save(captor.capture());
        assertEquals("Idea base", captor.getValue().getTitle());
        assertEquals("contenido", captor.getValue().getContent());
    }

    @Test
    void getIdeaById_deberiaRetornarIdea_siEsEditableParaUsuario() {
        Idea idea = mock(Idea.class);
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(ideaRepository.findById(5)).thenReturn(Optional.of(idea));
        when(idea.getId()).thenReturn(5);
        when(idea.getStoryId()).thenReturn(10);
        when(idea.getTitle()).thenReturn("Idea");
        when(idea.getContent()).thenReturn("Contenido");

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        IdeaDetailResponse response = ideaService.getIdeaById(5);

        assertEquals(5, response.id());
        assertEquals("Idea", response.title());
        assertEquals("Contenido", response.content());
    }

    @Test
    void getIdeasByStory_deberiaRetornarPagina() {
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        Idea idea = mock(Idea.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        Page<Idea> page = new PageImpl<>(List.of(idea), PageRequest.of(0, 20), 1);
        when(ideaRepository.findByStoryWithSearch(eq(10), eq("base"), any(Pageable.class))).thenReturn(page);

        when(idea.getId()).thenReturn(1);
        when(idea.getTitle()).thenReturn("Idea base");

        PageResponse<IdeaListItemResponse> response =
                ideaService.getIdeasByStory(10, "base", 0, 20, null);

        assertEquals(1, response.content().size());
        assertEquals("Idea base", response.content().get(0).title());
    }

    @Test
    void updateIdea_deberiaActualizarIdea() {
        Idea idea = mock(Idea.class);
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        Idea saved = mock(Idea.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(ideaRepository.findById(5)).thenReturn(Optional.of(idea));
        when(idea.getStoryId()).thenReturn(10);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        when(ideaRepository.save(idea)).thenReturn(saved);
        when(saved.getId()).thenReturn(5);
        when(saved.getTitle()).thenReturn("Idea actualizada");

        UpdateIdeaResponse response = ideaService.updateIdea(
                5, new UpdateIdeaRequest("Idea actualizada", "Nuevo contenido")
        );

        assertEquals(5, response.id());
        assertEquals("Idea actualizada", response.title());

        verify(idea).setTitle("Idea actualizada");
        verify(idea).setContent("Nuevo contenido");
    }

    @Test
    void deleteIdea_deberiaEliminarIdea() {
        Idea idea = mock(Idea.class);
        Story story = mock(Story.class);
        AppUser user = mock(AppUser.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(1);
        mockAuthenticated(principal);

        when(ideaRepository.findById(5)).thenReturn(Optional.of(idea));
        when(idea.getStoryId()).thenReturn(10);

        when(storyRepository.findById(10)).thenReturn(Optional.of(story));
        when(story.getOwnerUserId()).thenReturn(1);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1);
        when(user.getAccessLevel()).thenReturn(AccessLevel.user);

        MessageResponse response = ideaService.deleteIdea(5);

        assertEquals("Idea eliminada correctamente", response.message());
        verify(ideaRepository).delete(idea);
    }

    @Test
    void createIdea_deberiaLanzarUnauthorized_siPrincipalNoEsCustomUserDetails() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of())
        );

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> ideaService.createIdea(new CreateIdeaRequest(10, "Idea", "Contenido"))
        );

        assertEquals("No autenticado", ex.getMessage());
    }

    private void mockAuthenticated(CustomUserDetails principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
    }
}