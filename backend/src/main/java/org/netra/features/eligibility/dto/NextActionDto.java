package org.netra.features.eligibility.dto;

public class NextActionDto {
    private String actionType;
    private String label;
    private String route;

    public NextActionDto() {
    }

    public NextActionDto(String actionType, String label, String route) {
        this.actionType = actionType;
        this.label = label;
        this.route = route;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }
}
