package com.example.MyProject.Services;

import com.example.MyProject.Category.dto.CategoryRequest;
import com.example.MyProject.Category.dto.CategoryResponse;
import com.example.MyProject.Exception.CartBusinessException;
import com.example.MyProject.Exception.ResourceNotFoundException;
import com.example.MyProject.Models.Category;
import com.example.MyProject.Repository.CategoryRepository;
import com.example.MyProject.Repository.ProductRepository;
import com.example.MyProject.User.Dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public ApiResponse<List<CategoryResponse>> getAllCategories() {
        // One grouped query for all counts (not one query per category).
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : productRepository.countProductsPerCategory()) {
            counts.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }

        // Sorted by id so the order is stable (findAll() alone has no guaranteed order).
        List<CategoryResponse> data = categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "categoryId"))
                .stream()
                .map(c -> mapToResponse(c, counts.getOrDefault(c.getCategoryId(), 0L)))
                .collect(Collectors.toList());

        return ApiResponse.<List<CategoryResponse>>builder()
                .success(true)
                .message("Categories fetched")
                .data(data)
                .build();
    }

    @Transactional
    public ApiResponse<CategoryResponse> createCategory(CategoryRequest request) {
        String name = request.getName().trim();

        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new CartBusinessException("A category named '" + name + "' already exists.");
        }

        Category saved = categoryRepository.save(Category.builder()
                .name(name)
                .description(cleanDescription(request.getDescription()))
                .build());

        return ApiResponse.<CategoryResponse>builder()
                .success(true)
                .message("Category created")
                .data(mapToResponse(saved, 0L))
                .build();
    }

    @Transactional
    public ApiResponse<CategoryResponse> updateCategory(Long categoryId, CategoryRequest request) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));

        String name = request.getName().trim();

        if (categoryRepository.existsByNameIgnoreCaseAndCategoryIdNot(name, categoryId)) {
            throw new CartBusinessException("A category named '" + name + "' already exists.");
        }

        category.setName(name);
        category.setDescription(cleanDescription(request.getDescription()));
        Category saved = categoryRepository.save(category);

        return ApiResponse.<CategoryResponse>builder()
                .success(true)
                .message("Category updated")
                .data(mapToResponse(saved, productRepository.countByCategory_CategoryId(categoryId)))
                .build();
    }

    @Transactional
    public ApiResponse<Void> deleteCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));

        // A category that still has products can't be deleted: move or delete them first.
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

    private String cleanDescription(String description) {
        if (description == null) return null;
        String trimmed = description.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private CategoryResponse mapToResponse(Category category, long productCount) {
        return CategoryResponse.builder()
                .categoryId(category.getCategoryId())
                .name(category.getName())
                .description(category.getDescription())
                .productCount(productCount)
                .build();
    }
}