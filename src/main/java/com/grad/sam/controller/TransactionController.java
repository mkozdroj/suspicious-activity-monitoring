package com.grad.sam.controller;

import com.grad.sam.dto.response.AlertSummaryDto;
import com.grad.sam.dto.response.ScreeningResultDto;
import com.grad.sam.dto.response.WatchlistMatchDto;
import com.grad.sam.enums.AlertSeverity;
import com.grad.sam.enums.WatchlistListType;
import com.grad.sam.exception.DataNotFoundException;
import com.grad.sam.model.Alert;
import com.grad.sam.model.Txn;
import com.grad.sam.model.WatchlistMatch;
import com.grad.sam.repository.AccountRepository;
import com.grad.sam.repository.CustomerRepository;
import com.grad.sam.repository.TxnRepository;
import com.grad.sam.repository.WatchlistMatchRepository;
import com.grad.sam.service.ScreenTransactionService;
import com.grad.sam.model.Customer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    // GET /api/v1/customers/{id}
    @GetMapping("/customers/{id}")
    public ResponseEntity<Customer> getCustomer(@PathVariable Integer id) {

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException(
                        "Customer not found for id: " + id));
        return ResponseEntity.ok(customer);
    }

    // GET /api/v1/accounts/{id}/transactions
    @GetMapping("/accounts/{id}/transactions")
    public ResponseEntity<List<Txn>> getAccountTransactions(@PathVariable Integer id) {

        accountRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException(
                        "Account not found for id: " + id));

        List<Txn> txns = txnRepository.findByAccount_AccountId(id);
        return ResponseEntity.ok(txns);
    }

    // POST /api/v1/transactions/screen
    @PostMapping("/transactions/screen")
    public ResponseEntity<ScreeningResultDto> screenTransaction(
            @RequestBody Map<String, Integer> body) {

        Integer txnId = body.get("txnId");
        if (txnId == null) {
            throw new IllegalArgumentException("Request body must contain 'txnId'");
        }

        List<Alert> alerts = screenTransactionService.screenTransaction(txnId);

        Txn txn = txnRepository.findById(txnId)
                .orElseThrow(() -> new DataNotFoundException(
                        "Transaction not found for id: " + txnId));

        String customerName = txn.getAccount() != null
                && txn.getAccount().getCustomer() != null
                ? txn.getAccount().getCustomer().getFullName()
                : "Unknown";

        List<AlertSummaryDto> alertSummaries = alerts.stream()
                .map(a -> new AlertSummaryDto(
                        a.getAlertId(),
                        a.getAlertRule() != null
                                ? a.getAlertRule().getRuleCategory() : null,
                        resolveAlertSeverity(a.getAlertScore()),
                        a.getStatus(),
                        a.getTriggeredAt()))
                .toList();

        List<WatchlistMatch> wlMatches =
                watchlistMatchRepository.findByTxn_TxnId(txnId);

        List<WatchlistMatchDto> wlDtos = wlMatches.stream()
                .map(m -> new WatchlistMatchDto(
                        m.getMatchId(),
                        m.getWatchlist() != null
                                ? m.getWatchlist().getWatchlistId() : null,
                        m.getWatchlist() != null
                                ? m.getWatchlist().getEntityName() : null,
                        m.getWatchlist() != null
                                ? m.getWatchlist().getEntityType() : null,
                        m.getWatchlist() != null
                                && m.getWatchlist().getListType() != null
                                ? WatchlistListType.valueOf(
                                m.getWatchlist().getListType().name()) : null,
                        m.getMatchScore() != null
                                ? m.getMatchScore().toBigInteger() : null,
                        m.getMatchType(),
                        m.getStatus()))
                .toList();

        ScreeningResultDto result = new ScreeningResultDto(
                txn.getTxnId(),
                txn.getTxnRef(),
                customerName,
                txn.getStatus(),
                alertSummaries.size() + wlDtos.size(),
                alertSummaries,
                wlDtos
        );

        return ResponseEntity.ok(result);
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private AlertSeverity resolveAlertSeverity(Short score) {
        if (score == null) return AlertSeverity.LOW;
        if (score >= 90) return AlertSeverity.CRITICAL;
        if (score >= 70) return AlertSeverity.HIGH;
        if (score >= 40) return AlertSeverity.MEDIUM;
        return AlertSeverity.LOW;
    }
}