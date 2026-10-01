package com.example.MyProject.Services;


import com.example.MyProject.Category.dto.CategoryRequest;
import com.example.MyProject.Category.dto.CategoryResponse;
import com.example.MyProject.Models.Category;


import com.example.MyProject.Exception.CartBusinessException;
import com.example.MyProject.Exception.ResourceNotFoundException;
import com.example.MyProject.Repository.CategoryRepository;
import com.example.MyProject.Repository.ProductRepository;
import com.example.MyProject.User.Dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public ApiResponse<List<CategoryResponse>> getAllCategories() {
        List<CategoryResponse> data = categoryRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ApiResponse.<List<CategoryResponse>>builder()
                .success(true)
                .message("Categories fetched")
                .data(data)
                .build();
    }

    public ApiResponse<CategoryResponse> createCategory(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new CartBusinessException("A category named '" + request.getName() + "' already exists.");
        }

        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        Category saved = categoryRepository.save(category);
        return ApiResponse.<CategoryResponse>builder()
                .success(true)
                .message("Category created")
                .data(mapToResponse(saved))
                .build();
    }

    public ApiResponse<CategoryResponse> updateCategory(Long categoryId, CategoryRequest request) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));

        if (categoryRepository.existsByNameAndCategoryIdNot(request.getName(), categoryId)) {
            throw new CartBusinessException("A category named '" + request.getName() + "' already exists.");
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());

        Category saved = categoryRepository.save(category);
        return ApiResponse.<CategoryResponse>builder()
                .success(true)
                .message("Category updated")
                .data(mapToResponse(saved))
                .build();
    }

    public ApiResponse<Void> deleteCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));

        // Prevent deleting a category that still has products in it - doing so
        // would either orphan those products or cascade-delete them silently,
        // neither of which the admin likely intends. Ask them to move/delete
        // the products first instead.
        long productCount = productRepository.countByCategory_CategoryId(categoryId);
        if (productCount > 0) {
            throw new CartBusinessException(
                    "Can't delete '" + category.getName() + "' - it still has " + productCount +
                    " product(s) assigned to it. Move or delete those products first.");
        }

        categoryRepository.delete(category);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Category deleted")
                .data(null)
                .build();
    }

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .categoryId(category.getCategoryId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }
}