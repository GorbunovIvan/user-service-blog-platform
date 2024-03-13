package org.example.userservice.controller.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import org.example.userservice.BaseIntegrationTest;
import org.example.userservice.api.security.UserDetailsDto;
import org.example.userservice.model.security.Role;
import org.example.userservice.model.security.UserDetailsImpl;
import org.example.userservice.repository.security.UserDetailsRepository;
import org.example.userservice.service.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AuthenticationControllerTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @SpyBean
    private JwtTokenProvider jwtTokenProvider;
    @SpyBean
    private UserDetailsService userDetailsService;
    @SpyBean
    private AuthenticationManager authenticationManager;

    @SpyBean
    private PasswordEncoder passwordEncoder;

    @SpyBean
    private UserDetailsRepository userDetailsRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost/auth";
        RestAssured.port = port;
    }

    @Test
    void testLogin() throws JsonProcessingException {

        var username = "admin";
        var password = "admin";

        userDetailsRepository.save(new UserDetailsImpl(null, username, passwordEncoder.encode(password), Role.ADMIN, true));

        var userDetailsDto = new UserDetailsDto(username, password);

        var jsonResponse =
                given()
                        .contentType("application/json")
                        .body(objectMapper.writeValueAsString(userDetailsDto))
                .when()
                        .post("/login")
                .then()
                        .statusCode(200)
                        .extract()
                        .asPrettyString();

        Map<String, String> userCredentials = objectMapper.readValue(jsonResponse, new TypeReference<>() {});

        assertFalse(userCredentials.isEmpty());
        assertEquals(userDetailsDto.getUsername(), userCredentials.get("username"));

        verify(authenticationManager, times(1)).authenticate(any());
        verify(userDetailsService, atLeastOnce()).loadUserByUsername(userDetailsDto.getUsername());
        verify(jwtTokenProvider, times(1)).createToken(anyString(), anyCollection());
    }

    @Test
    void testLogin_Fail() throws JsonProcessingException {

        var username = "admin";
        var password = "admin";

        userDetailsRepository.save(new UserDetailsImpl(null, username, passwordEncoder.encode(password), Role.ADMIN, true));

        var userDetailsDto = new UserDetailsDto("wrong", "wrong");

        given()
                .contentType("application/json")
                .body(objectMapper.writeValueAsString(userDetailsDto))
        .when()
                .post("/login")
        .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());

        verify(authenticationManager, times(1)).authenticate(any());
        verify(jwtTokenProvider, never()).createToken(anyString(), anyCollection());
    }
}