package com.paybridge.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "evidence_items")
public class EvidenceItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String source;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "support_level", nullable = false)
    private String supportLevel;

    @Column(nullable = false)
    private boolean valid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_evidence_id", nullable = false)
    private ProviderEvidenceEntity providerEvidence;

    protected EvidenceItemEntity() {
    }

    public EvidenceItemEntity(
            String source,
            String description,
            String supportLevel,
            boolean valid,
            ProviderEvidenceEntity providerEvidence) {
        this.source = source;
        this.description = description;
        this.supportLevel = supportLevel;
        this.valid = valid;
        this.providerEvidence = providerEvidence;
    }

    public Long getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public String getDescription() {
        return description;
    }

    public String getSupportLevel() {
        return supportLevel;
    }

    public boolean isValid() {
        return valid;
    }

    public ProviderEvidenceEntity getProviderEvidence() {
        return providerEvidence;
    }

    public void setProviderEvidence(ProviderEvidenceEntity providerEvidence) {
        this.providerEvidence = providerEvidence;
    }
}
