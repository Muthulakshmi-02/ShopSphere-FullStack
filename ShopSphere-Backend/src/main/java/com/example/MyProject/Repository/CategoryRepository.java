package com.example.MyProject.Repository;

import com.example.MyProject.Models.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndCategoryIdNot(String name, Long categoryId);

    // Case-insensitive versions: "Toys" and "toys" must not both be allowed.
    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndCategoryIdNot(String name, Long categoryId);
}