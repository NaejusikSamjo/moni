package com.moni.trade.account.domain.repository;

import com.moni.trade.account.domain.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    Optional<Account> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}
