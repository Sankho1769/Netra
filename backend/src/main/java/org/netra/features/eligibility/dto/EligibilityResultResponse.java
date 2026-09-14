package org.netra.features.eligibility.dto;

import org.netra.features.eligibility.entity.ResultType;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EligibilityResultResponse {
    private UUID sessionId;
    private String ruleVersion;
    private ResultType result;
    private String title;
    private String message;
    private String disclaimer;
    private LocalDate estimatedNextEligibleDate;
    private List<DeferralDetailDto> deferralReasons = new ArrayList<>();
    private List<String> missingFields = new ArrayList<>();
    private List<NextActionDto> nextActions = new ArrayList<>();

    public EligibilityResultResponse() {
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public String getRuleVersion() {
        return ruleVersion;
    }

    public void setRuleVersion(String ruleVersion) {
        this.ruleVersion = ruleVersion;
    }

    public ResultType getResult() {
        return result;
    }

    public void setResult(ResultType result) {
        this.result = result;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }

    public LocalDate getEstimatedNextEligibleDate() {
        return estimatedNextEligibleDate;
    }

    public void setEstimatedNextEligibleDate(LocalDate estimatedNextEligibleDate) {
        this.estimatedNextEligibleDate = estimatedNextEligibleDate;
    }

    public List<DeferralDetailDto> getDeferralReasons() {
        return deferralReasons;
    }

    public void setDeferralReasons(List<DeferralDetailDto> deferralReasons) {
        this.deferralReasons = deferralReasons;
    }

    public List<String> getMissingFields() {
        return missingFields;
    }

    public void setMissingFields(List<String> missingFields) {
        this.missingFields = missingFields;
    }

    public List<NextActionDto> getNextActions() {
        return nextActions;
    }

    public void setNextActions(List<NextActionDto> nextActions) {
        this.nextActions = nextActions;
    }
}
