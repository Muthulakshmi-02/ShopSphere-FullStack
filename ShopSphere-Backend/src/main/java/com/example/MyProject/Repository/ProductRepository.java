package com.example.MyProject.Repository;

import com.example.MyProject.Models.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // --- Storefront search: name + category + price range, on the price the customer
    //     actually pays (effectivePrice = after discount / cheapest variant). ---
    Page<Product> findByNameContainingIgnoreCaseAndCategory_CategoryIdAndEffectivePriceBetween(
            String name, Long categoryId, Double min, Double max, Pageable pageable);

    Page<Product> findByNameContainingIgnoreCaseAndEffectivePriceBetween(
            String name, Double min, Double max, Pageable pageable);

    // Old versions (filtered by the ORIGINAL price). No longer used by the storefront.
    Page<Product> findByNameContainingIgnoreCaseAndCategory_CategoryIdAndPriceBetween(
            String name, Long categoryId, Double min, Double max, Pageable pageable);

    Page<Product> findByNameContainingIgnoreCaseAndPriceBetween(
            String name, Double min, Double max, Pageable pageable);

    // Used once at startup to fill in effectivePrice for products created before it existed.
    List<Product> findByEffectivePriceIsNull();

    boolean existsByName(String name);

    long countByCategory_CategoryId(Long categoryId);

    /** Row lock used during checkout so concurrent orders can't oversell low-stock products. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.productId = :productId")
    Optional<Product> findByIdForUpdate(@Param("productId") Long productId);
}