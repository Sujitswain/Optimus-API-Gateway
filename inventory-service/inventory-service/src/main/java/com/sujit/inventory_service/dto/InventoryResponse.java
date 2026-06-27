package com.sujit.inventory_service.dto;

public record InventoryResponse(Long id, Long productId, Integer quantity, String warehouse, String status) {
}
