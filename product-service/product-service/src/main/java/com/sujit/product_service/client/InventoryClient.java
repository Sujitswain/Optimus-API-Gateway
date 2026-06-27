package com.sujit.product_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "inventory-service")
public interface InventoryClient {

    @GetMapping("/api/inventory/product/{productId}/availability")
    InventoryAvailabilityResponse getAvailability(@PathVariable("productId") Long productId);

    record InventoryAvailabilityResponse(Long productId, Integer availableQuantity, Boolean available) {}
}
