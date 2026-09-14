import 'package:flutter/material.dart';
import '../../../../core/responsive/responsive.dart';
import '../../../../core/theme/netra_colors.dart';
import '../../../../core/theme/netra_spacing.dart';
import '../../../../core/theme/netra_typography.dart';
import '../../../eligibility/widgets/why_we_ask_sheet.dart';
import '../../state/eligibility_controller.dart';

class Step1BasicInfo extends StatelessWidget {
  final EligibilityController controller;

  const Step1BasicInfo({super.key, required this.controller});

  @override
  Widget build(BuildContext context) {
    final errors = controller.errors;
    final isDesktopOrTablet = !context.isMobile;

    return ResponsiveContainer.reading(
      scrollable: true,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            "Basic Information",
            style: NetraTypography.headlineMedium,
          ),
          NetraSpacing.gapH4,
          Text(
            "General health indicators help verify standard physical criteria before donating.",
            style: NetraTypography.bodyMedium,
          ),
          NetraSpacing.gapH24,

          // Adaptive Age & Weight inputs: 2-column on tablet/desktop, stacked on mobile
          if (isDesktopOrTablet)
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(child: _buildAgeField(context, errors)),
                NetraSpacing.gapW16,
                Expanded(child: _buildWeightField(context, errors)),
              ],
            )
          else ...[
            _buildAgeField(context, errors),
            NetraSpacing.gapH20,
            _buildWeightField(context, errors),
          ],
          NetraSpacing.gapH24,

          // Biological Sex
          _buildQuestionHeader(
            context,
            title: "Biological Sex",
            subtitle: "Required for clinical recovery interval calculation.",
            whyExplanation:
                "Under national guidelines, the required interval between donations differs by biological sex (90 days for men, 120 days for women) to protect healthy iron stores.",
            clinicalSource: "NBTC Guidelines Sec 3.3",
          ),
          NetraSpacing.gapH12,
          Row(
            children: [
              _buildSexCard("MALE", "Male", Icons.male_rounded),
              NetraSpacing.gapW12,
              _buildSexCard("FEMALE", "Female", Icons.female_rounded),
              NetraSpacing.gapW12,
              _buildSexCard("OTHER", "Other", Icons.person_outline_rounded),
            ],
          ),
          if (errors['BIOLOGICAL_SEX'] != null) ...[
            NetraSpacing.gapH8,
            Text(
              errors['BIOLOGICAL_SEX']!,
              style: NetraTypography.bodySmall.copyWith(color: NetraColors.errorRed),
            ),
          ],
          NetraSpacing.gapH32,
        ],
      ),
    );
  }

  Widget _buildAgeField(BuildContext context, Map<String, String> errors) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _buildQuestionHeader(
          context,
          title: "How old are you?",
          subtitle: "General criteria: 18 - 65 years.",
          whyExplanation:
              "Blood donation safety guidelines require donors to be at least 18 years old to ensure physical maturity and healthy blood volume recovery.",
          clinicalSource: "NBTC Guidelines Sec 3.1 & Schedule F Part XII-B",
        ),
        NetraSpacing.gapH8,
        TextField(
          keyboardType: TextInputType.number,
          style: NetraTypography.bodyLarge,
          decoration: InputDecoration(
            hintText: "Age in years (e.g. 24)",
            errorText: errors['AGE'],
            prefixIcon: const Icon(Icons.cake_outlined, color: NetraColors.textSecondary),
          ),
          controller: TextEditingController(text: controller.getAnswer('AGE'))
            ..selection = TextSelection.collapsed(offset: controller.getAnswer('AGE')?.length ?? 0),
          onChanged: (val) => controller.setAnswer('AGE', val),
        ),
      ],
    );
  }

  Widget _buildWeightField(BuildContext context, Map<String, String> errors) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _buildQuestionHeader(
          context,
          title: "What is your body weight (in kg)?",
          subtitle: "Minimum 45 kg required.",
          whyExplanation:
              "Whole blood donation takes roughly 350 ml to 450 ml of blood. A minimum weight of 45 kg ensures that this collection represents less than 13% of your total blood volume, preventing dizziness.",
          clinicalSource: "NBTC Guidelines Sec 3.2",
        ),
        NetraSpacing.gapH8,
        TextField(
          keyboardType: TextInputType.number,
          style: NetraTypography.bodyLarge,
          decoration: InputDecoration(
            hintText: "Weight in kg (e.g. 62)",
            errorText: errors['WEIGHT_KG'],
            prefixIcon: const Icon(Icons.monitor_weight_outlined, color: NetraColors.textSecondary),
          ),
          controller: TextEditingController(text: controller.getAnswer('WEIGHT_KG'))
            ..selection = TextSelection.collapsed(offset: controller.getAnswer('WEIGHT_KG')?.length ?? 0),
          onChanged: (val) => controller.setAnswer('WEIGHT_KG', val),
        ),
      ],
    );
  }

  Widget _buildSexCard(String key, String label, IconData icon) {
    final isSelected = controller.getAnswer('BIOLOGICAL_SEX') == key;

    return Expanded(
      child: InkWell(
        onTap: () => controller.setAnswer('BIOLOGICAL_SEX', key),
        borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 180),
          padding: const EdgeInsets.symmetric(vertical: 14),
          decoration: BoxDecoration(
            color: isSelected ? NetraColors.backgroundRed : NetraColors.surfaceWhite,
            borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
            border: Border.all(
              color: isSelected ? NetraColors.primaryRed : NetraColors.borderGray,
              width: isSelected ? 2.0 : 1.0,
            ),
          ),
          child: Column(
            children: [
              Icon(icon, size: 24, color: isSelected ? NetraColors.primaryRed : NetraColors.textSecondary),
              NetraSpacing.gapH4,
              Text(
                label,
                style: NetraTypography.titleSmall.copyWith(
                  fontWeight: isSelected ? FontWeight.w700 : FontWeight.w500,
                  color: isSelected ? NetraColors.primaryRed : NetraColors.textPrimary,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildQuestionHeader(
    BuildContext context, {
    required String title,
    required String subtitle,
    required String whyExplanation,
    String? clinicalSource,
  }) {
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
                explanation: whyExplanation,
                clinicalSource: clinicalSource,
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
        NetraSpacing.gapH4,
        Text(subtitle, style: NetraTypography.bodySmall),
      ],
    );
  }
}
