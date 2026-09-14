package org.netra.features.eligibility.dto;

import org.netra.features.eligibility.entity.QuestionType;
import java.util.List;

public class QuestionItemDto {
    private String questionKey;
    private int stepNumber;
    private String category;
    private String text;
    private String helpText;
    private QuestionType type;
    private List<QuestionOptionDto> options;
    private String validation;

    public QuestionItemDto() {
    }

    public QuestionItemDto(String questionKey, int stepNumber, String category, String text, String helpText,
                           QuestionType type, List<QuestionOptionDto> options, String validation) {
        this.questionKey = questionKey;
        this.stepNumber = stepNumber;
        this.category = category;
        this.text = text;
        this.helpText = helpText;
        this.type = type;
        this.options = options;
        this.validation = validation;
    }

    public String getQuestionKey() {
        return questionKey;
    }

    public void setQuestionKey(String questionKey) {
        this.questionKey = questionKey;
    }

    public int getStepNumber() {
        return stepNumber;
    }

    public void setStepNumber(int stepNumber) {
        this.stepNumber = stepNumber;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getHelpText() {
        return helpText;
    }

    public void setHelpText(String helpText) {
        this.helpText = helpText;
    }

    public QuestionType getType() {
        return type;
    }

    public void setType(QuestionType type) {
        this.type = type;
    }

    public List<QuestionOptionDto> getOptions() {
        return options;
    }

    public void setOptions(List<QuestionOptionDto> options) {
        this.options = options;
    }

    public String getValidation() {
        return validation;
    }

    public void setValidation(String validation) {
        this.validation = validation;
    }
}
