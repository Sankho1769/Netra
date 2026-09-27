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
        // In HTTP query strings, '+' is decoded as ' ' by servlet containers (e.g. "B " instead of "B+")
        String normalized = source.replace(" ", "+").trim();
        return BloodGroup.fromCode(normalized);
    }
}
