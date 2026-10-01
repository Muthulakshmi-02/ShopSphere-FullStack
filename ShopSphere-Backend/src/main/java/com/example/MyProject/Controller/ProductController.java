package com.example.MyProject.Controller;

import com.example.MyProject.Product.Dto.ProductRequest;
import com.example.MyProject.Product.Dto.ProductResponse;
import com.example.MyProject.Services.ProductService;
import com.example.MyProject.User.Dto.ApiResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "http://localhost:4500")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * UNIFIED SEARCH, FILTER, AND PAGINATION
     * Handled via Query Parameters for maximum flexibility.
     * * @param keyword    - Search by name/description
     *
     * @param categoryId - Filter by specific category
     * @param minPrice   - Minimum price bound
     * @param maxPrice   - Maximum price bound
     * @param page       - Page number (0-indexed)
     * @param size       - Number of items per page
     * @param sort       - Format: "field,direction" (e.g., "price,asc" or "price,desc")
     */
    @GetMapping
    @Transactional
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false, defaultValue = "0") Double minPrice,
            @RequestParam(required = false, defaultValue = "1000000") Double maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "price,asc") String sort) {

        // Split the sort string (e.g., "price,asc" -> ["price", "asc"])
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        Sort.Direction direction = (sortParts.length > 1 && sortParts[1].equalsIgnoreCase("desc"))
                ? Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

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
    @Transactional
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.deleteProduct(id));
    }
}