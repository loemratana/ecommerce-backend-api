package com.example.ecommerce_api.returns.dto;

import com.example.ecommerce_api.Enum.ReturnItemCondition;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** The condition of every item of the return, as found by the warehouse. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReceiveReturnRequest {

    @Valid
    @NotEmpty(message = "Give the condition of every returned item")
    private List<Item> items;

    @Size(max = 1000, message = "Note must not exceed 1000 characters")
    private String note;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        @NotNull(message = "Return item is required")
        private Long returnItemId;

        @NotNull(message = "Condition is required")
        private ReturnItemCondition condition;
    }
}
