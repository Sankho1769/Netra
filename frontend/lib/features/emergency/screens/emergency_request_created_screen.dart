import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../../../common/widgets/common_widgets.dart';
import '../../../core/responsive/responsive.dart';
import '../../../core/theme/netra_colors.dart';
import '../../../core/theme/netra_spacing.dart';
import '../../../core/theme/netra_typography.dart';
import '../../blood_request/models/blood_request.dart';
import '../../blood_request/screens/blood_request_details_screen.dart';
import '../../matching/screens/donor_matches_screen.dart';
import 'emergency_nearby_screen.dart';

class EmergencyRequestCreatedScreen extends StatelessWidget {
  final BloodRequestDetail request;

  const EmergencyRequestCreatedScreen({super.key, required this.request});

  @override
  Widget build(BuildContext context) {
    final dateFormat = DateFormat('MMM d, y • h:mm a');
    final formattedDeadline = dateFormat.format(request.requiredBy.toLocal());

    return ResponsiveScaffold(
      appBar: NetraAppBar(
        title: "Request Submitted",
        showBackButton: false,
      ),
      body: ResponsiveContainer.standard(
        scrollable: true,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Success & Alert Banner
            Container(
              padding: NetraSpacing.cardPaddingSpacious,
              decoration: BoxDecoration(
                color: const Color(0xFFFEF2F2),
                borderRadius: BorderRadius.circular(NetraSpacing.radiusLg),
                border: Border.all(color: const Color(0xFFFCA5A5), width: 1.5),
              ),
              child: Column(
                children: [
                  Container(
                    padding: const EdgeInsets.all(12),
                    decoration: const BoxDecoration(
                      color: Color(0xFFDC2626),
                      shape: BoxShape.circle,
                    ),
                    child: const Icon(Icons.check_rounded, color: NetraColors.surfaceWhite, size: 36),
                  ),
                  NetraSpacing.gapH12,
                  Text(
                    "Emergency Request Submitted",
                    style: NetraTypography.titleLarge.copyWith(
                      fontWeight: FontWeight.bold,
                      color: const Color(0xFF991B1B),
                    ),
                    textAlign: TextAlign.center,
                  ),
                  NetraSpacing.gapH8,
                  Text(
                    "This creates a CRITICAL blood request that becomes available through NETRA's supported emergency and discovery workflows.",
                    style: NetraTypography.bodyMedium.copyWith(color: NetraColors.textPrimary),
                    textAlign: TextAlign.center,
                  ),
                ],
              ),
            ),
            NetraSpacing.gapH24,

            // Request Summary Card
            Text(
              "Request Summary",
              style: NetraTypography.titleMedium.copyWith(fontWeight: FontWeight.bold),
            ),
            NetraSpacing.gapH8,
            Container(
              padding: NetraSpacing.cardPaddingStandard,
              decoration: BoxDecoration(
                color: NetraColors.surfaceWhite,
                borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
                border: Border.all(color: NetraColors.borderSubtle),
              ),
              child: Column(
                children: [
                  _buildSummaryRow("Blood Group", request.bloodGroup, isHighlight: true),
                  const Divider(height: 16),
                  _buildSummaryRow("Units Needed", "${request.unitsRequired} Units"),
                  const Divider(height: 16),
                  _buildSummaryRow("Hospital", request.hospitalName),
                  const Divider(height: 16),
                  _buildSummaryRow("Location", "${request.city}, ${request.state}"),
                  const Divider(height: 16),
                  _buildSummaryRow("Deadline", formattedDeadline),
                  const Divider(height: 16),
                  _buildSummaryRow("Priority", "CRITICAL", badgeColor: const Color(0xFFDC2626)),
                ],
              ),
            ),
            NetraSpacing.gapH20,

            // Hospital Advisory
            Container(
              padding: NetraSpacing.cardPaddingStandard,
              decoration: BoxDecoration(
                color: const Color(0xFFEFF6FF),
                borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
                border: Border.all(color: const Color(0xFFBFDBFE)),
              ),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Icon(Icons.info_outline_rounded, color: Color(0xFF2563EB), size: 22),
                  NetraSpacing.gapW12,
                  Expanded(
                    child: Text(
                      "Next Step: Please contact the hospital blood bank or nearby certified blood centers directly to check unit availability and coordinate fulfillment.",
                      style: NetraTypography.bodySmall.copyWith(color: const Color(0xFF1E40AF)),
                    ),
                  ),
                ],
              ),
            ),
            NetraSpacing.gapH16,

            // Truthful Privacy Notice
            Container(
              padding: NetraSpacing.cardPaddingStandard,
              decoration: BoxDecoration(
                color: NetraColors.backgroundGray,
                borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
                border: Border.all(color: NetraColors.borderSubtle),
              ),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Icon(Icons.shield_outlined, color: NetraColors.primaryRed, size: 20),
                  NetraSpacing.gapW12,
                  Expanded(
                    child: Text(
                      "NETRA does not expose donor personal contact information. Available contact and fulfillment actions are handled through the supported NETRA workflow and verified blood-bank processes.",
                      style: NetraTypography.bodySmall.copyWith(color: NetraColors.textSecondary),
                    ),
                  ),
                ],
              ),
            ),
            NetraSpacing.gapH24,

            // Action Buttons
            SizedBox(
              width: double.infinity,
              height: 48,
              child: ElevatedButton.icon(
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFFDC2626),
                  foregroundColor: NetraColors.surfaceWhite,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
                  ),
                ),
                onPressed: () {
                  Navigator.of(context).push(
                    MaterialPageRoute(
                      builder: (context) => BloodRequestDetailsScreen(requestId: request.id),
                    ),
                  );
                },
                icon: const Icon(Icons.visibility_outlined, size: 20),
                label: const Text("View Request Details & Status"),
              ),
            ),
            NetraSpacing.gapH12,
            SizedBox(
              width: double.infinity,
              height: 48,
              child: ElevatedButton.icon(
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFFB91C1C),
                  foregroundColor: NetraColors.surfaceWhite,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
                  ),
                ),
                onPressed: () {
                  Navigator.of(context).push(
                    MaterialPageRoute(
                      builder: (context) => DonorMatchesScreen(
                        requestId: request.id,
                        targetBloodGroup: request.bloodGroup,
                        hospitalName: request.hospitalName,
                      ),
                    ),
                  );
                },
                icon: const Icon(Icons.person_search_rounded, size: 20),
                label: const Text("View Matching Donors"),
              ),
            ),
            NetraSpacing.gapH12,
            SizedBox(
              width: double.infinity,
              height: 48,
              child: OutlinedButton.icon(
                style: OutlinedButton.styleFrom(
                  foregroundColor: const Color(0xFF2563EB),
                  side: const BorderSide(color: Color(0xFF2563EB)),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
                  ),
                ),
                onPressed: () {
                  Navigator.of(context).push(
                    MaterialPageRoute(
                      builder: (context) => const EmergencyNearbyScreen(initialTabIndex: 0),
                    ),
                  );
                },
                icon: const Icon(Icons.local_hospital_outlined, size: 20),
                label: const Text("Find Nearby Blood Centres"),
              ),
            ),
            NetraSpacing.gapH12,
            SizedBox(
              width: double.infinity,
              height: 48,
              child: TextButton(
                onPressed: () {
                  Navigator.of(context).popUntil((route) => route.isFirst);
                },
                child: const Text("Return to Home Dashboard"),
              ),
            ),
            NetraSpacing.gapH24,
          ],
        ),
      ),
    );
  }

  Widget _buildSummaryRow(String label, String value, {bool isHighlight = false, Color? badgeColor}) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(label, style: NetraTypography.bodyMedium.copyWith(color: NetraColors.textSecondary)),
        if (badgeColor != null)
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
            decoration: BoxDecoration(
              color: badgeColor,
              borderRadius: BorderRadius.circular(NetraSpacing.radiusSm),
            ),
            child: Text(
              value,
              style: NetraTypography.labelSmall.copyWith(color: NetraColors.surfaceWhite, fontWeight: FontWeight.bold),
            ),
          )
        else
          Text(
            value,
            style: NetraTypography.titleSmall.copyWith(
              fontWeight: isHighlight ? FontWeight.bold : FontWeight.w600,
              color: isHighlight ? const Color(0xFFDC2626) : NetraColors.textPrimary,
            ),
          ),
      ],
    );
  }
}
