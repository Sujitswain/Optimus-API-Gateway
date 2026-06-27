package com.sujit.inventory_service.controller;

import com.sujit.inventory_service.dto.InventoryRequest;
import com.sujit.inventory_service.dto.InventoryResponse;
import com.sujit.inventory_service.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<List<InventoryResponse>> getAll() {
        return ResponseEntity.ok(inventoryService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<InventoryResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(inventoryService.findById(id));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<InventoryResponse> getByProductId(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.findByProductId(productId));
    }

    @PostMapping
    public ResponseEntity<InventoryResponse> create(@Valid @RequestBody InventoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InventoryResponse> update(@PathVariable Long id, @Valid @RequestBody InventoryRequest request) {
        return ResponseEntity.ok(inventoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        inventoryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{productId}/add")
    public ResponseEntity<InventoryResponse> add(@PathVariable Long productId, @RequestParam Integer quantity) {
        return ResponseEntity.ok(inventoryService.addStock(productId, quantity));
    }

    @PatchMapping("/{productId}/deduct")
    public ResponseEntity<InventoryResponse> deduct(@PathVariable Long productId, @RequestParam Integer quantity) {
        return ResponseEntity.ok(inventoryService.deductStock(productId, quantity));
    }

    @PatchMapping("/{productId}/reserve")
    public ResponseEntity<InventoryResponse> reserve(@PathVariable Long productId, @RequestParam Integer quantity) {
        return ResponseEntity.ok(inventoryService.reserveStock(productId, quantity));
    }

    @PatchMapping("/{productId}/release")
    public ResponseEntity<InventoryResponse> release(@PathVariable Long productId, @RequestParam Integer quantity) {
        return ResponseEntity.ok(inventoryService.releaseStock(productId, quantity));
    }

    @GetMapping("/{productId}/availability")
    public ResponseEntity<InventoryResponse> availability(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.checkAvailability(productId));
    }
}
