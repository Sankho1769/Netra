package org.netra.features.donor.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class BloodGroupConverter implements AttributeConverter<BloodGroup, String> {

    @Override
    public String convertToDatabaseColumn(BloodGroup attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getCode();
    }

    @Override
    public BloodGroup convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        return BloodGroup.fromCode(dbData);
    }
}
