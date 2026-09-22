import 'package:flutter/material.dart';
import '../../../core/responsive/responsive_breakpoints.dart';
import '../../../core/theme/netra_colors.dart';
import '../../../core/theme/netra_spacing.dart';
import '../../../core/theme/netra_typography.dart';
import '../../../common/widgets/netra_button.dart';

class WhyWeAskSheet extends StatelessWidget {
  final String title;
  final String explanation;
  final String? clinicalSource;

  const WhyWeAskSheet({
    super.key,
    required this.title,
    required this.explanation,
    this.clinicalSource,
  });

  static void show(
    BuildContext context, {
    required String title,
    required String explanation,
    String? clinicalSource,
  }) {
    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.transparent,
      isScrollControlled: true,
      builder: (context) => WhyWeAskSheet(
        title: title,
        explanation: explanation,
        clinicalSource: clinicalSource,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final maxWidth = context.isDesktopOrWide ? 640.0 : double.infinity;

    return Center(
      child: ConstrainedBox(
        constraints: BoxConstraints(maxWidth: maxWidth),
        child: Container(
          decoration: const BoxDecoration(
            color: NetraColors.surfaceWhite,
            borderRadius: BorderRadius.vertical(
                top: Radius.circular(NetraSpacing.radiusXl)),
          ),
          padding: const EdgeInsets.fromLTRB(24, 16, 24, 32),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Center(
                child: Container(
                  width: 40,
                  height: 4,
                  margin: const EdgeInsets.only(bottom: 20),
                  decoration: BoxDecoration(
                    color: NetraColors.borderGray,
                    borderRadius: BorderRadius.circular(2),
                  ),
                ),
              ),
              Row(
                children: [
                  Container(
                    padding: NetraSpacing.paddingSm,
                    decoration: BoxDecoration(
                      color: NetraColors.backgroundRed,
                      borderRadius:
                          BorderRadius.circular(NetraSpacing.radiusMd),
                    ),
                    child: const Icon(Icons.help_outline_rounded,
                        color: NetraColors.primaryRed, size: 22),
                  ),
                  NetraSpacing.gapW12,
                  Expanded(
                    child: Text(
                      title,
                      style: NetraTypography.headlineSmall,
                    ),
                  ),
                ],
              ),
              NetraSpacing.gapH16,
              Text(
                explanation,
                style: NetraTypography.bodyLarge
                    .copyWith(color: NetraColors.textSecondary),
              ),
              if (clinicalSource != null && clinicalSource!.isNotEmpty) ...[
                NetraSpacing.gapH16,
                Container(
                  padding: NetraSpacing.paddingMd,
                  decoration: BoxDecoration(
                    color: NetraColors.backgroundGray,
                    borderRadius: BorderRadius.circular(NetraSpacing.radiusSm),
                    border: Border.all(color: NetraColors.borderSubtle),
                  ),
                  child: Row(
                    children: [
                      const Icon(Icons.verified_outlined,
                          size: 16, color: NetraColors.textMuted),
                      NetraSpacing.gapW8,
                      Expanded(
                        child: Text(
                          "Guideline Source: $clinicalSource",
                          style: NetraTypography.bodySmall.copyWith(
                            color: NetraColors.textMuted,
                            fontStyle: FontStyle.italic,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
              NetraSpacing.gapH24,
              NetraButton(
                text: "Got it",
                onPressed: () => Navigator.of(context).pop(),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
