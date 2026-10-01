package com.example.MyProject.Repository;

import com.example.MyProject.Models.ProductVariant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProduct_ProductId(Long productId);

    Optional<ProductVariant> findByVariantIdAndProduct_ProductId(Long variantId, Long productId);

    // Same locking pattern as ProductRepository.findByIdForUpdate - needed
    // so two concurrent checkouts for the same low-stock variant can't
    // both pass the stock check before either commits.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from ProductVariant v where v.variantId = :variantId")
    Optional<ProductVariant> findByIdForUpdate(Long variantId);

    void deleteByProduct_ProductId(Long productId);
}
