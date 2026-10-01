package com.example.MyProject.Services;

import com.example.MyProject.Enum.ProductStatus;
import com.example.MyProject.Exception.CartBusinessException;
import com.example.MyProject.Models.Category;
import com.example.MyProject.Models.Product;
import com.example.MyProject.Models.ProductVariant;
import com.example.MyProject.Product.Dto.ProductRequest;
import com.example.MyProject.Product.Dto.ProductResponse;
import com.example.MyProject.Product.Dto.ProductVariantRequest;
import com.example.MyProject.Product.Dto.ProductVariantResponse;
import com.example.MyProject.Repository.CartItemRepository;
import com.example.MyProject.Repository.CategoryRepository;
import com.example.MyProject.Repository.OrderItemRepository;
import com.example.MyProject.Repository.ProductRepository;
import com.example.MyProject.Repository.ProductVariantRepository;
import com.example.MyProject.Repository.WishlistRepository;
import com.example.MyProject.User.Dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final WishlistRepository wishlistRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final MessageSource messageSource;
    @Transactional(readOnly = true)
    public ApiResponse<Page<ProductResponse>> getFilteredProducts(
            String keyword, Long categoryId, Double min, Double max, Pageable pageable) {

        String search = (keyword == null) ? "" : keyword;
        Page<Product> products;

        if (categoryId != null) {
            products = productRepository.findByNameContainingIgnoreCaseAndCategory_CategoryIdAndPriceBetween(
                    search, categoryId, min, max, pageable);
        } else {
            products = productRepository.findByNameContainingIgnoreCaseAndPriceBetween(
                    search, min, max, pageable);
        }

        return ApiResponse.<Page<ProductResponse>>builder()
                .success(true)
                .data(products.map(this::mapToResponse))
                .message(getMessage("product.list.success"))
                .build();
    }

    @Transactional
    public ApiResponse<ProductResponse> createProduct(ProductRequest dto) {
        if (productRepository.existsByName(dto.getName())) {
            return ApiResponse.<ProductResponse>builder().success(false).message(getMessage("product.exists")).build();
        }

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Product product = Product.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .stock(dto.getStock())
                .imageUrl(dto.getImageUrl())
                .rating(dto.getRating())
                .reviewCount(dto.getReviewCount())
                .discountPercentage(dto.getDiscountPercentage())
                .category(category)
                .build();

        applyVariants(product, dto.getVariants());
        checkInventoryAndSetStatus(product);

        return ApiResponse.<ProductResponse>builder()
                .success(true)
                .data(mapToResponse(productRepository.save(product)))
                .message(getMessage("product.create.success"))
                .build();
    }

    @Transactional
    public ApiResponse<ProductResponse> updateProduct(Long id, ProductRequest dto) {
        Product product = productRepository.findById(id).orElseThrow(() -> new RuntimeException("Product not found"));
        Category category = categoryRepository.findById(dto.getCategoryId()).orElseThrow();

        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setStock(dto.getStock());
        // BUG FIX: image, rating, review count, and discount were never
        // applied on update before - editing a product silently kept its
        // old image no matter what the admin selected.
        product.setImageUrl(dto.getImageUrl());
        product.setRating(dto.getRating());
        product.setReviewCount(dto.getReviewCount());
        product.setDiscountPercentage(dto.getDiscountPercentage());
        product.setCategory(category);

        applyVariants(product, dto.getVariants());
        checkInventoryAndSetStatus(product);

        return ApiResponse.<ProductResponse>builder()
                .success(true)
                .data(mapToResponse(productRepository.save(product)))
                .message(getMessage("product.update.success"))
                .build();
    }

    // BUG FIX: needs an open transaction because mapToResponse() touches
    // product.getVariants() (a lazy @OneToMany) and getCategory(). With
    // spring.jpa.open-in-view=false, the DB session is already closed by
    // the time those are read, throwing LazyInitializationException - a 500
    // that the frontend surfaced to customers as "Product not found".
    // Same root cause as the wishlist-loading bug fixed earlier.
    @Transactional(readOnly = true)
    public ApiResponse<ProductResponse> getProductById(Long productId) {
        return productRepository.findById(productId)
                .map(p -> ApiResponse.<ProductResponse>builder().success(true).data(mapToResponse(p)).build())
                .orElse(ApiResponse.<ProductResponse>builder().success(false).message(getMessage("product.notfound")).build());
    }

    @Transactional
    public ApiResponse<Void> deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            return ApiResponse.<Void>builder().success(false).message(getMessage("product.notfound")).build();
        }

        // A product that's been ordered can't be hard-deleted - doing so
        // would either fail on the same FK constraint as wishlist/cart
        // below, or (worse, if the DB allowed it) silently corrupt past
        // customers' order history. Mark it out of stock instead.
        long orderCount = orderItemRepository.countByProduct_ProductId(id);
        if (orderCount > 0) {
            throw new CartBusinessException(
                    "This product has " + orderCount + " existing order(s) and can't be deleted, " +
                    "since that would break those customers' order history. " +
                    "Set it to Out of Stock instead if you want to stop selling it.");
        }

        // Wishlist entries and active cart items just reference a product a
        // shopper was interested in - safe to clean up before deleting.
        wishlistRepository.deleteByProduct_ProductId(id);
        cartItemRepository.deleteByProductId(id);

        productRepository.deleteById(id);
        return ApiResponse.<Void>builder().success(true).message(getMessage("product.delete.success")).build();
    }


    /**
     * Reconciles a product's variant set with the submitted list, rather
     * than blindly clearing and re-inserting everything. That naive
     * approach would break the moment an admin edited a product that had
     * ANY variant with order history - orphanRemoval would try to delete
     * an order-referenced variant row and hit the same FK violation class
     * we already fixed for whole products. Instead:
     *   - variants present in the request with a variantId get updated in place
     *   - variants with no variantId are new, get added
     *   - existing variants NOT in the request are removed, UNLESS they've
     *     been ordered - those are left alone instead of crashing, since
     *     removing them would corrupt past order history the same way
     *     hard-deleting an ordered product would.
     */
    private void applyVariants(Product product, List<ProductVariantRequest> variantRequests) {
        if (variantRequests == null) {
            variantRequests = List.of();
        }

        Map<Long, ProductVariant> existingById = product.getVariants().stream()
                .filter(v -> v.getVariantId() != null)
                .collect(Collectors.toMap(ProductVariant::getVariantId, v -> v));

        java.util.Set<Long> keptIds = new java.util.HashSet<>();

        for (ProductVariantRequest vr : variantRequests) {
            if (vr.getVariantId() != null && existingById.containsKey(vr.getVariantId())) {
                ProductVariant existing = existingById.get(vr.getVariantId());
                existing.setSize(vr.getSize());
                existing.setColor(vr.getColor());
                existing.setStock(vr.getStock() != null ? vr.getStock() : 0);
                existing.setPriceOverride(vr.getPriceOverride());
                existing.setSku(vr.getSku());
                keptIds.add(vr.getVariantId());
            } else {
                ProductVariant newVariant = ProductVariant.builder()
                        .product(product)
                        .size(vr.getSize())
                        .color(vr.getColor())
                        .stock(vr.getStock() != null ? vr.getStock() : 0)
                        .priceOverride(vr.getPriceOverride())
                        .sku(vr.getSku())
                        .build();
                product.getVariants().add(newVariant);
            }
        }

        // Remove variants that were dropped from the submitted list - but
        // only the ones nobody has ever ordered.
        product.getVariants().removeIf(v ->
                v.getVariantId() != null
                        && !keptIds.contains(v.getVariantId())
                        && orderItemRepository.countByVariant_VariantId(v.getVariantId()) == 0);
    }

    /**
     * Keeps status (and, for variant products, the base stock field used
     * for display/sorting) truthful. For a simple product (no variants),
     * this is unchanged from before - based on Product.stock directly.
     * For a product WITH variants, "in stock" means at least one variant
     * has stock, and the base stock field is kept as the sum of all
     * variant stocks purely so existing sort-by-stock / display logic
     * elsewhere keeps working without having to know about variants.
     */
    private void checkInventoryAndSetStatus(Product product) {
        if (!product.getVariants().isEmpty()) {
            int totalVariantStock = product.getVariants().stream()
                    .mapToInt(v -> v.getStock() != null ? v.getStock() : 0)
                    .sum();
            product.setStock(totalVariantStock);
        }

        if (product.getStock() <= 0) {
            product.setStatus(ProductStatus.OUT_OF_STOCK);
            log.error("CRITICAL: Product '{}' (ID: {}) is now OUT OF STOCK!", product.getName(), product.getProductId());
        } else {
            product.setStatus(ProductStatus.IN_STOCK);
        }
    }

    private String getMessage(String key) {
        return messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
    }

    // Public wrapper so other services (e.g. WishlistService) can reuse the
    // same mapping instead of duplicating it.
    public ProductResponse toResponse(Product product) {
        return mapToResponse(product);
    }

    private ProductResponse mapToResponse(Product product) {
        List<ProductVariantResponse> variantResponses = product.getVariants().stream()
                .map(v -> ProductVariantResponse.builder()
                        .variantId(v.getVariantId())
                        .size(v.getSize())
                        .color(v.getColor())
                        .stock(v.getStock())
                        .priceOverride(v.getPriceOverride())
                        .sku(v.getSku())
                        .build())
                .collect(Collectors.toList());

        return ProductResponse.builder()
                .productId(product.getProductId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .status(product.getStatus())
                .imageUrl(product.getImageUrl())
                .rating(product.getRating())
                .reviewCount(product.getReviewCount())
                .discountPercentage(product.getDiscountPercentage())
                // BUG FIX: product.getCategory() being unexpectedly null (broken/missing
                // reference) used to throw a NullPointerException here, which surfaced to
                // the customer as a 500 error - and the frontend was treating ANY error as
                // "Product not found", which is exactly the misleading behavior that was
                // reported. Falling back to a placeholder instead of crashing.
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : "Uncategorized")
                .categoryId(product.getCategory() != null ? product.getCategory().getCategoryId() : null)
                .variants(variantResponses)
                .build();
    }
}