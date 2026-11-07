package com.paybridge.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "evaluation_evidence")
public class EvaluationEvidenceEntity {

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
    @JoinColumn(name = "evaluation_id", nullable = false)
    private CompatibilityEvaluationEntity evaluation;

    protected EvaluationEvidenceEntity() {
    }

    public EvaluationEvidenceEntity(
            String source,
            String description,
            String supportLevel,
            boolean valid,
            CompatibilityEvaluationEntity evaluation) {
        this.source = source;
        this.description = description;
        this.supportLevel = supportLevel;
        this.valid = valid;
        this.evaluation = evaluation;
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

    public CompatibilityEvaluationEntity getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(CompatibilityEvaluationEntity evaluation) {
        this.evaluation = evaluation;
    }
}
