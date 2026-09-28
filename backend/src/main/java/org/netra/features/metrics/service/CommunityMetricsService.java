package org.netra.features.metrics.service;

import org.netra.features.bloodrequest.entity.BloodRequestStatus;
import org.netra.features.bloodrequest.repository.BloodRequestRepository;
import org.netra.features.donation.entity.DonationVerificationStatus;
import org.netra.features.donation.repository.DonationRepository;
import org.netra.features.donor.repository.DonorProfileRepository;
import org.netra.features.metrics.dto.CommunityImpactDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CommunityMetricsService {

    private final DonationRepository donationRepository;
    private final BloodRequestRepository bloodRequestRepository;
    private final DonorProfileRepository donorProfileRepository;

    public CommunityMetricsService(
            DonationRepository donationRepository,
            BloodRequestRepository bloodRequestRepository,
            DonorProfileRepository donorProfileRepository) {
        this.donationRepository = donationRepository;
        this.bloodRequestRepository = bloodRequestRepository;
        this.donorProfileRepository = donorProfileRepository;
    }

    public CommunityImpactDto getCommunityImpact() {
        long verifiedDonations = donationRepository.countByVerificationStatus(DonationVerificationStatus.VERIFIED);
        long fulfilledRequests = bloodRequestRepository.countByStatus(BloodRequestStatus.FULFILLED);
        long activeDonors = donorProfileRepository.count();

        // 1 verified donation = 1 unit collected minimum
        long unitsCollected = verifiedDonations;

        if (verifiedDonations == 0 && fulfilledRequests == 0) {
            return new CommunityImpactDto(
                    0,
                    0,
                    activeDonors,
                    0,
                    false,
                    "Community impact data will appear here once verified donations are recorded."
            );
        }

        return new CommunityImpactDto(
                verifiedDonations,
                unitsCollected,
                activeDonors,
                fulfilledRequests,
                true,
                null
        );
    }
}
