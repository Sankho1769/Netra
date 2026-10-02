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
import '../widgets/auth_text_field.dart';
import '../../home/home_screen.dart';
import 'register_screen.dart';
import 'verify_email_screen.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  DateTime? _lastBackPressTime;

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _handleLogin(AuthController authController) async {
    // Dismiss keyboard
    FocusScope.of(context).unfocus();

    if (!_formKey.currentState!.validate()) {
      return;
    }

    final success = await authController.login(
      _emailController.text.trim(),
      _passwordController.text,
    );

    if (success && mounted) {
      ScaffoldMessenger.of(context).clearSnackBars();
      showDialog(
        context: context,
        barrierDismissible: false,
        barrierColor: Colors.black.withValues(alpha: 0.75),
        builder: (_) => const _LoginSuccessDialog(),
      );
      await Future.delayed(const Duration(milliseconds: 1400));
      if (mounted) {
        ScaffoldMessenger.of(context).clearSnackBars();
        Navigator.of(context).pushAndRemoveUntil(
          MaterialPageRoute(builder: (_) => const HomeScreen()),
          (route) => false,
        );
      }
    } else if (!success && mounted && authController.isUnverified) {
      ScaffoldMessenger.of(context).clearSnackBars();
    }
  }

  @override
  Widget build(BuildContext context) {
    final authController = AuthScope.of(context);
    final isLoading = authController.status == AuthStatus.authenticating;
    final canPop = Navigator.of(context).canPop();

    return PopScope(
      canPop: canPop,
      onPopInvokedWithResult: (didPop, result) {
        if (didPop) return;
        final now = DateTime.now();
        if (_lastBackPressTime == null ||
            now.difference(_lastBackPressTime!) > const Duration(seconds: 2)) {
          _lastBackPressTime = now;
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text("Press back again to exit"),
              duration: Duration(seconds: 2),
              behavior: SnackBarBehavior.floating,
            ),
          );
        } else {
          SystemNavigator.pop();
        }
      },
      child: Scaffold(
        backgroundColor: NetraColors.backgroundGray,
        appBar: AppBar(
          backgroundColor: Colors.transparent,
          elevation: 0,
          leading: canPop
              ? IconButton(
                  icon: const Icon(Icons.arrow_back_rounded,
                      color: NetraColors.textPrimary),
                  onPressed: () => Navigator.of(context).maybePop(),
                )
              : null,
        ),
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: context.screenGutter,
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 440),
              child: NetraCard.clay(
                borderRadius: 24,
                clayDepth: 6.0,
                padding: const EdgeInsets.symmetric(
                  horizontal: NetraSpacing.xl,
                  vertical: NetraSpacing.xxl,
                ),
                child: Form(
                    key: _formKey,
                    child: AutofillGroup(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        mainAxisSize: MainAxisSize.min,
                      children: [
                        // App Brand Header
                        Center(
                          child: Container(
                            width: 64,
                            height: 64,
                            decoration: BoxDecoration(
                              borderRadius:
                                  BorderRadius.circular(NetraSpacing.radiusMd),
                              boxShadow: [
                                BoxShadow(
                                  color: NetraColors.primaryRed
                                      .withValues(alpha: 0.10),
                                  blurRadius: 16,
                                  offset: const Offset(0, 4),
                                ),
                              ],
                            ),
                            child: ClipRRect(
                              borderRadius:
                                  BorderRadius.circular(NetraSpacing.radiusMd),
                              child: Image.asset(
                                'assets/branding/netra_logo.png',
                                fit: BoxFit.contain,
                              ),
                            ),
                          ),
                        ),
                        NetraSpacing.gapH16,
                        Text(
                          "Welcome Back",
                          textAlign: TextAlign.center,
                          style: NetraTypography.headlineMedium,
                        ),
                        NetraSpacing.gapH4,
                        Text(
                          "Sign in to your NETRA account",
                          textAlign: TextAlign.center,
                          style: NetraTypography.bodyMedium.copyWith(
                            color: NetraColors.textSecondary,
                          ),
                        ),
                        NetraSpacing.gapH24,

                        // Error / Verification Banner
                        if (authController.errorMessage != null) ...[
                          Container(
                            padding: const EdgeInsets.all(NetraSpacing.md),
                            decoration: BoxDecoration(
                              color: authController.isUnverified
                                  ? const Color(0xFFFFFBEB)
                                  : NetraColors.backgroundRed,
                              borderRadius:
                                  BorderRadius.circular(NetraSpacing.radiusSm),
                              border: Border.all(
                                color: authController.isUnverified
                                    ? const Color(0xFFF59E0B)
                                    : NetraColors.errorRed.withValues(alpha: 0.4),
                              ),
                            ),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.stretch,
                              children: [
                                Row(
                                  children: [
                                    Icon(
                                      authController.isUnverified
                                          ? Icons.mark_email_unread_rounded
                                          : Icons.error_outline_rounded,
                                      color: authController.isUnverified
                                          ? const Color(0xFFB45309)
                                          : NetraColors.errorRed,
                                      size: 20,
                                    ),
                                    NetraSpacing.gapW12,
                                    Expanded(
                                      child: Text(
                                        authController.errorMessage!,
                                        style: NetraTypography.bodySmall.copyWith(
                                          color: authController.isUnverified
                                              ? const Color(0xFF92400E)
                                              : NetraColors.errorRed,
                                          fontWeight: authController.isUnverified
                                              ? FontWeight.w600
                                              : FontWeight.normal,
                                        ),
                                      ),
                                    ),
                                    IconButton(
                                      icon: Icon(
                                        Icons.close,
                                        size: 16,
                                        color: authController.isUnverified
                                            ? const Color(0xFFB45309)
                                            : NetraColors.errorRed,
                                      ),
                                      padding: EdgeInsets.zero,
                                      constraints: const BoxConstraints(),
                                      onPressed: () => authController.clearError(),
                                    ),
                                  ],
                                ),
                                if (authController.isUnverified) ...[
                                  NetraSpacing.gapH8,
                                  SizedBox(
                                    width: double.infinity,
                                    height: 38,
                                    child: ElevatedButton.icon(
                                      style: ElevatedButton.styleFrom(
                                        backgroundColor: const Color(0xFFD97706),
                                        foregroundColor: Colors.white,
                                        shape: RoundedRectangleBorder(
                                          borderRadius: BorderRadius.circular(8),
                                        ),
                                        elevation: 0,
                                      ),
                                      onPressed: () {
                                        final targetEmail = authController.unverifiedEmail ??
                                            _emailController.text.trim();
                                        Navigator.of(context).push(
                                          MaterialPageRoute(
                                            builder: (_) => VerifyEmailScreen(email: targetEmail),
                                          ),
                                        );
                                      },
                                      icon: const Icon(Icons.verified_rounded, size: 16),
                                      label: const Text(
                                        "Verify Email Now",
                                        style: TextStyle(
                                          fontSize: 13,
                                          fontWeight: FontWeight.bold,
                                        ),
                                      ),
                                    ),
                                  ),
                                ],
                              ],
                            ),
                          ),
                          NetraSpacing.gapH16,
                        ],

                        // Email Field
                        AuthTextField(
                          label: "Email Address",
                          hint: "name@example.com",
                          controller: _emailController,
                          autofocus: false,
                          autovalidateMode: AutovalidateMode.onUserInteraction,
                          autofillHints: const [
                            AutofillHints.email,
                            AutofillHints.username,
                          ],
                          keyboardType: TextInputType.emailAddress,
                          textInputAction: TextInputAction.next,
                          prefixIcon:
                              const Icon(Icons.email_outlined, size: 20),
                          validator: (val) {
                            if (val == null || val.trim().isEmpty) {
                              return "Email is required";
                            }
                            final emailRegex =
                                RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$');
                            if (!emailRegex.hasMatch(val.trim())) {
                              return "Enter a valid email address";
                            }
                            return null;
                          },
                        ),
                        NetraSpacing.gapH16,

                        // Password Field
                        AuthTextField(
                          label: "Password",
                          hint: "Enter your password",
                          controller: _passwordController,
                          isPassword: true,
                          autovalidateMode: AutovalidateMode.onUserInteraction,
                          autofillHints: const [AutofillHints.password],
                          textInputAction: TextInputAction.done,
                          prefixIcon:
                              const Icon(Icons.lock_outline_rounded, size: 20),
                          onFieldSubmitted: (_) => _handleLogin(authController),
                          validator: (val) {
                            if (val == null || val.isEmpty) {
                              return "Password is required";
                            }
                            return null;
                          },
                        ),
                        NetraSpacing.gapH24,

                        // Submit Button
                        NetraButton(
                          text: "Log In",
                          isLoading: isLoading,
                          onPressed: isLoading
                              ? null
                              : () => _handleLogin(authController),
                        ),
                        NetraSpacing.gapH16,

                        // Register Link
                        Wrap(
                          alignment: WrapAlignment.center,
                          crossAxisAlignment: WrapCrossAlignment.center,
                          children: [
                            Text(
                              "Don't have an account? ",
                              style: NetraTypography.bodyMedium
                                  .copyWith(color: NetraColors.textSecondary),
                            ),
                            TextButton(
                              onPressed: () {
                                authController.clearError();
                                Navigator.of(context).push(
                                  MaterialPageRoute(
                                      builder: (_) => const RegisterScreen()),
                                );
                              },
                              child: Text(
                                "Sign Up",
                                style: NetraTypography.titleSmall.copyWith(
                                  color: NetraColors.primaryRed,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                            ),
                          ],
                        ),

                        const Divider(height: 32),

                        // Guest Access Button
                        OutlinedButton.icon(
                          onPressed: () {
                            authController.clearError();
                            Navigator.of(context).pushAndRemoveUntil(
                              MaterialPageRoute(
                                  builder: (_) => const HomeScreen()),
                              (route) => false,
                            );
                          },
                          icon: const Icon(Icons.explore_outlined, size: 18),
                          label: const Text("Continue as Guest"),
                          style: OutlinedButton.styleFrom(
                            foregroundColor: NetraColors.textPrimary,
                            side:
                                const BorderSide(color: NetraColors.borderGray),
                            padding: const EdgeInsets.symmetric(vertical: 12),
                            shape: RoundedRectangleBorder(
                              borderRadius:
                                  BorderRadius.circular(NetraSpacing.radiusMd),
                            ),
                          ),
                        ),
                        NetraSpacing.gapH16,

                        // Privacy Footer
                        Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            const Icon(Icons.lock_outline,
                                size: 14, color: NetraColors.textSecondary),
                            NetraSpacing.gapW8,
                            Text(
                              "Secure & confidential health platform",
                              style: NetraTypography.bodySmall
                                  .copyWith(color: NetraColors.textSecondary),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
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

class _LoginSuccessDialog extends StatefulWidget {
  const _LoginSuccessDialog();

  @override
  State<_LoginSuccessDialog> createState() => _LoginSuccessDialogState();
}

class _LoginSuccessDialogState extends State<_LoginSuccessDialog>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller;
  late final Animation<double> _scaleAnimation;
  late final Animation<double> _rippleAnimation;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1200),
    )..repeat();

    _scaleAnimation = Tween<double>(begin: 0.85, end: 1.05).animate(
      CurvedAnimation(
        parent: _controller,
        curve: Curves.easeInOut,
      ),
    );

    _rippleAnimation = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(
        parent: _controller,
        curve: Curves.easeOut,
      ),
    );
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Dialog(
      backgroundColor: Colors.transparent,
      elevation: 0,
      child: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            SizedBox(
              width: 140,
              height: 140,
              child: Stack(
                alignment: Alignment.center,
                children: [
                  AnimatedBuilder(
                    animation: _controller,
                    builder: (_, __) {
                      final ripple = _rippleAnimation.value;
                      return Container(
                        width: 90 + ripple * 45,
                        height: 90 + ripple * 45,
                        decoration: BoxDecoration(
                          shape: BoxShape.circle,
                          border: Border.all(
                            color: const Color(0xFFFB7185).withValues(
                              alpha: (1.0 - ripple).clamp(0.0, 1.0),
                            ),
                            width: 2.5,
                          ),
                        ),
                      );
                    },
                  ),
                  ScaleTransition(
                    scale: _scaleAnimation,
                    child: Container(
                      width: 84,
                      height: 84,
                      decoration: BoxDecoration(
                        color: NetraColors.primaryRed,
                        shape: BoxShape.circle,
                        boxShadow: [
                          BoxShadow(
                            color: const Color(0xFFDC2626).withValues(alpha: 0.5),
                            blurRadius: 20,
                            offset: const Offset(0, 4),
                          ),
                        ],
                      ),
                      child: const Center(
                        child: Icon(
                          Icons.water_drop_rounded,
                          color: Colors.white,
                          size: 44,
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 20),
            const Text(
              "Welcome Back",
              style: TextStyle(
                fontSize: 22,
                fontWeight: FontWeight.w900,
                color: Colors.white,
                letterSpacing: 0.5,
              ),
            ),
            const SizedBox(height: 8),
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const SizedBox(
                  width: 14,
                  height: 14,
                  child: CircularProgressIndicator(
                    strokeWidth: 2,
                    valueColor: AlwaysStoppedAnimation<Color>(Color(0xFFFB7185)),
                  ),
                ),
                const SizedBox(width: 8),
                Text(
                  "Redirecting to dashboard...",
                  style: TextStyle(
                    fontSize: 13,
                    color: Colors.white.withValues(alpha: 0.8),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
