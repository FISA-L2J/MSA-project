package com.msa.account_service.service;

import com.msa.account_service.client.TransactionClient;
import com.msa.account_service.domain.Account;
import com.msa.account_service.dto.*;
import com.msa.account_service.repository.AccountRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

	private static final String ACCOUNT_NUMBER_PREFIX = "ACC-";

	private final AccountRepository accountRepository;
	private final TransactionClient transactionClient;

	@Transactional
	public Account ensureAccountForUser(Long userId) {
		return accountRepository.findByUserId(userId)
				.orElseGet(() -> {
					Account account = Account.createAccount(userId, ACCOUNT_NUMBER_PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
					return accountRepository.save(account);
				});
	}

	@Transactional
	@CircuitBreaker(name = "transactionService", fallbackMethod = "depositFallback")
	public DepositResponse deposit(DepositRequest request) {
		Long userId = getAuthenticatedUserId();
		ensureAccountForUser(userId);

		TransactionProcessRequest processRequest = TransactionProcessRequest.builder()
				.userId(userId)
				.amount(request.getAmount())
				.build();

		log.info("Requesting deposit for userId: {}, amount: {}", userId, request.getAmount());
		TransactionProcessResponse response = transactionClient.processDeposit(processRequest);

		return DepositResponse.builder()
				.transactionId(response.getTransactionId())
				.userId(userId)
				.amount(request.getAmount())
				.newBalance(response.getNewBalance())
				.status(response.getStatus())
				.createdAt(response.getCreatedAt())
				.build();
	}

	public DepositResponse depositFallback(DepositRequest request, Throwable t) {
		log.error("Transaction Service is unavailable. Fallback executed for deposit. Error: {}", t.getMessage());
		return DepositResponse.builder()
				.userId(getAuthenticatedUserId())
				.amount(request.getAmount())
				.newBalance(BigDecimal.ZERO)
				.status("FAILED")
				.build();
	}

	@Transactional
	@CircuitBreaker(name = "transactionService", fallbackMethod = "withdrawalFallback")
	public WithdrawalResponse withdrawal(WithdrawalRequest request) {
		Long userId = getAuthenticatedUserId();
		ensureAccountForUser(userId);

		TransactionProcessRequest processRequest = TransactionProcessRequest.builder()
				.userId(userId)
				.amount(request.getAmount())
				.build();

		log.info("Requesting withdrawal for userId: {}, amount: {}", userId, request.getAmount());
		TransactionProcessResponse response = transactionClient.processWithdrawal(processRequest);

		return WithdrawalResponse.builder()
				.transactionId(response.getTransactionId())
				.userId(userId)
				.amount(request.getAmount())
				.newBalance(response.getNewBalance())
				.status(response.getStatus())
				.createdAt(response.getCreatedAt())
				.build();
	}

	public WithdrawalResponse withdrawalFallback(WithdrawalRequest request, Throwable t) {
		log.error("Transaction Service is unavailable. Fallback executed for withdrawal. Error: {}", t.getMessage());
		return WithdrawalResponse.builder()
				.userId(getAuthenticatedUserId())
				.amount(request.getAmount())
				.newBalance(BigDecimal.ZERO)
				.status("FAILED")
				.build();
	}

	private Long getAuthenticatedUserId() {
		String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		return Long.parseLong(userIdStr);
	}
}
