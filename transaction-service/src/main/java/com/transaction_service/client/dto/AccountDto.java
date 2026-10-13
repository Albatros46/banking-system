package com.transaction_service.client.dto;

/*
    account-service AccountResponse'un burada ihtiyac duyulan alanlari
 */
public record AccountDto(
        String accountNumber,
        String accountHolderName,
        String status
) {
}
