package com.paybridge.service;

import com.paybridge.dto.CompatibilityEvaluationRequest;
import com.paybridge.dto.CompatibilityEvaluationResponse;
import com.paybridge.dto.CompatibilityStatus;
import com.paybridge.dto.EvidenceItem;
import com.paybridge.dto.QrParseResponse;
import com.paybridge.entity.CompatibilityEvaluationEntity;
import com.paybridge.entity.EvaluationEvidenceEntity;
import com.paybridge.entity.EvaluationLimitationEntity;
import com.paybridge.entity.EvidenceItemEntity;
import com.paybridge.entity.ProviderEvidenceEntity;
import com.paybridge.repository.CompatibilityEvaluationRepository;
import com.paybridge.repository.ProviderEvidenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests covering all 11 Day 9 persistence and repository interaction scenarios:
 * 1. Provider evidence persistence
 * 2. Evidence item persistence and ordering
 * 3. Exact-country lookup
 * 4. Wildcard-country fallback
 * 5. Default fixture initialization without duplicates
 * 6. Evaluation persistence
 * 7. Evaluation evidence persistence
 * 8. Evaluation limitation persistence and ordering
 * 9. Evaluation retrieval by ID
 * 10. Evaluation history sorting
 * 11. Database restart / reload persistence
 */
public class PersistenceScenariosTest {

    private Map<String, ProviderEvidenceEntity> providerTable;
    private Map<String, CompatibilityEvaluationEntity> evaluationTable;
    private ProviderEvidenceRepository providerEvidenceRepository;
    private CompatibilityEvaluationRepository evaluationRepository;
    private QrParserService qrParserService;

    private static final String SAMPLE_QR = "upi://pay?pa=merchant@icici&pn=Test%20Merchant&am=150.00&cu=INR";

    @BeforeEach
    void setUp() {
        providerTable = new HashMap<>();
        evaluationTable = new ConcurrentHashMap<>();

        providerEvidenceRepository = mock(ProviderEvidenceRepository.class);
        when(providerEvidenceRepository.findByProviderKeyAndSupportedCountry(anyString(), anyString()))
                .thenAnswer(inv -> {
                    String p = inv.getArgument(0);
                    String c = inv.getArgument(1);
                    return Optional.ofNullable(providerTable.get(p.trim().toLowerCase() + "::" + c.trim().toUpperCase()));
                });
        when(providerEvidenceRepository.save(any(ProviderEvidenceEntity.class)))
                .thenAnswer(inv -> {
                    ProviderEvidenceEntity e = inv.getArgument(0);
                    providerTable.put(e.getProviderKey().trim().toLowerCase() + "::" + e.getSupportedCountry().trim().toUpperCase(), e);
                    return e;
                });
        when(providerEvidenceRepository.findAll())
                .thenAnswer(inv -> new ArrayList<>(providerTable.values()));

        evaluationRepository = mock(CompatibilityEvaluationRepository.class);
        when(evaluationRepository.save(any(CompatibilityEvaluationEntity.class)))
                .thenAnswer(inv -> {
                    CompatibilityEvaluationEntity e = inv.getArgument(0);
                    evaluationTable.put(e.getEvaluationId(), e);
                    return e;
                });
        when(evaluationRepository.findById(anyString()))
                .thenAnswer(inv -> {
                    String id = inv.getArgument(0);
                    return Optional.ofNullable(evaluationTable.get(id.trim()));
                });
        when(evaluationRepository.findAll())
                .thenAnswer(inv -> new ArrayList<>(evaluationTable.values()));
        when(evaluationRepository.findAllByOrderByEvaluatedAtDesc())
                .thenAnswer(inv -> {
                    List<CompatibilityEvaluationEntity> list = new ArrayList<>(evaluationTable.values());
                    list.sort((a, b) -> b.getEvaluatedAt().compareTo(a.getEvaluatedAt()));
                    return list;
                });

        qrParserService = mock(QrParserService.class);
        Map<String, String> qrFields = new HashMap<>();
        qrFields.put("pa", "merchant@icici");
        qrFields.put("pn", "Test Merchant");
        qrFields.put("am", "150.00");
        qrFields.put("cu", "INR");
        when(qrParserService.parse(anyString()))
                .thenReturn(new QrParseResponse(
                        true,
                        qrFields,
                        List.of()
                ));
    }

    @Test
    @DisplayName("Scenario 1: Provider evidence persistence")
    void test01_providerEvidencePersistence() {
        ProviderEvidenceEntity entity = new ProviderEvidenceEntity(
                "custom-provider",
                "US",
                true,
                true,
                false,
                "Requires US merchant terminal registration."
        );

        assertNull(entity.getId(), "Generated ID is initially null before DB insertion");
        assertEquals("custom-provider", entity.getProviderKey());
        assertEquals("US", entity.getSupportedCountry());
        assertTrue(entity.isCoversQrPaymentScenario());
        assertTrue(entity.isActiveAndValid());
        assertFalse(entity.isConflicting());
        assertEquals("Requires US merchant terminal registration.", entity.getConditions());
        assertNotNull(entity.getEvidenceItems());
        assertTrue(entity.getEvidenceItems().isEmpty());

        providerEvidenceRepository.save(entity);

        Optional<ProviderEvidenceEntity> loaded = providerEvidenceRepository.findByProviderKeyAndSupportedCountry("custom-provider", "US");
        assertTrue(loaded.isPresent());
        assertEquals("custom-provider", loaded.get().getProviderKey());
        assertEquals("US", loaded.get().getSupportedCountry());
        assertTrue(loaded.get().isCoversQrPaymentScenario());
        assertTrue(loaded.get().isActiveAndValid());
        assertFalse(loaded.get().isConflicting());
    }

    @Test
    @DisplayName("Scenario 2: Evidence item persistence and ordering")
    void test02_evidenceItemPersistenceAndOrdering() {
        ProviderEvidenceEntity entity = new ProviderEvidenceEntity(
                "multi-evidence-provider",
                "DE",
                true,
                true,
                false,
                null
        );

        EvidenceItemEntity item1 = new EvidenceItemEntity(
                "Source A",
                "First documented source item",
                "SUPPORTED",
                true,
                null
        );
        EvidenceItemEntity item2 = new EvidenceItemEntity(
                "Source B",
                "Second documented source item",
                "SUPPORTED",
                true,
                null
        );

        entity.addEvidenceItem(item1);
        entity.addEvidenceItem(item2);

        // Verify bidirectional relationship
        assertSame(entity, item1.getProviderEvidence());
        assertSame(entity, item2.getProviderEvidence());
        assertEquals(2, entity.getEvidenceItems().size());

        // Verify ordering: maintains list order
        assertEquals("Source A", entity.getEvidenceItems().get(0).getSource());
        assertEquals("Source B", entity.getEvidenceItems().get(1).getSource());

        // Test removal
        entity.removeEvidenceItem(item1);
        assertNull(item1.getProviderEvidence());
        assertEquals(1, entity.getEvidenceItems().size());
        assertEquals("Source B", entity.getEvidenceItems().get(0).getSource());
    }

    @Test
    @DisplayName("Scenario 3: Exact-country lookup")
    void test03_exactCountryLookup() {
        EvidenceStore evidenceStore = new EvidenceStore(providerEvidenceRepository);

        // Register exact SG and wildcard * for upi-global
        evidenceStore.registerRecord(new EvidenceStore.ProviderEvidenceRecord(
                "test-exact",
                "SG",
                true,
                true,
                false,
                null,
                List.of(new EvidenceItem("SG Source", "Exact SG Route", "SUPPORTED", true))
        ));
        evidenceStore.registerRecord(new EvidenceStore.ProviderEvidenceRecord(
                "test-exact",
                "*",
                true,
                false,
                false,
                null,
                List.of(new EvidenceItem("Wildcard Source", "Fallback Route", "UNSUPPORTED", false))
        ));

        // When queried for SG, it must return exact SG match
        Optional<EvidenceStore.ProviderEvidenceRecord> result = evidenceStore.findEvidence("test-exact", "SG");
        assertTrue(result.isPresent());
        assertEquals("SG", result.get().supportedCountry());
        assertTrue(result.get().activeAndValid());
        assertEquals("SG Source", result.get().evidenceItems().get(0).source());
    }

    @Test
    @DisplayName("Scenario 4: Wildcard-country fallback")
    void test04_wildcardCountryFallback() {
        EvidenceStore evidenceStore = new EvidenceStore(providerEvidenceRepository);

        evidenceStore.registerRecord(new EvidenceStore.ProviderEvidenceRecord(
                "wildcard-only-provider",
                "*",
                true,
                true,
                false,
                null,
                List.of(new EvidenceItem("Global MoU", "Global routing specification", "SUPPORTED", true))
        ));

        // When queried for specific country JP (no exact record), should fallback to wildcard *
        Optional<EvidenceStore.ProviderEvidenceRecord> result = evidenceStore.findEvidence("wildcard-only-provider", "JP");
        assertTrue(result.isPresent());
        assertEquals("*", result.get().supportedCountry());
        assertEquals("Global MoU", result.get().evidenceItems().get(0).source());

        // When queried for a completely unknown provider, should return empty
        Optional<EvidenceStore.ProviderEvidenceRecord> unknown = evidenceStore.findEvidence("non-existent-provider", "JP");
        assertTrue(unknown.isEmpty());
    }

    @Test
    @DisplayName("Scenario 5: Default fixture initialization without duplicates")
    void test05_defaultFixtureInitializationWithoutDuplicates() {
        // Initial setup creates the 5 default fixtures
        EvidenceStore evidenceStore = new EvidenceStore(providerEvidenceRepository);
        assertEquals(5, providerTable.size(), "Initial database initialization should store 5 fixtures");

        // Re-run initialization multiple times
        evidenceStore.initDatabaseFixtures();
        evidenceStore.initDatabaseFixtures();

        // Count should remain exactly 5, no duplicate records inserted
        assertEquals(5, providerTable.size(), "Multiple initializations must be idempotent");

        // Confirm expected default fixtures exist
        assertTrue(providerEvidenceRepository.findByProviderKeyAndSupportedCountry("upi-global", "SG").isPresent());
        assertTrue(providerEvidenceRepository.findByProviderKeyAndSupportedCountry("rupay-global", "FR").isPresent());
        assertTrue(providerEvidenceRepository.findByProviderKeyAndSupportedCountry("legacy-provider", "*").isPresent());
        assertTrue(providerEvidenceRepository.findByProviderKeyAndSupportedCountry("ecommerce-only-provider", "*").isPresent());
        assertTrue(providerEvidenceRepository.findByProviderKeyAndSupportedCountry("conflict-provider", "*").isPresent());
    }

    @Test
    @DisplayName("Scenario 6: Evaluation persistence")
    void test06_evaluationPersistence() {
        EvidenceStore evidenceStore = new EvidenceStore(providerEvidenceRepository);
        CompatibilityEngineService engine = new CompatibilityEngineService(qrParserService, evidenceStore, evaluationRepository);

        CompatibilityEvaluationRequest request = new CompatibilityEvaluationRequest(
                SAMPLE_QR,
                "SG",
                "upi-global"
        );

        CompatibilityEvaluationResponse response = engine.evaluate(request);

        assertNotNull(response.evaluationId());
        assertEquals(CompatibilityStatus.COMPATIBLE, response.status());

        // Verify entity was saved in evaluationRepository
        Optional<CompatibilityEvaluationEntity> savedOpt = evaluationRepository.findById(response.evaluationId());
        assertTrue(savedOpt.isPresent());
        CompatibilityEvaluationEntity saved = savedOpt.get();
        assertEquals(response.evaluationId(), saved.getEvaluationId());
        assertEquals(CompatibilityStatus.COMPATIBLE, saved.getStatus());
        assertEquals(response.reason(), saved.getReason());
        assertEquals(response.evaluatedAt(), saved.getEvaluatedAt());
    }

    @Test
    @DisplayName("Scenario 7: Evaluation evidence persistence")
    void test07_evaluationEvidencePersistence() {
        EvidenceStore evidenceStore = new EvidenceStore(providerEvidenceRepository);
        CompatibilityEngineService engine = new CompatibilityEngineService(qrParserService, evidenceStore, evaluationRepository);

        CompatibilityEvaluationRequest request = new CompatibilityEvaluationRequest(
                SAMPLE_QR,
                "SG",
                "upi-global"
        );

        CompatibilityEvaluationResponse response = engine.evaluate(request);
        CompatibilityEvaluationEntity entity = evaluationTable.get(response.evaluationId());
        assertNotNull(entity);

        assertFalse(entity.getEvidence().isEmpty());
        EvaluationEvidenceEntity evidenceEntity = entity.getEvidence().get(0);
        assertEquals("NPCI International - PayNow Bilateral Agreement 2023", evidenceEntity.getSource());
        assertEquals("SUPPORTED", evidenceEntity.getSupportLevel());
        assertTrue(evidenceEntity.isValid());
        assertSame(entity, evidenceEntity.getEvaluation());

        // Test addEvidence and removeEvidence helper methods
        EvaluationEvidenceEntity extra = new EvaluationEvidenceEntity(
                "Source C", "Description C", "SUPPORTED", true, null
        );
        entity.addEvidence(extra);
        assertSame(entity, extra.getEvaluation());
        assertTrue(entity.getEvidence().contains(extra));

        entity.removeEvidence(extra);
        assertNull(extra.getEvaluation());
        assertFalse(entity.getEvidence().contains(extra));
    }

    @Test
    @DisplayName("Scenario 8: Evaluation limitation persistence and ordering")
    void test08_evaluationLimitationPersistenceAndOrdering() {
        EvidenceStore evidenceStore = new EvidenceStore(providerEvidenceRepository);
        CompatibilityEngineService engine = new CompatibilityEngineService(qrParserService, evidenceStore, evaluationRepository);

        CompatibilityEvaluationRequest request = new CompatibilityEvaluationRequest(
                SAMPLE_QR,
                "FR",
                "rupay-global"
        );

        CompatibilityEvaluationResponse response = engine.evaluate(request);
        CompatibilityEvaluationEntity entity = evaluationTable.get(response.evaluationId());
        assertNotNull(entity);

        List<EvaluationLimitationEntity> limitations = entity.getLimitations();
        assertFalse(limitations.isEmpty());

        // Verify sequential ordering
        for (int i = 0; i < limitations.size(); i++) {
            assertEquals(i, limitations.get(i).getItemOrder());
            assertSame(entity, limitations.get(i).getEvaluation());
            assertEquals(response.limitations().get(i), limitations.get(i).getLimitation());
        }

        // Test addLimitation and removeLimitation helper methods
        EvaluationLimitationEntity customLim = new EvaluationLimitationEntity(
                "Additional custom test limitation.",
                limitations.size(),
                null
        );
        entity.addLimitation(customLim);
        assertSame(entity, customLim.getEvaluation());
        assertTrue(entity.getLimitations().contains(customLim));

        entity.removeLimitation(customLim);
        assertNull(customLim.getEvaluation());
        assertFalse(entity.getLimitations().contains(customLim));
    }

    @Test
    @DisplayName("Scenario 9: Evaluation retrieval by ID")
    void test09_evaluationRetrievalById() {
        EvidenceStore evidenceStore = new EvidenceStore(providerEvidenceRepository);
        CompatibilityEngineService engine = new CompatibilityEngineService(qrParserService, evidenceStore, evaluationRepository);

        CompatibilityEvaluationResponse created = engine.evaluate(new CompatibilityEvaluationRequest(
                SAMPLE_QR,
                "SG",
                "upi-global"
        ));

        // Retrieve by ID
        Optional<CompatibilityEvaluationResponse> retrievedOpt = engine.findEvaluationById(created.evaluationId());
        assertTrue(retrievedOpt.isPresent());
        CompatibilityEvaluationResponse retrieved = retrievedOpt.get();

        assertEquals(created.evaluationId(), retrieved.evaluationId());
        assertEquals(created.status(), retrieved.status());
        assertEquals(created.reason(), retrieved.reason());
        assertEquals(created.evaluatedAt(), retrieved.evaluatedAt());
        assertEquals(created.evidence().size(), retrieved.evidence().size());
        assertEquals(created.evidence().get(0).source(), retrieved.evidence().get(0).source());
        assertEquals(created.limitations().size(), retrieved.limitations().size());
        assertEquals(created.limitations(), retrieved.limitations());

        // Unknown evaluation ID should return empty
        assertTrue(engine.findEvaluationById("UNKNOWN-EVAL-ID").isEmpty());
    }

    @Test
    @DisplayName("Scenario 10: Evaluation history sorting")
    void test10_evaluationHistorySorting() {
        EvidenceStore evidenceStore = new EvidenceStore(providerEvidenceRepository);
        CompatibilityEngineService engine = new CompatibilityEngineService(qrParserService, evidenceStore, evaluationRepository);

        Instant now = Instant.now();
        Instant t1 = now.minus(2, ChronoUnit.HOURS);
        Instant t2 = now.minus(1, ChronoUnit.HOURS);
        Instant t3 = now;

        CompatibilityEvaluationEntity e1 = new CompatibilityEvaluationEntity("EVAL-1", CompatibilityStatus.UNVERIFIED, "Reason 1", t1);
        CompatibilityEvaluationEntity e2 = new CompatibilityEvaluationEntity("EVAL-2", CompatibilityStatus.CONDITIONAL, "Reason 2", t2);
        CompatibilityEvaluationEntity e3 = new CompatibilityEvaluationEntity("EVAL-3", CompatibilityStatus.COMPATIBLE, "Reason 3", t3);

        evaluationTable.put("EVAL-1", e1);
        evaluationTable.put("EVAL-2", e2);
        evaluationTable.put("EVAL-3", e3);

        List<CompatibilityEvaluationResponse> history = engine.getAllEvaluations();
        assertEquals(3, history.size());

        // Must be in descending order: EVAL-3 (now), EVAL-2 (now-1h), EVAL-1 (now-2h)
        assertEquals("EVAL-3", history.get(0).evaluationId());
        assertEquals("EVAL-2", history.get(1).evaluationId());
        assertEquals("EVAL-1", history.get(2).evaluationId());
    }

    @Test
    @DisplayName("Scenario 11: Database restart / reload persistence")
    void test11_databaseRestartReloadPersistence() {
        // Step 1: Session 1 creates data in the repository
        EvidenceStore session1Store = new EvidenceStore(providerEvidenceRepository);
        CompatibilityEngineService session1Engine = new CompatibilityEngineService(qrParserService, session1Store, evaluationRepository);

        CompatibilityEvaluationResponse session1Evaluation = session1Engine.evaluate(new CompatibilityEvaluationRequest(
                SAMPLE_QR,
                "SG",
                "upi-global"
        ));

        String savedEvaluationId = session1Evaluation.evaluationId();
        assertNotNull(savedEvaluationId);

        // Step 2: Simulate service restart / reload with a new service instance (clean in-memory state)
        // pointing to the same persistent repository
        EvidenceStore session2Store = new EvidenceStore(providerEvidenceRepository);
        CompatibilityEngineService session2Engine = new CompatibilityEngineService(qrParserService, session2Store, evaluationRepository);

        // Step 3: Session 2 should immediately be able to load the evaluation saved by Session 1
        Optional<CompatibilityEvaluationResponse> reloadedOpt = session2Engine.findEvaluationById(savedEvaluationId);
        assertTrue(reloadedOpt.isPresent(), "Persisted evaluation must be readable by a fresh service instance");
        CompatibilityEvaluationResponse reloaded = reloadedOpt.get();

        assertEquals(savedEvaluationId, reloaded.evaluationId());
        assertEquals(CompatibilityStatus.COMPATIBLE, reloaded.status());
        assertEquals(session1Evaluation.reason(), reloaded.reason());
        assertEquals(session1Evaluation.evidence().size(), reloaded.evidence().size());
        assertEquals(session1Evaluation.limitations().size(), reloaded.limitations().size());

        // Step 4: Session 2 should also have access to the provider evidence records
        Optional<EvidenceStore.ProviderEvidenceRecord> providerRecord = session2Store.findEvidence("upi-global", "SG");
        assertTrue(providerRecord.isPresent());
        assertEquals("SG", providerRecord.get().supportedCountry());
    }

    @Test
    @DisplayName("Scenario 12: Update existing provider evidence record")
    void test12_updateExistingProviderEvidenceRecord() {
        EvidenceStore evidenceStore = new EvidenceStore(providerEvidenceRepository);

        // Register initial record
        evidenceStore.registerRecord(new EvidenceStore.ProviderEvidenceRecord(
                "updatable-provider",
                "US",
                true,
                true,
                false,
                "Initial conditions",
                List.of(new EvidenceItem("Source 1", "Desc 1", "SUPPORTED", true))
        ));

        Optional<EvidenceStore.ProviderEvidenceRecord> initial = evidenceStore.findEvidence("updatable-provider", "US");
        assertTrue(initial.isPresent());
        assertEquals("Initial conditions", initial.get().conditions());

        // Update with modified fields and conditions
        evidenceStore.registerRecord(new EvidenceStore.ProviderEvidenceRecord(
                "updatable-provider",
                "US",
                true,
                true,
                false,
                "Updated conditions",
                List.of(new EvidenceItem("Source 2", "Desc 2", "SUPPORTED", true))
        ));

        Optional<EvidenceStore.ProviderEvidenceRecord> updated = evidenceStore.findEvidence("updatable-provider", "US");
        assertTrue(updated.isPresent());
        assertEquals("Updated conditions", updated.get().conditions());
        assertEquals(1, updated.get().evidenceItems().size());
        assertEquals("Source 2", updated.get().evidenceItems().get(0).source());
    }

    @Test
    @DisplayName("Scenario 13: Update existing compatibility evaluation")
    void test13_updateExistingCompatibilityEvaluation() {
        EvidenceStore evidenceStore = new EvidenceStore(providerEvidenceRepository);
        CompatibilityEngineService engine = new CompatibilityEngineService(qrParserService, evidenceStore, evaluationRepository);

        Instant evaluatedAt = Instant.now();
        CompatibilityEvaluationResponse original = new CompatibilityEvaluationResponse(
                "EVAL-UPDATE-01",
                CompatibilityStatus.UNVERIFIED,
                "Initial unverified reason",
                List.of(),
                List.of("Limitation 1"),
                evaluatedAt
        );

        engine.saveEvaluation(original);

        Optional<CompatibilityEvaluationResponse> found1 = engine.findEvaluationById("EVAL-UPDATE-01");
        assertTrue(found1.isPresent());
        assertEquals(CompatibilityStatus.UNVERIFIED, found1.get().status());

        // Save updated evaluation with same ID
        CompatibilityEvaluationResponse updated = new CompatibilityEvaluationResponse(
                "EVAL-UPDATE-01",
                CompatibilityStatus.COMPATIBLE,
                "Updated compatible reason",
                List.of(new EvidenceItem("Source A", "Desc A", "SUPPORTED", true)),
                List.of("Limitation 1", "Limitation 2"),
                evaluatedAt.plusSeconds(60)
        );

        engine.saveEvaluation(updated);

        Optional<CompatibilityEvaluationResponse> found2 = engine.findEvaluationById("EVAL-UPDATE-01");
        assertTrue(found2.isPresent());
        assertEquals(CompatibilityStatus.COMPATIBLE, found2.get().status());
        assertEquals("Updated compatible reason", found2.get().reason());
        assertEquals(1, found2.get().evidence().size());
        assertEquals(2, found2.get().limitations().size());
    }
}
