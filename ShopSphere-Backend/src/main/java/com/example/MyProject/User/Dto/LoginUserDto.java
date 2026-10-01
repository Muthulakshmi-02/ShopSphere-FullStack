package com.example.MyProject.User.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginUserDto {
    @Email(message = "email.invalid")
    @NotBlank(message = "email.required")
    private String email;
    @NotBlank(message = "password.required")
    private String password;
}
