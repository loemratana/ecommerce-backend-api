package com.example.ecommerce_api.user.dto.request;


import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class UserFilterRequest {

    private String email;
    private String firstName;
    private String lastName;
    private Long role;

    private String city;
    private String state;
    private String country;




    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdAt;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime updatedAt;
}
