package com.example.MyProject.Controller;

import com.example.MyProject.Question.dto.AnswerRequest;
import com.example.MyProject.Question.dto.ProductQuestionResponse;
import com.example.MyProject.Question.dto.QuestionRequest;
import com.example.MyProject.Services.ProductQuestionService;
import com.example.MyProject.User.Dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProductQuestionController {

    private final ProductQuestionService questionService;

    @GetMapping("/api/products/{productId}/questions")
    public ResponseEntity<ApiResponse<List<ProductQuestionResponse>>> getQuestions(
            @PathVariable Long productId) {
        return ResponseEntity.ok(questionService.getQuestionsForProduct(productId));
    }

    @PostMapping("/api/products/{productId}/questions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProductQuestionResponse>> askQuestion(
            @PathVariable Long productId,
            @Valid @RequestBody QuestionRequest request) {
        return new ResponseEntity<>(questionService.askQuestion(productId, request), HttpStatus.CREATED);
    }

    @PutMapping("/api/questions/{questionId}/answer")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductQuestionResponse>> answerQuestion(
            @PathVariable Long questionId,
            @Valid @RequestBody AnswerRequest request) {
        return ResponseEntity.ok(questionService.answerQuestion(questionId, request));
    }

    @DeleteMapping("/api/questions/{questionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(
            @PathVariable Long questionId) {
        return ResponseEntity.ok(questionService.deleteQuestion(questionId));
    }
}
