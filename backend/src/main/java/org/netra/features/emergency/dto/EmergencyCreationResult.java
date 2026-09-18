package org.netra.features.emergency.dto;

import org.netra.features.bloodrequest.dto.BloodRequestDetailDto;

public record EmergencyCreationResult(BloodRequestDetailDto detail, boolean isReplay) {
}
