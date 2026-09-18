package org.netra.features.matching.rules;

import org.netra.features.donor.entity.BloodGroup;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Centralized compatibility rule set for preliminary donor candidate matching.
 *
 * Medical Reference Standards:
 * 1. National Blood Transfusion Council (NBTC), Ministry of Health and Family Welfare (MoHFW), India
 * 2. World Health Organization (WHO) Blood Transfusion Safety Technical Series
 * 3. AABB (American Association of Blood Banks) Technical Manual
 *
 * EXACT TRANSFUSION PRODUCT SCOPE:
 * - Current matrix scope applies STRICTLY to RED BLOOD CELL (RBC) and WHOLE BLOOD transfusions.
 * - Other blood products follow different biological and immunological principles and are
 *   EXPLICITLY OUTSIDE THE SCOPE of this matrix:
 *   * Fresh Frozen Plasma (FFP): follows reverse ABO plasma compatibility rules (AB is universal
 *     plasma donor; O is universal plasma recipient).
 *   * Platelets (random / apheresis): follow plasma and platelet antibody matching protocols.
 *   * Cryoprecipitate: follows fibrinogen and plasma compatibility guidelines.
 *
 * CLINICAL DECISION SUPPORT NOTICE:
 * - This is decision-support candidate matching utilized by NETRA strictly as a preliminary rule set.
 * - It does NOT constitute clinical clearance, crossmatching confirmation, or a transfusion order.
 * - Final transfusion compatibility, donor screening, pre-transfusion crossmatching,
 *   and clinical suitability MUST always be determined and verified by qualified blood-bank
 *   and clinical staff prior to any blood collection or transfusion.
 *
 * Rules:
 * - O- is the universal red blood cell donor.
 * - AB+ is the universal red blood cell recipient.
 * - Rh-negative patients may strictly receive only Rh-negative blood.
 * - Rh-positive patients may receive Rh-positive and Rh-negative blood of compatible ABO group.
 * - Exact blood group match is the highest clinical preference (EXACT).
 */
@Component
public class BloodCompatibilityMatrix {

    private static final Map<BloodGroup, Set<BloodGroup>> COMPATIBILITY_MAP = new EnumMap<>(BloodGroup.class);

    static {
        // Recipient O- can receive from O-
        COMPATIBILITY_MAP.put(BloodGroup.O_NEGATIVE, Set.of(BloodGroup.O_NEGATIVE));

        // Recipient O+ can receive from O+, O-
        COMPATIBILITY_MAP.put(BloodGroup.O_POSITIVE, Set.of(BloodGroup.O_POSITIVE, BloodGroup.O_NEGATIVE));

        // Recipient A- can receive from A-, O-
        COMPATIBILITY_MAP.put(BloodGroup.A_NEGATIVE, Set.of(BloodGroup.A_NEGATIVE, BloodGroup.O_NEGATIVE));

        // Recipient A+ can receive from A+, A-, O+, O-
        COMPATIBILITY_MAP.put(BloodGroup.A_POSITIVE, Set.of(
                BloodGroup.A_POSITIVE, BloodGroup.A_NEGATIVE,
                BloodGroup.O_POSITIVE, BloodGroup.O_NEGATIVE));

        // Recipient B- can receive from B-, O-
        COMPATIBILITY_MAP.put(BloodGroup.B_NEGATIVE, Set.of(BloodGroup.B_NEGATIVE, BloodGroup.O_NEGATIVE));

        // Recipient B+ can receive from B+, B-, O+, O-
        COMPATIBILITY_MAP.put(BloodGroup.B_POSITIVE, Set.of(
                BloodGroup.B_POSITIVE, BloodGroup.B_NEGATIVE,
                BloodGroup.O_POSITIVE, BloodGroup.O_NEGATIVE));

        // Recipient AB- can receive from AB-, A-, B-, O-
        COMPATIBILITY_MAP.put(BloodGroup.AB_NEGATIVE, Set.of(
                BloodGroup.AB_NEGATIVE, BloodGroup.A_NEGATIVE,
                BloodGroup.B_NEGATIVE, BloodGroup.O_NEGATIVE));

        // Recipient AB+ (universal recipient) can receive from all 8 blood groups
        COMPATIBILITY_MAP.put(BloodGroup.AB_POSITIVE, Set.of(
                BloodGroup.AB_POSITIVE, BloodGroup.AB_NEGATIVE,
                BloodGroup.A_POSITIVE, BloodGroup.A_NEGATIVE,
                BloodGroup.B_POSITIVE, BloodGroup.B_NEGATIVE,
                BloodGroup.O_POSITIVE, BloodGroup.O_NEGATIVE));
    }

    /**
     * Returns the set of all compatible donor blood groups for the given recipient blood group.
     *
     * @param recipientGroup the blood group requested / required by the patient
     * @return an unmodifiable set of compatible donor blood groups
     */
    public Set<BloodGroup> getCompatibleDonorGroups(BloodGroup recipientGroup) {
        if (recipientGroup == null) {
            return Collections.emptySet();
        }
        Set<BloodGroup> compatible = COMPATIBILITY_MAP.get(recipientGroup);
        return compatible != null ? Collections.unmodifiableSet(compatible) : Collections.emptySet();
    }

    /**
     * Determines the compatibility classification between a donor and recipient blood group.
     *
     * @param donorGroup blood group of candidate donor
     * @param recipientGroup blood group required by recipient
     * @return EXACT if identical, COMPATIBLE if clinically transfusable, INCOMPATIBLE otherwise
     */
    public CompatibilityType getCompatibilityType(BloodGroup donorGroup, BloodGroup recipientGroup) {
        if (donorGroup == null || recipientGroup == null) {
            return CompatibilityType.INCOMPATIBLE;
        }
        if (donorGroup == recipientGroup) {
            return CompatibilityType.EXACT;
        }
        Set<BloodGroup> compatibleGroups = COMPATIBILITY_MAP.get(recipientGroup);
        if (compatibleGroups != null && compatibleGroups.contains(donorGroup)) {
            return CompatibilityType.COMPATIBLE;
        }
        return CompatibilityType.INCOMPATIBLE;
    }

    /**
     * Checks if a donor blood group is compatible for a recipient.
     */
    public boolean isCompatible(BloodGroup donorGroup, BloodGroup recipientGroup) {
        return getCompatibilityType(donorGroup, recipientGroup) != CompatibilityType.INCOMPATIBLE;
    }
}
