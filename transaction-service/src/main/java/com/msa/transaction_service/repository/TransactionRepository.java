package com.msa.transaction_service.repository;

import com.msa.transaction_service.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
