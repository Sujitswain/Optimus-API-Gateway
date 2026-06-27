package com.sujit.order_service.service;

import com.sujit.order_service.client.InventoryClient;
import com.sujit.order_service.client.ProductClient;
import com.sujit.order_service.dto.CreateOrderRequest;
import com.sujit.order_service.dto.OrderItemRequest;
import com.sujit.order_service.dto.OrderItemResponse;
import com.sujit.order_service.dto.OrderResponse;
import com.sujit.order_service.entity.Order;
import com.sujit.order_service.entity.OrderItem;
import com.sujit.order_service.entity.OrderStatus;
import com.sujit.order_service.exception.OrderNotFoundException;
import com.sujit.order_service.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;

    public OrderService(OrderRepository orderRepository, ProductClient productClient, InventoryClient inventoryClient) {
        this.orderRepository = orderRepository;
        this.productClient = productClient;
        this.inventoryClient = inventoryClient;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Order order = new Order();
        order.setCustomerId(request.customerId());
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemRequest itemRequest : request.items()) {
            if (!Boolean.TRUE.equals(productClient.exists(itemRequest.productId()))) {
                throw new IllegalArgumentException("Product not found with id: " + itemRequest.productId());
            }

            InventoryClient.InventoryAvailabilityResponse availability = inventoryClient.getAvailability(itemRequest.productId());
            if (availability == null || !Boolean.TRUE.equals(availability.available())) {
                throw new IllegalStateException("Inventory not available for product id: " + itemRequest.productId());
            }

            OrderItem item = new OrderItem();
            item.setProductId(itemRequest.productId());
            item.setQuantity(itemRequest.quantity());
            item.setPrice(itemRequest.price());
            item.setSubtotal(itemRequest.price().multiply(BigDecimal.valueOf(itemRequest.quantity())));
            item.setOrder(order);
            order.getItems().add(item);
            total = total.add(item.getSubtotal());
        }

        order.setTotalAmount(total);
        Order saved = orderRepository.save(order);
        return toResponse(saved);
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream().map(this::toResponse).toList();
    }

    public OrderResponse getOrder(Long id) {
        return toResponse(findOrder(id));
    }

    @Transactional
    public OrderResponse cancelOrder(Long id) {
        Order order = findOrder(id);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return toResponse(order);
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        return toResponse(orderRepository.save(order));
    }

    private Order findOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getProductId(),
                        item.getQuantity(),
                        item.getPrice(),
                        item.getSubtotal()))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                itemResponses);
    }
}
