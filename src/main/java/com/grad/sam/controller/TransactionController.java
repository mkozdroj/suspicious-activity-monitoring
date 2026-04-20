package com.grad.sam.controller;

import com.grad.sam.enums.AlertSeverity;
import com.grad.sam.exception.DataNotFoundException;
import com.grad.sam.model.Customer;
import com.grad.sam.model.Txn;
import com.grad.sam.repository.AccountRepository;
import com.grad.sam.repository.CustomerRepository;
import com.grad.sam.repository.TxnRepository;
import com.grad.sam.repository.WatchlistMatchRepository;
import com.grad.sam.service.ScreenTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class TransactionController {

    private final ScreenTransactionService screenTransactionService;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final TxnRepository txnRepository;
    private final WatchlistMatchRepository watchlistMatchRepository;

    @GetMapping("/customers/{id}")
    public ResponseEntity<Customer> getCustomer(@PathVariable Integer id) {

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException(
                        "Customer not found for id: " + id));
        return ResponseEntity.ok(customer);
    }

    @GetMapping("/accounts/{id}/transactions")
    public ResponseEntity<List<Txn>> getAccountTransactions(@PathVariable Integer id) {

        accountRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException(
                        "Account not found for id: " + id));

        List<Txn> txns = txnRepository.findByAccount_AccountId(id);
        return ResponseEntity.ok(txns);
    }

    private AlertSeverity resolveAlertSeverity(Short score) {
        if (score == null) return AlertSeverity.LOW;
        if (score >= 90) return AlertSeverity.CRITICAL;
        if (score >= 70) return AlertSeverity.HIGH;
        if (score >= 40) return AlertSeverity.MEDIUM;
        return AlertSeverity.LOW;
    }
}
