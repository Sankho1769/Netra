package org.netra.features.donor.entity;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToBloodGroupConverter implements Converter<String, BloodGroup> {

    @Override
    public BloodGroup convert(String source) {
        if (source == null || source.trim().isEmpty()) {
            return null;
        }
        return BloodGroup.fromCode(source);
    }
}
