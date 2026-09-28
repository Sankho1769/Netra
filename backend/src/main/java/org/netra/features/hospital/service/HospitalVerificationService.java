package org.netra.features.hospital.service;

import org.netra.features.bloodbank.entity.BloodBank;
import org.netra.features.bloodbank.entity.BloodBankVerificationStatus;
import org.netra.features.bloodbank.repository.BloodBankRepository;
import org.netra.features.hospital.dto.HospitalVerificationResultDto;
import org.netra.features.hospital.dto.VerifiedHospitalDto;
import org.netra.features.hospital.dto.VerifyHospitalRequest;
import org.netra.features.hospital.entity.VerifiedHospital;
import org.netra.features.hospital.repository.VerifiedHospitalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
public class HospitalVerificationService {

    private static final Logger log = LoggerFactory.getLogger(HospitalVerificationService.class);

    private static final Pattern BLACKLISTED_PATTERN = Pattern.compile(
            ".*\\b(fake|scam|fraud|bogus|test\\s*hospital|dummy\\s*clinic|nowhere)\\b.*",
            Pattern.CASE_INSENSITIVE
    );

    private final VerifiedHospitalRepository verifiedHospitalRepository;
    private final BloodBankRepository bloodBankRepository;

    public HospitalVerificationService(
            VerifiedHospitalRepository verifiedHospitalRepository,
            BloodBankRepository bloodBankRepository) {
        this.verifiedHospitalRepository = verifiedHospitalRepository;
        this.bloodBankRepository = bloodBankRepository;
    }

    /**
     * Resolves and verifies an entered hospital/place against authoritative healthcare records.
     */
    public HospitalVerificationResultDto verifyHospital(VerifyHospitalRequest request) {
        if (request == null || request.getHospitalName() == null || request.getHospitalName().isBlank()) {
            return HospitalVerificationResultDto.unverified("", "", "", "", "Hospital name cannot be blank.");
        }

        String rawName = request.getHospitalName().trim();
        String rawAddress = request.getHospitalAddress() != null ? request.getHospitalAddress().trim() : "";
        String rawCity = request.getCity() != null ? request.getCity().trim() : "";
        String rawState = request.getState() != null ? request.getState().trim() : "";
        String placeId = request.getPlaceId() != null ? request.getPlaceId().trim() : null;

        // 1. Blacklist / Known Fraudulent check
        if (BLACKLISTED_PATTERN.matcher(rawName).matches()) {
            log.warn("Rejected hospital verification attempt for blacklisted name: '{}'", rawName);
            return HospitalVerificationResultDto.rejected(rawName, "Place rejected: flag indicates non-existent or abusive institution.");
        }

        // 2. Direct lookup by authoritative place_id
        if (placeId != null && !placeId.isBlank()) {
            Optional<VerifiedHospital> byPlace = verifiedHospitalRepository.findByPlaceId(placeId);
            if (byPlace.isPresent()) {
                VerifiedHospital vh = byPlace.get();
                return mapToVerifiedResult(vh, "Verified via authoritative place identifier.");
            }
        }

        // 3. Lookup in verified_hospitals by name and city
        if (!rawCity.isBlank()) {
            Optional<VerifiedHospital> byNameAndCity = verifiedHospitalRepository
                    .findByNameIgnoreCaseAndCityIgnoreCase(rawName, rawCity);
            if (byNameAndCity.isPresent()) {
                return mapToVerifiedResult(byNameAndCity.get(), "Verified matching hospital and city in clinical registry.");
            }
        }

        // 4. Lookup in verified_hospitals by name alone
        Optional<VerifiedHospital> byName = verifiedHospitalRepository.findByNameIgnoreCase(rawName);
        if (byName.isPresent()) {
            return mapToVerifiedResult(byName.get(), "Verified matching hospital in clinical registry.");
        }

        // 5. Lookup in authorized blood banks (all verified blood banks are authoritative collection places)
        List<BloodBank> banks = bloodBankRepository.findAll();
        for (BloodBank bank : banks) {
            if (bank.getVerificationStatus() == BloodBankVerificationStatus.VERIFIED) {
                if (bank.getName().equalsIgnoreCase(rawName) ||
                    (rawName.length() >= 5 && bank.getName().toLowerCase().contains(rawName.toLowerCase()))) {
                    return HospitalVerificationResultDto.verified(
                            null,
                            bank.getName(),
                            bank.getAddress(),
                            bank.getCity(),
                            bank.getState(),
                            bank.getPostalCode(),
                            "BB-" + bank.getId().toString(),
                            true,
                            bank.getLatitude(),
                            bank.getLongitude(),
                            "Verified against registered authorized blood center."
                    );
                }
            }
        }

        // 6. Fallback: Unverified place
        log.info("Hospital '{}' in '{}' not found in verified registry. Returning UNVERIFIED.", rawName, rawCity);
        return HospitalVerificationResultDto.unverified(
                rawName,
                rawAddress,
                rawCity,
                rawState,
                "Place not found in pre-verified healthcare registry. Request requires administrative review."
        );
    }

    /**
     * Searches verified hospitals and authorized blood banks by keyword and optional city.
     */
    public List<VerifiedHospitalDto> searchHospitals(String query, String city) {
        if (query == null || query.trim().isBlank()) {
            return Collections.emptyList();
        }

        String sanitizedQuery = query.trim();
        String sanitizedCity = (city != null && !city.trim().isBlank()) ? city.trim() : null;

        Map<String, VerifiedHospitalDto> results = new LinkedHashMap<>();

        // 1. Search verified_hospitals
        List<VerifiedHospital> hospitals = verifiedHospitalRepository.searchHospitals(sanitizedQuery, sanitizedCity);
        for (VerifiedHospital h : hospitals) {
            String key = (h.getName() + "|" + h.getCity()).toLowerCase();
            results.put(key, VerifiedHospitalDto.fromEntity(h));
        }

        // 2. Search authorized blood_banks
        List<BloodBank> banks = bloodBankRepository.findAll();
        for (BloodBank b : banks) {
            if (b.getVerificationStatus() == BloodBankVerificationStatus.VERIFIED) {
                boolean cityMatches = sanitizedCity == null || b.getCity().equalsIgnoreCase(sanitizedCity);
                boolean nameMatches = b.getName().toLowerCase().contains(sanitizedQuery.toLowerCase()) ||
                                      b.getAddress().toLowerCase().contains(sanitizedQuery.toLowerCase()) ||
                                      b.getCity().toLowerCase().contains(sanitizedQuery.toLowerCase());
                if (cityMatches && nameMatches) {
                    String key = (b.getName() + "|" + b.getCity()).toLowerCase();
                    if (!results.containsKey(key)) {
                        results.put(key, new VerifiedHospitalDto(
                                null,
                                b.getName(),
                                b.getAddress(),
                                b.getCity(),
                                b.getState(),
                                b.getPostalCode(),
                                "BB-" + b.getId().toString(),
                                true,
                                "VERIFIED",
                                b.getPhone()
                        ));
                    }
                }
            }
        }

        return new ArrayList<>(results.values());
    }

    private HospitalVerificationResultDto mapToVerifiedResult(VerifiedHospital vh, String notes) {
        return HospitalVerificationResultDto.verified(
                vh.getId(),
                vh.getName(),
                vh.getAddress(),
                vh.getCity(),
                vh.getState(),
                vh.getPostalCode(),
                vh.getPlaceId(),
                vh.getHasBloodBank(),
                vh.getLatitude(),
                vh.getLongitude(),
                notes
        );
    }
}
