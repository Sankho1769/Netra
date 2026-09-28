package org.netra.features.hospital;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.netra.features.hospital.provider.NominatimHospitalPlacesProvider;

import static org.junit.jupiter.api.Assertions.*;

class NominatimHospitalPlacesProviderTest {

    private NominatimHospitalPlacesProvider provider;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        provider = new NominatimHospitalPlacesProvider(
                objectMapper,
                "https://nominatim.openstreetmap.org/search",
                3000,
                true
        );
    }

    @Test
    @DisplayName("isAuthoritativeHealthcareInstitution: correctly identifies valid hospitals and clinics")
    void testHealthcareValidation_Valid() {
        ObjectNode hospitalNode = objectMapper.createObjectNode();
        hospitalNode.put("class", "amenity");
        hospitalNode.put("type", "hospital");
        hospitalNode.put("display_name", "SSKM Hospital, Kolkata, West Bengal");
        assertTrue(provider.isAuthoritativeHealthcareInstitution(hospitalNode));

        ObjectNode clinicNode = objectMapper.createObjectNode();
        clinicNode.put("class", "healthcare");
        clinicNode.put("type", "clinic");
        clinicNode.put("display_name", "Apollo Clinic, Salt Lake");
        assertTrue(provider.isAuthoritativeHealthcareInstitution(clinicNode));

        ObjectNode textNode = objectMapper.createObjectNode();
        textNode.put("class", "place");
        textNode.put("type", "house");
        textNode.put("display_name", "National Medical College and Hospital, Kolkata");
        assertTrue(provider.isAuthoritativeHealthcareInstitution(textNode));
    }

    @Test
    @DisplayName("isAuthoritativeHealthcareInstitution: filters out unrelated commercial businesses")
    void testHealthcareValidation_FiltersNonMedical() {
        ObjectNode shopNode = objectMapper.createObjectNode();
        shopNode.put("class", "shop");
        shopNode.put("type", "clothes");
        shopNode.put("display_name", "Hospital Road Fashion Boutique, Bangalore");
        assertFalse(provider.isAuthoritativeHealthcareInstitution(shopNode));

        ObjectNode cafeNode = objectMapper.createObjectNode();
        cafeNode.put("class", "amenity");
        cafeNode.put("type", "restaurant");
        cafeNode.put("display_name", "Medical Canteen, Delhi");
        assertFalse(provider.isAuthoritativeHealthcareInstitution(cafeNode));
    }

    @Test
    @DisplayName("searchHealthcarePlaces: rejects fraudulent query patterns immediately")
    void testFraudulentQueryBlocked() {
        var results = provider.searchHealthcarePlaces("Fake Scam Hospital", "Mumbai");
        assertTrue(results.isEmpty(), "Fraudulent queries must be blocked before network call");
    }

    @Test
    @DisplayName("searchHealthcarePlaces: returns empty on blank input or disabled provider")
    void testBlankOrDisabled() {
        assertTrue(provider.searchHealthcarePlaces("", null).isEmpty());
        assertTrue(provider.searchHealthcarePlaces("   ", "Delhi").isEmpty());

        NominatimHospitalPlacesProvider disabledProvider = new NominatimHospitalPlacesProvider(
                objectMapper, "https://nominatim.openstreetmap.org/search", 3000, false
        );
        assertTrue(disabledProvider.searchHealthcarePlaces("SSKM", "Kolkata").isEmpty());
    }
}
