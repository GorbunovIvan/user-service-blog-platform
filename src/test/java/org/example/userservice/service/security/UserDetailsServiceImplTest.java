package org.example.userservice.service.security;

import org.example.userservice.model.security.Role;
import org.example.userservice.model.security.UserDetailsImpl;
import org.example.userservice.repository.security.UserDetailsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
class UserDetailsServiceImplTest {

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @SpyBean
    private UserDetailsRepository userDetailsRepository;

    private List<UserDetailsImpl> usersInDB;

    @Value("${security.default-users.admin.username}")
    private String defaultUserAdminUsername;

    @BeforeEach
    void setUp() {

        assertEquals(1, userDetailsRepository.findAll().size());

        usersInDB = userDetailsRepository.saveAll(List.of(
                new UserDetailsImpl(null, "admin", "admin", Role.ADMIN, true),
                new UserDetailsImpl(null, "user", "user", Role.USER, true)
        ));

        assertFalse(usersInDB.isEmpty());

        Mockito.clearInvocations(userDetailsRepository);
    }

    @Test
    void testLoadUserByUsername() {
        for (var user : usersInDB) {
            var result = userDetailsService.loadUserByUsername(user.getUsername());
            assertEquals(user, result);
            verify(userDetailsRepository, times(1)).findByUsername(user.getUsername());
        }
        verify(userDetailsRepository, times(usersInDB.size())).findByUsername(anyString());
    }

    @Test
    void testLoadUserByUsername_NotFound() {
        var username = "-";
        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername(username));
        verify(userDetailsRepository, times(1)).findByUsername(username);
    }

    @Test
    void testCheckOrCreateDefaultUsers_Exists() {

        userDetailsService.checkOrCreateDefaultUsers();

        assertFalse(userDetailsRepository.findAll().isEmpty());

        var userCreatedOpt = userDetailsRepository.findByUsername(defaultUserAdminUsername);
        assertTrue(userCreatedOpt.isPresent());
        assertFalse(userCreatedOpt.get().getPassword().isBlank());

        verify(userDetailsRepository, never()).save(any(UserDetailsImpl.class));
    }

    @Test
    void testCheckOrCreateDefaultUsers_NotExists() {

        userDetailsRepository.deleteAll();

        userDetailsService.checkOrCreateDefaultUsers();

        assertFalse(userDetailsRepository.findAll().isEmpty());

        var userCreatedOpt = userDetailsRepository.findByUsername(defaultUserAdminUsername);
        assertTrue(userCreatedOpt.isPresent());
        assertFalse(userCreatedOpt.get().getPassword().isBlank());

        verify(userDetailsRepository, times(1)).save(any(UserDetailsImpl.class));
    }
}