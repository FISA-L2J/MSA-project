package com.msa.transaction_service.service;

import com.msa.transaction_service.domain.Balance;
import com.msa.transaction_service.domain.Status;
import com.msa.transaction_service.domain.Transaction;
import com.msa.transaction_service.domain.TransactionType;
import com.msa.transaction_service.dto.TransactionProcessRequest;
import com.msa.transaction_service.dto.TransactionProcessResponse;
import com.msa.transaction_service.exception.InsufficientBalanceException;
import com.msa.transaction_service.repository.BalanceRepository;
import com.msa.transaction_service.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

	private final BalanceRepository balanceRepository;
	private final TransactionRepository transactionRepository;

	@Transactional
	public TransactionProcessResponse processDeposit(TransactionProcessRequest request) {
		Long userId = request.getUserId();
		BigDecimal amount = request.getAmount();

		log.info("Processing deposit for userId: {}, amount: {}", userId, amount);

		Balance balance = balanceRepository.findByUserId(userId)
				.orElseGet(() -> balanceRepository.save(Balance.createForUser(userId)));

		balance.add(amount);
		balanceRepository.save(balance);

		Transaction transaction = Transaction.create(userId, TransactionType.DEPOSIT, amount, balance.getBalance());
		Transaction savedTransaction = transactionRepository.save(transaction);

		log.info("Deposit completed. transactionId: {}, newBalance: {}", savedTransaction.getId(), balance.getBalance());

		return TransactionProcessResponse.builder()
				.recordId(request.getRecordId())
				.transactionId(savedTransaction.getId())
				.newBalance(balance.getBalance())
				.status(Status.SUCCESS)
				.createdAt(savedTransaction.getCreatedAt())
				.build();
	}

	@Transactional
	public TransactionProcessResponse processWithdrawal(TransactionProcessRequest request) {
		Long userId = request.getUserId();
		BigDecimal amount = request.getAmount();

		log.info("Processing withdrawal for userId: {}, amount: {}", userId, amount);

		Balance balance = balanceRepository.findByUserId(userId)
				.orElseGet(() -> balanceRepository.save(Balance.createForUser(userId)));

		if (balance.getBalance().compareTo(amount) < 0) {
			log.warn("Insufficient balance for userId: {}. balance: {}, requested: {}", userId, balance.getBalance(), amount);
			throw new InsufficientBalanceException("Insufficient balance. Current: " + balance.getBalance() + ", Requested: " + amount);
		}

		balance.subtract(amount);
		balanceRepository.save(balance);

		Transaction transaction = Transaction.create(userId, TransactionType.WITHDRAWAL, amount, balance.getBalance());
		Transaction savedTransaction = transactionRepository.save(transaction);

		log.info("Withdrawal completed. transactionId: {}, newBalance: {}", savedTransaction.getId(), balance.getBalance());

		return TransactionProcessResponse.builder()
				.recordId(request.getRecordId())
				.transactionId(savedTransaction.getId())
				.newBalance(balance.getBalance())
				.status(Status.SUCCESS)
				.createdAt(savedTransaction.getCreatedAt())
				.build();
	}
}
