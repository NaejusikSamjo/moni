package com.moni.trade.account.domain.entity;

import com.moni.trade.global.entity.TradeBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "account")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account extends TradeBaseEntity {

    @Column(nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal balance;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalInvestment;

    @Version
    private Long version;

    public static Account create(UUID userId, BigDecimal initialBalance) {
        Account account = new Account();
        account.userId = userId;
        account.balance = initialBalance;
        account.totalInvestment = BigDecimal.ZERO;
        return account;
    }

    public void deductBalance(BigDecimal amount) {
        this.balance = this.balance.subtract(amount);
        this.totalInvestment = this.totalInvestment.add(amount);
    }

    public void addBalance(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }
}
