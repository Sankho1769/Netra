package org.netra.features.karma.dto;

import java.util.List;
import java.util.UUID;

public class KarmaSummaryDto {

    private UUID userId;
    private int balance;
    private String tier;
    private long totalTransactions;
    private List<KarmaTransactionDto> recentTransactions;

    public KarmaSummaryDto() {
    }

    public KarmaSummaryDto(
            UUID userId,
            int balance,
            String tier,
            long totalTransactions,
            List<KarmaTransactionDto> recentTransactions) {
        this.userId = userId;
        this.balance = balance;
        this.tier = tier;
        this.totalTransactions = totalTransactions;
        this.recentTransactions = recentTransactions;
    }

    public static String computeTier(int balance) {
        if (balance >= 500) return "Platinum Hero";
        if (balance >= 250) return "Gold Guardian";
        if (balance >= 100) return "Silver Lifesaver";
        if (balance >= 25) return "Active Contributor";
        return "Community Member";
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public int getBalance() {
        return balance;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public long getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(long totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public List<KarmaTransactionDto> getRecentTransactions() {
        return recentTransactions;
    }

    public void setRecentTransactions(List<KarmaTransactionDto> recentTransactions) {
        this.recentTransactions = recentTransactions;
    }
}
