package com.msa.account_service.service;

import com.msa.account_service.domain.Account;
import com.msa.account_service.domain.Status;
import com.msa.account_service.domain.TransactionRecord;
import com.msa.account_service.dto.*;
import com.msa.account_service.event.TransactionEventPublisher;
import com.msa.account_service.repository.AccountRepository;
import com.msa.account_service.repository.TransactionRecordRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

	private static final String ACCOUNT_NUMBER_PREFIX = "ACC-";

	private final AccountRepository accountRepository;
	private final TransactionEventPublisher transactionEventPublisher;
	private final TransactionRecordRepository transactionRecordRepository;


	@Transactional
	public AccountResponse createAccount(CreateAccountRequest request) {
		// In a real app we might check if user already exists
		Account account = Account.createAccount(request.getUserId(),
				ACCOUNT_NUMBER_PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
		account.setBalance(new BigDecimal(request.getInitialBalance()));
		Account savedAccount = accountRepository.save(account);
		return AccountResponse.builder()
				.accountId(savedAccount.getId())
				.userId(savedAccount.getUserId())
				.balance(savedAccount.getBalance().intValue())
				.build();
	}

	@Transactional
	public Account ensureAccountForUser(String userId) {
		return accountRepository.findByUserId(userId)
				.orElseGet(() -> {
					Account account = Account.createAccount(userId,
							ACCOUNT_NUMBER_PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
					return accountRepository.save(account);
				});
	}

	@Transactional
	@CircuitBreaker(name = "natsPublish", fallbackMethod = "depositFallback")
	public DepositResponse deposit(DepositRequest request) {
		String userId = getAuthenticatedUserId();
		ensureAccountForUser(userId);

		TransactionRecord record = transactionRecordRepository.save(
				TransactionRecord.builder()
						.userId(userId)
						.amount(request.getAmount())
						.type("DEPOSIT")
						.status(Status.PENDING)
						.build());

		TransactionProcessRequest processRequest = TransactionProcessRequest.builder()
				.recordId(record.getId())
				.userId(userId)
				.amount(request.getAmount())
				.build();

		log.info("Requesting deposit for userId: {}, amount: {}", userId, request.getAmount());

        transactionEventPublisher.publishDeposit(processRequest);

		return DepositResponse.builder()
				.transactionId(record.getId())
                .userId(userId)
                .amount(request.getAmount())
                .newBalance(BigDecimal.ZERO)
                .status(Status.PENDING)
                .createdAt(record.getCreatedAt())
                .build();
	}

	public DepositResponse depositFallback(DepositRequest request, Throwable t) {
		log.error("Transaction Service is unavailable. Fallback executed for deposit. Error: {}", t.getMessage());
		return DepositResponse.builder()
				.userId(getAuthenticatedUserId())
				.amount(request.getAmount())
				.newBalance(BigDecimal.ZERO)
				.status(Status.FAILED)
				.build();
	}

	@Transactional
	@CircuitBreaker(name = "natsPublish", fallbackMethod = "withdrawalFallback")
	public WithdrawalResponse withdrawal(WithdrawalRequest request) {
		String userId = getAuthenticatedUserId();
		ensureAccountForUser(userId);

		TransactionRecord record = transactionRecordRepository.save(
				TransactionRecord.builder()
						.userId(userId)
						.amount(request.getAmount())
						.type("WITHDRAWAL")
						.status(Status.PENDING)
						.build());

		TransactionProcessRequest processRequest = TransactionProcessRequest.builder()
				.recordId(record.getId())
				.userId(userId)
				.amount(request.getAmount())
				.build();

		log.info("Requesting withdrawal for userId: {}, amount: {}", userId, request.getAmount());
        transactionEventPublisher.publishWithdrawal(processRequest);

		return WithdrawalResponse.builder()
				.transactionId(record.getId())
				.userId(userId)
				.amount(request.getAmount())
				.newBalance(BigDecimal.ZERO)
				.status(Status.PENDING)
				.createdAt(record.getCreatedAt())
				.build();
	}

	public WithdrawalResponse withdrawalFallback(WithdrawalRequest request, Throwable t) {
		log.error("Transaction Service is unavailable. Fallback executed for withdrawal. Error: {}", t.getMessage());
		return WithdrawalResponse.builder()
				.userId(getAuthenticatedUserId())
				.amount(request.getAmount())
				.newBalance(BigDecimal.ZERO)
				.status(Status.FAILED)
				.build();
	}

	private String getAuthenticatedUserId() {
		org.springframework.security.oauth2.jwt.Jwt jwt = (org.springframework.security.oauth2.jwt.Jwt) SecurityContextHolder
				.getContext().getAuthentication().getPrincipal();
		return jwt.getSubject();
	}
}
