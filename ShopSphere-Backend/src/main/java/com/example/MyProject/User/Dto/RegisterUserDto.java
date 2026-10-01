package com.example.MyProject.User.Dto;
import com.example.MyProject.Enum.Role;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterUserDto {
    @NotBlank(message = "username.required")
    @Size(min = 3, max = 30, message = "username.size")
    private String userName;

    @Email(message = "email.invalid")
    @NotBlank(message = "email.required")
    private String email;

    @NotBlank(message = "password.required")
    @Size(min = 8, message = "password.size")
    private String password;

    // No longer required from the client: the server always assigns Role.USER
    // on self-registration (see UserService.registerUser) to prevent
    // privilege escalation. Any value sent here is ignored.
    private Role role;
}


