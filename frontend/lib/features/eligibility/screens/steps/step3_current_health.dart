import 'package:flutter/material.dart';
import '../../../../core/responsive/responsive.dart';
import '../../../../core/theme/netra_colors.dart';
import '../../../../core/theme/netra_spacing.dart';
import '../../../../core/theme/netra_typography.dart';
import '../../../eligibility/widgets/why_we_ask_sheet.dart';
import '../../../eligibility/widgets/yes_no_selector.dart';
import '../../state/eligibility_controller.dart';

class Step3CurrentHealth extends StatelessWidget {
  final EligibilityController controller;

  const Step3CurrentHealth({super.key, required this.controller});

  @override
  Widget build(BuildContext context) {
    final isFemale = controller.getAnswer('BIOLOGICAL_SEX') == 'FEMALE';

    return ResponsiveContainer.reading(
      scrollable: true,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            "Current Health",
            style: NetraTypography.headlineMedium,
          ),
          NetraSpacing.gapH4,
          Text(
            "We want to ensure you feel great on donation day and that your blood is safe for someone in need.",
            style: NetraTypography.bodyMedium,
          ),
          NetraSpacing.gapH24,

          // Q1: Feeling well
          _buildHealthQuestion(
            context,
            title: "Are you currently feeling well and in good health?",
            whyText: "Donors must feel healthy and energetic on donation day to avoid vasovagal dizziness or fatigue.",
            source: "NBTC Guidelines Sec 4.1",
            questionKey: "CURRENTLY_FEELING_WELL",
          ),
          NetraSpacing.gapH20,

          // Q2: Fever / illness in 14 days
          _buildHealthQuestion(
            context,
            title: "Fever, cold, cough, or infection in the past 14 days?",
            whyText: "A 14-day symptom-free deferral allows your immune system to fully recover and prevents transmission of active viruses.",
            source: "NBTC Guidelines Sec 4.2",
            questionKey: "FEVER_OR_ILLNESS_14D",
          ),
          NetraSpacing.gapH20,

          // Q3: Prescription medications
          _buildHealthQuestion(
            context,
            title: "Are you taking antibiotics or blood thinners?",
            whyText: "Certain active medications in the bloodstream can impact vulnerable patients receiving transfusions. Medical staff will review specific drug safety.",
            source: "NBTC Guidelines Sec 4.4",
            questionKey: "CURRENT_MEDICATION",
          ),
          NetraSpacing.gapH20,

          // Q4: Pregnancy / Childbirth (if applicable)
          if (isFemale || controller.getAnswer('BIOLOGICAL_SEX') == 'OTHER') ...[
            _buildHealthQuestion(
              context,
              title: "Are you pregnant, nursing, or delivered in the last 12 months?",
              whyText: "Maternal iron stores need protection during and following pregnancy and lactation. Guidelines recommend waiting 12 months post-delivery.",
              source: "NBTC Guidelines Sec 4.5",
              questionKey: "PREGNANCY_OR_CHILDBIRTH",
            ),
            NetraSpacing.gapH20,
          ],
          NetraSpacing.gapH16,
        ],
      ),
    );
  }

  Widget _buildHealthQuestion(
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
                    const Icon(Icons.help_outline_rounded, size: 16, color: NetraColors.primaryRed),
                    NetraSpacing.gapW4,
                    Text("Why", style: NetraTypography.labelSmall.copyWith(color: NetraColors.primaryRed)),
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
            style: NetraTypography.bodySmall.copyWith(color: NetraColors.errorRed),
          ),
        ],
      ],
    );
  }
}
