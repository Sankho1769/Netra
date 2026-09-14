package org.netra.features.eligibility.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public class SubmitAnswersRequest {

    @NotEmpty(message = "Answers list cannot be empty")
    @Size(max = 50, message = "Exceeded maximum allowed answers per submission")
    @Valid
    private List<AnswerItemDto> answers;

    public SubmitAnswersRequest() {
    }

    public SubmitAnswersRequest(List<AnswerItemDto> answers) {
        this.answers = answers;
    }

    public List<AnswerItemDto> getAnswers() {
        return answers;
    }

    public void setAnswers(List<AnswerItemDto> answers) {
        this.answers = answers;
    }
}
