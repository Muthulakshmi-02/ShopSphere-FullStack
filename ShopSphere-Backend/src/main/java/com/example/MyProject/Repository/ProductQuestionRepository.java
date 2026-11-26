package com.example.MyProject.Repository;

import com.example.MyProject.Models.ProductQuestion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductQuestionRepository extends JpaRepository<ProductQuestion, Long> {

    @EntityGraph(attributePaths = { "user" })
    List<ProductQuestion> findByProduct_ProductIdOrderByCreatedAtDesc(Long productId);

    long countByProduct_ProductId(Long productId);
}
