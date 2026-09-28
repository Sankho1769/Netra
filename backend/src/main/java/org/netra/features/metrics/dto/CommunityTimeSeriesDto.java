package org.netra.features.metrics.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CommunityTimeSeriesDto {

    private int days;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<CommunityMetricPointDto> dataPoints = new ArrayList<>();
    private long totalRequestsReceived;
    private long totalRequestsFulfilled;
    private long totalDonations;
    private long totalUnitsCollected;
    private long totalEmergencyRequests;
    private long totalEmergencyFulfilled;
    private long activeDonors;
    private long verifiedCenters;
    private boolean hasData;
    private String emptyStateMessage;

    public CommunityTimeSeriesDto() {
    }

    public CommunityTimeSeriesDto(
            int days,
            LocalDate startDate,
            LocalDate endDate,
            List<CommunityMetricPointDto> dataPoints,
            long totalRequestsReceived,
            long totalRequestsFulfilled,
            long totalDonations,
            long totalUnitsCollected,
            long totalEmergencyRequests,
            long totalEmergencyFulfilled,
            long activeDonors,
            long verifiedCenters,
            boolean hasData,
            String emptyStateMessage) {
        this.days = days;
        this.startDate = startDate;
        this.endDate = endDate;
        this.dataPoints = dataPoints != null ? dataPoints : new ArrayList<>();
        this.totalRequestsReceived = totalRequestsReceived;
        this.totalRequestsFulfilled = totalRequestsFulfilled;
        this.totalDonations = totalDonations;
        this.totalUnitsCollected = totalUnitsCollected;
        this.totalEmergencyRequests = totalEmergencyRequests;
        this.totalEmergencyFulfilled = totalEmergencyFulfilled;
        this.activeDonors = activeDonors;
        this.verifiedCenters = verifiedCenters;
        this.hasData = hasData;
        this.emptyStateMessage = emptyStateMessage;
    }

    public int getDays() {
        return days;
    }

    public void setDays(int days) {
        this.days = days;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public List<CommunityMetricPointDto> getDataPoints() {
        return dataPoints;
    }

    public void setDataPoints(List<CommunityMetricPointDto> dataPoints) {
        this.dataPoints = dataPoints;
    }

    public long getTotalRequestsReceived() {
        return totalRequestsReceived;
    }

    public void setTotalRequestsReceived(long totalRequestsReceived) {
        this.totalRequestsReceived = totalRequestsReceived;
    }

    public long getTotalRequestsFulfilled() {
        return totalRequestsFulfilled;
    }

    public void setTotalRequestsFulfilled(long totalRequestsFulfilled) {
        this.totalRequestsFulfilled = totalRequestsFulfilled;
    }

    public long getTotalDonations() {
        return totalDonations;
    }

    public void setTotalDonations(long totalDonations) {
        this.totalDonations = totalDonations;
    }

    public long getTotalUnitsCollected() {
        return totalUnitsCollected;
    }

    public void setTotalUnitsCollected(long totalUnitsCollected) {
        this.totalUnitsCollected = totalUnitsCollected;
    }

    public long getTotalEmergencyRequests() {
        return totalEmergencyRequests;
    }

    public void setTotalEmergencyRequests(long totalEmergencyRequests) {
        this.totalEmergencyRequests = totalEmergencyRequests;
    }

    public long getTotalEmergencyFulfilled() {
        return totalEmergencyFulfilled;
    }

    public void setTotalEmergencyFulfilled(long totalEmergencyFulfilled) {
        this.totalEmergencyFulfilled = totalEmergencyFulfilled;
    }

    public long getActiveDonors() {
        return activeDonors;
    }

    public void setActiveDonors(long activeDonors) {
        this.activeDonors = activeDonors;
    }

    public long getVerifiedCenters() {
        return verifiedCenters;
    }

    public void setVerifiedCenters(long verifiedCenters) {
        this.verifiedCenters = verifiedCenters;
    }

    public boolean isHasData() {
        return hasData;
    }

    public void setHasData(boolean hasData) {
        this.hasData = hasData;
    }

    public String getEmptyStateMessage() {
        return emptyStateMessage;
    }

    public void setEmptyStateMessage(String emptyStateMessage) {
        this.emptyStateMessage = emptyStateMessage;
    }
}
