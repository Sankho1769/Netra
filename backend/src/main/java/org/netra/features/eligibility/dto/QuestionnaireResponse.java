package org.netra.features.eligibility.dto;

import java.util.List;

public class QuestionnaireResponse {
    private String ruleVersion;
    private int totalSteps;
    private List<QuestionSectionDto> sections;

    public QuestionnaireResponse() {
    }

    public QuestionnaireResponse(String ruleVersion, int totalSteps, List<QuestionSectionDto> sections) {
        this.ruleVersion = ruleVersion;
        this.totalSteps = totalSteps;
        this.sections = sections;
    }

    public String getRuleVersion() {
        return ruleVersion;
    }

    public void setRuleVersion(String ruleVersion) {
        this.ruleVersion = ruleVersion;
    }

    public int getTotalSteps() {
        return totalSteps;
    }

    public void setTotalSteps(int totalSteps) {
        this.totalSteps = totalSteps;
    }

    public List<QuestionSectionDto> getSections() {
        return sections;
    }

    public void setSections(List<QuestionSectionDto> sections) {
        this.sections = sections;
    }
}
