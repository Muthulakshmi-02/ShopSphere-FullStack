package com.example.MyProject.User.Dto;
import com.example.MyProject.Cart.dto.CartRequest;
import lombok.*;
@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String message;
}
