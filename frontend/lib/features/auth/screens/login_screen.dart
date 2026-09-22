import 'package:flutter/material.dart';
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

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();

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
      Navigator.of(context).pushReplacement(
        MaterialPageRoute(builder: (_) => const HomeScreen()),
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
        leading: Navigator.of(context).canPop()
            ? IconButton(
                icon: const Icon(Icons.arrow_back_rounded,
                    color: NetraColors.textPrimary),
                onPressed: () => Navigator.of(context).pop(),
              )
            : null,
      ),
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: context.screenGutter,
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 440),
              child: Card(
                elevation: context.isMobile ? 0 : 2,
                color: NetraColors.surfaceWhite,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(NetraSpacing.radiusLg),
                  side: BorderSide(
                    color: context.isMobile
                        ? Colors.transparent
                        : NetraColors.borderGray,
                  ),
                ),
                child: Padding(
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
                        // App Brand Header
                        Center(
                          child: Container(
                            padding: const EdgeInsets.all(12),
                            decoration: BoxDecoration(
                              color: NetraColors.backgroundRed,
                              borderRadius:
                                  BorderRadius.circular(NetraSpacing.radiusMd),
                            ),
                            child: const Icon(
                              Icons.water_drop_rounded,
                              color: NetraColors.primaryRed,
                              size: 32,
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

                        // Error Banner
                        if (authController.errorMessage != null) ...[
                          Container(
                            padding: const EdgeInsets.all(NetraSpacing.md),
                            decoration: BoxDecoration(
                              color: NetraColors.backgroundRed,
                              borderRadius:
                                  BorderRadius.circular(NetraSpacing.radiusSm),
                              border: Border.all(
                                  color: NetraColors.errorRed.withOpacity(0.4)),
                            ),
                            child: Row(
                              children: [
                                const Icon(Icons.error_outline_rounded,
                                    color: NetraColors.errorRed, size: 20),
                                NetraSpacing.gapW12,
                                Expanded(
                                  child: Text(
                                    authController.errorMessage!,
                                    style: NetraTypography.bodySmall
                                        .copyWith(color: NetraColors.errorRed),
                                  ),
                                ),
                                IconButton(
                                  icon: const Icon(Icons.close,
                                      size: 16, color: NetraColors.errorRed),
                                  padding: EdgeInsets.zero,
                                  constraints: const BoxConstraints(),
                                  onPressed: () => authController.clearError(),
                                ),
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
                        Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Text(
                              "Don't have an account?",
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
                            Navigator.of(context).pushReplacement(
                              MaterialPageRoute(
                                  builder: (_) => const HomeScreen()),
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
    );
  }
}
