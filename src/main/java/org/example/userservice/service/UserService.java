package org.example.userservice.service;

import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.example.userservice.api.UserRequestDTO;
import org.example.userservice.model.User;
import org.example.userservice.model.UserMainFields;
import org.example.userservice.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Log4j2
public class UserService {

    private final UserRepository userRepository;

    public List<User> getAll() {
        return userRepository.findAll();
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElse(null);
    }

    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElse(null);
    }

    public User create(@Nonnull UserMainFields user) {

        if (getByUsername(user.getUsername()) != null) {
            var errorMessage = "User with username '" + user.getUsername() + "' already exists";
            log.error(errorMessage);
            throw new ResponseStatusException(HttpStatus.CONFLICT, errorMessage);
        }

        var userEntity = getUserEntityOutOfDTO(user);
        if (userEntity == null) {
            var errorMessage = "Unknown type of user - " + user;
            log.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }

        return userRepository.save(userEntity);
    }

    @Transactional
    public User update(Long id, @Nonnull UserMainFields user) {

        var userById = getById(id);
        if (userById == null) {
            var errorMessage = "User with id '" + id + "' does not exist";
            log.error(errorMessage);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, errorMessage);
        }

        var userByUsername = getByUsername(user.getUsername());
        if (userByUsername != null && !userByUsername.getId().equals(id)) {
            var errorMessage = "User with username '" + id + "' already exists";
            log.warn(errorMessage);
            throw new ResponseStatusException(HttpStatus.CONFLICT, errorMessage);
        }

        var userEntity = getUserEntityOutOfDTO(user);
        if (userEntity == null) {
            var errorMessage = "Unknown type of user - " + user;
            log.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }

        if (Optional.ofNullable(userEntity.getPassword()).orElse("").isBlank()) {
            userEntity.setPassword(userById.getPassword());
        }

        userEntity.setId(id);
        return userRepository.save(userEntity);
    }

    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }

    protected User getUserEntityOutOfDTO(UserMainFields user) {
        if (user instanceof User userEntity) {
            return userEntity;
        }
        if (user instanceof UserRequestDTO userDTO) {
            return userDTO.toUser();
        }
        return null;
    }
}
