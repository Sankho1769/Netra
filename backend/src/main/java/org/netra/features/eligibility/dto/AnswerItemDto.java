package org.netra.features.eligibility.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AnswerItemDto {

    @NotBlank(message = "Question key is required")
    @Size(max = 64, message = "Question key exceeds maximum allowed length")
    private String questionKey;

    @NotBlank(message = "Answer value is required")
    @Size(max = 1024, message = "Answer value exceeds maximum allowed length")
    private String value;

    public AnswerItemDto() {
    }

    public AnswerItemDto(String questionKey, String value) {
        this.questionKey = questionKey;
        this.value = value;
    }

    public String getQuestionKey() {
        return questionKey;
    }

    public void setQuestionKey(String questionKey) {
        this.questionKey = questionKey;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
