package com.grad.sam.repository;

import com.grad.sam.enums.RiskRating;
import com.grad.sam.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    Optional<Customer> findByCustomerRef(String customerRef);

    List<Customer> findByRiskRating(RiskRating riskRating);

    List<Customer> findByKycStatus(String kycStatus);

    List<Customer> findByIsActive(Boolean isActive);

    List<Customer> findByIsPep(Boolean isPep);

    List<Customer> findByNationality(String nationality);
}
