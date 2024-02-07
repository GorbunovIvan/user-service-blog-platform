package org.example.userservice.service.security;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.example.userservice.model.security.Role;
import org.example.userservice.model.security.UserDetailsImpl;
import org.example.userservice.repository.security.UserDetailsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserDetailsRepository userDetailsRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${security.default-users.admin.username}")
    private String defaultUserAdminUsername;

    @Value("${security.default-users.admin.password}")
    private String defaultUserAdminPassword;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userDetailsRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid username - " + username));
    }

    @PostConstruct
    private void init() {
        checkOrCreateDefaultUsers();
    }

    protected void checkOrCreateDefaultUsers() {

        // Presence check
        var defaultAdminOpt = userDetailsRepository.findByUsername(defaultUserAdminUsername);

        if (defaultAdminOpt.isEmpty()) {

            log.warn("Default admin-user was not found, trying to create one.");

            // Creating a user
            var defaultAdmin = new UserDetailsImpl();
            defaultAdmin.setUsername(defaultUserAdminUsername);
            defaultAdmin.setPassword(passwordEncoder.encode(defaultUserAdminPassword));
            defaultAdmin.setRole(Role.ADMIN);
            defaultAdmin.setIsActive(true);
            userDetailsRepository.save(defaultAdmin);

            log.info("Default admin-user was created");
        }
    }
}
