package com.sujit.order_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderItemRequest(
        @NotNull Long productId,
        @Min(1) Integer quantity,
        @NotNull java.math.BigDecimal price
) {
}
