package org.netra.features.metrics.dto;

import java.time.LocalDate;

public class CommunityMetricPointDto {

    private LocalDate date;
    private long bloodRequestsReceived;
    private long bloodRequestsFulfilled;
    private long donationsRecorded;
    private long unitsCollected;
    private long emergencyRequests;
    private long emergencyRequestsFulfilled;

    public CommunityMetricPointDto() {
    }

    public CommunityMetricPointDto(
            LocalDate date,
            long bloodRequestsReceived,
            long bloodRequestsFulfilled,
            long donationsRecorded,
            long unitsCollected,
            long emergencyRequests,
            long emergencyRequestsFulfilled) {
        this.date = date;
        this.bloodRequestsReceived = bloodRequestsReceived;
        this.bloodRequestsFulfilled = bloodRequestsFulfilled;
        this.donationsRecorded = donationsRecorded;
        this.unitsCollected = unitsCollected;
        this.emergencyRequests = emergencyRequests;
        this.emergencyRequestsFulfilled = emergencyRequestsFulfilled;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public long getBloodRequestsReceived() {
        return bloodRequestsReceived;
    }

    public void setBloodRequestsReceived(long bloodRequestsReceived) {
        this.bloodRequestsReceived = bloodRequestsReceived;
    }

    public long getBloodRequestsFulfilled() {
        return bloodRequestsFulfilled;
    }

    public void setBloodRequestsFulfilled(long bloodRequestsFulfilled) {
        this.bloodRequestsFulfilled = bloodRequestsFulfilled;
    }

    public long getDonationsRecorded() {
        return donationsRecorded;
    }

    public void setDonationsRecorded(long donationsRecorded) {
        this.donationsRecorded = donationsRecorded;
    }

    public long getUnitsCollected() {
        return unitsCollected;
    }

    public void setUnitsCollected(long unitsCollected) {
        this.unitsCollected = unitsCollected;
    }

    public long getEmergencyRequests() {
        return emergencyRequests;
    }

    public void setEmergencyRequests(long emergencyRequests) {
        this.emergencyRequests = emergencyRequests;
    }

    public long getEmergencyRequestsFulfilled() {
        return emergencyRequestsFulfilled;
    }

    public void setEmergencyRequestsFulfilled(long emergencyRequestsFulfilled) {
        this.emergencyRequestsFulfilled = emergencyRequestsFulfilled;
    }
}
