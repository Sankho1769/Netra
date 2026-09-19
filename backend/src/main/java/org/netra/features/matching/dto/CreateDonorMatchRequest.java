package org.netra.features.matching.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request DTO for creating a persistent donor match.
 *
 * {@code candidateReference}: Authorized internal donor-selection reference. Not a secret.
 * Decouples the candidate selection API from the internal user account identifier ({@code users.id}).
 */
public class CreateDonorMatchRequest {

    @NotNull(message = "candidateReference must not be null")
    private UUID candidateReference;

    public CreateDonorMatchRequest() {
    }

    public CreateDonorMatchRequest(UUID candidateReference) {
        this.candidateReference = candidateReference;
    }

    public UUID getCandidateReference() {
        return candidateReference;
    }

    public void setCandidateReference(UUID candidateReference) {
        this.candidateReference = candidateReference;
    }
}
