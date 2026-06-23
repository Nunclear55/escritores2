package com.nunclear.escritores.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_deberiaContinuar_siNoHayAuthorizationHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, customUserDetailsService);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilter_deberiaContinuar_siAuthorizationNoEmpiezaConBearer() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic abc");
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, customUserDetailsService);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilter_deberiaContinuar_siTokenNoEsValido() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.isTokenValid("token123")).thenReturn(false);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(jwtService).isTokenValid("token123");
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(customUserDetailsService);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilter_deberiaAutenticarUsuario_siTokenEsValidoYNoHayAuthPrevia() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getAuthorities()).thenReturn(List.of());

        when(jwtService.isTokenValid("token123")).thenReturn(true);
        when(jwtService.extractUserId("token123")).thenReturn(1);
        when(customUserDetailsService.loadById(1)).thenReturn(userDetails);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(userDetails, SecurityContextHolder.getContext().getAuthentication().getPrincipal());

        verify(jwtService).isTokenValid("token123");
        verify(jwtService).extractUserId("token123");
        verify(customUserDetailsService).loadById(1);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_noDebeSobrescribirAuth_siYaExisteAuthenticationEnContext() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        var existingAuth = mock(org.springframework.security.core.Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        when(jwtService.isTokenValid("token123")).thenReturn(true);
        when(jwtService.extractUserId("token123")).thenReturn(1);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertEquals(existingAuth, SecurityContextHolder.getContext().getAuthentication());
        verify(jwtService).isTokenValid("token123");
        verify(jwtService).extractUserId("token123");
        verifyNoInteractions(customUserDetailsService);
        verify(filterChain).doFilter(request, response);
    }
}