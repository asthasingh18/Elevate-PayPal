package com.paybridge.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "provider_evidence",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"provider_key", "supported_country"}
    )
)
public class ProviderEvidenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_key", nullable = false)
    private String providerKey;

    @Column(name = "supported_country", nullable = false, length = 10)
    private String supportedCountry;

    @Column(nullable = false)
    private boolean coversQrPaymentScenario;

    @Column(nullable = false)
    private boolean activeAndValid;

    @Column(nullable = false)
    private boolean conflicting;

    @Column(columnDefinition = "TEXT")
    private String conditions;

    @OneToMany(
        mappedBy = "providerEvidence",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @OrderBy("id ASC")
    private List<EvidenceItemEntity> evidenceItems = new ArrayList<>();

    protected ProviderEvidenceEntity() {
    }

    public ProviderEvidenceEntity(
            String providerKey,
            String supportedCountry,
            boolean coversQrPaymentScenario,
            boolean activeAndValid,
            boolean conflicting,
            String conditions) {
        this.providerKey = providerKey;
        this.supportedCountry = supportedCountry;
        this.coversQrPaymentScenario = coversQrPaymentScenario;
        this.activeAndValid = activeAndValid;
        this.conflicting = conflicting;
        this.conditions = conditions;
    }

    public void update(
            boolean coversQrPaymentScenario,
            boolean activeAndValid,
            boolean conflicting,
            String conditions) {
        this.coversQrPaymentScenario = coversQrPaymentScenario;
        this.activeAndValid = activeAndValid;
        this.conflicting = conflicting;
        this.conditions = conditions;
    }

    public void addEvidenceItem(EvidenceItemEntity evidenceItem) {
        if (evidenceItem != null) {
            evidenceItems.add(evidenceItem);
            evidenceItem.setProviderEvidence(this);
        }
    }

    public void removeEvidenceItem(EvidenceItemEntity evidenceItem) {
        if (evidenceItem != null) {
            evidenceItems.remove(evidenceItem);
            evidenceItem.setProviderEvidence(null);
        }
    }

    public Long getId() {
        return id;
    }

    public String getProviderKey() {
        return providerKey;
    }

    public String getSupportedCountry() {
        return supportedCountry;
    }

    public boolean isCoversQrPaymentScenario() {
        return coversQrPaymentScenario;
    }

    public boolean isActiveAndValid() {
        return activeAndValid;
    }

    public boolean isConflicting() {
        return conflicting;
    }

    public String getConditions() {
        return conditions;
    }

    public List<EvidenceItemEntity> getEvidenceItems() {
        return evidenceItems;
    }
}
