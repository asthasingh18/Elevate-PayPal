package com.paybridge.repository;

import com.paybridge.entity.ProviderEvidenceEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProviderEvidenceRepository
        extends JpaRepository<ProviderEvidenceEntity, Long> {

    @EntityGraph(attributePaths = {"evidenceItems"})
    Optional<ProviderEvidenceEntity> findByProviderKeyAndSupportedCountry(
            String providerKey,
            String supportedCountry
    );
}
