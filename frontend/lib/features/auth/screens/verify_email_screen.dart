import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../../../core/responsive/responsive.dart';
import '../../../core/theme/netra_colors.dart';
import '../../../core/theme/netra_spacing.dart';
import '../../../core/theme/netra_typography.dart';
import '../../../common/widgets/common_widgets.dart';
import '../models/auth_models.dart';
import '../state/auth_controller.dart';
import '../state/auth_scope.dart';
import '../../home/home_screen.dart';
import 'login_screen.dart';

class VerifyEmailScreen extends StatefulWidget {
  final String email;
  final bool fromRegistration;

  const VerifyEmailScreen({
    super.key,
    required this.email,
    this.fromRegistration = false,
  });

  @override
  State<VerifyEmailScreen> createState() => _VerifyEmailScreenState();
}

class _VerifyEmailScreenState extends State<VerifyEmailScreen> {
  final _formKey = GlobalKey<FormState>();
  final _codeController = TextEditingController();
  int _cooldownSeconds = 60;
  Timer? _cooldownTimer;
  bool _isResending = false;

  @override
  void initState() {
    super.initState();
    _startCooldown();
  }

  void _startCooldown() {
    _cooldownTimer?.cancel();
    setState(() => _cooldownSeconds = 60);
    _cooldownTimer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (!mounted) {
        timer.cancel();
        return;
      }
      if (_cooldownSeconds > 0) {
        setState(() => _cooldownSeconds--);
      } else {
        timer.cancel();
      }
    });
  }

  @override
  void dispose() {
    _cooldownTimer?.cancel();
    _codeController.dispose();
    super.dispose();
  }

  Future<void> _handleVerify(AuthController authController) async {
    FocusScope.of(context).unfocus();
    if (!_formKey.currentState!.validate()) return;

    final code = _codeController.text.trim();
    final success = await authController.verifyEmail(
      email: widget.email,
      code: code,
    );

    if (success && mounted) {
      await showDialog(
        context: context,
        barrierDismissible: false,
        barrierColor: Colors.black.withValues(alpha: 0.70),
        builder: (_) => AlertDialog(
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(NetraSpacing.radiusLg),
          ),
          contentPadding: const EdgeInsets.all(NetraSpacing.xxl),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Container(
                width: 64,
                height: 64,
                decoration: BoxDecoration(
                  color: Colors.green.shade50,
                  shape: BoxShape.circle,
                ),
                child: Icon(
                  Icons.verified_user_rounded,
                  color: Colors.green.shade700,
                  size: 36,
                ),
              ),
              NetraSpacing.gapH16,
              Text(
                "Account Verified",
                textAlign: TextAlign.center,
                style: NetraTypography.titleLarge.copyWith(
                  fontWeight: FontWeight.bold,
                ),
              ),
              NetraSpacing.gapH8,
              Text(
                "Your email has been verified. Welcome to the NETRA healthcare blood network!",
                textAlign: TextAlign.center,
                style: NetraTypography.bodyMedium.copyWith(
                  color: NetraColors.textSecondary,
                ),
              ),
              NetraSpacing.gapH24,
              SizedBox(
                width: double.infinity,
                child: NetraButton(
                  text: "Enter NETRA",
                  icon: Icons.arrow_forward_rounded,
                  onPressed: () {
                    ScaffoldMessenger.of(context).clearSnackBars();
                    Navigator.of(context).pushAndRemoveUntil(
                      MaterialPageRoute(builder: (_) => const HomeScreen()),
                      (route) => false,
                    );
                  },
                ),
              ),
            ],
          ),
        ),
      );
    }
  }

  Future<void> _handleResend(AuthController authController) async {
    if (_cooldownSeconds > 0 || _isResending) return;

    setState(() => _isResending = true);
    final success = await authController.resendVerification(email: widget.email);
    if (!mounted) return;
    setState(() => _isResending = false);

    if (success) {
      _startCooldown();
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            "A fresh verification challenge was sent to ${widget.email}.",
          ),
          backgroundColor: Colors.green.shade800,
          behavior: SnackBarBehavior.floating,
        ),
      );
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            authController.errorMessage ?? "Could not resend code. Please try again.",
          ),
          backgroundColor: Colors.red.shade800,
          behavior: SnackBarBehavior.floating,
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final authController = AuthScope.of(context);
    final isLoading = authController.status == AuthStatus.authenticating;

    return Scaffold(
      backgroundColor: NetraColors.backgroundGray,
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_rounded, color: NetraColors.textPrimary),
          onPressed: () {
            if (Navigator.of(context).canPop()) {
              Navigator.of(context).pop();
            } else {
              Navigator.of(context).pushReplacement(
                MaterialPageRoute(builder: (_) => const LoginScreen()),
              );
            }
          },
        ),
      ),
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: context.screenGutter,
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 460),
              child: NetraCard.clay(
                borderRadius: 24,
                clayDepth: 6.0,
                padding: const EdgeInsets.symmetric(
                  horizontal: NetraSpacing.xl,
                  vertical: NetraSpacing.xxl,
                ),
                child: Form(
                  key: _formKey,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      // Header Badge
                      Center(
                        child: Container(
                          width: 64,
                          height: 64,
                          decoration: BoxDecoration(
                            color: NetraColors.primaryRed.withValues(alpha: 0.10),
                            shape: BoxShape.circle,
                          ),
                          child: const Icon(
                            Icons.mark_email_read_rounded,
                            color: NetraColors.primaryRed,
                            size: 32,
                          ),
                        ),
                      ),
                      NetraSpacing.gapH16,
                      Text(
                        "Verify Your Account",
                        textAlign: TextAlign.center,
                        style: NetraTypography.headlineMedium,
                      ),
                      NetraSpacing.gapH6,
                      Text(
                        "A 6-digit verification code has been dispatched to:",
                        textAlign: TextAlign.center,
                        style: NetraTypography.bodyMedium.copyWith(
                          color: NetraColors.textSecondary,
                        ),
                      ),
                      NetraSpacing.gapH4,
                      Text(
                        widget.email,
                        textAlign: TextAlign.center,
                        style: NetraTypography.bodyMedium.copyWith(
                          fontWeight: FontWeight.bold,
                          color: NetraColors.textPrimary,
                        ),
                      ),
                      NetraSpacing.gapH24,

                      // Error message banner if present
                      if (authController.errorMessage != null) ...[
                        Container(
                          padding: const EdgeInsets.all(NetraSpacing.md),
                          decoration: BoxDecoration(
                            color: NetraColors.backgroundRed,
                            borderRadius: BorderRadius.circular(NetraSpacing.radiusSm),
                            border: Border.all(
                              color: NetraColors.errorRed.withValues(alpha: 0.4),
                            ),
                          ),
                          child: Row(
                            children: [
                              const Icon(
                                Icons.error_outline_rounded,
                                color: NetraColors.errorRed,
                                size: 20,
                              ),
                              NetraSpacing.gapW12,
                              Expanded(
                                child: Text(
                                  authController.errorMessage!,
                                  style: NetraTypography.bodySmall.copyWith(
                                    color: NetraColors.errorRed,
                                  ),
                                ),
                              ),
                              IconButton(
                                icon: const Icon(
                                  Icons.close,
                                  size: 16,
                                  color: NetraColors.errorRed,
                                ),
                                padding: EdgeInsets.zero,
                                constraints: const BoxConstraints(),
                                onPressed: () => authController.clearError(),
                              ),
                            ],
                          ),
                        ),
                        NetraSpacing.gapH16,
                      ],

                      // Code Input Field
                      Text(
                        "Enter 6-Digit Code",
                        style: NetraTypography.labelMedium.copyWith(
                          color: NetraColors.textPrimary,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                      NetraSpacing.gapH8,
                      TextFormField(
                        controller: _codeController,
                        keyboardType: TextInputType.number,
                        textAlign: TextAlign.center,
                        style: const TextStyle(
                          fontSize: 26,
                          letterSpacing: 10,
                          fontWeight: FontWeight.bold,
                          color: NetraColors.textPrimary,
                        ),
                        maxLength: 6,
                        inputFormatters: [
                          FilteringTextInputFormatter.digitsOnly,
                          LengthLimitingTextInputFormatter(6),
                        ],
                        decoration: InputDecoration(
                          counterText: '',
                          hintText: '000000',
                          hintStyle: TextStyle(
                            fontSize: 26,
                            letterSpacing: 10,
                            color: Colors.grey.shade400,
                          ),
                          filled: true,
                          fillColor: const Color(0xFFF8FAFC),
                          contentPadding: const EdgeInsets.symmetric(
                            vertical: 16,
                            horizontal: 20,
                          ),
                          border: OutlineInputBorder(
                            borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
                            borderSide: const BorderSide(color: Color(0xFFE2E8F0)),
                          ),
                          focusedBorder: OutlineInputBorder(
                            borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
                            borderSide: const BorderSide(
                              color: NetraColors.primaryRed,
                              width: 2,
                            ),
                          ),
                        ),
                        validator: (val) {
                          if (val == null || val.trim().length != 6) {
                            return 'Enter the complete 6-digit code';
                          }
                          return null;
                        },
                      ),
                      NetraSpacing.gapH24,

                      // Verify CTA
                      NetraButton(
                        text: "Verify Email",
                        isLoading: isLoading,
                        onPressed: isLoading ? null : () => _handleVerify(authController),
                      ),
                      NetraSpacing.gapH16,

                      // Resend Action with Cooldown
                      Wrap(
                        alignment: WrapAlignment.center,
                        crossAxisAlignment: WrapCrossAlignment.center,
                        spacing: 4,
                        children: [
                          Text(
                            "Didn't receive the code? ",
                            style: NetraTypography.bodySmall.copyWith(
                              color: NetraColors.textSecondary,
                            ),
                          ),
                          if (_cooldownSeconds > 0)
                            Text(
                              "Resend in ${_cooldownSeconds}s",
                              style: NetraTypography.bodySmall.copyWith(
                                color: NetraColors.textMuted,
                                fontWeight: FontWeight.w600,
                              ),
                            )
                          else
                            InkWell(
                              onTap: _isResending ? null : () => _handleResend(authController),
                              child: Text(
                                _isResending ? "Sending..." : "Resend Code",
                                style: NetraTypography.bodySmall.copyWith(
                                  color: NetraColors.primaryRed,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                            ),
                        ],
                      ),
                      NetraSpacing.gapH20,

                      // Back to login
                      Center(
                        child: TextButton.icon(
                          onPressed: () {
                            Navigator.of(context).pushReplacement(
                              MaterialPageRoute(builder: (_) => const LoginScreen()),
                            );
                          },
                          icon: const Icon(Icons.arrow_back, size: 16),
                          label: const Text("Return to Sign In"),
                          style: TextButton.styleFrom(
                            foregroundColor: NetraColors.textSecondary,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
