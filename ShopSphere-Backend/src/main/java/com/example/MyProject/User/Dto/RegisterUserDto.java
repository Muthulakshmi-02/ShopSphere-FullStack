package com.example.MyProject.User.Dto;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterUserDto {
    
    // Messages are literal text on purpose: the old values ("email.invalid", "password.size")
    // are not wrapped in {braces}, so once validation runs the user would literally see
    // "password.size" as the error.
    @NotBlank(message = "username.required.")
    @Size(min = 3, max = 30, message = "username.size")
    private String userName;
 
    @NotBlank(message = "email.required.")
    @Email(message = "email.invalid")
    @Size(max = 100, message = "email.size")
    private String email;
 
    // max 72: BCrypt silently ignores everything after 72 bytes.
    @NotBlank(message = "Password.required")
    @Size(min = 8, max = 72, message = "password.size")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
            message = "Password must contain at least one letter and one number.")
    private String password;
}


