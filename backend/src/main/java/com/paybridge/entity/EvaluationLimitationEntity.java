package com.paybridge.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "evaluation_limitations")
public class EvaluationLimitationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String limitation;

    @Column(name = "item_order", nullable = false)
    private int itemOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluation_id", nullable = false)
    private CompatibilityEvaluationEntity evaluation;

    protected EvaluationLimitationEntity() {
    }

    public EvaluationLimitationEntity(
            String limitation,
            int itemOrder,
            CompatibilityEvaluationEntity evaluation) {
        this.limitation = limitation;
        this.itemOrder = itemOrder;
        this.evaluation = evaluation;
    }

    public Long getId() {
        return id;
    }

    public String getLimitation() {
        return limitation;
    }

    public int getItemOrder() {
        return itemOrder;
    }

    public CompatibilityEvaluationEntity getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(CompatibilityEvaluationEntity evaluation) {
        this.evaluation = evaluation;
    }
}
