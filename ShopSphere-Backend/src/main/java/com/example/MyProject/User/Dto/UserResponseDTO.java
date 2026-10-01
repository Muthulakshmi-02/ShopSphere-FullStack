package com.example.MyProject.User.Dto;
import com.example.MyProject.Enum.Role;
import lombok.*;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDTO {
    private Long userId;
    private String userName;
    private String email;
    private Role role;
}
