package com.example.ecommerce_api.review.dto;


import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewerResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String profileImage;
}
