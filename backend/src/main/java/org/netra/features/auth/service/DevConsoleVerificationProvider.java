package org.netra.features.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(type = "org.netra.features.auth.service.ProductionEmailVerificationProvider")
public class DevConsoleVerificationProvider implements VerificationProvider {

    private static final Logger log = LoggerFactory.getLogger(DevConsoleVerificationProvider.class);

    // In-memory test sink for automated integration testing
    private static volatile String lastSentEmail;
    private static volatile String lastSentCode;

    @Override
    public void sendVerificationCode(String email, String code) {
        lastSentEmail = email;
        lastSentCode = code;
        // Clinical safety & zero-trust: Mask email in logs and never expose code in production logs
        String masked = email != null && email.contains("@")
                ? email.substring(0, Math.min(3, email.indexOf("@"))) + "***" + email.substring(email.indexOf("@"))
                : "***";
        log.info("[DEV EMAIL SINK] Verification challenge dispatched for recipient: {} [Code: {}]", masked, code);
    }

    public static String getLastSentEmail() {
        return lastSentEmail;
    }

    public static String getLastSentCode() {
        return lastSentCode;
    }

    public static void clear() {
        lastSentEmail = null;
        lastSentCode = null;
    }
}
