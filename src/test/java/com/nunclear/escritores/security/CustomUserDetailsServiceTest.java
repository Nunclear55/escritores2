package com.nunclear.escritores.security;

import com.nunclear.escritores.entity.AppUser;
import com.nunclear.escritores.enums.AccessLevel;
import com.nunclear.escritores.enums.AccountState;
import com.nunclear.escritores.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void loadUserByUsername_deberiaRetornarCustomUserDetails() {
        AppUser user = buildUser();
        when(appUserRepository.findByLoginNameIgnoreCase("juan")).thenReturn(Optional.of(user));

        UserDetails result = customUserDetailsService.loadUserByUsername("juan");

        assertNotNull(result);
        assertInstanceOf(CustomUserDetails.class, result);
        assertEquals("juan", result.getUsername());
    }

    @Test
    void loadUserByUsername_deberiaLanzarUsernameNotFound_siNoExiste() {
        when(appUserRepository.findByLoginNameIgnoreCase("juan")).thenReturn(Optional.empty());

        UsernameNotFoundException ex = assertThrows(
                UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername("juan")
        );

        assertEquals("Usuario no encontrado", ex.getMessage());
    }

    @Test
    void loadById_deberiaRetornarCustomUserDetails() {
        AppUser user = buildUser();
        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));

        CustomUserDetails result = customUserDetailsService.loadById(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("juan", result.getUsername());
    }

    @Test
    void loadById_deberiaLanzarUsernameNotFound_siNoExiste() {
        when(appUserRepository.findById(1)).thenReturn(Optional.empty());

        UsernameNotFoundException ex = assertThrows(
                UsernameNotFoundException.class,
                () -> customUserDetailsService.loadById(1)
        );

        assertEquals("Usuario no encontrado", ex.getMessage());
    }

    private AppUser buildUser() {
        AppUser user = new AppUser();
        user.setId(1);
        user.setLoginName("juan");
        user.setPasswordHash("hash");
        user.setAccessLevel(AccessLevel.user);
        user.setAccountState(AccountState.active);
        return user;
    }
}