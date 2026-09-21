package org.netra.features.notification.dto;

public class MarkAllReadResponseDto {

    private int updatedCount;

    public MarkAllReadResponseDto() {
    }

    public MarkAllReadResponseDto(int updatedCount) {
        this.updatedCount = updatedCount;
    }

    public int getUpdatedCount() {
        return updatedCount;
    }

    public void setUpdatedCount(int updatedCount) {
        this.updatedCount = updatedCount;
    }
}
