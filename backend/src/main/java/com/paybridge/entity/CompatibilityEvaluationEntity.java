package com.paybridge.entity;

import com.paybridge.dto.CompatibilityStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "compatibility_evaluations")
public class CompatibilityEvaluationEntity {

    @Id
    @Column(name = "evaluation_id", nullable = false, length = 20)
    private String evaluationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompatibilityStatus status;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;

    @Column(name = "merchant_name")
    private String merchantName;

    @Column(name = "payee_address")
    private String payeeAddress;

    @Column(name = "candidate_provider", length = 50)
    private String candidateProvider;

    @Column(name = "visitor_country", length = 10)
    private String visitorCountry;

    @Column(name = "amount", length = 30)
    private String amount;

    @Column(name = "currency", length = 10)
    private String currency;

    @Column(name = "qr_payload", columnDefinition = "TEXT")
    private String qrPayload;

    @OneToMany(
        mappedBy = "evaluation",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @OrderBy("id ASC")
    private List<EvaluationEvidenceEntity> evidence = new ArrayList<>();

    @OneToMany(
        mappedBy = "evaluation",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @OrderBy("itemOrder ASC")
    private List<EvaluationLimitationEntity> limitations = new ArrayList<>();

    protected CompatibilityEvaluationEntity() {
    }

    public CompatibilityEvaluationEntity(
            String evaluationId,
            CompatibilityStatus status,
            String reason,
            Instant evaluatedAt) {
        this.evaluationId = evaluationId;
        this.status = status;
        this.reason = reason;
        this.evaluatedAt = evaluatedAt;
    }

    public CompatibilityEvaluationEntity(
            String evaluationId,
            CompatibilityStatus status,
            String reason,
            Instant evaluatedAt,
            String merchantName,
            String payeeAddress,
            String candidateProvider,
            String visitorCountry,
            String amount,
            String currency,
            String qrPayload) {
        this.evaluationId = evaluationId;
        this.status = status;
        this.reason = reason;
        this.evaluatedAt = evaluatedAt;
        this.merchantName = merchantName;
        this.payeeAddress = payeeAddress;
        this.candidateProvider = candidateProvider;
        this.visitorCountry = visitorCountry;
        this.amount = amount;
        this.currency = currency;
        this.qrPayload = qrPayload;
    }

    public void update(CompatibilityStatus status, String reason, Instant evaluatedAt) {
        this.status = status;
        this.reason = reason;
        this.evaluatedAt = evaluatedAt;
    }

    public void updateDetails(
            String merchantName,
            String payeeAddress,
            String candidateProvider,
            String visitorCountry,
            String amount,
            String currency,
            String qrPayload) {
        this.merchantName = merchantName;
        this.payeeAddress = payeeAddress;
        this.candidateProvider = candidateProvider;
        this.visitorCountry = visitorCountry;
        this.amount = amount;
        this.currency = currency;
        this.qrPayload = qrPayload;
    }

    public void addEvidence(EvaluationEvidenceEntity evidenceItem) {
        if (evidenceItem != null) {
            evidence.add(evidenceItem);
            evidenceItem.setEvaluation(this);
        }
    }

    public void removeEvidence(EvaluationEvidenceEntity evidenceItem) {
        if (evidenceItem != null) {
            evidence.remove(evidenceItem);
            evidenceItem.setEvaluation(null);
        }
    }

    public void addLimitation(EvaluationLimitationEntity limitationItem) {
        if (limitationItem != null) {
            limitations.add(limitationItem);
            limitationItem.setEvaluation(this);
        }
    }

    public void removeLimitation(EvaluationLimitationEntity limitationItem) {
        if (limitationItem != null) {
            limitations.remove(limitationItem);
            limitationItem.setEvaluation(null);
        }
    }

    public String getEvaluationId() {
        return evaluationId;
    }

    public CompatibilityStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public Instant getEvaluatedAt() {
        return evaluatedAt;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public String getPayeeAddress() {
        return payeeAddress;
    }

    public String getCandidateProvider() {
        return candidateProvider;
    }

    public String getVisitorCountry() {
        return visitorCountry;
    }

    public String getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getQrPayload() {
        return qrPayload;
    }

    public List<EvaluationEvidenceEntity> getEvidence() {
        return evidence;
    }

    public List<EvaluationLimitationEntity> getLimitations() {
        return limitations;
    }
}
