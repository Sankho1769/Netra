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
  final _confirmPasswordController = TextEditingController();

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
    _confirmPasswordController.dispose();
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

    String rawPhone = _phoneController.text.replaceAll(RegExp(r'[\s\-\(\)]'), '');
    if (!rawPhone.startsWith('+91')) {
      if (rawPhone.startsWith('91') && rawPhone.length == 12) {
        rawPhone = '+$rawPhone';
      } else {
        rawPhone = '+91$rawPhone';
      }
    }

    final success = await authController.register(
      fullName: _fullNameController.text.trim(),
      email: _emailController.text.trim().toLowerCase(),
      phone: rawPhone,
      password: _passwordController.text,
    );

    if (success && mounted) {
      await showDialog(
        context: context,
        barrierDismissible: false,
        barrierColor: Colors.black.withValues(alpha: 0.65),
        builder: (_) => _RegisterSuccessDialog(
          fullName: _fullNameController.text.trim(),
          email: _emailController.text.trim().toLowerCase(),
          phone: rawPhone,
          onProceed: () {
            Navigator.of(context).pushAndRemoveUntil(
              MaterialPageRoute(builder: (_) => const HomeScreen()),
              (route) => false,
            );
          },
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
          icon: const Icon(Icons.arrow_back_rounded,
              color: NetraColors.textPrimary),
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
                        // Header
                        Center(
                          child: Container(
                            padding: const EdgeInsets.all(12),
                            decoration: BoxDecoration(
                              color: NetraColors.backgroundRed,
                              borderRadius:
                                  BorderRadius.circular(NetraSpacing.radiusMd),
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
                              borderRadius:
                                  BorderRadius.circular(NetraSpacing.radiusSm),
                              border: Border.all(
                                  color: NetraColors.errorRed
                                      .withValues(alpha: 0.4)),
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

                        // Full Name
                        AuthTextField(
                          label: "Full Name",
                          hint: "Jolly Banerjee",
                          controller: _fullNameController,
                          autofocus: true,
                          textInputAction: TextInputAction.next,
                          prefixIcon:
                              const Icon(Icons.badge_outlined, size: 20),
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

                        // Mobile Number
                        AuthTextField(
                          label: "Mobile Number *",
                          hint: "+91 98765 43210",
                          controller: _phoneController,
                          keyboardType: TextInputType.phone,
                          textInputAction: TextInputAction.next,
                          prefixIcon:
                              const Icon(Icons.phone_outlined, size: 20),
                          validator: (val) {
                            if (val == null || val.trim().isEmpty) {
                              return "Mobile number is required";
                            }
                            final clean =
                                val.replaceAll(RegExp(r'[\s\-\(\)]'), '');
                            final phoneRegex =
                                RegExp(r'^(?:\+91|91)?[6-9]\d{9}$');
                            if (!phoneRegex.hasMatch(clean)) {
                              return "Enter a valid 10-digit Indian mobile number";
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
                          textInputAction: TextInputAction.next,
                          prefixIcon:
                              const Icon(Icons.lock_outline_rounded, size: 20),
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
                            borderRadius:
                                BorderRadius.circular(NetraSpacing.radiusSm),
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
                              _buildRuleItem("At least one uppercase letter",
                                  _hasUpperCase),
                              _buildRuleItem("At least one lowercase letter",
                                  _hasLowerCase),
                              _buildRuleItem(
                                  "At least one number (0-9)", _hasDigit),
                            ],
                          ),
                        ),
                        NetraSpacing.gapH16,

                        // Confirm Password
                        AuthTextField(
                          label: "Confirm Password *",
                          hint: "Re-enter your password",
                          controller: _confirmPasswordController,
                          isPassword: true,
                          textInputAction: TextInputAction.done,
                          prefixIcon:
                              const Icon(Icons.lock_outline_rounded, size: 20),
                          onFieldSubmitted: (_) =>
                              _handleRegister(authController),
                          validator: (val) {
                            if (val == null || val.isEmpty) {
                              return "Please confirm your password";
                            }
                            if (val != _passwordController.text) {
                              return "Passwords do not match";
                            }
                            return null;
                          },
                        ),
                        NetraSpacing.gapH16,

                        // Clinical Privacy Note (Zero medical data at registration)
                        Container(
                          padding: const EdgeInsets.all(NetraSpacing.md),
                          decoration: BoxDecoration(
                            color: NetraColors.backgroundGreen,
                            borderRadius:
                                BorderRadius.circular(NetraSpacing.radiusSm),
                          ),
                          child: Row(
                            children: [
                              const Icon(Icons.verified_user_outlined,
                                  color: NetraColors.successGreen, size: 20),
                              NetraSpacing.gapW12,
                              Expanded(
                                child: Text(
                                  "Zero medical data collected at registration. Clinical suitability is assessed separately and confidentially.",
                                  style: NetraTypography.bodySmall.copyWith(
                                      color: NetraColors.successGreen),
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
                          onPressed: isLoading
                              ? null
                              : () => _handleRegister(authController),
                        ),
                        NetraSpacing.gapH16,

                        // Login Link
                        Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Text(
                              "Already have an account?",
                              style: NetraTypography.bodyMedium
                                  .copyWith(color: NetraColors.textSecondary),
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
            satisfied
                ? Icons.check_circle_rounded
                : Icons.radio_button_unchecked_rounded,
            size: 14,
            color: satisfied
                ? NetraColors.successGreen
                : NetraColors.textSecondary,
          ),
          NetraSpacing.gapW8,
          Text(
            text,
            style: NetraTypography.bodySmall.copyWith(
              color: satisfied
                  ? NetraColors.successGreen
                  : NetraColors.textSecondary,
            ),
          ),
        ],
      ),
    );
  }
}

class _RegisterSuccessDialog extends StatefulWidget {
  final String fullName;
  final String email;
  final String phone;
  final VoidCallback onProceed;

  const _RegisterSuccessDialog({
    required this.fullName,
    required this.email,
    required this.phone,
    required this.onProceed,
  });

  @override
  State<_RegisterSuccessDialog> createState() => _RegisterSuccessDialogState();
}

class _RegisterSuccessDialogState extends State<_RegisterSuccessDialog>
    with SingleTickerProviderStateMixin {
  late final AnimationController _animController;
  late final Animation<double> _scaleAnimation;

  @override
  void initState() {
    super.initState();
    _animController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 500),
    );
    _scaleAnimation = CurvedAnimation(
      parent: _animController,
      curve: Curves.easeOutBack,
    );
    _animController.forward();
  }

  @override
  void dispose() {
    _animController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final initial = widget.fullName.isNotEmpty
        ? widget.fullName[0].toUpperCase()
        : 'U';

    return ScaleTransition(
      scale: _scaleAnimation,
      child: Dialog(
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(24),
        ),
        backgroundColor: Colors.white,
        insetPadding: const EdgeInsets.symmetric(horizontal: 24, vertical: 32),
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              // Emerald Checkmark Icon (matching reference)
              Container(
                width: 64,
                height: 64,
                decoration: BoxDecoration(
                  color: const Color(0xFFDCFCE7),
                  shape: BoxShape.circle,
                  border: Border.all(
                    color: const Color(0xFF86EFAC),
                    width: 2,
                  ),
                ),
                child: const Center(
                  child: Icon(
                    Icons.check_rounded,
                    color: Color(0xFF16A34A),
                    size: 36,
                  ),
                ),
              ),
              const SizedBox(height: 18),

              // Title
              const Text(
                "Registration Successful!",
                style: TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.w800,
                  color: NetraColors.textPrimary,
                ),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 8),

              // Description
              Text(
                "Welcome to NETRA. Your profile has been created securely. You are now part of our lifesaver network.",
                style: TextStyle(
                  fontSize: 13,
                  color: Colors.grey.shade600,
                  height: 1.4,
                ),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 20),

              // User Preview Card (matching reference login.html)
              Container(
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  color: const Color(0xFFF8FAFC),
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(color: const Color(0xFFE2E8F0)),
                ),
                child: Row(
                  children: [
                    CircleAvatar(
                      radius: 22,
                      backgroundColor: NetraColors.backgroundRed,
                      child: Text(
                        initial,
                        style: const TextStyle(
                          color: NetraColors.primaryRed,
                          fontWeight: FontWeight.bold,
                          fontSize: 18,
                        ),
                      ),
                    ),
                    const SizedBox(width: 14),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            widget.fullName,
                            style: const TextStyle(
                              fontSize: 15,
                              fontWeight: FontWeight.w700,
                              color: NetraColors.textPrimary,
                            ),
                          ),
                          const SizedBox(height: 2),
                          Row(
                            children: [
                              Container(
                                width: 7,
                                height: 7,
                                decoration: const BoxDecoration(
                                  color: Color(0xFF16A34A),
                                  shape: BoxShape.circle,
                                ),
                              ),
                              const SizedBox(width: 5),
                              Expanded(
                                child: Text(
                                  widget.email,
                                  style: TextStyle(
                                    fontSize: 11,
                                    color: Colors.grey.shade600,
                                  ),
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 2),
                          Text(
                            widget.phone,
                            style: TextStyle(
                              fontSize: 11,
                              color: Colors.grey.shade500,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 24),

              // Go to Dashboard Button
              ElevatedButton(
                style: ElevatedButton.styleFrom(
                  backgroundColor: NetraColors.primaryRed,
                  foregroundColor: Colors.white,
                  minimumSize: const Size(double.infinity, 48),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(14),
                  ),
                  elevation: 2,
                ),
                onPressed: widget.onProceed,
                child: const Text(
                  "Go to Dashboard",
                  style: TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.bold,
                    letterSpacing: 0.3,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
