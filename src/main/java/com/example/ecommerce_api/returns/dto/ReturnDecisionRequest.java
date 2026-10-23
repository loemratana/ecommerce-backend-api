package com.example.ecommerce_api.returns.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** Optional note for approve / reject, shown to the customer. */
@Data
public class ReturnDecisionRequest {

    @Size(max = 1000, message = "Note must not exceed 1000 characters")
    private String note;
}
