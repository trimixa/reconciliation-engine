package com.maang.reconciliation.consumer.infrastructure.persistence;

import com.maang.reconciliation.consumer.domain.Anomaly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// JpaRepository gives us built-in methods like .save(), .findAll(), and .findById() automatically!
@Repository
public interface AnomalyRepository extends JpaRepository<Anomaly, String> {
    long countByStatus(String status);
}