package com.grad.sam.service;

import com.grad.sam.enums.TxnStatus;
import com.grad.sam.exception.DataNotFoundException;
import com.grad.sam.model.Txn;
import com.grad.sam.repository.TxnRepository;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class TxnService {

    private final TxnRepository txnRepository;

    @Transactional
    public Txn saveTxn(@NotNull Txn txn) {
        Txn saved = txnRepository.save(txn);
        log.info("Saved txn: {} (id: {})", saved.getTxnRef(), saved.getTxnId());
        return saved;
    }

    @Transactional(readOnly = true)
    public String getStatus(@NotNull @Positive Integer txnId) {
        return txnRepository.findById(txnId)
                .map(txn -> txn.getStatus().name())
                .orElseThrow(() -> new DataNotFoundException("Transaction not found for id: " + txnId));
    }

    @Transactional
    public void updateTxnStatus(@NotNull @Positive Integer txnId,
                                @NotNull TxnStatus status) {
        int rows = txnRepository.updateStatus(txnId, status);
        if (rows == 0) {
            throw new DataNotFoundException("Transaction not found for id: " + txnId);
        }
        log.info("Updated txn {} status to {}", txnId, status);
    }
}
