package org.netra.features.metrics.service;

import org.netra.features.bloodbank.entity.BloodBankVerificationStatus;
import org.netra.features.bloodbank.repository.BloodBankRepository;
import org.netra.features.bloodrequest.entity.BloodRequest;
import org.netra.features.bloodrequest.entity.BloodRequestStatus;
import org.netra.features.bloodrequest.entity.BloodRequestUrgency;
import org.netra.features.bloodrequest.repository.BloodRequestRepository;
import org.netra.features.donation.entity.Donation;
import org.netra.features.donation.entity.DonationVerificationStatus;
import org.netra.features.donation.repository.DonationRepository;
import org.netra.features.donor.repository.DonorProfileRepository;
import org.netra.features.hospital.repository.VerifiedHospitalRepository;
import org.netra.features.metrics.dto.CommunityImpactDto;
import org.netra.features.metrics.dto.CommunityMetricPointDto;
import org.netra.features.metrics.dto.CommunityTimeSeriesDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CommunityMetricsService {

    private final DonationRepository donationRepository;
    private final BloodRequestRepository bloodRequestRepository;
    private final DonorProfileRepository donorProfileRepository;
    private final VerifiedHospitalRepository verifiedHospitalRepository;
    private final BloodBankRepository bloodBankRepository;

    @Autowired
    public CommunityMetricsService(
            DonationRepository donationRepository,
            BloodRequestRepository bloodRequestRepository,
            DonorProfileRepository donorProfileRepository,
            @Autowired(required = false) VerifiedHospitalRepository verifiedHospitalRepository,
            @Autowired(required = false) BloodBankRepository bloodBankRepository) {
        this.donationRepository = donationRepository;
        this.bloodRequestRepository = bloodRequestRepository;
        this.donorProfileRepository = donorProfileRepository;
        this.verifiedHospitalRepository = verifiedHospitalRepository;
        this.bloodBankRepository = bloodBankRepository;
    }

    public CommunityMetricsService(
            DonationRepository donationRepository,
            BloodRequestRepository bloodRequestRepository,
            DonorProfileRepository donorProfileRepository) {
        this(donationRepository, bloodRequestRepository, donorProfileRepository, null, null);
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
                    "Community impact data will appear here once verified activity is recorded."
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

    public CommunityTimeSeriesDto getTimeSeries(int days) {
        int boundedDays = Math.max(1, Math.min(365, days));
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate startDate = today.minusDays(boundedDays - 1);
        Instant startInstant = startDate.atStartOfDay().toInstant(ZoneOffset.UTC);

        List<BloodRequest> createdRequests = bloodRequestRepository.findByCreatedAtGreaterThanEqual(startInstant);
        List<BloodRequest> fulfilledRequests = bloodRequestRepository.findByFulfilledAtGreaterThanEqual(startInstant);
        List<Donation> donations = donationRepository.findByDonationDateGreaterThanEqualAndVerificationStatus(
                startDate, DonationVerificationStatus.VERIFIED);

        long activeDonors = donorProfileRepository.count();
        long verifiedHospitals = verifiedHospitalRepository != null ? verifiedHospitalRepository.countByVerificationStatus("VERIFIED") : 0;
        long verifiedBloodBanks = bloodBankRepository != null ? bloodBankRepository.countByVerificationStatus(BloodBankVerificationStatus.VERIFIED) : 0;
        long verifiedCenters = verifiedHospitals + verifiedBloodBanks;

        List<CommunityMetricPointDto> dataPoints = new ArrayList<>();
        long totalReceived = 0;
        long totalFulfilled = 0;
        long totalDonations = 0;
        long totalUnits = 0;
        long totalEmergency = 0;
        long totalEmergencyFulfilled = 0;

        for (int i = 0; i < boundedDays; i++) {
            LocalDate date = startDate.plusDays(i);

            long receivedOnDate = createdRequests.stream()
                    .filter(r -> r.getCreatedAt() != null && r.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate().equals(date))
                    .count();

            long fulfilledOnDate = fulfilledRequests.stream()
                    .filter(r -> r.getFulfilledAt() != null && r.getFulfilledAt().atZone(ZoneOffset.UTC).toLocalDate().equals(date))
                    .count();

            long donationsOnDate = donations.stream()
                    .filter(d -> d.getDonationDate() != null && d.getDonationDate().equals(date))
                    .count();

            long unitsOnDate = donationsOnDate;

            long emergencyOnDate = createdRequests.stream()
                    .filter(r -> r.getUrgency() == BloodRequestUrgency.CRITICAL
                            && r.getCreatedAt() != null
                            && r.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate().equals(date))
                    .count();

            long emergencyFulfilledOnDate = fulfilledRequests.stream()
                    .filter(r -> r.getUrgency() == BloodRequestUrgency.CRITICAL
                            && r.getFulfilledAt() != null
                            && r.getFulfilledAt().atZone(ZoneOffset.UTC).toLocalDate().equals(date))
                    .count();

            totalReceived += receivedOnDate;
            totalFulfilled += fulfilledOnDate;
            totalDonations += donationsOnDate;
            totalUnits += unitsOnDate;
            totalEmergency += emergencyOnDate;
            totalEmergencyFulfilled += emergencyFulfilledOnDate;

            dataPoints.add(new CommunityMetricPointDto(
                    date,
                    receivedOnDate,
                    fulfilledOnDate,
                    donationsOnDate,
                    unitsOnDate,
                    emergencyOnDate,
                    emergencyFulfilledOnDate
            ));
        }

        boolean hasData = (totalReceived + totalDonations + totalFulfilled) > 0;
        String emptyNotice = hasData ? null : "Community impact data will appear here once verified activity is recorded.";

        return new CommunityTimeSeriesDto(
                boundedDays,
                startDate,
                today,
                dataPoints,
                totalReceived,
                totalFulfilled,
                totalDonations,
                totalUnits,
                totalEmergency,
                totalEmergencyFulfilled,
                activeDonors,
                verifiedCenters,
                hasData,
                emptyNotice
        );
    }
}
