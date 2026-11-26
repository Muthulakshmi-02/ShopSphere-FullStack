package com.example.MyProject.Question.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionRequest {

    @NotBlank(message = "Question cannot be blank.")
    @Size(min = 5, max = 1000, message = "Question must be between 5 and 1000 characters.")
    private String question;
}
