package com.account_service.exceptions;

public class AccountAlreadyExistsException extends RuntimeException {
    public AccountAlreadyExistsException(String email) {
        super("Für diese E-Mail-Adresse existiert bereits ein Konto: " + email);
    }
}
