package org.example.userservice.api.security;

import lombok.*;

@NoArgsConstructor @AllArgsConstructor
@Getter @Setter
@EqualsAndHashCode
@ToString
public class UserDetailsDto {
    private String username;
    private String password;
}
