package com.msa.transaction_service.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "balances", uniqueConstraints = @UniqueConstraint(columnNames = "userId"))
public class Balance {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Version
	private Long version;

	private Long userId;

	@Column(name = "balance_amount", precision = 19, scale = 2)
	private BigDecimal balance;

	@Builder
	public Balance(Long userId, BigDecimal balance) {
		this.userId = userId;
		this.balance = balance;
	}

	public static Balance createForUser(Long userId) {
		return Balance.builder()
				.userId(userId)
				.balance(BigDecimal.ZERO)
				.build();
	}

	public void add(BigDecimal amount) {
		this.balance = this.balance.add(amount);
	}

	public void subtract(BigDecimal amount) {
		this.balance = this.balance.subtract(amount);
	}
}
