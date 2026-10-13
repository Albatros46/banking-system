package com.account_service.dtos.request;

import com.account_service.enums.AccountType;
import jakarta.validation.constraints.Email;
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
public class CreateAccountRequest {

    @NotBlank(message = "Name des Kontoinhabers ist erforderlich.")
    private String accountHolderName;

    @NotBlank(message = "E-Mail-Adresse ist erforderlich.")
    @Email(message = "Ungültiges E-Mail-Format.")
    private String email;

    @NotBlank(message = "Telefonnummer ist erforderlich.")
    private String phone;

    @NotNull(message = "Kontoart ist erforderlich.")
    private AccountType accountType;

    @NotNull(message = "Ersteinzahlung ist erforderlich.")
    @Positive(message = "Die Ersteinzahlung muss positiv sein.")
    private BigDecimal initialDeposit;

}
