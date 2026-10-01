package com.example.MyProject.Repository;

import com.example.MyProject.Enum.ProductStatus;
import com.example.MyProject.Models.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // 1. Unified Search: Search by name or description + Category + Price Range
    Page<Product> findByNameContainingIgnoreCaseAndCategory_CategoryIdAndPriceBetween(
            String name, Long categoryId, Double min, Double max, Pageable pageable);

    // 2. Search by name or description + Price Range (Across all categories)
    Page<Product> findByNameContainingIgnoreCaseAndPriceBetween(
            String name, Double min, Double max, Pageable pageable);

    boolean existsByName(String name);

    long countByCategory_CategoryId(Long categoryId);

    /**
     * Row-level lock used during checkout so two concurrent orders for the
     * same low-stock product can't both read/pass the stock check before
     * either commits (which would oversell / drive stock negative). The
     * second transaction blocks here until the first commits or rolls back.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.productId = :productId")
    Optional<Product> findByIdForUpdate(Long productId);
}