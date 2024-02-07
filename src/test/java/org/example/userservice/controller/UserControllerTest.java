package org.example.userservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.restassured.RestAssured;
import jakarta.annotation.PostConstruct;
import org.example.userservice.api.UserRequestDTO;
import org.example.userservice.model.User;
import org.example.userservice.model.UserMainFields;
import org.example.userservice.model.security.Role;
import org.example.userservice.service.UserService;
import org.example.userservice.service.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class UserControllerTest {

    @LocalServerPort
    private int port;

    @SpyBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;
    private final JavaTimeModule javaTimeModule = new JavaTimeModule();

    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    private String token;

    private List<User> usersInDB = new ArrayList<>();

    @PostConstruct
    private void init() {
        this.token = jwtTokenProvider.createToken("test", Role.ADMIN.getPermissions());
    }

    @BeforeEach
    void setUp() {

        objectMapper.registerModule(javaTimeModule);

        RestAssured.baseURI = "http://localhost/api/v1/users";
        RestAssured.port = port;

        assertTrue(userService.getAll().isEmpty(), "Looks like the database being used is not a test one. Make sure you are using the test database for this test.");

        usersInDB = List.of(
            userService.create(new UserRequestDTO("username1", "password1", LocalDate.now().minusYears(15), "+11111111")),
            userService.create(new UserRequestDTO("username2", "password2", LocalDate.now().minusMonths(25), "+22222222")),
            userService.create(new UserRequestDTO("username3", "password3", LocalDate.now().minusWeeks(39), "+33333333")),
            userService.create(new UserRequestDTO("username4", "password4", LocalDate.now().minusDays(1), "+44444444"))
        );

        assertFalse(userService.getAll().isEmpty());

        Mockito.clearInvocations(userService);
    }

    @Test
    void testJWTAuthorization_NoToken() {

        when()
            .get()
        .then()
            .statusCode(HttpStatus.FORBIDDEN.value());

        verify(userService, never()).getAll();
    }

    @Test
    void testGetAll() throws JsonProcessingException {

        var jsonResponse =
                given()
                    .header("Authorization", token)
                .when()
                    .get()
                .then()
                    .statusCode(200)
                    .extract()
                    .asPrettyString();

        Set<User> usersFound = objectMapper.readValue(jsonResponse, new TypeReference<>() {});
        assertEquals(new HashSet<>(usersInDB), usersFound);

        verify(userService, times(1)).getAll();
    }

    @Test
    void testGetById() throws JsonProcessingException {

        for (var user : usersInDB) {

            var jsonResponse =
                    given()
                        .header("Authorization", token)
                    .when()
                        .get("/{id}", user.getId())
                    .then()
                        .statusCode(200)
                        .extract()
                        .asPrettyString();

            var userFound = objectMapper.readValue(jsonResponse, User.class);

            assertUserEquals(user, userFound);
            verify(userService, times(1)).getById(user.getId());
        }

        verify(userService, times(usersInDB.size())).getById(anyLong());
    }

    @Test
    void testGetById_NotFound() {

        var id = 0L;

        given()
            .header("Authorization", token)
        .when()
            .get("/{id}", id)
        .then()
            .statusCode(404);

        verify(userService, times(1)).getById(id);
    }

    @Test
    void testGetByUsername() throws JsonProcessingException {

        for (var user : usersInDB) {

            var jsonResponse =
                    given()
                        .header("Authorization", token)
                    .when()
                        .get("/username/{username}", user.getUsername())
                    .then()
                        .statusCode(200)
                        .extract()
                        .asPrettyString();

            var userFound = objectMapper.readValue(jsonResponse, User.class);

            assertUserEquals(user, userFound);
            verify(userService, times(1)).getByUsername(user.getUsername());
        }

        verify(userService, times(usersInDB.size())).getByUsername(anyString());
    }

    @Test
    void testGetByUsername_NotFound() {

        var username = "-";

        given()
            .header("Authorization", token)
        .when()
            .get("/username/{username}", username)
        .then()
            .statusCode(404);

        verify(userService, times(1)).getByUsername(username);
    }

    @Test
    void testCreate() throws JsonProcessingException {

        var newUser = new UserRequestDTO("new_username", "new_password", LocalDate.now(), "+99999999");

        var jsonResponse =
                given()
                    .header("Authorization", token)
                    .contentType("application/json")
                    .body(objectMapper.writeValueAsString(newUser))
                .when()
                    .post()
                .then()
                    .statusCode(200)
                    .extract()
                    .asPrettyString();

       User userCreated = objectMapper.readValue(jsonResponse, User.class);

        assertNotNull(userCreated);
        assertNotNull(userCreated.getId());
        assertUserEquals(newUser.toUser(), userCreated);

        verify(userService, times(1)).create(any(UserMainFields.class));
    }

    @Test
    void testCreate_AlreadyExistsByUsername() throws JsonProcessingException {

        for (var user : usersInDB) {

            var newUser = new UserRequestDTO(user.getUsername(), "new_password", LocalDate.now(), "+99999999");

            given()
                .header("Authorization", token)
                .contentType("application/json")
                .body(objectMapper.writeValueAsString(newUser))
            .when()
                .post()
            .then()
                .statusCode(HttpStatus.CONFLICT.value());
        }

        verify(userService, times(usersInDB.size())).create(any(UserRequestDTO.class));
    }

    @Test
    void testUpdate() throws JsonProcessingException {

        for (var user : usersInDB) {

            var userToUpdate = new UserRequestDTO(
                    user.getUsername()+"1",
                    user.getPassword()+"1",
                    user.getBirthDate().minusMonths(2),
                    user.getPhoneNumber()+"1");

            var jsonResponse
                    = given()
                        .header("Authorization", token)
                        .contentType("application/json")
                        .body(objectMapper.writeValueAsString(userToUpdate))
                    .when()
                        .put("/{id}", user.getId())
                    .then()
                        .statusCode(200)
                        .extract()
                        .asPrettyString();

            User userUpdated = objectMapper.readValue(jsonResponse, User.class);

            assertUserEquals(userToUpdate.toUser(), userUpdated);
        }

        verify(userService, times(usersInDB.size())).update(anyLong(), any(UserRequestDTO.class));
    }

    @Test
    void testUpdate_NotFound() throws JsonProcessingException {

        var id = 0L;
        var userToUpdate = new UserRequestDTO();

        given()
            .header("Authorization", token)
            .contentType("application/json")
            .body(objectMapper.writeValueAsString(userToUpdate))
        .when()
            .put("/{id}", id)
        .then()
            .statusCode(HttpStatus.NOT_FOUND.value());

        verify(userService, times(1)).update(anyLong(), any(UserRequestDTO.class));
    }

    @Test
    void testDeleteById() {

        assertFalse(userService.getAll().isEmpty());

        for (var user : usersInDB) {

            given()
                .header("Authorization", token)
            .when()
                .delete("/{id}", user.getId())
           .then()
                .statusCode(200)
                .extract()
                .asPrettyString();

            verify(userService, times(1)).deleteById(user.getId());
        }

        verify(userService, times(usersInDB.size())).deleteById(anyLong());
        assertTrue(userService.getAll().isEmpty());
    }

    private void assertUserEquals(User userExpected, User userActual) {
        assertEquals(userExpected, userActual);
        assertEquals(userExpected.getPassword(), userActual.getPassword());
        assertEquals(userExpected.getBirthDate(), userActual.getBirthDate());
        assertEquals(userExpected.getPhoneNumber(), userActual.getPhoneNumber());
    }
}