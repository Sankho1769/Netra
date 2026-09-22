import 'package:flutter/material.dart';
import '../../../../core/responsive/responsive.dart';
import '../../../../core/theme/netra_colors.dart';
import '../../../../core/theme/netra_spacing.dart';
import '../../../../core/theme/netra_typography.dart';
import '../../../eligibility/widgets/why_we_ask_sheet.dart';
import '../../../eligibility/widgets/yes_no_selector.dart';
import '../../state/eligibility_controller.dart';

class Step4DonationSafety extends StatelessWidget {
  final EligibilityController controller;

  const Step4DonationSafety({super.key, required this.controller});

  @override
  Widget build(BuildContext context) {
    return ResponsiveContainer.reading(
      scrollable: true,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            "Donation Safety",
            style: NetraTypography.headlineMedium,
          ),
          NetraSpacing.gapH4,
          Text(
            "Procedures and health history help safeguard both you and the transfusion recipient.",
            style: NetraTypography.bodyMedium,
          ),
          NetraSpacing.gapH24,

          // Q1: Tattoo / Piercing
          _buildSafetyQuestion(
            context,
            title:
                "Tattoo, body piercing, or acupuncture in the past 6 months?",
            whyText:
                "Skin-penetrating procedures carry a small window period for blood-borne infections. Standard health policy requires a 6-month deferral.",
            source: "NBTC Guidelines Sec 5.1",
            questionKey: "TATTOO_OR_PIERCING_6M",
          ),
          NetraSpacing.gapH20,

          // Q2: Surgery
          _buildSafetyQuestion(
            context,
            title:
                "Major surgery in the past 12 months (or minor in 6 months)?",
            whyText:
                "Surgery causes physiological stress and temporary blood loss. Waiting allows complete tissue healing and hemoglobin replenishment.",
            source: "NBTC Guidelines Sec 5.2",
            questionKey: "MAJOR_SURGERY_12M",
          ),
          NetraSpacing.gapH20,

          // Q3: Dental procedure
          _buildSafetyQuestion(
            context,
            title: "Tooth extraction or oral surgery in the past 72 hours?",
            whyText:
                "Invasive dental procedures can introduce transient harmless mouth bacteria into the circulation, requiring 72 hours to completely clear.",
            source: "NBTC Guidelines Sec 5.3",
            questionKey: "DENTAL_PROCEDURE_72H",
          ),
          NetraSpacing.gapH20,

          // Q4: Chronic / Cardiac
          _buildSafetyQuestion(
            context,
            title:
                "History of heart condition, seizures, or bleeding disorders?",
            whyText:
                "Certain chronic conditions require direct personal clearance by an authorized blood centre medical officer to guarantee your safety during blood collection.",
            source: "NBTC Guidelines Sec 5.4",
            questionKey: "CHRONIC_OR_CARDIAC_CONDITION",
          ),
          NetraSpacing.gapH16,
        ],
      ),
    );
  }

  Widget _buildSafetyQuestion(
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
