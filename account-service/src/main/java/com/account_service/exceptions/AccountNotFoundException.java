package com.account_service.exceptions;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String accountNumber) {
        super("Konto nicht gefunden: " + accountNumber);
    }
}
