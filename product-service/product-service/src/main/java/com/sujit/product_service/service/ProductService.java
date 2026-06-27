package com.sujit.product_service.service;

import com.sujit.product_service.dto.ProductRequest;
import com.sujit.product_service.dto.ProductResponse;
import com.sujit.product_service.entity.Product;
import com.sujit.product_service.exception.CategoryNotFoundException;
import com.sujit.product_service.exception.ProductNotFoundException;
import com.sujit.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryGateway categoryGateway;
    private final InventoryGateway inventoryGateway;

    public List<ProductResponse> findAll() {
        return productRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public ProductResponse findById(Long id) {
        return productRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    public ProductResponse create(ProductRequest request) {
        validateCategory(request.categoryId());
        validateInventoryAvailability(request.categoryId());
        Product p = new Product();
        p.setName(request.name());
        p.setDescription(request.description());
        p.setPrice(request.price());
        p.setCategoryId(request.categoryId());
        p.setSku(request.sku());
        p.setActive(Boolean.TRUE.equals(request.active()));
        return toResponse(productRepository.save(p));
    }

    public ProductResponse update(Long id, ProductRequest request) {
        validateCategory(request.categoryId());
        validateInventoryAvailability(id);
        Product p = productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
        p.setName(request.name());
        p.setDescription(request.description());
        p.setPrice(request.price());
        p.setCategoryId(request.categoryId());
        p.setSku(request.sku());
        p.setActive(Boolean.TRUE.equals(request.active()));
        return toResponse(productRepository.save(p));
    }

    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        productRepository.deleteById(id);
    }

    public ProductResponse activate(Long id) {
        Product product = productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
        product.setActive(true);
        return toResponse(productRepository.save(product));
    }

    public ProductResponse deactivate(Long id) {
        Product product = productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
        product.setActive(false);
        return toResponse(productRepository.save(product));
    }

    public List<ProductResponse> findByCategory(Long categoryId) {
        return productRepository.findByCategoryId(categoryId).stream().map(this::toResponse).toList();
    }

    public List<ProductResponse> search(String q) {
        return productRepository.findByNameContainingIgnoreCaseOrSkuContainingIgnoreCase(q, q).stream().map(this::toResponse).toList();
    }

    public ProductResponse findBySku(String sku) {
        return productRepository.findBySku(sku)
                .map(this::toResponse)
                .orElseThrow(() -> new ProductNotFoundException(-1L));
    }

    public boolean existsById(Long id) {
        return productRepository.existsById(id);
    }

    private void validateCategory(Long categoryId) {
        boolean available = categoryGateway.isCategoryAvailable(categoryId);
        if (!available) {
            throw new CategoryNotFoundException("Category not found");
        }
    }

    private void validateInventoryAvailability(Long productId) {
        boolean available = inventoryGateway.hasAvailableInventory(productId);
        if (!available) {
            throw new IllegalStateException("Inventory not available for this product");
        }
    }

    private ProductResponse toResponse(Product p) {
        return new ProductResponse(p.getId(), p.getName(), p.getDescription(), p.getPrice(), p.getSku(), p.getCategoryId(), p.isActive());
    }
}
