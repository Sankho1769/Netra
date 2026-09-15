package org.netra.features.donor.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.netra.core.exception.ValidationException;

public enum BloodGroup {
    A_POSITIVE("A+"),
    A_NEGATIVE("A-"),
    B_POSITIVE("B+"),
    B_NEGATIVE("B-"),
    O_POSITIVE("O+"),
    O_NEGATIVE("O-"),
    AB_POSITIVE("AB+"),
    AB_NEGATIVE("AB-");

    private final String code;

    BloodGroup(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static BloodGroup fromCode(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException("Blood group is required.");
        }
        String normalized = value.trim().toUpperCase();
        for (BloodGroup bg : values()) {
            if (bg.code.equalsIgnoreCase(normalized) || bg.name().equalsIgnoreCase(normalized)) {
                return bg;
            }
        }
        throw new ValidationException("Invalid blood group '" + value + "'. Supported values: A+, A-, B+, B-, O+, O-, AB+, AB-.");
    }
}
