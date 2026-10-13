package com.account_service.controller;

import com.account_service.dtos.request.CreateAccountRequest;
import com.account_service.dtos.response.AccountResponse;
import com.account_service.services.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.createAccount(request));
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getAccount(accountNumber));
    }

    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getBalance(accountNumber));
    }

    @PutMapping("/{accountNumber}/block")
    public ResponseEntity<String> blockAccount(@PathVariable String accountNumber) {
        accountService.blockAccount(accountNumber);
        return ResponseEntity.ok("Konto erfolgreich gesperrt.");
    }
    /*
    * SAGA Step 1 - Deduct Balance
    * Called by Transaction Service when transfer is initiated
    * */
    @PutMapping("/{accountNumber}/deduct")
    public ResponseEntity<String> deductBalance(@PathVariable String accountNumber, @RequestParam BigDecimal amount) {
        accountService.deductBalance(accountNumber,amount);
        return ResponseEntity.ok("Betrag erfolgreich abgebucht.");
    }
    /*
    SAGA Step 4 - Transaction completed -> Credit receiver
    Called by Transaction Service
     */
    @PutMapping("/{accountNumber}/credit")
    public ResponseEntity<String> creditBalance(@PathVariable String accountNumber, @RequestParam BigDecimal amount) {
        accountService.creditBalance(accountNumber,amount);
        return ResponseEntity.ok("Betrag erfolgreich gutgeschrieben.");
    }
    /*
    SAGA Compensating transaction endpoint
    Called by Transaction Service
    Fraud detected / credit failed -> refund sender (undo step 1, daily usage is restored)
     */
    @PutMapping("/{accountNumber}/refund")
    public ResponseEntity<String> refundBalance(@PathVariable String accountNumber, @RequestParam BigDecimal amount) {
        accountService.refundBalance(accountNumber,amount);
        return ResponseEntity.ok("Betrag erfolgreich zurückerstattet.");
    }
}
