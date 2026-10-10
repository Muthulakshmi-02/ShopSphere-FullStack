package com.example.MyProject.Services;

import com.example.MyProject.Enum.ProductStatus;
import com.example.MyProject.Exception.CartBusinessException;
import com.example.MyProject.Exception.ResourceNotFoundException;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final WishlistRepository wishlistRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderItemRepository orderItemRepositsory;
    private final MessageSource messageSource;
    private final PricingService pricing;
  

    @Transactional(readOnly = true)
    public ApiResponse<Page<ProductResponse>> getFilteredProducts(
            String keyword, Long categoryId, Double min, Double max, Pageable pageable) {

        String search = (keyword == null) ? "" : keyword;
        Page<Product> products;

        // Filtering uses effectivePrice (what the customer pays), matching the sort order.
        if (categoryId != null) {
            products = productRepository.findByNameContainingIgnoreCaseAndCategory_CategoryIdAndEffectivePriceBetween(
                    search, categoryId, min, max, pageable);
        } else {
            products = productRepository.findByNameContainingIgnoreCaseAndEffectivePriceBetween(
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
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        Product product = Product.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .stock(dto.getStock() != null ? dto.getStock() : 0)
                .imageUrl(dto.getImageUrl())
                 .reviewCount(0)
                .discountPercentage(dto.getDiscountPercentage())
                .category(category)
                .build();

        applyVariants(product, dto.getVariants());
        recalculateEffectivePrice(product);
        checkInventoryAndSetStatus(product);

        return ApiResponse.<ProductResponse>builder()
                .success(true)
                .data(mapToResponse(productRepository.save(product)))
                .message(getMessage("product.create.success"))
                .build();
    }

    @Transactional
    public ApiResponse<ProductResponse> updateProduct(Long id, ProductRequest dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (!product.getName().equals(dto.getName()) && productRepository.existsByName(dto.getName())) {
            return ApiResponse.<ProductResponse>builder().success(false).message(getMessage("product.exists")).build();
        }

        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setStock(dto.getStock() != null ? dto.getStock() : 0);
        product.setImageUrl(dto.getImageUrl());
        // product.setRating(dto.getRating());
        // product.setReviewCount(dto.getReviewCount());
        product.setDiscountPercentage(dto.getDiscountPercentage());
        product.setCategory(category);

        applyVariants(product, dto.getVariants());
        recalculateEffectivePrice(product);
        checkInventoryAndSetStatus(product);

        return ApiResponse.<ProductResponse>builder()
                .success(true)
                .data(mapToResponse(productRepository.save(product)))
                .message(getMessage("product.update.success"))
                .build();
    }

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

        long orderCount = orderItemRepositsory.countByProduct_ProductId(id);
        if (orderCount > 0) {
            throw new CartBusinessException(
                    "This product has " + orderCount + " existing order(s) and can't be deleted, " +
                    "since that would break those customers' order history. " +
                    "Set its stock to 0 instead if you want to stop selling it.");
        }

        wishlistRepository.deleteByProduct_ProductId(id);
        cartItemRepository.deleteByProductId(id);

        productRepository.deleteById(id);
        return ApiResponse.<Void>builder().success(true).message(getMessage("product.delete.success")).build();
    }

    /**
     * Fills in effectivePrice for products created before that column existed.
     * Called once at startup by EffectivePriceBackfill; does nothing when all rows are filled.
     */
    @Transactional
    public int backfillEffectivePrices() {
        List<Product> missing = productRepository.findByEffectivePriceIsNull();
        missing.forEach(this::recalculateEffectivePrice);
        productRepository.saveAll(missing);
        return missing.size();
    }

    /** Lowest price a customer can pay for one unit, after discount (cheapest variant if any). */
    private void recalculateEffectivePrice(Product product) {
        double base = pricing.unitPrice(product, null);
        double lowest = product.getVariants().stream()
                .mapToDouble(v -> pricing.unitPrice(product, v))
                .min()
                .orElse(base);
        product.setEffectivePrice(lowest);
    }

    /**
     * Reconciles a product's variants with the submitted list:
     *   - variants with a known variantId are updated in place
     *   - variants without an id are new
     *   - variants missing from the request are removed, UNLESS they have order history
     *     (those are kept). Cart lines pointing at a removed variant are deleted first.
     */
    private void applyVariants(Product product, List<ProductVariantRequest> variantRequests) {
        if (variantRequests == null) {
            variantRequests = List.of();
        }

        // Two identical size/colour variants would confuse stock and the cart.
        Set<String> seen = new HashSet<>();
        for (ProductVariantRequest vr : variantRequests) {
            String key = (vr.getSize() == null ? "" : vr.getSize().trim().toLowerCase())
                    + "|" + (vr.getColor() == null ? "" : vr.getColor().trim().toLowerCase());
            if (!seen.add(key)) {
                throw new CartBusinessException("Each size/colour combination can only be added once.");
            }
        }

        Map<Long, ProductVariant> existingById = product.getVariants().stream()
                .filter(v -> v.getVariantId() != null)
                .collect(Collectors.toMap(ProductVariant::getVariantId, v -> v));

        Set<Long> keptIds = new HashSet<>();

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

        List<ProductVariant> toRemove = product.getVariants().stream()
                .filter(v -> v.getVariantId() != null
                        && !keptIds.contains(v.getVariantId())
                        && orderItemRepositsory.countByVariant_VariantId(v.getVariantId()) == 0)
                .collect(Collectors.toList());

        for (ProductVariant v : toRemove) {
            cartItemRepository.deleteByVariant_VariantId(v.getVariantId());
        }
        product.getVariants().removeAll(toRemove);
    }

    /**
     * Keeps status truthful. For variant products the base stock is the sum of variant stocks.
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
            log.warn("Product '{}' (ID: {}) is now OUT OF STOCK", product.getName(), product.getProductId());
        } else {
            product.setStatus(ProductStatus.IN_STOCK);
        }
    }

    private String getMessage(String key) {
        return messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
    }

    // Public wrapper so other services (e.g. WishlistService) can reuse the mapping.
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
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : "Uncategorized")
                .categoryId(product.getCategory() != null ? product.getCategory().getCategoryId() : null)
                .variants(variantResponses)
                .build();
    }
}