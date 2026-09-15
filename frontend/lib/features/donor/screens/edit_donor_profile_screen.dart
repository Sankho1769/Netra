import 'package:flutter/material.dart';
import '../../../core/responsive/responsive.dart';
import '../../../core/theme/netra_colors.dart';
import '../../../core/theme/netra_spacing.dart';
import '../../../core/theme/netra_typography.dart';
import '../../../common/widgets/common_widgets.dart';
import '../../auth/services/secure_token_storage.dart';
import '../models/donor_profile.dart';
import '../state/donor_controller.dart';

class EditDonorProfileScreen extends StatefulWidget {
  final DonorProfile? currentProfile;
  final DonorController controller;

  const EditDonorProfileScreen({
    super.key,
    this.currentProfile,
    required this.controller,
  });

  @override
  State<EditDonorProfileScreen> createState() => _EditDonorProfileScreenState();
}

class _EditDonorProfileScreenState extends State<EditDonorProfileScreen> {
  late String _selectedBloodGroup;
  late String _selectedAvailability;
  final SecureTokenStorage _tokenStorage = PlatformSecureTokenStorage();
  bool _isSaving = false;

  @override
  void initState() {
    super.initState();
    _selectedBloodGroup = widget.currentProfile?.bloodGroup ?? 'O+';
    _selectedAvailability = widget.currentProfile?.availabilityStatus ?? 'AVAILABLE';
  }

  Future<void> _handleSave() async {
    setState(() => _isSaving = true);

    final token = await _tokenStorage.getAccessToken();
    if (token == null) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text("Session expired. Please log in again.")),
        );
        setState(() => _isSaving = false);
      }
      return;
    }

    bool success;
    if (widget.currentProfile == null) {
      success = await widget.controller.createDonorProfile(
        token,
        CreateDonorProfileRequest(
          bloodGroup: _selectedBloodGroup,
          availabilityStatus: _selectedAvailability,
        ),
      );
    } else {
      success = await widget.controller.updateDonorProfile(
        token,
        UpdateDonorProfileRequest(
          bloodGroup: _selectedBloodGroup,
          availabilityStatus: _selectedAvailability,
        ),
      );
    }

    if (mounted) {
      setState(() => _isSaving = false);
      if (success) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text("Donor preferences saved successfully.")),
        );
        Navigator.of(context).pop();
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(widget.controller.errorMessage ?? "Failed to save donor preferences."),
            backgroundColor: NetraColors.errorRed,
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final isNew = widget.currentProfile == null;

    return Scaffold(
      backgroundColor: NetraColors.backgroundGray,
      appBar: NetraAppBar(
        title: isNew ? "Setup Donor Profile" : "Edit Donor Preferences",
      ),
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: context.screenGutter,
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 580),
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
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      Text(
                        "Blood Group",
                        style: NetraTypography.titleLarge,
                      ),
                      NetraSpacing.gapH4,
                      Text(
                        "Select your verified or self-reported blood group.",
                        style: NetraTypography.bodyMedium.copyWith(color: NetraColors.textSecondary),
                      ),
                      NetraSpacing.gapH16,

                      // 8 Blood Group Selector Buttons in Grid
                      Wrap(
                        spacing: 12,
                        runSpacing: 12,
                        children: BloodGroupConstants.supported.map((bg) {
                          final isSelected = _selectedBloodGroup == bg;
                          return InkWell(
                            onTap: () => setState(() => _selectedBloodGroup = bg),
                            borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
                            child: AnimatedContainer(
                              duration: const Duration(milliseconds: 150),
                              width: 88,
                              height: 52,
                              decoration: BoxDecoration(
                                color: isSelected ? NetraColors.primaryRed : NetraColors.surfaceWhite,
                                borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
                                border: BorderSide(
                                  color: isSelected ? NetraColors.primaryRed : NetraColors.borderGray,
                                  width: isSelected ? 2 : 1,
                                ),
                              ),
                              alignment: Alignment.center,
                              child: Text(
                                bg,
                                style: NetraTypography.titleMedium.copyWith(
                                  color: isSelected ? NetraColors.surfaceWhite : NetraColors.textPrimary,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                            ),
                          );
                        }).toList(),
                      ),
                      NetraSpacing.gapH12,

                      // Self-Reported Disclaimer
                      Container(
                        padding: const EdgeInsets.all(NetraSpacing.md),
                        decoration: BoxDecoration(
                          color: NetraColors.backgroundRed,
                          borderRadius: BorderRadius.circular(NetraSpacing.radiusSm),
                        ),
                        child: Row(
                          children: [
                            const Icon(Icons.info_outline_rounded, color: NetraColors.primaryRed, size: 18),
                            NetraSpacing.gapW12,
                            Expanded(
                              child: Text(
                                "Your blood group is self-reported until verified by an authorized blood bank upon clinical screening.",
                                style: NetraTypography.bodySmall.copyWith(color: NetraColors.darkRed),
                              ),
                            ),
                          ],
                        ),
                      ),
                      NetraSpacing.gapH24,

                      Text(
                        "Donation Availability",
                        style: NetraTypography.titleLarge,
                      ),
                      NetraSpacing.gapH4,
                      Text(
                        "Control whether you can be considered for approved donor contact requests.",
                        style: NetraTypography.bodyMedium.copyWith(color: NetraColors.textSecondary),
                      ),
                      NetraSpacing.gapH16,

                      _buildAvailabilityOption(
                        status: 'AVAILABLE',
                        title: "Available",
                        subtitle: "You may be considered for approved donor requests.",
                        icon: Icons.check_circle_outline_rounded,
                        activeColor: NetraColors.successGreen,
                      ),
                      NetraSpacing.gapH12,
                      _buildAvailabilityOption(
                        status: 'PAUSED',
                        title: "Paused",
                        subtitle: "Temporarily pause donor matching and notifications.",
                        icon: Icons.pause_circle_outline_rounded,
                        activeColor: Colors.amber.shade800,
                      ),
                      NetraSpacing.gapH12,
                      _buildAvailabilityOption(
                        status: 'UNAVAILABLE',
                        title: "Unavailable",
                        subtitle: "You will not be considered for donor contact.",
                        icon: Icons.do_not_disturb_on_outlined,
                        activeColor: NetraColors.errorRed,
                      ),
                      NetraSpacing.gapH32,

                      // Actions
                      NetraButton(
                        text: isNew ? "Create Donor Profile" : "Save Preferences",
                        isLoading: _isSaving,
                        onPressed: _isSaving ? null : _handleSave,
                      ),
                      NetraSpacing.gapH12,
                      NetraButton.outlined(
                        text: "Cancel",
                        onPressed: _isSaving ? null : () => Navigator.of(context).pop(),
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

  Widget _buildAvailabilityOption({
    required String status,
    required String title,
    required String subtitle,
    required IconData icon,
    required Color activeColor,
  }) {
    final isSelected = _selectedAvailability == status;

    return InkWell(
      onTap: () => setState(() => _selectedAvailability = status),
      borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
      child: Container(
        padding: const EdgeInsets.all(NetraSpacing.md),
        decoration: BoxDecoration(
          color: isSelected ? activeColor.withOpacity(0.08) : NetraColors.surfaceWhite,
          borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
          border: BorderSide(
            color: isSelected ? activeColor : NetraColors.borderGray,
            width: isSelected ? 2 : 1,
          ),
        ),
        child: Row(
          children: [
            Icon(icon, color: isSelected ? activeColor : NetraColors.textSecondary, size: 24),
            NetraSpacing.gapW16,
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: NetraTypography.titleSmall.copyWith(
                      color: isSelected ? activeColor : NetraColors.textPrimary,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  NetraSpacing.gapH4,
                  Text(subtitle, style: NetraTypography.bodySmall),
                ],
              ),
            ),
            Radio<String>(
              value: status,
              groupValue: _selectedAvailability,
              activeColor: activeColor,
              onChanged: (val) {
                if (val != null) setState(() => _selectedAvailability = val);
              },
            ),
          ],
        ),
      ),
    );
  }
}
