package com.msa.transaction_service.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long recordId;

    private String userId;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private LocalDateTime createdAt;

    @Builder
    public Transaction(Long recordId, String userId, TransactionType type, BigDecimal amount, BigDecimal balanceAfter,
                       LocalDateTime createdAt) {
        this.recordId = recordId;
        this.userId = userId;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.createdAt = createdAt;
    }

    public static Transaction create(Long recordId, String userId, TransactionType type, BigDecimal amount, BigDecimal balanceAfter) {
        return Transaction.builder()
                .recordId(recordId)
                .userId(userId)
                .type(type)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
