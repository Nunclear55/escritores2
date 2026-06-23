package com.nunclear.escritores.security;

import com.nunclear.escritores.entity.AppUser;
import com.nunclear.escritores.enums.AccessLevel;
import com.nunclear.escritores.enums.AccountState;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class CustomUserDetailsTest {

    @Test
    void constructor_deberiaMapearCamposCorrectamente() {
        AppUser user = new AppUser();
        user.setId(1);
        user.setLoginName("juan");
        user.setPasswordHash("hash");
        user.setAccessLevel(AccessLevel.ADMIN);
        user.setAccountState(AccountState.ACTIVE);

        CustomUserDetails details = new CustomUserDetails(user);

        assertEquals(1, details.getId());
        assertEquals("juan", details.getUsername());
        assertEquals("hash", details.getPassword());
        assertEquals("admin", details.getAccessLevel());
        assertEquals("active", details.getAccountState());
    }

    @Test
    void getAuthorities_deberiaRetornarRolConPrefijoRole() {
        AppUser user = new AppUser();
        user.setId(1);
        user.setLoginName("juan");
        user.setPasswordHash("hash");
        user.setAccessLevel(AccessLevel.MODERATOR);
        user.setAccountState(AccountState.ACTIVE);

        CustomUserDetails details = new CustomUserDetails(user);
        Collection<SimpleGrantedAuthority> authorities =
                (Collection<SimpleGrantedAuthority>) details.getAuthorities();

        assertEquals(1, authorities.size());
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_MODERATOR")));
    }

    @Test
    void isAccountNonExpired_deberiaRetornarTrue() {
        CustomUserDetails details = buildDetails(AccountState.ACTIVE);
        assertTrue(details.isAccountNonExpired());
    }

    @Test
    void isCredentialsNonExpired_deberiaRetornarTrue() {
        CustomUserDetails details = buildDetails(AccountState.ACTIVE);
        assertTrue(details.isCredentialsNonExpired());
    }

    @Test
    void isAccountNonLocked_deberiaRetornarTrue_siEstadoActivo() {
        CustomUserDetails details = buildDetails(AccountState.ACTIVE);
        assertTrue(details.isAccountNonLocked());
    }

    @Test
    void isAccountNonLocked_deberiaRetornarFalse_siEstadoBanned() {
        CustomUserDetails details = buildDetails(AccountState.BANNED);
        assertFalse(details.isAccountNonLocked());
    }

    @Test
    void isEnabled_deberiaRetornarTrue_siNoEstaBanned() {
        CustomUserDetails details = buildDetails(AccountState.ACTIVE);
        assertTrue(details.isEnabled());
    }

    @Test
    void isEnabled_deberiaRetornarFalse_siEstaBanned() {
        CustomUserDetails details = buildDetails(AccountState.BANNED);
        assertFalse(details.isEnabled());
    }

    private CustomUserDetails buildDetails(AccountState state) {
        AppUser user = new AppUser();
        user.setId(1);
        user.setLoginName("juan");
        user.setPasswordHash("hash");
        user.setAccessLevel(AccessLevel.USER);
        user.setAccountState(state);
        return new CustomUserDetails(user);
    }
}