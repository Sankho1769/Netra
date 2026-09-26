package org.netra.features.bloodrequest.entity;

/**
 * Trust & verification lifecycle status for blood requests.
 * Distinguishes unverified community requests from medically verified requests.
 */
public enum BloodRequestVerificationStatus {
    UNVERIFIED,
    VERIFIED,
    REJECTED
}
