package com.transaction_service.exceptions;

/*
    Transfer altyapi hatasi yuzunden baslatilamadi (orn. Kafka'ya ulasilamadi)
 */
public class TransferProcessingException extends RuntimeException {
    public TransferProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
