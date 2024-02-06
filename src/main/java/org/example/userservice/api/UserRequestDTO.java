package org.example.userservice.api;

import lombok.*;
import org.example.userservice.model.User;
import org.example.userservice.model.UserMainFields;

import java.time.LocalDate;

@NoArgsConstructor @AllArgsConstructor
@Getter @Setter
@EqualsAndHashCode
@ToString
public class UserRequestDTO implements UserMainFields {

    private String username;
    private String password;
    private LocalDate birthDate;
    private String phoneNumber;

    public UserRequestDTO(User user) {
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.birthDate = user.getBirthDate();
        this.phoneNumber = user.getPhoneNumber();
    }

    public User toUser() {
        return new User(
                null,
                username,
                password,
                birthDate,
                phoneNumber);
    }
}
