package org.netra.features.notification.provider;

public class PushDeliveryResult {

    public enum Status {
        SUCCESS,
        FAILED_TRANSIENT,
        FAILED_INVALID_TOKEN
    }

    private final Status status;
    private final String message;
    private final String providerMessageId;

    public PushDeliveryResult(Status status, String message, String providerMessageId) {
        this.status = status;
        this.message = message;
        this.providerMessageId = providerMessageId;
    }

    public static PushDeliveryResult success(String providerMessageId) {
        return new PushDeliveryResult(Status.SUCCESS, "Delivery succeeded", providerMessageId);
    }

    public static PushDeliveryResult failedTransient(String message) {
        return new PushDeliveryResult(Status.FAILED_TRANSIENT, message, null);
    }

    public static PushDeliveryResult failedInvalidToken(String message) {
        return new PushDeliveryResult(Status.FAILED_INVALID_TOKEN, message, null);
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public boolean isInvalidToken() {
        return status == Status.FAILED_INVALID_TOKEN;
    }
}
