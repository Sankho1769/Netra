package org.netra.features.notification.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "netra.notifications.push.provider", havingValue = "noop", matchIfMissing = true)
public class NoopPushNotificationProvider implements PushNotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(NoopPushNotificationProvider.class);

    @Override
    public PushDeliveryResult sendPush(String deviceToken, String title, String body, Map<String, String> data) {
        String maskedToken = maskToken(deviceToken);
        log.info("NOOP push provider simulated delivery to token: {}, title: {}", maskedToken, title);
        return PushDeliveryResult.success("noop-" + UUID.randomUUID());
    }

    @Override
    public String getProviderName() {
        return "NOOP";
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 8) {
            return "***";
        }
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }
}
