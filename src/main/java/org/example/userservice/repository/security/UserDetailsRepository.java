package org.example.userservice.repository.security;

import org.example.userservice.model.security.UserDetailsImpl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserDetailsRepository extends JpaRepository<UserDetailsImpl, Integer> {
    Optional<UserDetailsImpl> findByUsername(@Param("username") String username);
}
