package com.paybridge.repository;

import com.paybridge.entity.CompatibilityEvaluationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompatibilityEvaluationRepository
        extends JpaRepository<CompatibilityEvaluationEntity, String> {

    List<CompatibilityEvaluationEntity> findAllByOrderByEvaluatedAtDesc();
}
