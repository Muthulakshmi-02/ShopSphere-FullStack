package com.example.MyProject.Controller;
import com.example.MyProject.Repository.ProductRepository;
import com.example.MyProject.User.Dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Catalog-wide stock counts for the admin Products tab.
 * The cards used to count only the 10 products on the CURRENT PAGE, so with more than 10
 * products "In stock / Low stock / Out of stock" were wrong.
 */
@RestController
@RequestMapping("/api/admin/products/stats")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductStatsController {

    // "Low stock" = 1 to 9 units. The admin table badge uses the same rule (stock < 10).
    private static final int LOW_STOCK_BELOW = 10;

    private final ProductRepository productRepository;

    public record ProductStatsResponse(long total, long inStock, long lowStock, long outOfStock) {}

    @GetMapping
    public ResponseEntity<ApiResponse<ProductStatsResponse>> getStats() {
        long total = productRepository.count();
        long outOfStock = productRepository.countByStockLessThanEqual(0);
        long inStock = productRepository.countByStockGreaterThan(0);
        long lowStock = productRepository.countByStockBetween(1, LOW_STOCK_BELOW - 1);

        return ResponseEntity.ok(ApiResponse.<ProductStatsResponse>builder()
                .success(true)
                .data(new ProductStatsResponse(total, inStock, lowStock, outOfStock))
                .build());
    }
}