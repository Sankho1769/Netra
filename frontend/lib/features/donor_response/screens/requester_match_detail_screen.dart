import 'package:flutter/material.dart';
import '../models/donor_match_response_model.dart';
import '../widgets/match_status_badge.dart';

/// Screen presenting safe, privacy-preserving details of a matched donor to the requester.
class RequesterMatchDetailScreen extends StatelessWidget {
  final RequesterDonorMatch match;

  const RequesterMatchDetailScreen({
    super.key,
    required this.match,
  });

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Donor Match Details'),
        backgroundColor: const Color(0xFFDC2626),
        foregroundColor: Colors.white,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            _buildPrivacySafeguardBanner(),
            const SizedBox(height: 12),
            _buildClinicalDisclaimerBanner(),
            const SizedBox(height: 16),
            _buildDonorOverviewCard(),
            const SizedBox(height: 16),
            _buildMatchTimelineCard(),
          ],
        ),
      ),
    );
  }

  Widget _buildClinicalDisclaimerBanner() {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: const Color(0xFFFFFBEB),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: const Color(0xFFFDE68A), width: 1),
      ),
      child: const Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.info_outline, color: Color(0xFFB45309), size: 20),
          SizedBox(width: 10),
          Expanded(
            child: Text(
              'Clinical Advisory: Matching status and preliminary compatibility do not guarantee a donor\'s medical clearance or current donation suitability. Official screening, crossmatching, and eligibility verification must be conducted by certified blood bank or hospital staff.',
              style: TextStyle(
                fontSize: 12.5,
                color: Color(0xFF92400E),
                height: 1.35,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildPrivacySafeguardBanner() {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: const Color(0xFFEFF6FF),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: const Color(0xFF93C5FD), width: 1),
      ),
      child: const Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.shield_outlined, color: Color(0xFF1D4ED8), size: 20),
          SizedBox(width: 10),
          Expanded(
            child: Text(
              'NETRA Privacy Guarantee: Donor phone numbers, email addresses, exact home/work locations, and medical questionnaire answers are strictly protected and never disclosed.',
              style: TextStyle(
                fontSize: 12.5,
                color: Color(0xFF1E3A8A),
                height: 1.35,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildDonorOverviewCard() {
    return Card(
      elevation: 2,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  match.donorDisplayName,
                  style: const TextStyle(
                      fontSize: 18, fontWeight: FontWeight.bold),
                ),
                MatchStatusBadge(status: match.responseStatus),
              ],
            ),
            const Divider(height: 24),
            _buildDetailRow(
              Icons.bloodtype_outlined,
              'Blood Group',
              match.bloodGroup,
              trailingWidget: match.isVerified
                  ? Container(
                      padding: const EdgeInsets.symmetric(
                          horizontal: 8, vertical: 2),
                      decoration: BoxDecoration(
                        color: const Color(0xFFDCFCE7),
                        borderRadius: BorderRadius.circular(4),
                      ),
                      child: const Text(
                        'Verified at matching',
                        style: TextStyle(
                          fontSize: 11,
                          color: Color(0xFF15803D),
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    )
                  : null,
            ),
            _buildDetailRow(
              Icons.check_circle_outline,
              'Availability',
              match.availabilityStatus,
            ),
            if (match.distanceKm != null)
              _buildDetailRow(
                Icons.route_outlined,
                'Approx Distance',
                '${match.distanceKm!.toStringAsFixed(1)} km away',
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildMatchTimelineCard() {
    return Card(
      elevation: 2,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('Match Timeline',
                style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold)),
            const SizedBox(height: 12),
            _buildDetailRow(
              Icons.schedule_outlined,
              'Matched At',
              match.createdAt.toLocal().toString().substring(0, 16),
            ),
            _buildDetailRow(
              Icons.timer_off_outlined,
              'Response Expiration',
              match.expiresAt.toLocal().toString().substring(0, 16),
            ),
            if (match.respondedAt != null)
              _buildDetailRow(
                Icons.done_all_outlined,
                'Responded At',
                match.respondedAt!.toLocal().toString().substring(0, 16),
              ),
            _buildDetailRow(
              Icons.history_outlined,
              'Last Updated',
              match.updatedAt.toLocal().toString().substring(0, 16),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildDetailRow(IconData icon, String label, String value,
      {Widget? trailingWidget}) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, size: 18, color: Colors.grey.shade600),
          const SizedBox(width: 10),
          SizedBox(
            width: 130,
            child: Text(label,
                style: TextStyle(color: Colors.grey.shade600, fontSize: 13)),
          ),
          Expanded(
            child: Row(
              children: [
                Text(
                  value,
                  style: const TextStyle(
                      fontWeight: FontWeight.w600, fontSize: 13),
                ),
                if (trailingWidget != null) ...[
                  const SizedBox(width: 8),
                  trailingWidget,
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }
}
