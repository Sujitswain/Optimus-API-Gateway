package com.sujit.inventory_service.service;

import com.sujit.inventory_service.dto.InventoryRequest;
import com.sujit.inventory_service.dto.InventoryResponse;
import com.sujit.inventory_service.entity.Inventory;
import com.sujit.inventory_service.exception.InventoryNotFoundException;
import com.sujit.inventory_service.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public List<InventoryResponse> findAll() {
        return inventoryRepository.findAll().stream().map(this::toResponse).toList();
    }

    public InventoryResponse findById(Long id) {
        return inventoryRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new InventoryNotFoundException(id));
    }

    public InventoryResponse findByProductId(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .map(this::toResponse)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found for product id: " + productId));
    }

    public InventoryResponse create(InventoryRequest request) {
        Inventory inventory = new Inventory();
        inventory.setProductId(request.productId());
        inventory.setQuantity(request.quantity());
        inventory.setWarehouse(request.warehouse());
        inventory.setStatus(Optional.ofNullable(request.status()).orElse("AVAILABLE"));
        return toResponse(inventoryRepository.save(inventory));
    }

    public InventoryResponse update(Long id, InventoryRequest request) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new InventoryNotFoundException(id));
        inventory.setProductId(request.productId());
        inventory.setQuantity(request.quantity());
        inventory.setWarehouse(request.warehouse());
        inventory.setStatus(Optional.ofNullable(request.status()).orElse(inventory.getStatus()));
        return toResponse(inventoryRepository.save(inventory));
    }

    public void delete(Long id) {
        if (!inventoryRepository.existsById(id)) {
            throw new InventoryNotFoundException(id);
        }
        inventoryRepository.deleteById(id);
    }

    public InventoryResponse addStock(Long productId, Integer quantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found for product id: " + productId));
        inventory.setQuantity(inventory.getQuantity() + quantity);
        return toResponse(inventoryRepository.save(inventory));
    }

    public InventoryResponse deductStock(Long productId, Integer quantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found for product id: " + productId));
        inventory.setQuantity(Math.max(0, inventory.getQuantity() - quantity));
        return toResponse(inventoryRepository.save(inventory));
    }

    public InventoryResponse reserveStock(Long productId, Integer quantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found for product id: " + productId));
        inventory.setQuantity(Math.max(0, inventory.getQuantity() - quantity));
        inventory.setStatus("RESERVED");
        return toResponse(inventoryRepository.save(inventory));
    }

    public InventoryResponse releaseStock(Long productId, Integer quantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found for product id: " + productId));
        inventory.setQuantity(inventory.getQuantity() + quantity);
        inventory.setStatus("AVAILABLE");
        return toResponse(inventoryRepository.save(inventory));
    }

    public InventoryResponse checkAvailability(Long productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found for product id: " + productId));
        return new InventoryResponse(inventory.getId(), inventory.getProductId(), inventory.getQuantity(), inventory.getWarehouse(), inventory.getStatus());
    }

    private InventoryResponse toResponse(Inventory inventory) {
        return new InventoryResponse(inventory.getId(), inventory.getProductId(), inventory.getQuantity(), inventory.getWarehouse(), inventory.getStatus());
    }
}
