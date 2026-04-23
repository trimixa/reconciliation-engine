package com.maang.reconciliation.consumer.repository;

import com.maang.reconciliation.consumer.model.Anomaly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// JpaRepository gives us built-in methods like .save(), .findAll(), and .findById() automatically!
@Repository
public interface AnomalyRepository extends JpaRepository<Anomaly, String> {
}