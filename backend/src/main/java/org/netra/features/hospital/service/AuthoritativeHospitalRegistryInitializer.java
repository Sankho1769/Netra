package org.netra.features.hospital.service;

import org.netra.features.hospital.entity.VerifiedHospital;
import org.netra.features.hospital.repository.VerifiedHospitalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Initializes the authoritative verified clinical hospital registry on startup
 * if the registry table is empty (e.g. in dev profiles or environments without Flyway).
 * Contains strictly verified, official medical institutions matching V19 migration.
 */
@Component
@Order(1)
public class AuthoritativeHospitalRegistryInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AuthoritativeHospitalRegistryInitializer.class);

    private final VerifiedHospitalRepository repository;

    public AuthoritativeHospitalRegistryInitializer(VerifiedHospitalRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() == 0) {
            log.info("Authoritative verified hospital registry is empty. Seeding official institutions...");
            List<VerifiedHospital> hospitals = List.of(
                new VerifiedHospital("SSKM Hospital (IPGMER)", "244 AJC Bose Road, Bhowanipore", "Kolkata", "West Bengal", "700020", 22.5398, 88.3426, "PLACE-IN-CCU-SSKM", true, "VERIFIED", "+913322231589", "INTERNAL_REGISTRY", "HOSPITAL"),
                new VerifiedHospital("Medical College and Hospital, Kolkata", "88 College Street, College Square", "Kolkata", "West Bengal", "700073", 22.5735, 88.3629, "PLACE-IN-CCU-MCH", true, "VERIFIED", "+913322551621", "INTERNAL_REGISTRY", "HOSPITAL"),
                new VerifiedHospital("NRS Medical College and Hospital", "138 AJC Bose Road, Sealdah", "Kolkata", "West Bengal", "700014", 22.5645, 88.3698, "PLACE-IN-CCU-NRS", true, "VERIFIED", "+913322860033", "INTERNAL_REGISTRY", "HOSPITAL"),
                new VerifiedHospital("Safdarjung Hospital", "Ring Road, Opposite AIIMS", "New Delhi", "Delhi", "110029", 28.5702, 77.2081, "PLACE-IN-DEL-SAFDARJUNG", true, "VERIFIED", "+911126165060", "INTERNAL_REGISTRY", "HOSPITAL"),
                new VerifiedHospital("Max Super Speciality Hospital, Saket", "1 2, Press Enclave Marg, Saket", "New Delhi", "Delhi", "110017", 28.5283, 77.2117, "PLACE-IN-DEL-MAXSAKET", true, "VERIFIED", "+911126515050", "INTERNAL_REGISTRY", "HOSPITAL"),
                new VerifiedHospital("Narayana Institute of Cardiac Sciences", "258/A, Bommasandra Industrial Area", "Bengaluru", "Karnataka", "560099", 12.8152, 77.6942, "PLACE-IN-BLR-NARAYANA", true, "VERIFIED", "+918071222222", "INTERNAL_REGISTRY", "HOSPITAL"),
                new VerifiedHospital("Tata Memorial Hospital", "Dr. E Borges Road, Parel", "Mumbai", "Maharashtra", "400012", 19.0048, 72.8433, "PLACE-IN-BOM-TMH", true, "VERIFIED", "+912224177000", "INTERNAL_REGISTRY", "HOSPITAL"),
                new VerifiedHospital("Lilavati Hospital and Research Centre", "A-791, Bandra Reclamation, Bandra West", "Mumbai", "Maharashtra", "400050", 19.0514, 72.8291, "PLACE-IN-BOM-LILAVATI", true, "VERIFIED", "+912226751000", "INTERNAL_REGISTRY", "HOSPITAL"),
                new VerifiedHospital("Nizam's Institute of Medical Sciences", "Punjagutta Road", "Hyderabad", "Telangana", "500082", 17.4222, 78.4529, "PLACE-IN-HYD-NIMS", true, "VERIFIED", "+914023489000", "INTERNAL_REGISTRY", "HOSPITAL")
            );
            repository.saveAll(hospitals);
            log.info("Successfully seeded {} authoritative verified hospitals.", hospitals.size());
        }
    }
}
