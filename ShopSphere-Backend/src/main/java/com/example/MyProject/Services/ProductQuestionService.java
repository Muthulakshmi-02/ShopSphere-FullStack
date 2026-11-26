package com.example.MyProject.Services;

import com.example.MyProject.Exception.ResourceNotFoundException;
import com.example.MyProject.Models.Product;
import com.example.MyProject.Models.ProductQuestion;
import com.example.MyProject.Models.User;
import com.example.MyProject.Question.dto.AnswerRequest;
import com.example.MyProject.Question.dto.ProductQuestionResponse;
import com.example.MyProject.Question.dto.QuestionRequest;
import com.example.MyProject.Repository.ProductQuestionRepository;
import com.example.MyProject.Repository.ProductRepository;
import com.example.MyProject.Repository.UserRepository;
import com.example.MyProject.User.Dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductQuestionService {

    private final ProductQuestionRepository questionRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public ApiResponse<List<ProductQuestionResponse>> getQuestionsForProduct(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found");
        }
        List<ProductQuestionResponse> list = questionRepository
                .findByProduct_ProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ApiResponse.<List<ProductQuestionResponse>>builder()
                .success(true)
                .data(list)
                .message("Questions fetched successfully")
                .build();
    }

    @Transactional
    public ApiResponse<ProductQuestionResponse> askQuestion(Long productId, QuestionRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        User user = getAuthenticatedUser();

        ProductQuestion pq = ProductQuestion.builder()
                .product(product)
                .user(user)
                .question(request.getQuestion().trim())
                .createdAt(LocalDateTime.now())
                .build();

        ProductQuestion saved = questionRepository.save(pq);

        return ApiResponse.<ProductQuestionResponse>builder()
                .success(true)
                .data(mapToResponse(saved))
                .message("Your question has been posted. Our team will answer soon.")
                .build();
    }

    @Transactional
    public ApiResponse<ProductQuestionResponse> answerQuestion(Long questionId, AnswerRequest request) {
        ProductQuestion pq = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));

        User admin = getAuthenticatedUser();
        String responder = "ShopSphere Support (" + admin.getUserName() + ")";

        pq.setAnswer(request.getAnswer().trim());
        pq.setAnsweredBy(responder);
        pq.setAnsweredAt(LocalDateTime.now());

        ProductQuestion saved = questionRepository.save(pq);

        return ApiResponse.<ProductQuestionResponse>builder()
                .success(true)
                .data(mapToResponse(saved))
                .message("Answer published successfully.")
                .build();
    }

    @Transactional
    public ApiResponse<Void> deleteQuestion(Long questionId) {
        if (!questionRepository.existsById(questionId)) {
            throw new ResourceNotFoundException("Question not found");
        }
        questionRepository.deleteById(questionId);

        return ApiResponse.<Void>builder()
                .success(true)
                .message("Question deleted successfully.")
                .build();
    }

    private ProductQuestionResponse mapToResponse(ProductQuestion pq) {
        return ProductQuestionResponse.builder()
                .id(pq.getId())
                .productId(pq.getProduct().getProductId())
                .askerName(pq.getUser() != null ? pq.getUser().getUserName() : "Customer")
                .question(pq.getQuestion())
                .answer(pq.getAnswer())
                .answeredBy(pq.getAnsweredBy())
                .createdAt(pq.getCreatedAt())
                .answeredAt(pq.getAnsweredAt())
                .build();
    }

    private User getAuthenticatedUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResourceNotFoundException("No authenticated user found");
        }
        Object principal = auth.getPrincipal();
        String email = (principal instanceof UserDetails)
                ? ((UserDetails) principal).getUsername()
                : principal.toString();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
