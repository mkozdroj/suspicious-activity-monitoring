package com.grad.sam.repository;

import com.grad.sam.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Integer> {

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByCustomer_CustomerId(Integer customerId);
}
