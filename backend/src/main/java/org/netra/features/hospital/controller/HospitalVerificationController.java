package org.netra.features.hospital.controller;

import jakarta.validation.Valid;
import org.netra.features.hospital.dto.HospitalVerificationResultDto;
import org.netra.features.hospital.dto.VerifiedHospitalDto;
import org.netra.features.hospital.dto.VerifyHospitalRequest;
import org.netra.features.hospital.service.HospitalVerificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hospitals")
public class HospitalVerificationController {

    private final HospitalVerificationService hospitalVerificationService;

    public HospitalVerificationController(HospitalVerificationService hospitalVerificationService) {
        this.hospitalVerificationService = hospitalVerificationService;
    }

    /**
     * Search for verified hospitals and healthcare centers by name, address, or city.
     */
    @GetMapping("/search")
    public ResponseEntity<List<VerifiedHospitalDto>> searchHospitals(
            @RequestParam(name = "query") String query,
            @RequestParam(name = "city", required = false) String city) {
        List<VerifiedHospitalDto> results = hospitalVerificationService.searchHospitals(query, city);
        return ResponseEntity.ok(results);
    }

    /**
     * Verify whether a specified hospital/place exists in the authoritative registry.
     */
    @PostMapping("/verify")
    public ResponseEntity<HospitalVerificationResultDto> verifyHospital(
            @Valid @RequestBody VerifyHospitalRequest request) {
        HospitalVerificationResultDto result = hospitalVerificationService.verifyHospital(request);
        return ResponseEntity.ok(result);
    }
}
