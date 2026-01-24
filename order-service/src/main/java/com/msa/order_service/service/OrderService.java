package com.msa.order_service.service;

import com.msa.order_service.client.PaymentClient;
import com.msa.order_service.domain.Order;
import com.msa.order_service.domain.OrderStatus;
import com.msa.order_service.dto.OrderRequest;
import com.msa.order_service.dto.OrderResponse;
import com.msa.order_service.dto.PaymentRequest;
import com.msa.order_service.dto.PaymentResponse;
import com.msa.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import org.springframework.security.core.context.SecurityContextHolder;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentClient paymentClient;

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        String authenticatedUserIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long authenticatedUserId = Long.parseLong(authenticatedUserIdStr);

        log.info("Creating order for userId: {}, productId: {}", authenticatedUserId, request.getProductId());

        Order order = Order.createOrder(
                authenticatedUserId,
                request.getProductId(),
                request.getProductName(),
                request.getQuantity(),
                request.getUnitPrice());

        Order savedOrder = orderRepository.save(order);
        log.info("Order created successfully with ID: {}", savedOrder.getId());

        PaymentRequest paymentRequest = PaymentRequest.builder()
                .orderId(savedOrder.getId())
                .userId(savedOrder.getUserId())
                .amount(savedOrder.getTotalAmount())
                .paymentMethod(request.getPaymentMethod())
                .build();

        log.info("Requesting payment for orderId: {}", savedOrder.getId());
        PaymentResponse paymentResponse = paymentClient.processPayment(paymentRequest);

        if ("SUCCESS".equals(paymentResponse.getStatus())) {
            savedOrder.updateStatus(OrderStatus.COMPLETED);
            log.info("Payment successful for orderId: {}", savedOrder.getId());
        } else {
            savedOrder.updateStatus(OrderStatus.FAILED);
            log.warn("Payment failed for orderId: {}", savedOrder.getId());
        }

        return OrderResponse.from(savedOrder);
    }

    public OrderResponse paymentFallback(OrderRequest request, Throwable t) {
        log.error("Payment Service is unavailable. Fallback executed for userId: {}. Error: {}", request.getUserId(),
                t.getMessage());

        return OrderResponse.builder()
                .orderId(0L)
                .userId(request.getUserId())
                .productId(request.getProductId())
                .productName(request.getProductName())
                .quantity(request.getQuantity())
                .unitPrice(request.getUnitPrice())
                .totalAmount(request.getUnitPrice().multiply(java.math.BigDecimal.valueOf(request.getQuantity())))
                .status(OrderStatus.FAILED)
                .build();
    }
}
