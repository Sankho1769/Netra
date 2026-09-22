import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../../../core/responsive/responsive.dart';
import '../../../core/theme/netra_colors.dart';
import '../../../core/theme/netra_spacing.dart';
import '../../../core/theme/netra_typography.dart';
import '../../../common/widgets/common_widgets.dart';
import '../../bloodbank/screens/nearby_blood_banks_screen.dart';
import '../models/eligibility_models.dart';

class EligibilityResultScreen extends StatelessWidget {
  final EligibilityResult result;

  const EligibilityResultScreen({super.key, required this.result});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: NetraColors.backgroundGray,
      appBar: NetraAppBar(
        title: "Pre-Screening Result",
        showBackButton: false,
        actions: [
          IconButton(
            icon: const Icon(Icons.close_rounded),
            tooltip: 'Close and Return Home',
            onPressed: () =>
                Navigator.of(context).popUntil((route) => route.isFirst),
          ),
        ],
      ),
      body: SafeArea(
        child: ResponsiveContainer.reading(
          scrollable: true,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Result Status Card
              _buildStatusCard(context),
              NetraSpacing.gapH20,

              // Prominent Medical Disclaimer Banner (Mandatory)
              NetraDisclaimerBanner(customText: result.disclaimer),
              NetraSpacing.gapH24,

              // Deferral Details (if TEMPORARY_DEFERRAL or MEDICAL_REVIEW_REQUIRED)
              if (result.deferralReasons.isNotEmpty) ...[
                Text(
                  "Assessment Details",
                  style: NetraTypography.titleLarge,
                ),
                NetraSpacing.gapH12,
                ...result.deferralReasons
                    .map((reason) => _buildDeferralReasonCard(reason)),
                NetraSpacing.gapH24,
              ],

              // Missing Fields (if INSUFFICIENT_INFORMATION)
              if (result.missingFields.isNotEmpty) ...[
                Text(
                  "Missing Information",
                  style: NetraTypography.titleLarge,
                ),
                NetraSpacing.gapH12,
                Container(
                  padding: NetraSpacing.cardPadding,
                  decoration: BoxDecoration(
                    color: NetraColors.insufficientBlueBg,
                    borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
                    border:
                        Border.all(color: NetraColors.insufficientBlueBorder),
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        "The following required fields were missing from your responses:",
                        style: NetraTypography.bodyMedium
                            .copyWith(color: NetraColors.textPrimary),
                      ),
                      NetraSpacing.gapH8,
                      ...result.missingFields.map((field) => Padding(
                            padding: const EdgeInsets.symmetric(vertical: 2),
                            child: Row(
                              children: [
                                const Icon(Icons.fiber_manual_record,
                                    size: 8,
                                    color: NetraColors.insufficientBlue),
                                NetraSpacing.gapW8,
                                Text(
                                  field,
                                  style: NetraTypography.titleSmall,
                                ),
                              ],
                            ),
                          )),
                    ],
                  ),
                ),
                NetraSpacing.gapH24,
              ],

              // Next Steps Section
              Text(
                "Recommended Actions",
                style: NetraTypography.titleLarge,
              ),
              NetraSpacing.gapH12,
              ..._buildActionButtons(context),
              NetraSpacing.gapH32,
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildStatusCard(BuildContext context) {
    Color cardBg;
    Color borderColor;
    Color iconColor;
    IconData statusIcon;
    String badgeText;

    switch (result.result) {
      case ResultType.LIKELY_ELIGIBLE:
        cardBg = NetraColors.eligibleGreenBg;
        borderColor = NetraColors.eligibleGreenBorder;
        iconColor = NetraColors.eligibleGreen;
        statusIcon = Icons.check_circle_rounded;
        badgeText = "LIKELY ELIGIBLE";
        break;
      case ResultType.TEMPORARY_DEFERRAL:
        cardBg = NetraColors.deferralAmberBg;
        borderColor = NetraColors.deferralAmberBorder;
        iconColor = NetraColors.deferralAmber;
        statusIcon = Icons.schedule_rounded;
        badgeText = "TEMPORARY DEFERRAL";
        break;
      case ResultType.MEDICAL_REVIEW_REQUIRED:
        cardBg = NetraColors.medicalReviewOrangeBg;
        borderColor = NetraColors.medicalReviewOrangeBorder;
        iconColor = NetraColors.medicalReviewOrange;
        statusIcon = Icons.medical_services_outlined;
        badgeText = "MEDICAL REVIEW REQUIRED";
        break;
      case ResultType.INSUFFICIENT_INFORMATION:
        cardBg = NetraColors.insufficientBlueBg;
        borderColor = NetraColors.insufficientBlueBorder;
        iconColor = NetraColors.insufficientBlue;
        statusIcon = Icons.info_outline_rounded;
        badgeText = "INFORMATION NEEDED";
        break;
    }

    return Container(
      width: double.infinity,
      padding: NetraSpacing.cardPaddingSpacious,
      decoration: BoxDecoration(
        color: cardBg,
        borderRadius: BorderRadius.circular(NetraSpacing.radiusLg),
        border: Border.all(color: borderColor, width: 1.5),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                decoration: BoxDecoration(
                  color: iconColor,
                  borderRadius: BorderRadius.circular(NetraSpacing.radiusFull),
                ),
                child: Text(
                  badgeText,
                  style: NetraTypography.labelSmall.copyWith(
                    color: NetraColors.surfaceWhite,
                    letterSpacing: 0.5,
                  ),
                ),
              ),
              const Spacer(),
              Icon(statusIcon, color: iconColor, size: 28),
            ],
          ),
          NetraSpacing.gapH16,
          Text(
            result.title,
            style: NetraTypography.headlineMedium.copyWith(color: iconColor),
          ),
          NetraSpacing.gapH8,
          Text(
            result.message,
            style: NetraTypography.bodyLarge,
          ),

          // Safe estimated next eligible date display
          if (result.estimatedNextEligibleDate != null) ...[
            NetraSpacing.gapH16,
            Container(
              padding: const EdgeInsets.symmetric(
                  horizontal: NetraSpacing.md, vertical: 10),
              decoration: BoxDecoration(
                color: NetraColors.surfaceWhite,
                borderRadius: BorderRadius.circular(NetraSpacing.radiusSm),
                border: Border.all(color: borderColor),
              ),
              child: Row(
                children: [
                  Icon(Icons.event_available_rounded,
                      size: 20, color: iconColor),
                  NetraSpacing.gapW12,
                  Expanded(
                    child: Text(
                      "Estimated Next Eligible Date: ${DateFormat.yMMMMd().format(result.estimatedNextEligibleDate!)}",
                      style:
                          NetraTypography.titleSmall.copyWith(color: iconColor),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildDeferralReasonCard(DeferralReason reason) {
    return Container(
      margin: const EdgeInsets.only(bottom: NetraSpacing.sm),
      padding: NetraSpacing.cardPadding,
      decoration: BoxDecoration(
        color: NetraColors.surfaceWhite,
        borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
        border: Border.all(color: NetraColors.borderGray),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            reason.displayText,
            style: NetraTypography.titleSmall,
          ),
          NetraSpacing.gapH4,
          Text(
            reason.recommendedAction,
            style: NetraTypography.bodyMedium,
          ),
        ],
      ),
    );
  }

  List<Widget> _buildActionButtons(BuildContext context) {
    switch (result.result) {
      case ResultType.LIKELY_ELIGIBLE:
        return [
          NetraButton(
            text: "Find Nearby Blood Banks",
            icon: Icons.location_on_outlined,
            onPressed: () {
              Navigator.of(context).push(
                MaterialPageRoute(
                    builder: (context) => const NearbyBloodBanksScreen()),
              );
            },
          ),
          NetraSpacing.gapH12,
          NetraButton.outlined(
            text: "Find Donation Events",
            icon: Icons.event_outlined,
            onPressed: () {
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(
                    content: Text("Upcoming blood donation events loaded.")),
              );
            },
          ),
          NetraSpacing.gapH12,
          NetraButton.outlined(
            text: "Register for Donation",
            icon: Icons.app_registration_rounded,
            onPressed: () {
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(
                    content: Text("Opening donor appointment registration.")),
              );
            },
          ),
        ];

      case ResultType.TEMPORARY_DEFERRAL:
        return [
          if (result.estimatedNextEligibleDate != null) ...[
            NetraButton(
              text: "Set Reminder for Next Date",
              icon: Icons.notifications_active_outlined,
              onPressed: () {
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(
                    content: Text(
                      "Reminder scheduled for ${DateFormat.yMMMMd().format(result.estimatedNextEligibleDate!)}.",
                    ),
                  ),
                );
              },
            ),
            NetraSpacing.gapH12,
          ],
          NetraButton.outlined(
            text: "Find Blood Banks",
            icon: Icons.local_hospital_outlined,
            onPressed: () {
              Navigator.of(context).push(
                MaterialPageRoute(
                    builder: (context) => const NearbyBloodBanksScreen()),
              );
            },
          ),
        ];

      case ResultType.MEDICAL_REVIEW_REQUIRED:
        return [
          NetraButton(
            text: "Contact / Find Blood Centre Doctor",
            icon: Icons.medical_services_outlined,
            onPressed: () {
              Navigator.of(context).push(
                MaterialPageRoute(
                    builder: (context) => const NearbyBloodBanksScreen()),
              );
            },
          ),
        ];

      case ResultType.INSUFFICIENT_INFORMATION:
        return [
          NetraButton(
            text: "Return to Questionnaire",
            icon: Icons.edit_note_rounded,
            onPressed: () => Navigator.of(context).pop(),
          ),
        ];
    }
  }
}
