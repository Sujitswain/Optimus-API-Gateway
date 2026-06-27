package com.sujit.order_service.dto;

import java.math.BigDecimal;

public record OrderItemResponse(Long id, Long productId, Integer quantity, BigDecimal price, BigDecimal subtotal) {
}
