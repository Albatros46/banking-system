package com.transaction_service.dtos.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransferRequest {

    @NotBlank(message = "Auftraggeberkonto ist erforderlich.")
    private String fromAccountNumber;

    @NotBlank(message = "Empfängerkonto ist erforderlich.")
    private String toAccountNumber;

    @NotNull(message = "Betrag ist erforderlich.")
    @Positive(message = "Der Betrag muss positiv sein.")
    @Digits(integer = 13, fraction = 2, message = "Der Betrag darf höchstens 2 Nachkommastellen haben.")
    @DecimalMax(value = "9999999999999.99", message = "Der Betrag ist zu hoch.")
    private BigDecimal amount;
}
