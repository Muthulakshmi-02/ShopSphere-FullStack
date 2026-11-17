package com.example.MyProject.Controller;

import com.example.MyProject.Product.Dto.ProductRequest;
import com.example.MyProject.Product.Dto.ProductResponse;
import com.example.MyProject.Services.ProductService;
import com.example.MyProject.User.Dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private static final int MAX_PAGE_SIZE = 50;

    // Only these sort keys are accepted. "price" is mapped to effectivePrice so that
    // "Low to High" sorts by what the customer actually pays (after discount).
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "price", "effectivePrice",
            "effectivePrice", "effectivePrice",
            "name", "name",
            "rating", "rating");

    private final ProductService productService;

    /**
     * UNIFIED SEARCH, FILTER, AND PAGINATION
     *
     * @param keyword    - Search by name
     * @param categoryId - Filter by specific category
     * @param minPrice   - Minimum price bound
     * @param maxPrice   - Maximum price bound
     * @param page       - Page number (0-indexed)
     * @param size       - Items per page (max 50)
     * @param sort       - "field,direction", e.g. "price,asc". Allowed fields: price, name, rating
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false, defaultValue = "0") Double minPrice,
            @RequestParam(required = false, defaultValue = "1000000") Double maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "price,asc") String sort) {

        String[] sortParts = sort.split(",");
        String sortField = SORT_FIELDS.getOrDefault(sortParts[0].trim(), "effectivePrice");
        Sort.Direction direction = (sortParts.length > 1 && sortParts[1].trim().equalsIgnoreCase("desc"))
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);

        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(direction, sortField));

        return ResponseEntity.ok(productService.getFilteredProducts(
                keyword, categoryId, minPrice, maxPrice, pageable));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(productService.getProductById(productId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductRequest dto) {
        return ResponseEntity.ok(productService.createProduct(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest dto) {
        return ResponseEntity.ok(productService.updateProduct(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.deleteProduct(id));
    }
}
