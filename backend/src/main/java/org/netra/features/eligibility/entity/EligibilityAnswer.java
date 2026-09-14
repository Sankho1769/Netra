package org.netra.features.eligibility.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "eligibility_answers", uniqueConstraints = {
    @UniqueConstraint(name = "uq_session_question", columnNames = {"session_id", "question_key"})
})
public class EligibilityAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private EligibilitySession session;

    @Column(name = "question_key", nullable = false, length = 64)
    private String questionKey;

    @Column(name = "answer_value", nullable = false, columnDefinition = "TEXT")
    private String answerValue;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public EligibilityAnswer() {
    }

    public EligibilityAnswer(EligibilitySession session, String questionKey, String answerValue) {
        this.session = session;
        this.questionKey = questionKey;
        this.answerValue = answerValue;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public EligibilitySession getSession() {
        return session;
    }

    public void setSession(EligibilitySession session) {
        this.session = session;
    }

    public String getQuestionKey() {
        return questionKey;
    }

    public void setQuestionKey(String questionKey) {
        this.questionKey = questionKey;
    }

    public String getAnswerValue() {
        return answerValue;
    }

    public void setAnswerValue(String answerValue) {
        this.answerValue = answerValue;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
