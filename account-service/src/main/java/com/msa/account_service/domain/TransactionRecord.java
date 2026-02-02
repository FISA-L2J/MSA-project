package com.msa.account_service.domain;

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
@Table(name = "transaction_records")
public class TransactionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Long transactionId;

    private String userId;
    private BigDecimal amount;
    private String type; // DEPOSIT, WITHDRAWAL

    @Enumerated(EnumType.STRING)
    private Status status; // PENDING -> SUCCESS/FAILED

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    public TransactionRecord(String userId, BigDecimal amount, String type, Status status) {
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.status = status;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void updateStatus(Status status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }
}
