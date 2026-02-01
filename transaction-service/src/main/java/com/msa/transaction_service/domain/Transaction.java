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

	private String userId;

	@Enumerated(EnumType.STRING)
	private TransactionType type;

	private BigDecimal amount;
	private BigDecimal balanceAfter;
	private LocalDateTime createdAt;

	@Builder
	public Transaction(String userId, TransactionType type, BigDecimal amount, BigDecimal balanceAfter,
			LocalDateTime createdAt) {
		this.userId = userId;
		this.type = type;
		this.amount = amount;
		this.balanceAfter = balanceAfter;
		this.createdAt = createdAt;
	}

	public static Transaction create(String userId, TransactionType type, BigDecimal amount, BigDecimal balanceAfter) {
		return Transaction.builder()
				.userId(userId)
				.type(type)
				.amount(amount)
				.balanceAfter(balanceAfter)
				.createdAt(LocalDateTime.now())
				.build();
	}
}
