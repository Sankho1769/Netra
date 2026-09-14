import 'package:flutter/material.dart';
import '../../../core/responsive/responsive.dart';
import '../../../core/theme/netra_colors.dart';
import '../../../core/theme/netra_spacing.dart';
import '../../../common/widgets/common_widgets.dart';
import '../state/eligibility_controller.dart';
import '../widgets/step_progress_bar.dart';
import 'eligibility_result_screen.dart';
import 'steps/step1_basic_info.dart';
import 'steps/step2_recent_donation.dart';
import 'steps/step3_current_health.dart';
import 'steps/step4_donation_safety.dart';
import 'steps/step5_predonation_check.dart';
import 'steps/step6_review_answers.dart';

class EligibilityFlowScreen extends StatefulWidget {
  final EligibilityController controller;

  const EligibilityFlowScreen({super.key, required this.controller});

  @override
  State<EligibilityFlowScreen> createState() => _EligibilityFlowScreenState();
}

class _EligibilityFlowScreenState extends State<EligibilityFlowScreen> {
  @override
  void initState() {
    super.initState();
    widget.controller.addListener(_onControllerUpdate);
  }

  @override
  void dispose() {
    widget.controller.removeListener(_onControllerUpdate);
    super.dispose();
  }

  void _onControllerUpdate() {
    if (widget.controller.result != null && mounted) {
      Navigator.of(context).pushReplacement(
        MaterialPageRoute(
          builder: (context) => EligibilityResultScreen(
            result: widget.controller.result!,
          ),
        ),
      );
    } else if (widget.controller.errorMessage != null && mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(widget.controller.errorMessage!),
          backgroundColor: NetraColors.darkRed,
          behavior: SnackBarBehavior.floating,
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final controller = widget.controller;

    return ListenableBuilder(
      listenable: controller,
      builder: (context, child) {
        return Scaffold(
          backgroundColor: NetraColors.backgroundGray,
          appBar: NetraAppBar(
            title: "Donation Self-Check",
            leading: IconButton(
              icon: const Icon(Icons.close_rounded),
              tooltip: 'Cancel Check',
              onPressed: () => Navigator.of(context).pop(),
            ),
            showBackButton: false,
          ),
          body: SafeArea(
            child: Column(
              children: [
                Center(
                  child: ConstrainedBox(
                    constraints: const BoxConstraints(maxWidth: ResponsiveBreakpoints.maxContentWidthStandard),
                    child: StepProgressBar(
                      currentStep: controller.currentStep,
                      totalSteps: controller.totalSteps,
                      onBack: controller.currentStep > 1 ? controller.previousStep : null,
                    ),
                  ),
                ),
                Expanded(
                  child: _buildCurrentStep(controller),
                ),
                if (controller.currentStep < controller.totalSteps) _buildBottomActions(controller),
              ],
            ),
          ),
        );
      },
    );
  }

  Widget _buildCurrentStep(EligibilityController controller) {
    switch (controller.currentStep) {
      case 1:
        return Step1BasicInfo(controller: controller);
      case 2:
        return Step2RecentDonation(controller: controller);
      case 3:
        return Step3CurrentHealth(controller: controller);
      case 4:
        return Step4DonationSafety(controller: controller);
      case 5:
        return Step5PreDonationCheck(controller: controller);
      case 6:
        return Step6ReviewAnswers(
          controller: controller,
          onCheckEligibility: () => controller.submitAndEvaluate(context),
        );
      default:
        return const SizedBox.shrink();
    }
  }

  Widget _buildBottomActions(EligibilityController controller) {
    return Container(
      decoration: const BoxDecoration(
        color: NetraColors.surfaceWhite,
        border: Border(top: BorderSide(color: NetraColors.borderGray, width: 0.5)),
      ),
      child: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: ResponsiveBreakpoints.maxContentWidthReading),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: NetraSpacing.lg, vertical: NetraSpacing.md),
            child: Row(
              children: [
                if (controller.currentStep > 1) ...[
                  Expanded(
                    flex: 1,
                    child: NetraButton.outlined(
                      text: "Back",
                      onPressed: controller.previousStep,
                    ),
                  ),
                  NetraSpacing.gapW12,
                ],
                Expanded(
                  flex: 2,
                  child: NetraButton(
                    text: "Continue",
                    onPressed: () {
                      final success = controller.nextStep();
                      if (!success && controller.errors.isNotEmpty) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(
                            content: Text("Please complete the required questions to continue."),
                            duration: Duration(seconds: 2),
                            behavior: SnackBarBehavior.floating,
                          ),
                        );
                      }
                    },
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
