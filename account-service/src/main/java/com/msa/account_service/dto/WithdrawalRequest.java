package com.msa.account_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WithdrawalRequest {

	@NotNull(message = "amount must not be null")
	@DecimalMin(value = "0.01", message = "amount must be greater than 0")
	private BigDecimal amount;
}
