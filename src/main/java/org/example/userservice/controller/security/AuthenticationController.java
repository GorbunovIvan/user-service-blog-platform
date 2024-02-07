package org.example.userservice.controller.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.example.userservice.api.security.UserDetailsDto;
import org.example.userservice.service.security.jwt.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Log4j2
public class AuthenticationController {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserDetailsDto userDetailsDto) {

        try {
            log.info("Token required for username '{}'", userDetailsDto.getUsername());

            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(userDetailsDto.getUsername(), userDetailsDto.getPassword()));

            var user = userDetailsService.loadUserByUsername(userDetailsDto.getUsername());
            String token = jwtTokenProvider.createToken(user.getUsername(), user.getAuthorities());

            var response = new HashMap<String, String>();
            response.put("username", user.getUsername());
            response.put("token", token);

            log.info("Token was provided to the user '{}'", user.getUsername());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to provide token to the user - {}", e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        var handler = new SecurityContextLogoutHandler();
        handler.logout(request, response, null);
    }
}
