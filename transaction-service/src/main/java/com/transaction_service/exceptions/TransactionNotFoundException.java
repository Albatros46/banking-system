package com.transaction_service.exceptions;

public class TransactionNotFoundException extends RuntimeException {
    public TransactionNotFoundException(String transactionId) {
        super("Transaktion nicht gefunden: " + transactionId);
    }
}
