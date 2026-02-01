package com.msa.account_service.repository;

import com.msa.account_service.domain.TransactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRecordRepository extends JpaRepository<TransactionRecord, Long> {

    List<TransactionRecord> findByUserId(Long userId);
}
