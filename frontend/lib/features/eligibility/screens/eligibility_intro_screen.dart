import 'package:flutter/material.dart';
import '../../../core/responsive/responsive.dart';
import '../../../core/theme/netra_colors.dart';
import '../../../core/theme/netra_spacing.dart';
import '../../../core/theme/netra_typography.dart';
import '../../../common/widgets/common_widgets.dart';
import '../services/eligibility_api_service.dart';
import '../state/eligibility_controller.dart';
import 'eligibility_flow_screen.dart';

class EligibilityIntroScreen extends StatelessWidget {
  const EligibilityIntroScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: const NetraAppBar(
        title: "Donation Eligibility",
      ),
      backgroundColor: NetraColors.backgroundGray,
      body: SafeArea(
        child: ResponsiveContainer.reading(
          scrollable: true,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              NetraSpacing.gapH8,
              // Visual Icon & Header
              Center(
                child: Container(
                  width: 72,
                  height: 72,
                  decoration: const BoxDecoration(
                    color: NetraColors.backgroundRed,
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(
                    Icons.favorite_rounded,
                    color: NetraColors.primaryRed,
                    size: 36,
                  ),
                ),
              ),
              NetraSpacing.gapH20,
              Center(
                child: Text(
                  "Check Donation Eligibility",
                  textAlign: TextAlign.center,
                  style: NetraTypography.headlineLarge,
                ),
              ),
              NetraSpacing.gapH8,
              Center(
                child: Text(
                  "A quick 3-minute preliminary self-check before you visit the blood bank.",
                  textAlign: TextAlign.center,
                  style: NetraTypography.bodyMedium,
                ),
              ),
              NetraSpacing.gapH28,

              // Overview Cards
              _buildFeatureItem(
                Icons.rule_folder_outlined,
                "Official Clinical Rules",
                "Screened against National Blood Transfusion Council (NBTC) criteria.",
              ),
              NetraSpacing.gapH12,
              _buildFeatureItem(
                Icons.privacy_tip_outlined,
                "Strict Healthcare Privacy",
                "Your health responses are evaluated securely and never shared with other donors or third parties.",
              ),
              NetraSpacing.gapH12,
              _buildFeatureItem(
                Icons.restore_outlined,
                "Automatic Recovery Intervals",
                "No manual math needed; safe donation intervals are calculated for you automatically.",
              ),
              NetraSpacing.gapH28,

              // Mandatory Disclaimer
              const NetraDisclaimerBanner(),
              NetraSpacing.gapH32,

              // Action
              NetraButton(
                text: "Start Eligibility Check",
                onPressed: () {
                  final controller = EligibilityController(apiService: EligibilityApiService());
                  Navigator.of(context).push(
                    MaterialPageRoute(
                      builder: (context) => EligibilityFlowScreen(controller: controller),
                    ),
                  );
                },
              ),
              NetraSpacing.gapH24,
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildFeatureItem(IconData icon, String title, String subtitle) {
    return NetraCard.outlined(
      padding: NetraSpacing.cardPadding,
      child: Row(
        children: [
          Container(
            padding: NetraSpacing.paddingSm,
            decoration: BoxDecoration(
              color: NetraColors.backgroundRed,
              borderRadius: BorderRadius.circular(NetraSpacing.radiusSm),
            ),
            child: Icon(icon, color: NetraColors.primaryRed, size: 20),
          ),
          NetraSpacing.gapW16,
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: NetraTypography.titleSmall,
                ),
                NetraSpacing.gapH4,
                Text(
                  subtitle,
                  style: NetraTypography.bodySmall,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
