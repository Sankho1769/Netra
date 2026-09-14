package org.netra.features.eligibility.dto;

public class DeferralDetailDto {
    private String code;
    private String displayText;
    private String recommendedAction;

    public DeferralDetailDto() {
    }

    public DeferralDetailDto(String code, String displayText, String recommendedAction) {
        this.code = code;
        this.displayText = displayText;
        this.recommendedAction = recommendedAction;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDisplayText() {
        return displayText;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
    }
}
