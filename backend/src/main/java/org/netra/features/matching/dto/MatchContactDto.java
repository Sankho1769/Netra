package org.netra.features.matching.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.netra.features.donor.entity.BloodGroup;
import org.netra.features.matching.entity.MatchStatus;

import java.util.UUID;

/**
 * Authoritative contact information revealed strictly to confirmed match participants
 * (Requester and Accepted Donor) following mutual confirmation.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MatchContactDto {

    private UUID matchId;
    private UUID bloodRequestId;
    private MatchStatus matchStatus;
    private UUID donorUserId;
    private String donorName;
    private String donorPhone;
    private BloodGroup donorBloodGroup;
    private UUID requesterUserId;
    private String requesterName;
    private String requesterPhone;
    private String hospitalName;
    private String hospitalAddress;
    private String city;
    private String state;
    private String coordinationNotice =
            "Contact details are shared strictly for blood donation coordination. Commercial transactions, harassment, or misuse are strictly prohibited.";

    public MatchContactDto() {
    }

    public MatchContactDto(
            UUID matchId,
            UUID bloodRequestId,
            MatchStatus matchStatus,
            UUID donorUserId,
            String donorName,
            String donorPhone,
            BloodGroup donorBloodGroup,
            UUID requesterUserId,
            String requesterName,
            String requesterPhone,
            String hospitalName,
            String hospitalAddress,
            String city,
            String state) {
        this.matchId = matchId;
        this.bloodRequestId = bloodRequestId;
        this.matchStatus = matchStatus;
        this.donorUserId = donorUserId;
        this.donorName = donorName;
        this.donorPhone = donorPhone;
        this.donorBloodGroup = donorBloodGroup;
        this.requesterUserId = requesterUserId;
        this.requesterName = requesterName;
        this.requesterPhone = requesterPhone;
        this.hospitalName = hospitalName;
        this.hospitalAddress = hospitalAddress;
        this.city = city;
        this.state = state;
    }

    public UUID getMatchId() {
        return matchId;
    }

    public void setMatchId(UUID matchId) {
        this.matchId = matchId;
    }

    public UUID getBloodRequestId() {
        return bloodRequestId;
    }

    public void setBloodRequestId(UUID bloodRequestId) {
        this.bloodRequestId = bloodRequestId;
    }

    public MatchStatus getMatchStatus() {
        return matchStatus;
    }

    public void setMatchStatus(MatchStatus matchStatus) {
        this.matchStatus = matchStatus;
    }

    public UUID getDonorUserId() {
        return donorUserId;
    }

    public void setDonorUserId(UUID donorUserId) {
        this.donorUserId = donorUserId;
    }

    public String getDonorName() {
        return donorName;
    }

    public void setDonorName(String donorName) {
        this.donorName = donorName;
    }

    public String getDonorPhone() {
        return donorPhone;
    }

    public void setDonorPhone(String donorPhone) {
        this.donorPhone = donorPhone;
    }

    public BloodGroup getDonorBloodGroup() {
        return donorBloodGroup;
    }

    public void setDonorBloodGroup(BloodGroup donorBloodGroup) {
        this.donorBloodGroup = donorBloodGroup;
    }

    public UUID getRequesterUserId() {
        return requesterUserId;
    }

    public void setRequesterUserId(UUID requesterUserId) {
        this.requesterUserId = requesterUserId;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
    }

    public String getRequesterPhone() {
        return requesterPhone;
    }

    public void setRequesterPhone(String requesterPhone) {
        this.requesterPhone = requesterPhone;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
    }

    public String getHospitalAddress() {
        return hospitalAddress;
    }

    public void setHospitalAddress(String hospitalAddress) {
        this.hospitalAddress = hospitalAddress;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCoordinationNotice() {
        return coordinationNotice;
    }

    public void setCoordinationNotice(String coordinationNotice) {
        this.coordinationNotice = coordinationNotice;
    }
}
