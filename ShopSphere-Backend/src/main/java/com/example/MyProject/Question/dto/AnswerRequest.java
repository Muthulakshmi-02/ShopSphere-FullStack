package com.example.MyProject.Question.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnswerRequest {

    @NotBlank(message = "Answer cannot be blank.")
    @Size(min = 2, max = 2000, message = "Answer must be between 2 and 2000 characters.")
    private String answer;
}
