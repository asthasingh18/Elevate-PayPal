package com.paybridge.service;

import com.paybridge.dto.EvidenceItem;
import com.paybridge.entity.EvidenceItemEntity;
import com.paybridge.entity.ProviderEvidenceEntity;
import com.paybridge.repository.ProviderEvidenceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of documented provider capabilities and bilateral agreements.
 *
 * IMPORTANT LIMITATIONS:
 * - A test fixture does not establish real-world provider support.
 * - Compatibility evaluations reflect documented technical and regulatory capabilities only.
 */
@Component
public class EvidenceStore {

    public record ProviderEvidenceRecord(
            String providerKey,
            String supportedCountry, // uppercase 2-letter ISO country code or "*" for all
            boolean coversQrPaymentScenario,
            boolean activeAndValid,
            boolean conflicting,
            String conditions, // null if fully compatible
            List<EvidenceItem> evidenceItems
    ) {
    }

    private final ProviderEvidenceRepository repository;
    private final Map<String, ProviderEvidenceRecord> store = new ConcurrentHashMap<>();

    /**
     * Default constructor for unit tests without database.
     * Operates in-memory and loads default fixtures into memory.
     */
    public EvidenceStore() {
        this(null);
    }

    /**
     * Spring-injected constructor for database-backed persistence.
     */
    @Autowired
    public EvidenceStore(@Autowired(required = false) ProviderEvidenceRepository repository) {
        this.repository = repository;
        if (this.repository != null) {
            initDatabaseFixtures();
        } else {
            loadDefaultDocumentedFixtures();
        }
    }

    public synchronized void initDatabaseFixtures() {
        if (repository == null) {
            return;
        }
        for (ProviderEvidenceRecord fixture : getDefaultFixtures()) {
            String normProvider = fixture.providerKey().trim().toLowerCase();
            String normCountry = fixture.supportedCountry().trim().toUpperCase();
            if (repository.findByProviderKeyAndSupportedCountry(normProvider, normCountry).isEmpty()) {
                ProviderEvidenceEntity entity = new ProviderEvidenceEntity(
                        normProvider,
                        normCountry,
                        fixture.coversQrPaymentScenario(),
                        fixture.activeAndValid(),
                        fixture.conflicting(),
                        fixture.conditions()
                );
                if (fixture.evidenceItems() != null) {
                    for (EvidenceItem item : fixture.evidenceItems()) {
                        EvidenceItemEntity itemEntity = new EvidenceItemEntity(
                                item.source(),
                                item.description(),
                                item.supportLevel(),
                                item.valid(),
                                entity
                        );
                        entity.addEvidenceItem(itemEntity);
                    }
                }
                repository.save(entity);
            }
        }
    }

    private List<ProviderEvidenceRecord> getDefaultFixtures() {
        return List.of(
                // COMP-01: Documented bilateral linkage with Singapore (SG) for upi-global
                new ProviderEvidenceRecord(
                        "upi-global",
                        "SG",
                        true,
                        true,
                        false,
                        null,
                        List.of(new EvidenceItem(
                                "NPCI International - PayNow Bilateral Agreement 2023",
                                "Direct bilateral linkage confirms routing support between Singapore and Indian UPI merchants.",
                                "SUPPORTED",
                                true
                        ))
                ),
                // COMP-02: Conditional support requiring acquiring bank enablement (France / FR)
                new ProviderEvidenceRecord(
                        "rupay-global",
                        "FR",
                        true,
                        true,
                        false,
                        "Merchant terminal acquiring bank must have international UPI acceptance enabled; payer bank must permit cross-border debit.",
                        List.of(new EvidenceItem(
                                "Lyra Network & NPCI International Agreement",
                                "Cross-border acceptance enabled conditionally for enrolled merchant acquiring terminals.",
                                "CONDITIONAL",
                                true
                        ))
                ),
                // COMP-04: Historical document that is expired / outdated
                new ProviderEvidenceRecord(
                        "legacy-provider",
                        "*",
                        true,
                        false,
                        false,
                        null,
                        List.of(new EvidenceItem(
                                "Pilot Cross-Border Memorandum 2021",
                                "Memorandum of understanding expired in 2023. No current bilateral routing agreement active.",
                                "UNSUPPORTED",
                                false
                        ))
                ),
                // COMP-05: Provider capability covers e-commerce only, not in-person UPI QR payments
                new ProviderEvidenceRecord(
                        "ecommerce-only-provider",
                        "*",
                        false,
                        true,
                        false,
                        null,
                        List.of(new EvidenceItem(
                                "Digital Wallet E-Commerce Gateway Guidelines",
                                "Provider supports web checkout redirects, but does not support in-person UPI QR payment routing.",
                                "UNSUPPORTED",
                                true
                        ))
                ),
                // COMP-06: Conflicting evidence sources
                new ProviderEvidenceRecord(
                        "conflict-provider",
                        "*",
                        true,
                        true,
                        true,
                        null,
                        List.of(
                                new EvidenceItem(
                                        "Bilateral Routing Technical Specification 2024",
                                        "Technical protocol specification defined for merchant settlement.",
                                        "SUPPORTED",
                                        true
                                ),
                                new EvidenceItem(
                                        "Foreign Exchange Regulatory Advisory 2025",
                                        "Cross-border merchant settlement suspended pending compliance review.",
                                        "UNSUPPORTED",
                                        true
                                )
                        )
                )
        );
    }

    private void loadDefaultDocumentedFixtures() {
        for (ProviderEvidenceRecord record : getDefaultFixtures()) {
            registerRecord(record);
        }
    }

    @Transactional
    public void registerRecord(ProviderEvidenceRecord record) {
        if (record == null) {
            return;
        }
        String normProvider = record.providerKey().trim().toLowerCase();
        String normCountry = record.supportedCountry().trim().toUpperCase();

        if (repository != null) {
            Optional<ProviderEvidenceEntity> existingOpt = repository.findByProviderKeyAndSupportedCountry(normProvider, normCountry);
            ProviderEvidenceEntity entity;
            if (existingOpt.isPresent()) {
                entity = existingOpt.get();
                entity.update(
                        record.coversQrPaymentScenario(),
                        record.activeAndValid(),
                        record.conflicting(),
                        record.conditions()
                );
                entity.getEvidenceItems().clear();
            } else {
                entity = new ProviderEvidenceEntity(
                        normProvider,
                        normCountry,
                        record.coversQrPaymentScenario(),
                        record.activeAndValid(),
                        record.conflicting(),
                        record.conditions()
                );
            }
            if (record.evidenceItems() != null) {
                for (EvidenceItem item : record.evidenceItems()) {
                    EvidenceItemEntity itemEntity = new EvidenceItemEntity(
                            item.source(),
                            item.description(),
                            item.supportLevel(),
                            item.valid(),
                            entity
                    );
                    entity.addEvidenceItem(itemEntity);
                }
            }
            repository.save(entity);
        }

        String key = buildKey(normProvider, normCountry);
        store.put(key, record);
    }

    @Transactional(readOnly = true)
    public Optional<ProviderEvidenceRecord> findEvidence(String provider, String country) {
        if (provider == null) {
            return Optional.empty();
        }
        String normalizedProvider = provider.trim().toLowerCase();
        String normalizedCountry = country != null ? country.trim().toUpperCase() : "";

        if (repository != null) {
            // First check exact provider + country match
            Optional<ProviderEvidenceEntity> exact = repository.findByProviderKeyAndSupportedCountry(normalizedProvider, normalizedCountry);
            if (exact.isPresent()) {
                return Optional.of(mapToRecord(exact.get()));
            }

            // Fallback to wildcard country match
            Optional<ProviderEvidenceEntity> wildcard = repository.findByProviderKeyAndSupportedCountry(normalizedProvider, "*");
            if (wildcard.isPresent()) {
                return Optional.of(mapToRecord(wildcard.get()));
            }

            return Optional.empty();
        }

        // In-memory fallback
        String exactKey = buildKey(normalizedProvider, normalizedCountry);
        if (store.containsKey(exactKey)) {
            return Optional.of(store.get(exactKey));
        }

        String wildcardKey = buildKey(normalizedProvider, "*");
        if (store.containsKey(wildcardKey)) {
            return Optional.of(store.get(wildcardKey));
        }

        return Optional.empty();
    }

    private ProviderEvidenceRecord mapToRecord(ProviderEvidenceEntity entity) {
        List<EvidenceItem> items = new ArrayList<>();
        if (entity.getEvidenceItems() != null) {
            for (EvidenceItemEntity itemEntity : entity.getEvidenceItems()) {
                items.add(new EvidenceItem(
                        itemEntity.getSource(),
                        itemEntity.getDescription(),
                        itemEntity.getSupportLevel(),
                        itemEntity.isValid()
                ));
            }
        }
        return new ProviderEvidenceRecord(
                entity.getProviderKey(),
                entity.getSupportedCountry(),
                entity.isCoversQrPaymentScenario(),
                entity.isActiveAndValid(),
                entity.isConflicting(),
                entity.getConditions(),
                Collections.unmodifiableList(items)
        );
    }

    private String buildKey(String provider, String country) {
        return provider.trim().toLowerCase() + "::" + country.trim().toUpperCase();
    }

    public void resetToDefaults() {
        store.clear();
        loadDefaultDocumentedFixtures();
        if (repository != null) {
            initDatabaseFixtures();
        }
    }
}
