package com.account_service.entity;

import com.account_service.enums.AccountStatus;
import com.account_service.enums.AccountType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
//@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name="accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column(nullable = false,unique = true)
    private String accountNumber;
    @Column(nullable = false)
    private String accountHolderName;
    @Column(nullable = false)
    private String email;
    @Column(nullable = false)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal dailyTransactionLimit;
    // Bugun gonderilen toplam tutar. Gun degisince sifirlanir (bkz. lastTransactionDate)
    @Column(precision = 15, scale = 2)
    private BigDecimal dailyTransactionTotal;
    private LocalDate lastTransactionDate;
    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
