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
@Table(name = "accounts")
public class Account {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String userId;
	private String accountNumber;
	private LocalDateTime createdAt;

	private BigDecimal balance = BigDecimal.ZERO;

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

	public BigDecimal getBalance() {
		return this.balance;
	}

	@Builder
	public Account(String userId, String accountNumber, LocalDateTime createdAt) {
		this.userId = userId;
		this.accountNumber = accountNumber;
		this.createdAt = createdAt;
	}

	public static Account createAccount(String userId, String accountNumber) {
		return Account.builder()
				.userId(userId)
				.accountNumber(accountNumber)
				.createdAt(LocalDateTime.now())
				.build();
	}
}
