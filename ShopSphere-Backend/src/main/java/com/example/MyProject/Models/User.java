package com.example.MyProject.Models;
import com.example.MyProject.Enum.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @NotBlank(message = "username.required")
    @Size(min = 3, max = 30, message = "username.size")
    @Column(unique = true, nullable = false, length = 30)
    private String userName;

    @Email(message = "email.invalid")
    @NotBlank(message = "email.required")
    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @NotBlank(message = "password.required")
    @Size(min = 8, max = 255, message = "password.size")
    @Column(nullable = false, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;
}
