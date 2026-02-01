package com.msa.account_service.repository;

import com.msa.account_service.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

	Optional<Account> findByUserId(String userId);
}
