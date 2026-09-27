package org.netra.features.donor.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.netra.core.exception.ValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StringToBloodGroupConverterTest {

    private final StringToBloodGroupConverter converter = new StringToBloodGroupConverter();

    @ParameterizedTest(name = "converts ''{0}'' to {1}")
    @CsvSource({
            "O+, O_POSITIVE",
            "O-, O_NEGATIVE",
            "A+, A_POSITIVE",
            "A-, A_NEGATIVE",
            "B+, B_POSITIVE",
            "B-, B_NEGATIVE",
            "AB+, AB_POSITIVE",
            "AB-, AB_NEGATIVE",
            // URL decoded space variants (when query param + is decoded as space)
            "'O ', O_POSITIVE",
            "'A ', A_POSITIVE",
            "'B ', B_POSITIVE",
            "'AB ', AB_POSITIVE",
            // Standard enum constant names
            "O_POSITIVE, O_POSITIVE",
            "O_NEGATIVE, O_NEGATIVE",
            "A_POSITIVE, A_POSITIVE",
            "A_NEGATIVE, A_NEGATIVE",
            "B_POSITIVE, B_POSITIVE",
            "B_NEGATIVE, B_NEGATIVE",
            "AB_POSITIVE, AB_POSITIVE",
            "AB_NEGATIVE, AB_NEGATIVE",
            // Case-insensitive variants
            "o+, O_POSITIVE",
            "b_positive, B_POSITIVE"
    })
    @DisplayName("Should successfully convert medical codes, URL-decoded spaces, and enum constants")
    void shouldConvertValidInputs(String input, BloodGroup expected) {
        BloodGroup result = converter.convert(input);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("Should return null for empty/whitespace input")
    void shouldReturnNullForEmptyInput() {
        assertThat(converter.convert(null)).isNull();
        assertThat(converter.convert("")).isNull();
        assertThat(converter.convert("   ")).isNull();
    }

    @Test
    @DisplayName("Should throw ValidationException for invalid blood group strings")
    void shouldThrowForInvalidStrings() {
        assertThatThrownBy(() -> converter.convert("XYZ"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid blood group 'XYZ'");
    }
}
