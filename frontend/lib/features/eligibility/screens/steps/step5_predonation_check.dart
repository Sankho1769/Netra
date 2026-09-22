import 'package:flutter/material.dart';
import '../../../../core/responsive/responsive.dart';
import '../../../../core/theme/netra_colors.dart';
import '../../../../core/theme/netra_spacing.dart';
import '../../../../core/theme/netra_typography.dart';
import '../../../eligibility/widgets/why_we_ask_sheet.dart';
import '../../../eligibility/widgets/yes_no_selector.dart';
import '../../state/eligibility_controller.dart';

class Step5PreDonationCheck extends StatelessWidget {
  final EligibilityController controller;

  const Step5PreDonationCheck({super.key, required this.controller});

  @override
  Widget build(BuildContext context) {
    return ResponsiveContainer.reading(
      scrollable: true,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            "Pre-Donation Check",
            style: NetraTypography.headlineMedium,
          ),
          NetraSpacing.gapH4,
          Text(
            "These quick readiness questions help you have a smooth, comfortable donation experience.",
            style: NetraTypography.bodyMedium,
          ),
          NetraSpacing.gapH24,

          // Q1: Sleep
          _buildCheckQuestion(
            context,
            title:
                "Did you have at least 4 to 6 hours of sound sleep last night?",
            whyText:
                "Being well rested keeps blood pressure steady and minimizes tiredness after donation.",
            source: "Pre-donation Guidance",
            questionKey: "SLEEP_HOURS_LAST_NIGHT",
          ),
          NetraSpacing.gapH20,

          // Q2: Meal
          _buildCheckQuestion(
            context,
            title: "Have you had a meal or snack within the last 4 hours?",
            whyText:
                "Donating on an empty stomach can trigger hypoglycemia and lightheadedness. A light snack is recommended.",
            source: "NBTC Donor Counseling",
            questionKey: "MEAL_WITHIN_4_HOURS",
          ),
          NetraSpacing.gapH20,

          // Q3: Hydration
          _buildCheckQuestion(
            context,
            title: "Have you had plenty of water or fluids today?",
            whyText:
                "Good hydration supports blood volume and makes veins easier to find for a quick, painless donation.",
            source: "Pre-donation Guidance",
            questionKey: "HYDRATED_TODAY",
          ),
          NetraSpacing.gapH16,
        ],
      ),
    );
  }

  Widget _buildCheckQuestion(
    BuildContext context, {
    required String title,
    required String whyText,
    required String source,
    required String questionKey,
  }) {
    final error = controller.errors[questionKey];

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Expanded(
              child: Text(
                title,
                style: NetraTypography.titleMedium,
              ),
            ),
            InkWell(
              onTap: () => WhyWeAskSheet.show(
                context,
                title: title,
                explanation: whyText,
                clinicalSource: source,
              ),
              borderRadius: BorderRadius.circular(NetraSpacing.radiusLg),
              child: Padding(
                padding: NetraSpacing.paddingXs,
                child: Row(
                  children: [
                    const Icon(Icons.help_outline_rounded,
                        size: 16, color: NetraColors.primaryRed),
                    NetraSpacing.gapW4,
                    Text("Why",
                        style: NetraTypography.labelSmall
                            .copyWith(color: NetraColors.primaryRed)),
                  ],
                ),
              ),
            ),
          ],
        ),
        NetraSpacing.gapH12,
        YesNoSelector(
          value: controller.getBoolAnswer(questionKey),
          onChanged: (val) => controller.setBoolAnswer(questionKey, val),
        ),
        if (error != null) ...[
          NetraSpacing.gapH8,
          Text(
            error,
            style:
                NetraTypography.bodySmall.copyWith(color: NetraColors.errorRed),
          ),
        ],
      ],
    );
  }
}
