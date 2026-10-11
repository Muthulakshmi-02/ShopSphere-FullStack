package com.example.MyProject.Category.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryResponse {
    private Long categoryId;
    private String name;
    private String description;

    // NEW: the real number of products in this category, counted by the server. Use this on
    // BOTH the Categories page and the shop sidebar so the two can never disagree again
    // (they used to show 12/8/9/14/7 vs 2/0/2/1/2).
    private long productCount;
}