package com.example.MyProject.Question.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductQuestionResponse {
    private Long id;
    private Long productId;
    private String askerName;
    private String question;
    private String answer;
    private String answeredBy;
    private LocalDateTime createdAt;
    private LocalDateTime answeredAt;
}
