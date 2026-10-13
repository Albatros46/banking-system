package com.account_service.exceptions;

/*
    Hesap aktif degil, bakiye yetersiz, gecersiz tutar gibi is kurali ihlalleri
 */
public class InvalidAccountOperationException extends RuntimeException {
    public InvalidAccountOperationException(String message) {
        super(message);
    }
}
