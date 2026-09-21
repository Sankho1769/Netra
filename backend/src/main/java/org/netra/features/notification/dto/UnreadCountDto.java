package org.netra.features.notification.dto;

public class UnreadCountDto {

    private long unreadCount;

    public UnreadCountDto() {
    }

    public UnreadCountDto(long unreadCount) {
        this.unreadCount = unreadCount;
    }

    public long getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(long unreadCount) {
        this.unreadCount = unreadCount;
    }
}
