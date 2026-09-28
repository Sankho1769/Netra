package org.netra.features.hospital.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.netra.features.hospital.dto.VerifiedHospitalDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;

/**
 * OpenStreetMap Nominatim implementation of HospitalPlacesProvider.
 * Provides live, dynamic multi-region hospital resolution across Kolkata, West Bengal,
 * Delhi, Mumbai, Bengaluru, and all regions of India.
 *
 * Strict healthcare classification filters ensure non-medical commercial entities
 * cannot spoof hospitals or clinical registries.
 */
@Component
public class NominatimHospitalPlacesProvider implements HospitalPlacesProvider {

    private static final Logger log = LoggerFactory.getLogger(NominatimHospitalPlacesProvider.class);

    private static final Set<String> ALLOWED_CLASSES = Set.of("amenity", "healthcare");
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "hospital", "clinic", "doctors", "blood_bank", "healthcare", "health_post", "nursing_home"
    );

    private static final Pattern MEDICAL_KEYWORD_PATTERN = Pattern.compile(
            ".*\\b(hospital|clinic|medical|health|dispensary|arogya|swasthya|nursing home|care center|blood bank)\\b.*",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern FRAUD_BLACKLIST_PATTERN = Pattern.compile(
            ".*\\b(fake|scam|fraud|bogus|test hospital|dummy clinic|nowhere)\\b.*",
            Pattern.CASE_INSENSITIVE
    );

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final Duration requestTimeout;
    private final boolean enabled;

    public NominatimHospitalPlacesProvider(
            ObjectMapper objectMapper,
            @Value("${netra.places.nominatim.url:https://nominatim.openstreetmap.org/search}") String baseUrl,
            @Value("${netra.places.nominatim.timeout-ms:3000}") long timeoutMs,
            @Value("${netra.places.nominatim.enabled:true}") boolean enabled) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.baseUrl = baseUrl;
        this.requestTimeout = Duration.ofMillis(timeoutMs > 0 ? timeoutMs : 3000);
        this.enabled = enabled;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(this.requestTimeout)
                .build();
    }

    @Override
    public List<VerifiedHospitalDto> searchHealthcarePlaces(String query, String city) {
        if (!enabled || query == null || query.trim().isBlank()) {
            return Collections.emptyList();
        }

        String rawQuery = query.trim();
        if (FRAUD_BLACKLIST_PATTERN.matcher(rawQuery).matches()) {
            log.warn("Blocked places lookup for fraudulent query: {}", rawQuery);
            return Collections.emptyList();
        }

        // Build search query. Append medical keyword if user typed a general name without hospital/clinic
        String effectiveQuery = rawQuery;
        if (!MEDICAL_KEYWORD_PATTERN.matcher(rawQuery).matches()) {
            effectiveQuery = rawQuery + " hospital";
        }
        if (city != null && !city.trim().isBlank() && !rawQuery.toLowerCase().contains(city.trim().toLowerCase())) {
            effectiveQuery = effectiveQuery + " " + city.trim();
        }

        try {
            String encodedQuery = URLEncoder.encode(effectiveQuery, StandardCharsets.UTF_8);
            String url = String.format(
                    "%s?format=json&addressdetails=1&countrycodes=in&limit=10&q=%s",
                    baseUrl, encodedQuery
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Netra-Blood-Network/1.0 (production-audit@netra.org)")
                    .header("Accept", "application/json")
                    .timeout(requestTimeout)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("Nominatim places lookup returned non-200 status: {} for query: {}", response.statusCode(), effectiveQuery);
                return Collections.emptyList();
            }

            JsonNode root = objectMapper.readTree(response.body());
            if (!root.isArray()) {
                return Collections.emptyList();
            }

            List<VerifiedHospitalDto> results = new ArrayList<>();
            for (JsonNode item : root) {
                if (!isAuthoritativeHealthcareInstitution(item)) {
                    continue;
                }

                VerifiedHospitalDto dto = parseItemToDto(item);
                if (dto != null) {
                    results.add(dto);
                }
            }

            return results;

        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("Nominatim places provider failed or timed out: {}", ex.getMessage());
            return Collections.emptyList();
        } catch (Exception ex) {
            log.warn("Unexpected error in Nominatim places provider: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    private static final Set<String> DISALLOWED_TYPES = Set.of(
            "restaurant", "cafe", "fast_food", "bar", "pub", "bank", "fuel", "parking",
            "hotel", "guest_house", "hostel", "clothes", "shop", "supermarket", "convenience"
    );

    /**
     * Validates that the place returned by the external provider is genuinely a medical/healthcare institution.
     */
    public boolean isAuthoritativeHealthcareInstitution(JsonNode item) {
        if (item == null) return false;

        String itemClass = item.path("class").asText("").toLowerCase();
        String itemType = item.path("type").asText("").toLowerCase();
        String displayName = item.path("display_name").asText("");

        // Immediately reject non-medical commercial or hospitality entities
        if (DISALLOWED_TYPES.contains(itemType) || itemClass.equals("shop")) {
            return false;
        }

        // Check if class and type match healthcare amenities
        if (ALLOWED_CLASSES.contains(itemClass) && ALLOWED_TYPES.contains(itemType)) {
            return true;
        }

        // Check if display name confirms medical institution
        if (MEDICAL_KEYWORD_PATTERN.matcher(displayName).matches()) {
            return true;
        }

        return false;
    }

    private VerifiedHospitalDto parseItemToDto(JsonNode item) {
        try {
            String displayName = item.path("display_name").asText("");
            if (displayName.isBlank()) return null;

            JsonNode addressNode = item.path("address");
            String hospitalName = extractHospitalName(item, addressNode, displayName);

            String city = extractCity(addressNode);
            String state = addressNode.path("state").asText("").trim();
            String postalCode = addressNode.path("postcode").asText("").trim();
            String address = buildFormattedAddress(addressNode, displayName);

            double lat = Double.parseDouble(item.path("lat").asText("0.0"));
            double lon = Double.parseDouble(item.path("lon").asText("0.0"));
            long osmId = item.path("osm_id").asLong(0L);
            String placeId = osmId > 0 ? "OSM-" + osmId : "EXT-" + Math.abs(displayName.hashCode());

            String placeType = item.path("type").asText("hospital").toUpperCase();
            if (placeType.isBlank() || placeType.equals("AMENITY")) {
                placeType = "HOSPITAL";
            }

            boolean hasBloodBank = displayName.toLowerCase().contains("blood") ||
                    hospitalName.toLowerCase().contains("blood") ||
                    placeType.contains("BLOOD");

            return new VerifiedHospitalDto(
                    null,
                    hospitalName,
                    address,
                    city,
                    state,
                    postalCode,
                    placeId,
                    hasBloodBank,
                    "VERIFIED",
                    null,
                    lat,
                    lon,
                    "EXTERNAL_PROVIDER",
                    placeType
            );
        } catch (Exception ex) {
            log.warn("Error parsing places item: {}", ex.getMessage());
            return null;
        }
    }

    private String extractHospitalName(JsonNode item, JsonNode addressNode, String displayName) {
        if (addressNode.hasNonNull("hospital")) {
            return addressNode.path("hospital").asText().trim();
        }
        if (addressNode.hasNonNull("clinic")) {
            return addressNode.path("clinic").asText().trim();
        }
        if (addressNode.hasNonNull("amenity")) {
            return addressNode.path("amenity").asText().trim();
        }

        // Parse first part of display name before comma
        String[] parts = displayName.split(",");
        if (parts.length > 0 && !parts[0].trim().isBlank()) {
            return parts[0].trim();
        }

        return displayName;
    }

    private String extractCity(JsonNode addressNode) {
        if (addressNode.hasNonNull("city") && !addressNode.path("city").asText().isBlank()) {
            return addressNode.path("city").asText().trim();
        }
        if (addressNode.hasNonNull("town") && !addressNode.path("town").asText().isBlank()) {
            return addressNode.path("town").asText().trim();
        }
        if (addressNode.hasNonNull("state_district") && !addressNode.path("state_district").asText().isBlank()) {
            return addressNode.path("state_district").asText().trim();
        }
        if (addressNode.hasNonNull("suburb") && !addressNode.path("suburb").asText().isBlank()) {
            return addressNode.path("suburb").asText().trim();
        }
        return "Unknown";
    }

    private String buildFormattedAddress(JsonNode addressNode, String fallbackDisplayName) {
        List<String> addressParts = new ArrayList<>();
        if (addressNode.hasNonNull("road") && !addressNode.path("road").asText().isBlank()) {
            addressParts.add(addressNode.path("road").asText().trim());
        }
        if (addressNode.hasNonNull("suburb") && !addressNode.path("suburb").asText().isBlank()) {
            addressParts.add(addressNode.path("suburb").asText().trim());
        }
        if (addressNode.hasNonNull("city") && !addressNode.path("city").asText().isBlank()) {
            addressParts.add(addressNode.path("city").asText().trim());
        }

        if (addressParts.isEmpty()) {
            String[] parts = fallbackDisplayName.split(",");
            if (parts.length > 1) {
                return parts[1].trim();
            }
            return fallbackDisplayName;
        }

        return String.join(", ", addressParts);
    }
}
