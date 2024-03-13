package org.example.userservice.service;

import org.example.userservice.BaseIntegrationTest;
import org.example.userservice.api.UserRequestDTO;
import org.example.userservice.model.User;
import org.example.userservice.model.UserMainFields;
import org.example.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
class UserServiceTest extends BaseIntegrationTest {

    @Autowired
    private UserService userService;

    @SpyBean
    private UserRepository userRepository;

    private List<User> usersInDB;

    @BeforeEach
    void setUp() {

        assertTrue(userRepository.findAll().isEmpty());

        usersInDB = userRepository.saveAll(List.of(
                    new User(null, "username1", "password1", LocalDate.now().minusYears(15), "+11111111"),
                    new User(null, "username2", "password2", LocalDate.now().minusMonths(25), "+22222222"),
                    new User(null, "username3", "password3", LocalDate.now().minusWeeks(39), "+33333333"),
                    new User(null, "username4", "password4", LocalDate.now().minusDays(1), "+44444444")
                ));

        assertFalse(usersInDB.isEmpty());

        Mockito.clearInvocations(userRepository);
    }

    @Test
    void testGetAll() {
        var result = userService.getAll();
        assertEquals(new HashSet<>(usersInDB), new HashSet<>(result));
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void testGetById() {
        for (var user : usersInDB) {
            var result = userService.getById(user.getId());
            assertUserEquals(user, result);
            verify(userRepository, times(1)).findById(user.getId());
        }
        verify(userRepository, times(usersInDB.size())).findById(anyLong());
    }

    @Test
    void testGetById_NotFound() {
        var id = 0L;
        var result = userService.getById(id);
        assertNull(result);
        verify(userRepository, times(1)).findById(id);
    }

    @Test
    void testGetByUsername() {
        for (var user : usersInDB) {
            var result = userService.getByUsername(user.getUsername());
            assertUserEquals(user, result);
            verify(userRepository, times(1)).findByUsername(user.getUsername());
        }
        verify(userRepository, times(usersInDB.size())).findByUsername(anyString());
    }

    @Test
    void testGetByUsername_NotFound() {
        var username = "-";
        var result = userService.getByUsername(username);
        assertNull(result);
        verify(userRepository, times(1)).findByUsername(username);
    }

    @Test
    void testCreate() {

        var newUser = new UserRequestDTO("new_username", "new_password", LocalDate.now(), "+99999999");

        var userCreated = userService.create(newUser);
        assertNotNull(userCreated);
        assertNotNull(userCreated.getId());
        assertUserEquals(newUser.toUser(), userCreated);

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testCreate_AlreadyExistsByUsername() {

        var newUser = new UserRequestDTO(null, "new_password", LocalDate.now(), "+99999999");

        for (var user : usersInDB) {
            newUser.setUsername(user.getUsername());
            assertThrows(RuntimeException.class, () -> userService.create(newUser));
        }

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testUpdate() {

        for (var user : usersInDB) {

            var userToUpdate = new UserRequestDTO(
                    user.getUsername()+"1",
                    user.getPassword()+"1",
                    user.getBirthDate().minusMonths(2),
                    user.getPhoneNumber()+"1");

            var result = userService.update(user.getId(), userToUpdate);

            assertUserEquals(userToUpdate.toUser(), result);
            verify(userRepository, times(1)).existsById(user.getId());
        }

        verify(userRepository, times(usersInDB.size())).save(any(User.class));
    }

    @Test
    void testUpdate_NotFound() {

        var id = 0L;
        var userToUpdate = new UserRequestDTO();

        assertThrows(RuntimeException.class, () -> userService.update(id, userToUpdate));

        verify(userRepository, times(1)).existsById(id);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testDeleteById() {

        assertFalse(userRepository.findAll().isEmpty());

        for (var user : usersInDB) {
            userService.deleteById(user.getId());
            verify(userRepository, times(1)).deleteById(user.getId());
        }

        verify(userRepository, times(usersInDB.size())).deleteById(anyLong());
        assertTrue(userRepository.findAll().isEmpty());
    }

    @Test
    void testGetUserEntityOutOfDTO_FromUser() {
        for (var user : usersInDB) {
            var result = userService.getUserEntityOutOfDTO(user);
            assertSame(user, result);
        }
    }

    @Test
    void testGetUserEntityOutOfDTO_FromUserRequestDTO() {
        for (var user : usersInDB) {
            var result = userService.getUserEntityOutOfDTO(new UserRequestDTO(user));
            assertUserEquals(user, result);
        }
    }

    @Test
    void testGetUserEntityOutOfDTO_FromDifferentType() {
        var differentType = new UserMainFields() {
            @Override
            public String getUsername() {
                return null;
            }
            @Override
            public String getPassword() {
                return null;
            }
        };

        var result = userService.getUserEntityOutOfDTO(differentType);
        assertNull(result);
    }

    private void assertUserEquals(User userExpected, User userActual) {
        assertEquals(userExpected, userActual);
        assertEquals(userExpected.getPassword(), userActual.getPassword());
        assertEquals(userExpected.getBirthDate(), userActual.getBirthDate());
        assertEquals(userExpected.getPhoneNumber(), userActual.getPhoneNumber());
    }
}