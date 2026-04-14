package com.grad.sam.rules;

import com.grad.sam.model.Account;
import com.grad.sam.model.Txn;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class RuleContext {
    Txn txn;
    Account account;
    List<Txn> recentTxns;
}