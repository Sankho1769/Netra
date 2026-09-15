package org.netra.features.bloodbank.entity;

import java.time.Duration;
import java.time.Instant;

public enum InventoryFreshness {
    FRESH,
    RECENT,
    STALE,
    UNKNOWN;

    public static InventoryFreshness evaluate(Instant lastUpdatedAt, Instant now) {
        if (lastUpdatedAt == null) {
            return UNKNOWN;
        }
        if (now == null) {
            now = Instant.now();
        }

        Duration elapsed = Duration.between(lastUpdatedAt, now);
        if (elapsed.isNegative()) {
            return FRESH;
        }

        long hours = elapsed.toHours();
        if (hours < 2) {
            return FRESH;
        } else if (hours < 6) {
            return RECENT;
        } else if (hours < 24) {
            return STALE;
        } else {
            return UNKNOWN;
        }
    }
}
