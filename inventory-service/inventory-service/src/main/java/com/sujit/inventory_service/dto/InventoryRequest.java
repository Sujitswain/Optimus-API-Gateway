package com.sujit.inventory_service.dto;

import jakarta.validation.constraints.NotNull;

public record InventoryRequest(
        @NotNull Long productId,
        @NotNull Integer quantity,
        @NotNull String warehouse,
        String status
) {
}
