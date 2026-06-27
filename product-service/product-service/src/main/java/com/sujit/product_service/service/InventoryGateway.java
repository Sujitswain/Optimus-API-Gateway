package com.sujit.product_service.service;

import com.sujit.product_service.client.InventoryClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryGateway {

    private final InventoryClient inventoryClient;

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "fallbackInventoryCheck")
    public boolean hasAvailableInventory(Long productId) {
        InventoryClient.InventoryAvailabilityResponse availability = inventoryClient.getAvailability(productId);
        return availability != null && Boolean.TRUE.equals(availability.available()) && availability.availableQuantity() != null && availability.availableQuantity() > 0;
    }

    private boolean fallbackInventoryCheck(Long productId, Exception ex) {
        log.error("Error occurred while checking inventory availability for product ID: {}", productId, ex);
        return false;
    }
}
