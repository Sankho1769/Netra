package org.netra.features.metrics.dto;

public class CommunityImpactDto {

    private long totalVerifiedDonations;
    private long totalUnitsCollected;
    private long activeDonorsCount;
    private long fulfilledRequestsCount;
    private boolean hasData;
    private String notice;

    public CommunityImpactDto() {
    }

    public CommunityImpactDto(
            long totalVerifiedDonations,
            long totalUnitsCollected,
            long activeDonorsCount,
            long fulfilledRequestsCount,
            boolean hasData,
            String notice) {
        this.totalVerifiedDonations = totalVerifiedDonations;
        this.totalUnitsCollected = totalUnitsCollected;
        this.activeDonorsCount = activeDonorsCount;
        this.fulfilledRequestsCount = fulfilledRequestsCount;
        this.hasData = hasData;
        this.notice = notice;
    }

    public long getTotalVerifiedDonations() {
        return totalVerifiedDonations;
    }

    public void setTotalVerifiedDonations(long totalVerifiedDonations) {
        this.totalVerifiedDonations = totalVerifiedDonations;
    }

    public long getTotalUnitsCollected() {
        return totalUnitsCollected;
    }

    public void setTotalUnitsCollected(long totalUnitsCollected) {
        this.totalUnitsCollected = totalUnitsCollected;
    }

    public long getActiveDonorsCount() {
        return activeDonorsCount;
    }

    public void setActiveDonorsCount(long activeDonorsCount) {
        this.activeDonorsCount = activeDonorsCount;
    }

    public long getFulfilledRequestsCount() {
        return fulfilledRequestsCount;
    }

    public void setFulfilledRequestsCount(long fulfilledRequestsCount) {
        this.fulfilledRequestsCount = fulfilledRequestsCount;
    }

    public boolean isHasData() {
        return hasData;
    }

    public void setHasData(boolean hasData) {
        this.hasData = hasData;
    }

    public String getNotice() {
        return notice;
    }

    public void setNotice(String notice) {
        this.notice = notice;
    }
}
