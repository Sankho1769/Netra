import 'package:flutter/material.dart';
import '../../../../core/responsive/responsive.dart';
import '../../../../core/theme/netra_colors.dart';
import '../../../../core/theme/netra_spacing.dart';
import '../../../../core/theme/netra_typography.dart';
import '../../../../common/widgets/common_widgets.dart';
import '../../state/eligibility_controller.dart';

class Step6ReviewAnswers extends StatelessWidget {
  final EligibilityController controller;
  final VoidCallback onCheckEligibility;

  const Step6ReviewAnswers({
    super.key,
    required this.controller,
    required this.onCheckEligibility,
  });

  String _formatBoolAnswer(String? val) {
    if (val == null || val.isEmpty) return "Not answered";
    return val.toLowerCase() == 'true' ? "Yes" : "No";
  }

  @override
  Widget build(BuildContext context) {
    final answers = controller.answers;
    final isDesktopOrTablet = !context.isMobile;

    final cardBasic = _buildReviewCard(
      title: "Basic Information",
      stepTarget: 1,
      items: [
        {"label": "Age", "value": answers['AGE'] != null ? "${answers['AGE']} years" : "Not provided"},
        {"label": "Weight", "value": answers['WEIGHT_KG'] != null ? "${answers['WEIGHT_KG']} kg" : "Not provided"},
        {"label": "Biological Sex", "value": answers['BIOLOGICAL_SEX'] ?? "Not selected"},
      ],
    );

    final cardDonation = _buildReviewCard(
      title: "Donation History",
      stepTarget: 2,
      items: [
        {
          "label": "Donated before",
          "value": answers['PREVIOUS_DONATION'] == null
              ? "Not answered"
              : (answers['PREVIOUS_DONATION'] == 'true' ? "Yes" : "First-time donor")
        },
        if (answers['PREVIOUS_DONATION'] == 'true')
          {"label": "Last donation date", "value": answers['LAST_DONATION_DATE'] ?? "Not entered"},
      ],
    );

    final cardHealth = _buildReviewCard(
      title: "Current Health",
      stepTarget: 3,
      items: [
        {"label": "Feeling well today", "value": _formatBoolAnswer(answers['CURRENTLY_FEELING_WELL'])},
        {"label": "Fever/illness in 14d", "value": _formatBoolAnswer(answers['FEVER_OR_ILLNESS_14D'])},
        {"label": "Current medication", "value": _formatBoolAnswer(answers['CURRENT_MEDICATION'])},
        if (answers.containsKey('PREGNANCY_OR_CHILDBIRTH'))
          {"label": "Pregnancy/lactation", "value": _formatBoolAnswer(answers['PREGNANCY_OR_CHILDBIRTH'])},
      ],
    );

    final cardSafety = _buildReviewCard(
      title: "Donation Safety",
      stepTarget: 4,
      items: [
        {"label": "Tattoo/piercing (6m)", "value": _formatBoolAnswer(answers['TATTOO_OR_PIERCING_6M'])},
        {"label": "Surgery history (12m)", "value": _formatBoolAnswer(answers['MAJOR_SURGERY_12M'])},
        {"label": "Dental procedure (72h)", "value": _formatBoolAnswer(answers['DENTAL_PROCEDURE_72H'])},
        {"label": "Cardiac/chronic condition", "value": _formatBoolAnswer(answers['CHRONIC_OR_CARDIAC_CONDITION'])},
      ],
    );

    final cardReadiness = _buildReviewCard(
      title: "Readiness Check",
      stepTarget: 5,
      items: [
        {"label": "Adequate sleep (4-6h)", "value": _formatBoolAnswer(answers['SLEEP_HOURS_LAST_NIGHT'])},
        {"label": "Meal within 4 hours", "value": _formatBoolAnswer(answers['MEAL_WITHIN_4_HOURS'])},
        {"label": "Adequate hydration", "value": _formatBoolAnswer(answers['HYDRATED_TODAY'])},
      ],
    );

    return ResponsiveContainer.standard(
      scrollable: true,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            "Review Your Answers",
            style: NetraTypography.headlineMedium,
          ),
          NetraSpacing.gapH4,
          Text(
            "Please verify that your responses are accurate before submitting for eligibility assessment.",
            style: NetraTypography.bodyMedium,
          ),
          NetraSpacing.gapH20,

          // Responsive Card Layout: 2-column on tablet/desktop, 1-column on mobile
          if (isDesktopOrTablet) ...[
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(child: cardBasic),
                NetraSpacing.gapW16,
                Expanded(child: cardDonation),
              ],
            ),
            NetraSpacing.gapH16,
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(child: cardHealth),
                NetraSpacing.gapW16,
                Expanded(child: cardSafety),
              ],
            ),
            NetraSpacing.gapH16,
            cardReadiness,
          ] else ...[
            cardBasic,
            NetraSpacing.gapH12,
            cardDonation,
            NetraSpacing.gapH12,
            cardHealth,
            NetraSpacing.gapH12,
            cardSafety,
            NetraSpacing.gapH12,
            cardReadiness,
          ],
          NetraSpacing.gapH24,

          // Medical Disclaimer
          const NetraDisclaimerBanner(compact: true),
          NetraSpacing.gapH24,

          // Action Buttons
          Row(
            children: [
              Expanded(
                flex: 2,
                child: NetraButton.outlined(
                  text: "Edit Answers",
                  onPressed: () => controller.jumpToStep(1),
                ),
              ),
              NetraSpacing.gapW12,
              Expanded(
                flex: 3,
                child: NetraButton(
                  text: "Check Eligibility",
                  isLoading: controller.isLoading,
                  onPressed: controller.isLoading ? null : onCheckEligibility,
                ),
              ),
            ],
          ),
          NetraSpacing.gapH32,
        ],
      ),
    );
  }

  Widget _buildReviewCard({
    required String title,
    required int stepTarget,
    required List<Map<String, String>> items,
  }) {
    return NetraCard.outlined(
      padding: NetraSpacing.cardPadding,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                title,
                style: NetraTypography.titleMedium,
              ),
              InkWell(
                onTap: () => controller.jumpToStep(stepTarget),
                borderRadius: BorderRadius.circular(NetraSpacing.radiusSm),
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  child: Text(
                    "Edit",
                    style: NetraTypography.labelSmall.copyWith(color: NetraColors.primaryRed),
                  ),
                ),
              ),
            ],
          ),
          const Divider(height: 16, color: NetraColors.borderGray),
          ...items.map((item) {
            final isUnanswered = item['value'] == "Not answered" || item['value'] == "Not provided" || item['value'] == "Not selected";
            return Padding(
              padding: const EdgeInsets.symmetric(vertical: 4),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    item['label']!,
                    style: NetraTypography.bodyMedium,
                  ),
                  Text(
                    item['value']!,
                    style: NetraTypography.titleSmall.copyWith(
                      color: isUnanswered ? NetraColors.errorRed : NetraColors.textPrimary,
                    ),
                  ),
                ],
              ),
            );
          }),
        ],
      ),
    );
  }
}
