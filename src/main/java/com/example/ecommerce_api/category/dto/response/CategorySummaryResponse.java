package com.example.ecommerce_api.category.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data

@Builder
public class CategorySummaryResponse {


    private Long id;
    private String name;
    private String slug;

}
