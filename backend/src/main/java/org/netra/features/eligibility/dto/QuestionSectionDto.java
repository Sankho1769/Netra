package org.netra.features.eligibility.dto;

import java.util.List;

public class QuestionSectionDto {
    private int stepNumber;
    private String stepTitle;
    private String stepSubtitle;
    private List<QuestionItemDto> questions;

    public QuestionSectionDto() {
    }

    public QuestionSectionDto(int stepNumber, String stepTitle, String stepSubtitle, List<QuestionItemDto> questions) {
        this.stepNumber = stepNumber;
        this.stepTitle = stepTitle;
        this.stepSubtitle = stepSubtitle;
        this.questions = questions;
    }

    public int getStepNumber() {
        return stepNumber;
    }

    public void setStepNumber(int stepNumber) {
        this.stepNumber = stepNumber;
    }

    public String getStepTitle() {
        return stepTitle;
    }

    public void setStepTitle(String stepTitle) {
        this.stepTitle = stepTitle;
    }

    public String getStepSubtitle() {
        return stepSubtitle;
    }

    public void setStepSubtitle(String stepSubtitle) {
        this.stepSubtitle = stepSubtitle;
    }

    public List<QuestionItemDto> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionItemDto> questions) {
        this.questions = questions;
    }
}
