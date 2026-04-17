package com.grad.sam.repository;

import com.grad.sam.enums.InvestigationOutcome;
import com.grad.sam.enums.InvestigationState;
import com.grad.sam.enums.Priority;
import com.grad.sam.model.Investigation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvestigationRepository extends JpaRepository<Investigation, Integer> {

    Optional<Investigation> findByInvestigationRef(String investigationRef);

    Optional<Investigation> findByAlert_AlertId(Integer alertId);

    List<Investigation> findByCustomer_CustomerId(Integer customerId);

    List<Investigation> findByState(InvestigationState state);

    List<Investigation> findByOutcome(InvestigationOutcome outcome);

    List<Investigation> findByOpenedBy(String openedBy);

    List<Investigation> findByPriority(Priority priority);
}
