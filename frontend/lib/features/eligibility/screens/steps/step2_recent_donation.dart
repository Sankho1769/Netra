import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../../../../core/responsive/responsive.dart';
import '../../../../core/theme/netra_colors.dart';
import '../../../../core/theme/netra_spacing.dart';
import '../../../../core/theme/netra_typography.dart';
import '../../../eligibility/widgets/why_we_ask_sheet.dart';
import '../../../eligibility/widgets/yes_no_selector.dart';
import '../../state/eligibility_controller.dart';

class Step2RecentDonation extends StatelessWidget {
  final EligibilityController controller;

  const Step2RecentDonation({super.key, required this.controller});

  @override
  Widget build(BuildContext context) {
    final errors = controller.errors;
    final bool hasDonatedBefore = controller.getBoolAnswer('PREVIOUS_DONATION') == true;
    final daysElapsed = controller.daysSinceLastDonation;
    final isFemale = controller.getAnswer('BIOLOGICAL_SEX') == 'FEMALE';
    final int requiredInterval = isFemale ? 120 : 90;

    return ResponsiveContainer.reading(
      scrollable: true,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            "Recent Donation",
            style: NetraTypography.headlineMedium,
          ),
          NetraSpacing.gapH4,
          Text(
            "We automatically check your safe recovery window to make sure your red blood cells have fully replenished.",
            style: NetraTypography.bodyMedium,
          ),
          NetraSpacing.gapH24,

          // Question: Have you donated before?
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Expanded(
                child: Text(
                  "Have you donated blood before?",
                  style: NetraTypography.titleMedium,
                ),
              ),
              InkWell(
                onTap: () => WhyWeAskSheet.show(
                  context,
                  title: "Previous Donation History",
                  explanation:
                      "First-time donors and repeat donors have slightly different guidelines. We also use this to ensure sufficient time has elapsed between donations.",
                  clinicalSource: "NBTC Guidelines Sec 3.3",
                ),
                borderRadius: BorderRadius.circular(NetraSpacing.radiusLg),
                child: Padding(
                  padding: NetraSpacing.paddingXs,
                  child: Row(
                    children: [
                      const Icon(Icons.help_outline_rounded, size: 16, color: NetraColors.primaryRed),
                      NetraSpacing.gapW4,
                      Text(
                        "Why we ask",
                        style: NetraTypography.labelSmall.copyWith(color: NetraColors.primaryRed),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
          NetraSpacing.gapH12,
          YesNoSelector(
            value: controller.getBoolAnswer('PREVIOUS_DONATION'),
            onChanged: (val) {
              controller.setBoolAnswer('PREVIOUS_DONATION', val);
              if (val == false) {
                controller.setAnswer('LAST_DONATION_DATE', '');
              }
            },
          ),
          if (errors['PREVIOUS_DONATION'] != null) ...[
            NetraSpacing.gapH8,
            Text(
              errors['PREVIOUS_DONATION']!,
              style: NetraTypography.bodySmall.copyWith(color: NetraColors.errorRed),
            ),
          ],
          NetraSpacing.gapH24,

          // Conditional: Date of last donation
          if (hasDonatedBefore) ...[
            Text(
              "Date of your most recent whole blood donation",
              style: NetraTypography.titleMedium,
            ),
            NetraSpacing.gapH4,
            Text(
              "Select the date. We will calculate the elapsed days automatically for you.",
              style: NetraTypography.bodySmall,
            ),
            NetraSpacing.gapH12,
            InkWell(
              onTap: () async {
                final initial = DateTime.now().subtract(const Duration(days: 95));
                final picked = await showDatePicker(
                  context: context,
                  initialDate: initial,
                  firstDate: DateTime.now().subtract(const Duration(days: 365 * 3)),
                  lastDate: DateTime.now(),
                  helpText: "SELECT MOST RECENT DONATION DATE",
                );
                if (picked != null) {
                  controller.setAnswer('LAST_DONATION_DATE', DateFormat('yyyy-MM-dd').format(picked));
                }
              },
              borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
              child: Container(
                padding: NetraSpacing.paddingLg,
                decoration: BoxDecoration(
                  color: NetraColors.surfaceWhite,
                  borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
                  border: Border.all(
                    color: errors['LAST_DONATION_DATE'] != null ? NetraColors.errorRed : NetraColors.borderGray,
                    width: 1,
                  ),
                ),
                child: Row(
                  children: [
                    const Icon(Icons.calendar_month_outlined, color: NetraColors.primaryRed),
                    NetraSpacing.gapW12,
                    Expanded(
                      child: Text(
                        controller.getAnswer('LAST_DONATION_DATE') != null &&
                                controller.getAnswer('LAST_DONATION_DATE')!.isNotEmpty
                            ? DateFormat.yMMMMd().format(DateTime.parse(controller.getAnswer('LAST_DONATION_DATE')!))
                            : "Tap to select donation date",
                        style: NetraTypography.bodyLarge.copyWith(
                          color: controller.getAnswer('LAST_DONATION_DATE')?.isNotEmpty == true
                              ? NetraColors.textPrimary
                              : NetraColors.textMuted,
                        ),
                      ),
                    ),
                    const Icon(Icons.arrow_drop_down, color: NetraColors.textSecondary),
                  ],
                ),
              ),
            ),
            if (errors['LAST_DONATION_DATE'] != null) ...[
              NetraSpacing.gapH8,
              Text(
                errors['LAST_DONATION_DATE']!,
                style: NetraTypography.bodySmall.copyWith(color: NetraColors.errorRed),
              ),
            ],

            // Automatic Interval Calculation Banner
            if (daysElapsed != null) ...[
              NetraSpacing.gapH16,
              Container(
                padding: NetraSpacing.paddingMd,
                decoration: BoxDecoration(
                  color: daysElapsed >= requiredInterval
                      ? NetraColors.eligibleGreenBg
                      : NetraColors.deferralAmberBg,
                  borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
                  border: Border.all(
                    color: daysElapsed >= requiredInterval
                        ? NetraColors.eligibleGreenBorder
                        : NetraColors.deferralAmberBorder,
                  ),
                ),
                child: Row(
                  children: [
                    Icon(
                      daysElapsed >= requiredInterval
                          ? Icons.check_circle_rounded
                          : Icons.schedule_rounded,
                      color: daysElapsed >= requiredInterval
                          ? NetraColors.eligibleGreen
                          : NetraColors.deferralAmber,
                      size: 24,
                    ),
                    NetraSpacing.gapW12,
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            "$daysElapsed days since last donation",
                            style: NetraTypography.titleSmall.copyWith(
                              color: daysElapsed >= requiredInterval
                                  ? NetraColors.eligibleGreen
                                  : NetraColors.deferralAmber,
                            ),
                          ),
                          NetraSpacing.gapH4,
                          Text(
                            daysElapsed >= requiredInterval
                                ? "Required interval satisfied ($requiredInterval days required)."
                                : "Standard guideline requires $requiredInterval days between whole-blood donations.",
                            style: NetraTypography.bodySmall,
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ],
          NetraSpacing.gapH32,
        ],
      ),
    );
  }
}
