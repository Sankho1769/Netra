import 'package:flutter/material.dart';
import '../../../core/responsive/responsive.dart';
import '../../../core/theme/netra_colors.dart';
import '../../../core/theme/netra_spacing.dart';
import '../../../core/theme/netra_typography.dart';
import '../../../common/widgets/common_widgets.dart';
import '../state/auth_controller.dart';
import '../state/auth_scope.dart';
import '../widgets/auth_text_field.dart';
import '../../home/home_screen.dart';

class RegisterScreen extends StatefulWidget {
  const RegisterScreen({super.key});

  @override
  State<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends State<RegisterScreen> {
  final _formKey = GlobalKey<FormState>();
  final _fullNameController = TextEditingController();
  final _emailController = TextEditingController();
  final _phoneController = TextEditingController();
  final _passwordController = TextEditingController();

  bool _hasMinLength = false;
  bool _hasUpperCase = false;
  bool _hasLowerCase = false;
  bool _hasDigit = false;

  @override
  void initState() {
    super.initState();
    _passwordController.addListener(_updatePasswordStrength);
  }

  @override
  void dispose() {
    _passwordController.removeListener(_updatePasswordStrength);
    _fullNameController.dispose();
    _emailController.dispose();
    _phoneController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  void _updatePasswordStrength() {
    final text = _passwordController.text;
    setState(() {
      _hasMinLength = text.length >= 8;
      _hasUpperCase = text.contains(RegExp(r'[A-Z]'));
      _hasLowerCase = text.contains(RegExp(r'[a-z]'));
      _hasDigit = text.contains(RegExp(r'[0-9]'));
    });
  }

  Future<void> _handleRegister(AuthController authController) async {
    FocusScope.of(context).unfocus();

    if (!_formKey.currentState!.validate()) {
      return;
    }

    final success = await authController.register(
      fullName: _fullNameController.text.trim(),
      email: _emailController.text.trim(),
      phone: _phoneController.text.trim().isEmpty ? null : _phoneController.text.trim(),
      password: _passwordController.text,
    );

    if (success && mounted) {
      Navigator.of(context).pushAndRemoveUntil(
        MaterialPageRoute(builder: (_) => const HomeScreen()),
        (route) => false,
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
          onPressed: () => Navigator.of(context).pop(),
        ),
      ),
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: context.screenGutter,
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 460),
              child: Card(
                elevation: context.isMobile ? 0 : 2,
                color: NetraColors.surfaceWhite,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(NetraSpacing.radiusLg),
                  side: BorderSide(
                    color: context.isMobile ? Colors.transparent : NetraColors.borderGray,
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
                        // Header
                        Center(
                          child: Container(
                            padding: const EdgeInsets.all(12),
                            decoration: BoxDecoration(
                              color: NetraColors.backgroundRed,
                              borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
                            ),
                            child: const Icon(
                              Icons.person_add_outlined,
                              color: NetraColors.primaryRed,
                              size: 32,
                            ),
                          ),
                        ),
                        NetraSpacing.gapH16,
                        Text(
                          "Create Account",
                          textAlign: TextAlign.center,
                          style: NetraTypography.headlineMedium,
                        ),
                        NetraSpacing.gapH4,
                        Text(
                          "Join the NETRA lifesaving network",
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
                              borderRadius: BorderRadius.circular(NetraSpacing.radiusSm),
                              border: BorderSide(color: NetraColors.errorRed.withOpacity(0.4)),
                            ),
                            child: Row(
                              children: [
                                const Icon(Icons.error_outline_rounded, color: NetraColors.errorRed, size: 20),
                                NetraSpacing.gapW12,
                                Expanded(
                                  child: Text(
                                    authController.errorMessage!,
                                    style: NetraTypography.bodySmall.copyWith(color: NetraColors.errorRed),
                                  ),
                                ),
                                IconButton(
                                  icon: const Icon(Icons.close, size: 16, color: NetraColors.errorRed),
                                  padding: EdgeInsets.zero,
                                  constraints: const BoxConstraints(),
                                  onPressed: () => authController.clearError(),
                                ),
                              ],
                            ),
                          ),
                          NetraSpacing.gapH16,
                        ],

                        // Full Name
                        AuthTextField(
                          label: "Full Name",
                          hint: "John Doe",
                          controller: _fullNameController,
                          textInputAction: TextInputAction.next,
                          prefixIcon: const Icon(Icons.badge_outlined, size: 20),
                          validator: (val) {
                            if (val == null || val.trim().isEmpty) {
                              return "Full name is required";
                            }
                            if (val.trim().length < 2) {
                              return "Name must be at least 2 characters";
                            }
                            return null;
                          },
                        ),
                        NetraSpacing.gapH16,

                        // Email
                        AuthTextField(
                          label: "Email Address",
                          hint: "name@example.com",
                          controller: _emailController,
                          keyboardType: TextInputType.emailAddress,
                          textInputAction: TextInputAction.next,
                          prefixIcon: const Icon(Icons.email_outlined, size: 20),
                          validator: (val) {
                            if (val == null || val.trim().isEmpty) {
                              return "Email is required";
                            }
                            final emailRegex = RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$');
                            if (!emailRegex.hasMatch(val.trim())) {
                              return "Enter a valid email address";
                            }
                            return null;
                          },
                        ),
                        NetraSpacing.gapH16,

                        // Phone (Optional)
                        AuthTextField(
                          label: "Phone Number (Optional)",
                          hint: "+91 98765 43210",
                          controller: _phoneController,
                          keyboardType: TextInputType.phone,
                          textInputAction: TextInputAction.next,
                          prefixIcon: const Icon(Icons.phone_outlined, size: 20),
                          validator: (val) {
                            if (val != null && val.trim().isNotEmpty) {
                              final phoneRegex = RegExp(r'^\+?[0-9\s\-]{7,16}$');
                              if (!phoneRegex.hasMatch(val.trim())) {
                                return "Enter a valid phone number";
                              }
                            }
                            return null;
                          },
                        ),
                        NetraSpacing.gapH16,

                        // Password
                        AuthTextField(
                          label: "Password",
                          hint: "Create a secure password",
                          controller: _passwordController,
                          isPassword: true,
                          textInputAction: TextInputAction.done,
                          prefixIcon: const Icon(Icons.lock_outline_rounded, size: 20),
                          onFieldSubmitted: (_) => _handleRegister(authController),
                          validator: (val) {
                            if (val == null || val.isEmpty) {
                              return "Password is required";
                            }
                            if (val.length < 8) {
                              return "Password must be at least 8 characters";
                            }
                            if (!RegExp(r'[A-Z]').hasMatch(val)) {
                              return "Password must include at least one uppercase letter";
                            }
                            if (!RegExp(r'[a-z]').hasMatch(val)) {
                              return "Password must include at least one lowercase letter";
                            }
                            if (!RegExp(r'[0-9]').hasMatch(val)) {
                              return "Password must include at least one digit";
                            }
                            return null;
                          },
                        ),
                        NetraSpacing.gapH12,

                        // Password Checklist UI
                        Container(
                          padding: const EdgeInsets.all(NetraSpacing.md),
                          decoration: BoxDecoration(
                            color: NetraColors.backgroundGray,
                            borderRadius: BorderRadius.circular(NetraSpacing.radiusSm),
                          ),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                "Password requirements:",
                                style: NetraTypography.labelSmall.copyWith(
                                  color: NetraColors.textSecondary,
                                  fontWeight: FontWeight.w600,
                                ),
                              ),
                              NetraSpacing.gapH8,
                              _buildRuleItem("8+ characters", _hasMinLength),
                              _buildRuleItem("At least one uppercase letter", _hasUpperCase),
                              _buildRuleItem("At least one lowercase letter", _hasLowerCase),
                              _buildRuleItem("At least one number (0-9)", _hasDigit),
                            ],
                          ),
                        ),
                        NetraSpacing.gapH16,

                        // Clinical Privacy Note (Zero medical data at registration)
                        Container(
                          padding: const EdgeInsets.all(NetraSpacing.md),
                          decoration: BoxDecoration(
                            color: NetraColors.backgroundGreen,
                            borderRadius: BorderRadius.circular(NetraSpacing.radiusSm),
                          ),
                          child: Row(
                            children: [
                              const Icon(Icons.verified_user_outlined, color: NetraColors.successGreen, size: 20),
                              NetraSpacing.gapW12,
                              Expanded(
                                child: Text(
                                  "Zero medical data collected at registration. Clinical suitability is assessed separately and confidentially.",
                                  style: NetraTypography.bodySmall.copyWith(color: NetraColors.successGreen),
                                ),
                              ),
                            ],
                          ),
                        ),
                        NetraSpacing.gapH24,

                        // Submit Button
                        NetraButton(
                          text: "Create Account",
                          isLoading: isLoading,
                          onPressed: isLoading ? null : () => _handleRegister(authController),
                        ),
                        NetraSpacing.gapH16,

                        // Login Link
                        Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Text(
                              "Already have an account?",
                              style: NetraTypography.bodyMedium.copyWith(color: NetraColors.textSecondary),
                            ),
                            TextButton(
                              onPressed: () {
                                authController.clearError();
                                Navigator.of(context).pop();
                              },
                              child: Text(
                                "Sign In",
                                style: NetraTypography.titleSmall.copyWith(
                                  color: NetraColors.primaryRed,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
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

  Widget _buildRuleItem(String text, bool satisfied) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2.0),
      child: Row(
        children: [
          Icon(
            satisfied ? Icons.check_circle_rounded : Icons.radio_button_unchecked_rounded,
            size: 14,
            color: satisfied ? NetraColors.successGreen : NetraColors.textSecondary,
          ),
          NetraSpacing.gapW8,
          Text(
            text,
            style: NetraTypography.bodySmall.copyWith(
              color: satisfied ? NetraColors.successGreen : NetraColors.textSecondary,
            ),
          ),
        ],
      ),
    );
  }
}
