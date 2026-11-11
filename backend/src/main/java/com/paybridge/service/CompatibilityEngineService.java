package com.paybridge.service;

import com.paybridge.dto.CompatibilityEvaluationRequest;
import com.paybridge.dto.CompatibilityEvaluationResponse;
import com.paybridge.dto.CompatibilityStatus;
import com.paybridge.dto.EvidenceItem;
import com.paybridge.dto.QrParseResponse;
import com.paybridge.entity.CompatibilityEvaluationEntity;
import com.paybridge.entity.EvaluationEvidenceEntity;
import com.paybridge.entity.EvaluationLimitationEntity;
import com.paybridge.exception.InvalidEvaluationRequestException;
import com.paybridge.repository.CompatibilityEvaluationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Deterministic payment compatibility evaluation engine.
 *
 * Evaluates candidate payment routes using explicit documented rules and evidence checks.
 *
 * IMPORTANT PRINCIPLES:
 * - A parsed QR payload does not establish merchant identity or legitimacy.
 * - Route compatibility claims must be supported by documented evidence; unknown support remains UNVERIFIED.
 * - PayPal Sandbox checkout is a separate payment-flow demonstration and does not transfer funds to the
 *   merchant encoded in a scanned UPI QR code.
 */
@Service
public class CompatibilityEngineService {

    private static final Pattern COUNTRY_CODE_PATTERN = Pattern.compile("^[A-Za-z]{2}$");
    private static final Set<String> ISO_COUNTRY_CODES = Set.of(Locale.getISOCountries());

    private final QrParserService qrParserService;
    private final EvidenceStore evidenceStore;
    private final CompatibilityEvaluationRepository evaluationRepository;

    // In-memory evaluation store for fallback and unit tests without a database
    private final Map<String, CompatibilityEvaluationResponse> evaluationHistory = new ConcurrentHashMap<>();

    /**
     * Default constructor for unit tests without database.
     * Operates in-memory.
     */
    public CompatibilityEngineService(QrParserService qrParserService, EvidenceStore evidenceStore) {
        this(qrParserService, evidenceStore, null);
    }

    /**
     * Spring-injected constructor for database persistence.
     */
    @Autowired
    public CompatibilityEngineService(
            QrParserService qrParserService,
            EvidenceStore evidenceStore,
            @Autowired(required = false) CompatibilityEvaluationRepository evaluationRepository) {
        this.qrParserService = qrParserService;
        this.evidenceStore = evidenceStore;
        this.evaluationRepository = evaluationRepository;
    }

    @Transactional
    public CompatibilityEvaluationResponse evaluate(CompatibilityEvaluationRequest request) {
        validateRequest(request);

        // 1. Verify that the QR payload is structurally valid UPI data.
        // Successful QR parsing confirms format validity, but never implies payment compatibility.
        QrParseResponse parsedQr = qrParserService.parse(request.qrText());

        String candidate = request.candidateProvider().trim().toLowerCase();
        String country = request.visitorCountry().trim().toUpperCase();
        String evaluationId = "EVAL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Instant evaluatedAt = Instant.now();

        // 2. Deterministic evaluation rules
        CompatibilityStatus status;
        String reason;
        List<EvidenceItem> evidence = new ArrayList<>();
        List<String> limitations = new ArrayList<>();

        // Universal baseline limitations
        limitations.add("Parsing does not verify merchant legal identity, bank account legitimacy, or terminal security.");
        limitations.add("Compatibility evaluation reflects documented technical capabilities and does not guarantee bank authorization at transaction time.");

        // Rule for PayPal: PayPal Sandbox is a separate test workflow, not a direct UPI settlement rail
        if ("paypal".equals(candidate)) {
            status = CompatibilityStatus.UNVERIFIED;
            reason = "No authoritative evidence establishes direct payment rail routing from PayPal to the merchant's UPI address. " +
                    "PayPal Sandbox demonstrates an isolated payment checkout and does not transfer funds to domestic UPI VPAs.";
            limitations.add("PayPal Sandbox checkout is a separate demonstration flow and does not pay the merchant encoded in a scanned UPI QR.");
            limitations.add("Compatibility has not been confirmed.");
        } else {
            var evidenceRecordOpt = evidenceStore.findEvidence(candidate, country);

            if (evidenceRecordOpt.isEmpty()) {
                // COMP-03: No evidence available -> UNVERIFIED
                status = CompatibilityStatus.UNVERIFIED;
                reason = "Provider support has not been verified. No authoritative documentation establishes payment route compatibility for '" +
                        candidate + "' from country '" + country + "'.";
                limitations.add("Compatibility has not been confirmed. Unverified status indicates lack of documented evidence.");
            } else {
                var record = evidenceRecordOpt.get();
                if (record.evidenceItems() != null) {
                    evidence.addAll(record.evidenceItems().stream()
                            .filter(item -> item != null)
                            .toList());
                }

                if (!record.activeAndValid()) {
                    // COMP-04: Evidence is expired or outdated -> UNVERIFIED
                    status = CompatibilityStatus.UNVERIFIED;
                    reason = "Documented evidence for provider '" + candidate + "' is expired or outdated; current compatibility cannot be confirmed.";
                    limitations.add("Historical or expired documentation cannot be used to verify active payment capabilities.");
                } else if (record.evidenceItems() == null || record.evidenceItems().isEmpty()
                        || record.evidenceItems().stream().anyMatch(item ->
                                item == null
                                || !item.valid()
                                || item.source() == null || item.source().isBlank()
                                || item.description() == null || item.description().isBlank()
                                || item.supportLevel() == null || item.supportLevel().isBlank())) {
                    status = CompatibilityStatus.UNVERIFIED;
                    reason = "Provider evidence is missing or incomplete; current compatibility cannot be confirmed.";
                    limitations.add("Compatibility requires at least one complete, valid evidence item.");
                } else if (record.conflicting()) {
                    // COMP-06: Conflicting evidence sources -> UNVERIFIED
                    status = CompatibilityStatus.UNVERIFIED;
                    reason = "Conflicting evidence sources detected regarding provider cross-border routing. Unresolved discrepancies prevent verification.";
                    limitations.add("Contradictory regulatory or technical documentation requires formal clarification before support can be established.");
                } else if (!record.coversQrPaymentScenario()) {
                    // COMP-05: Capability does not cover in-person UPI QR payment -> UNVERIFIED
                    status = CompatibilityStatus.UNVERIFIED;
                    reason = "Provider capabilities do not support the UPI QR payment scenario.";
                    limitations.add("Provider support for other scenarios (such as web e-commerce) does not extend to in-person UPI QR merchant payments.");
                } else if (record.conditions() != null && !record.conditions().isBlank()) {
                    // COMP-02: Support depends on additional conditions -> CONDITIONAL
                    status = CompatibilityStatus.CONDITIONAL;
                    reason = "Route is supported conditionally: " + record.conditions();
                    limitations.add("Transaction execution requires all documented prerequisites to be satisfied by the merchant acquiring bank and payer institution.");
                } else {
                    // COMP-01: Reliable, unexpired, non-conflicting evidence establishes support -> COMPATIBLE
                    status = CompatibilityStatus.COMPATIBLE;
                    reason = "Documented bilateral linkage confirms routing support for '" + candidate + "' from " + country + " to INR UPI merchants.";
                }
            }
        }

        // Extract parsed QR parameters for audit persistence
        String payeeAddress = (parsedQr.fields() != null) ? parsedQr.fields().get("pa") : null;
        String merchantName = (parsedQr.fields() != null) ? parsedQr.fields().get("pn") : null;
        String amount = (parsedQr.fields() != null) ? parsedQr.fields().get("am") : null;
        String currency = (parsedQr.fields() != null) ? parsedQr.fields().get("cu") : null;

        CompatibilityEvaluationResponse response = new CompatibilityEvaluationResponse(
                evaluationId,
                status,
                reason,
                evidence,
                limitations,
                evaluatedAt,
                merchantName,
                payeeAddress,
                candidate,
                country,
                amount,
                currency
        );

        saveEvaluation(response, request.qrText());
        return response;
    }

    private void validateRequest(CompatibilityEvaluationRequest request) {
        if (request == null) {
            throw new InvalidEvaluationRequestException("Request body must not be null.");
        }
        if (request.qrText() == null || request.qrText().isBlank()) {
            throw new InvalidEvaluationRequestException("qrText must not be empty or blank.");
        }
        if (request.visitorCountry() == null || request.visitorCountry().isBlank()) {
            throw new InvalidEvaluationRequestException("visitorCountry must not be empty or blank.");
        }
        String normalizedCountry = request.visitorCountry().trim().toUpperCase(Locale.ROOT);
        if (!COUNTRY_CODE_PATTERN.matcher(normalizedCountry).matches()
                || !ISO_COUNTRY_CODES.contains(normalizedCountry)) {
            throw new InvalidEvaluationRequestException("visitorCountry must be a 2-letter ISO country code (e.g., 'DE', 'SG', 'US').");
        }
        if (request.candidateProvider() == null || request.candidateProvider().isBlank()) {
            throw new InvalidEvaluationRequestException("candidateProvider must not be empty or blank.");
        }
    }

    @Transactional(readOnly = true)
    public Optional<CompatibilityEvaluationResponse> findEvaluationById(String evaluationId) {
        if (evaluationId == null || evaluationId.isBlank()) {
            return Optional.empty();
        }
        String cleanId = evaluationId.trim();

        if (evaluationRepository != null) {
            Optional<CompatibilityEvaluationEntity> entityOpt = evaluationRepository.findById(cleanId);
            if (entityOpt.isPresent()) {
                return Optional.of(mapToResponse(entityOpt.get()));
            }
        }

        return Optional.ofNullable(evaluationHistory.get(cleanId));
    }

    @Transactional
    public void saveEvaluation(CompatibilityEvaluationResponse response) {
        saveEvaluation(response, null);
    }

    @Transactional
    public void saveEvaluation(CompatibilityEvaluationResponse response, String qrPayload) {
        if (response == null || response.evaluationId() == null) {
            return;
        }

        // Always update in-memory map for fast access and fallback
        evaluationHistory.put(response.evaluationId(), response);

        if (evaluationRepository != null) {
            CompatibilityEvaluationEntity entity = evaluationRepository.findById(response.evaluationId())
                    .orElseGet(() -> new CompatibilityEvaluationEntity(
                            response.evaluationId(),
                            response.status(),
                            response.reason(),
                            response.evaluatedAt(),
                            response.merchantName(),
                            response.payeeAddress(),
                            response.candidateProvider(),
                            response.visitorCountry(),
                            response.amount(),
                            response.currency(),
                            qrPayload
                    ));
            entity.update(response.status(), response.reason(), response.evaluatedAt());
            entity.updateDetails(
                    response.merchantName(),
                    response.payeeAddress(),
                    response.candidateProvider(),
                    response.visitorCountry(),
                    response.amount(),
                    response.currency(),
                    qrPayload
            );
            entity.getEvidence().clear();
            entity.getLimitations().clear();

            if (response.evidence() != null) {
                for (EvidenceItem item : response.evidence()) {
                    if (item != null) {
                        EvaluationEvidenceEntity evidenceEntity = new EvaluationEvidenceEntity(
                                item.source(),
                                item.description(),
                                item.supportLevel(),
                                item.valid(),
                                entity
                        );
                        entity.addEvidence(evidenceEntity);
                    }
                }
            }

            if (response.limitations() != null) {
                int order = 0;
                for (String limitation : response.limitations()) {
                    if (limitation != null) {
                        EvaluationLimitationEntity limitationEntity = new EvaluationLimitationEntity(
                                limitation,
                                order++,
                                entity
                        );
                        entity.addLimitation(limitationEntity);
                    }
                }
            }

            evaluationRepository.save(entity);
        }
    }

    @Transactional(readOnly = true)
    public Map<String, CompatibilityEvaluationResponse> getEvaluationHistory() {
        if (evaluationRepository != null) {
            Map<String, CompatibilityEvaluationResponse> map = new HashMap<>();
            for (CompatibilityEvaluationEntity entity : evaluationRepository.findAll()) {
                map.put(entity.getEvaluationId(), mapToResponse(entity));
            }
            return Collections.unmodifiableMap(map);
        }
        return Collections.unmodifiableMap(evaluationHistory);
    }

    @Transactional(readOnly = true)
    public List<CompatibilityEvaluationResponse> getAllEvaluations() {
        if (evaluationRepository != null) {
            List<CompatibilityEvaluationEntity> entities = evaluationRepository.findAllByOrderByEvaluatedAtDesc();
            List<CompatibilityEvaluationResponse> list = new ArrayList<>();
            for (CompatibilityEvaluationEntity entity : entities) {
                list.add(mapToResponse(entity));
            }
            list.sort((a, b) -> {
                int cmp = b.evaluatedAt().compareTo(a.evaluatedAt());
                if (cmp != 0) return cmp;
                return b.evaluationId().compareTo(a.evaluationId());
            });
            return Collections.unmodifiableList(list);
        }

        List<CompatibilityEvaluationResponse> list = new ArrayList<>(evaluationHistory.values());
        list.sort((a, b) -> {
            int cmp = b.evaluatedAt().compareTo(a.evaluatedAt());
            if (cmp != 0) return cmp;
            return b.evaluationId().compareTo(a.evaluationId());
        });
        return Collections.unmodifiableList(list);
    }

    private CompatibilityEvaluationResponse mapToResponse(CompatibilityEvaluationEntity entity) {
        List<EvidenceItem> evidence = new ArrayList<>();
        if (entity.getEvidence() != null) {
            for (EvaluationEvidenceEntity ev : entity.getEvidence()) {
                evidence.add(new EvidenceItem(
                        ev.getSource(),
                        ev.getDescription(),
                        ev.getSupportLevel(),
                        ev.isValid()
                ));
            }
        }

        List<String> limitations = new ArrayList<>();
        if (entity.getLimitations() != null) {
            for (EvaluationLimitationEntity lim : entity.getLimitations()) {
                limitations.add(lim.getLimitation());
            }
        }

        return new CompatibilityEvaluationResponse(
                entity.getEvaluationId(),
                entity.getStatus(),
                entity.getReason(),
                Collections.unmodifiableList(evidence),
                Collections.unmodifiableList(limitations),
                entity.getEvaluatedAt(),
                entity.getMerchantName(),
                entity.getPayeeAddress(),
                entity.getCandidateProvider(),
                entity.getVisitorCountry(),
                entity.getAmount(),
                entity.getCurrency()
        );
    }
}
