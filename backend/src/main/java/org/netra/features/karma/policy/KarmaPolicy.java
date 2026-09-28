package org.netra.features.karma.policy;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configurable Karma business rules and point weighting policy.
 * Can be overridden via application.yml (netra.karma.*)
 */
@Component
@ConfigurationProperties(prefix = "netra.karma")
public class KarmaPolicy {

    private int verifiedDonationPoints = 50;
    private int bloodRequestFulfilledPoints = 30;
    private int donorCommitmentCompletedPoints = 25;
    private int eventParticipationPoints = 40;
    private int fakeRequestPenalty = 50;
    private int donorNoShowPenalty = 30;

    public int getVerifiedDonationPoints() {
        return verifiedDonationPoints;
    }

    public void setVerifiedDonationPoints(int verifiedDonationPoints) {
        this.verifiedDonationPoints = verifiedDonationPoints;
    }

    public int getBloodRequestFulfilledPoints() {
        return bloodRequestFulfilledPoints;
    }

    public void setBloodRequestFulfilledPoints(int bloodRequestFulfilledPoints) {
        this.bloodRequestFulfilledPoints = bloodRequestFulfilledPoints;
    }

    public int getDonorCommitmentCompletedPoints() {
        return donorCommitmentCompletedPoints;
    }

    public void setDonorCommitmentCompletedPoints(int donorCommitmentCompletedPoints) {
        this.donorCommitmentCompletedPoints = donorCommitmentCompletedPoints;
    }

    public int getEventParticipationPoints() {
        return eventParticipationPoints;
    }

    public void setEventParticipationPoints(int eventParticipationPoints) {
        this.eventParticipationPoints = eventParticipationPoints;
    }

    public int getFakeRequestPenalty() {
        return fakeRequestPenalty;
    }

    public void setFakeRequestPenalty(int fakeRequestPenalty) {
        this.fakeRequestPenalty = fakeRequestPenalty;
    }

    public int getDonorNoShowPenalty() {
        return donorNoShowPenalty;
    }

    public void setDonorNoShowPenalty(int donorNoShowPenalty) {
        this.donorNoShowPenalty = donorNoShowPenalty;
    }
}
