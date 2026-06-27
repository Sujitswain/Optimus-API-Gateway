package com.sujit.product_service.service;

import com.sujit.product_service.client.CategoryClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CategoryGateway {

    private final CategoryClient categoryClient;

    @CircuitBreaker(name = "categoryService", fallbackMethod = "fallbackCategoryCheck")
    public boolean isCategoryAvailable(Long categoryId) {
        CategoryClient.CategoryResponse category = categoryClient.getCategoryById(categoryId);
        return category != null && category.id() != null && Boolean.TRUE.equals(category.active());
    }

    private boolean fallbackCategoryCheck(Long categoryId, Exception ex) {
        log.error("Error occurred while checking category availability for ID: {}", categoryId, ex);
        return false;
    }
}
