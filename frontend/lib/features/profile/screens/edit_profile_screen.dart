import 'package:flutter/material.dart';
import '../../../core/responsive/responsive.dart';
import '../../../core/theme/netra_colors.dart';
import '../../../core/theme/netra_spacing.dart';
import '../../../core/theme/netra_typography.dart';
import '../../../common/widgets/common_widgets.dart';
import '../../auth/services/secure_token_storage.dart';
import '../models/user_profile.dart';
import '../state/profile_controller.dart';

class EditProfileScreen extends StatefulWidget {
  final UserProfile currentProfile;
  final ProfileController controller;

  const EditProfileScreen({
    super.key,
    required this.currentProfile,
    required this.controller,
  });

  @override
  State<EditProfileScreen> createState() => _EditProfileScreenState();
}

class _EditProfileScreenState extends State<EditProfileScreen> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _nameController;
  late final TextEditingController _phoneController;
  final SecureTokenStorage _tokenStorage = PlatformSecureTokenStorage();
  bool _isSaving = false;

  @override
  void initState() {
    super.initState();
    _nameController =
        TextEditingController(text: widget.currentProfile.fullName);
    _phoneController =
        TextEditingController(text: widget.currentProfile.phone ?? '');
  }

  @override
  void dispose() {
    _nameController.dispose();
    _phoneController.dispose();
    super.dispose();
  }

  Future<void> _handleSave() async {
    FocusScope.of(context).unfocus();

    if (!_formKey.currentState!.validate()) {
      return;
    }

    setState(() => _isSaving = true);

    final token = await _tokenStorage.getAccessToken();
    if (token == null) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
              content: Text("Session expired. Please log in again.")),
        );
        setState(() => _isSaving = false);
      }
      return;
    }

    final success = await widget.controller.updateProfile(
      token,
      UpdateUserProfileRequest(
        fullName: _nameController.text.trim(),
        phone: _phoneController.text.trim().isEmpty
            ? null
            : _phoneController.text.trim(),
      ),
    );

    if (mounted) {
      setState(() => _isSaving = false);
      if (success) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text("Profile updated successfully.")),
        );
        Navigator.of(context).pop();
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
                widget.controller.errorMessage ?? "Failed to update profile."),
            backgroundColor: NetraColors.errorRed,
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: NetraColors.backgroundGray,
      appBar: const NetraAppBar(title: "Edit Personal Profile"),
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: context.screenGutter,
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 560),
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
                        Text(
                          "Personal Information",
                          style: NetraTypography.headlineSmall,
                        ),
                        NetraSpacing.gapH8,
                        Text(
                          "Update your basic identity details. Medical and donation details are managed separately.",
                          style: NetraTypography.bodyMedium
                              .copyWith(color: NetraColors.textSecondary),
                        ),
                        NetraSpacing.gapH24,

                        // Full Name
                        NetraTextField(
                          label: "Full Name",
                          hint: "John Doe",
                          controller: _nameController,
                        ),
                        NetraSpacing.gapH16,

                        // Email (Read-only policy)
                        NetraTextField(
                          label: "Email Address",
                          hint: widget.currentProfile.email,
                          readOnly: true,
                          helperText:
                              "Email cannot be modified directly without an authenticated verification flow.",
                        ),
                        NetraSpacing.gapH16,

                        // Phone Number
                        NetraTextField(
                          label: "Phone Number (Optional)",
                          hint: "+91 98765 43210",
                          controller: _phoneController,
                          keyboardType: TextInputType.phone,
                          helperText:
                              "Used only for urgent blood donation contact when availability is enabled.",
                        ),
                        NetraSpacing.gapH24,

                        // Save Button
                        NetraButton(
                          text: "Save Changes",
                          isLoading: _isSaving,
                          onPressed: _isSaving ? null : _handleSave,
                        ),
                        NetraSpacing.gapH12,

                        // Cancel Button
                        NetraButton.outlined(
                          text: "Cancel",
                          onPressed: _isSaving
                              ? null
                              : () => Navigator.of(context).pop(),
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
