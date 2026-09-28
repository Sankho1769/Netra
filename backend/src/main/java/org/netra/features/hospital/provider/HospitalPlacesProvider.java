package org.netra.features.hospital.provider;

import org.netra.features.hospital.dto.VerifiedHospitalDto;
import java.util.List;

/**
 * Authoritative provider contract for dynamic, multi-region healthcare institutions search and resolution.
 */
public interface HospitalPlacesProvider {

    /**
     * Searches for verified healthcare places (hospitals, clinics, medical colleges, blood banks).
     *
     * @param query institution name or locality keyword
     * @param city optional city filter
     * @return list of verified healthcare institutions
     */
    List<VerifiedHospitalDto> searchHealthcarePlaces(String query, String city);
}
