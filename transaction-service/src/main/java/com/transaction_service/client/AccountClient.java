package com.transaction_service.client;

import com.transaction_service.client.dto.AccountDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

/*
    account-service REST endpoint'leri (SAGA adimlari)
 */
@FeignClient(name = "account-service", url = "${app.services.account-service.url}", path = "/api/v1/accounts")
public interface AccountClient {

    @GetMapping("/{accountNumber}")
    AccountDto getAccount(@PathVariable("accountNumber") String accountNumber);

    // SAGA Step 1
    @PutMapping("/{accountNumber}/deduct")
    String deductBalance(@PathVariable("accountNumber") String accountNumber, @RequestParam("amount") BigDecimal amount);

    // SAGA Step 4
    @PutMapping("/{accountNumber}/credit")
    String creditBalance(@PathVariable("accountNumber") String accountNumber, @RequestParam("amount") BigDecimal amount);

    // SAGA compensation
    @PutMapping("/{accountNumber}/refund")
    String refundBalance(@PathVariable("accountNumber") String accountNumber, @RequestParam("amount") BigDecimal amount);
}
