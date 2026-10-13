package com.transaction_service.repositories;

import com.transaction_service.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, String> {

    List<Transaction> findByFromAccountNumberOrToAccountNumberOrderByCreatedAtDesc(String fromAccountNumber, String toAccountNumber);
}
