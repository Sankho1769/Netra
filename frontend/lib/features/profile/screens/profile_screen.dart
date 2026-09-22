import 'package:flutter/material.dart';
import '../../../core/responsive/responsive.dart';
import '../../../core/theme/netra_colors.dart';
import '../../../core/theme/netra_spacing.dart';
import '../../../core/theme/netra_typography.dart';
import '../../../common/widgets/common_widgets.dart';
import '../../auth/services/secure_token_storage.dart';
import '../../auth/state/auth_scope.dart';
import '../../auth/screens/login_screen.dart';
import '../../donor/models/donor_profile.dart';
import '../../donor/screens/donor_profile_screen.dart';
import '../../donor/screens/edit_donor_profile_screen.dart';
import '../../donor/state/donor_controller.dart';
import '../models/user_profile.dart';
import '../state/profile_controller.dart';
import 'edit_profile_screen.dart';

class ProfileScreen extends StatefulWidget {
  const ProfileScreen({super.key});

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  final ProfileController _profileController = ProfileController();
  final DonorController _donorController = DonorController();
  final SecureTokenStorage _tokenStorage = PlatformSecureTokenStorage();
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _profileController.addListener(_onStateChanged);
    _donorController.addListener(_onStateChanged);
    _loadData();
  }

  @override
  void dispose() {
    _profileController.removeListener(_onStateChanged);
    _donorController.removeListener(_onStateChanged);
    _profileController.dispose();
    _donorController.dispose();
    super.dispose();
  }

  void _onStateChanged() {
    if (mounted) setState(() {});
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    final token = await _tokenStorage.getAccessToken();
    if (token != null) {
      await Future.wait([
        _profileController.loadProfile(token),
        _donorController.loadDonorProfile(token),
      ]);
    }
    if (mounted) {
      setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_isLoading) {
      return const Scaffold(
        appBar: NetraAppBar(title: "Account & Profile"),
        body: Center(
            child: NetraLoadingIndicator(message: "Loading your profile...")),
      );
    }

    final userProfile = _profileController.profile;
    final donorProfile = _donorController.donorProfile;

    if (userProfile == null) {
      return Scaffold(
        appBar: const NetraAppBar(title: "Account & Profile"),
        body: Center(
          child: Padding(
            padding: context.screenGutter,
            child: NetraErrorView(
              message:
                  _profileController.errorMessage ?? "Unable to load profile.",
              onRetry: _loadData,
            ),
          ),
        ),
      );
    }

    return Scaffold(
      backgroundColor: NetraColors.backgroundGray,
      appBar: const NetraAppBar(title: "Account & Profile"),
      body: SafeArea(
        child: ResponsiveContainer.standard(
          scrollable: true,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // 1. Identity Header Card
              _buildHeaderCard(context, userProfile),
              NetraSpacing.gapH20,

              // 2. Personal Information Card
              _buildPersonalInfoCard(context, userProfile),
              NetraSpacing.gapH20,

              // 3. Donor Profile Card
              _buildDonorCard(context, donorProfile),
              NetraSpacing.gapH20,

              // 4. Privacy & Data Separation Notice
              _buildPrivacyCard(context),
              NetraSpacing.gapH24,

              // 5. Account Security Actions
              _buildSecuritySection(context),
              NetraSpacing.gapH32,
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildHeaderCard(BuildContext context, UserProfile profile) {
    final initials =
        profile.fullName.isNotEmpty ? profile.fullName[0].toUpperCase() : 'U';

    return Card(
      elevation: context.isMobile ? 0 : 2,
      color: NetraColors.surfaceWhite,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(NetraSpacing.radiusLg),
        side: BorderSide(
          color: context.isMobile ? Colors.transparent : NetraColors.borderGray,
        ),
      ),
      child: Padding(
        padding: const EdgeInsets.all(NetraSpacing.xl),
        child: Row(
          children: [
            CircleAvatar(
              radius: 32,
              backgroundColor: NetraColors.backgroundRed,
              child: Text(
                initials,
                style: NetraTypography.headlineMedium.copyWith(
                  color: NetraColors.primaryRed,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
            NetraSpacing.gapW16,
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(profile.fullName, style: NetraTypography.titleLarge),
                  NetraSpacing.gapH4,
                  Text(profile.email,
                      style: NetraTypography.bodyMedium
                          .copyWith(color: NetraColors.textSecondary)),
                  NetraSpacing.gapH8,
                  Wrap(
                    spacing: 8,
                    runSpacing: 4,
                    children: [
                      ...profile.roles.map((r) => NetraChip(
                            label: r.replaceFirst('ROLE_', ''),
                            type: NetraChipType.outline,
                          )),
                      NetraChip(
                        label: profile.status,
                        type: profile.status == 'ACTIVE'
                            ? NetraChipType.statusSuccess
                            : NetraChipType.neutral,
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildPersonalInfoCard(BuildContext context, UserProfile profile) {
    return Card(
      elevation: 0,
      color: NetraColors.surfaceWhite,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(NetraSpacing.radiusLg),
        side: const BorderSide(color: NetraColors.borderGray),
      ),
      child: Padding(
        padding: const EdgeInsets.all(NetraSpacing.xl),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text("Personal Details", style: NetraTypography.titleMedium),
                TextButton.icon(
                  onPressed: () async {
                    await Navigator.of(context).push(
                      MaterialPageRoute(
                        builder: (_) => EditProfileScreen(
                          currentProfile: profile,
                          controller: _profileController,
                        ),
                      ),
                    );
                    _loadData();
                  },
                  icon: const Icon(Icons.edit_outlined, size: 16),
                  label: const Text("Edit"),
                ),
              ],
            ),
            const Divider(height: 16),
            _buildDetailRow("Full Name", profile.fullName),
            _buildDetailRow("Email", profile.email, note: "Read-only"),
            _buildDetailRow("Phone", profile.phone ?? "Not provided"),
          ],
        ),
      ),
    );
  }

  Widget _buildDonorCard(BuildContext context, DonorProfile? donorProfile) {
    return Card(
      elevation: 0,
      color: NetraColors.surfaceWhite,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(NetraSpacing.radiusLg),
        side: const BorderSide(color: NetraColors.borderGray),
      ),
      child: Padding(
        padding: const EdgeInsets.all(NetraSpacing.xl),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Row(
                  children: [
                    const Icon(Icons.volunteer_activism_outlined,
                        color: NetraColors.primaryRed, size: 20),
                    NetraSpacing.gapW8,
                    Text("Donor Profile", style: NetraTypography.titleMedium),
                  ],
                ),
                TextButton.icon(
                  onPressed: () async {
                    if (donorProfile != null) {
                      await Navigator.of(context).push(
                        MaterialPageRoute(
                          builder: (_) => DonorProfileScreen(
                            donorProfile: donorProfile,
                            controller: _donorController,
                          ),
                        ),
                      );
                    } else {
                      await Navigator.of(context).push(
                        MaterialPageRoute(
                          builder: (_) => EditDonorProfileScreen(
                            currentProfile: null,
                            controller: _donorController,
                          ),
                        ),
                      );
                    }
                    _loadData();
                  },
                  icon: Icon(
                      donorProfile != null
                          ? Icons.chevron_right_rounded
                          : Icons.add_rounded,
                      size: 18),
                  label: Text(donorProfile != null ? "View" : "Setup"),
                ),
              ],
            ),
            const Divider(height: 16),
            if (donorProfile != null) ...[
              Row(
                children: [
                  Container(
                    padding:
                        const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                    decoration: BoxDecoration(
                      color: NetraColors.backgroundRed,
                      borderRadius:
                          BorderRadius.circular(NetraSpacing.radiusSm),
                    ),
                    child: Text(
                      donorProfile.bloodGroup,
                      style: NetraTypography.titleMedium.copyWith(
                        color: NetraColors.primaryRed,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                  NetraSpacing.gapW12,
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          donorProfile.isVerified
                              ? "Clinically Verified"
                              : "Self-reported",
                          style: NetraTypography.titleSmall,
                        ),
                        NetraSpacing.gapH2,
                        Text(
                          "Availability: ${donorProfile.availabilityStatus}",
                          style: NetraTypography.bodySmall
                              .copyWith(color: NetraColors.textSecondary),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ] else ...[
              Text(
                "You have not set up a blood donor profile yet. Configure your blood group to help when urgent needs arise.",
                style: NetraTypography.bodySmall
                    .copyWith(color: NetraColors.textSecondary),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildPrivacyCard(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(NetraSpacing.md),
      decoration: BoxDecoration(
        color: NetraColors.backgroundGray,
        borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
        border: Border.all(color: NetraColors.borderGray),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Icon(Icons.privacy_tip_outlined,
              color: NetraColors.textSecondary, size: 20),
          NetraSpacing.gapW12,
          Expanded(
            child: Text(
              "Privacy Guarantee: Your pre-donation screening answers and clinical eligibility records are kept strictly separated from your user identity and are never exposed publicly.",
              style: NetraTypography.bodySmall
                  .copyWith(color: NetraColors.textSecondary),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSecuritySection(BuildContext context) {
    final authController = AuthScope.maybeOf(context);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text("Account Security", style: NetraTypography.titleLarge),
        NetraSpacing.gapH12,
        NetraButton.outlined(
          text: "Log Out",
          icon: Icons.logout_rounded,
          onPressed: () async {
            await authController?.logout();
            if (context.mounted) {
              Navigator.of(context).pushAndRemoveUntil(
                MaterialPageRoute(builder: (_) => const LoginScreen()),
                (route) => false,
              );
            }
          },
        ),
        NetraSpacing.gapH8,
        Center(
          child: TextButton.icon(
            onPressed: () async {
              final confirm = await showDialog<bool>(
                context: context,
                builder: (ctx) => AlertDialog(
                  title: const Text("Log Out Everywhere?"),
                  content: const Text(
                    "This will revoke all active refresh tokens and terminate all sessions across all your devices.",
                  ),
                  actions: [
                    TextButton(
                      onPressed: () => Navigator.of(ctx).pop(false),
                      child: const Text("Cancel"),
                    ),
                    TextButton(
                      onPressed: () => Navigator.of(ctx).pop(true),
                      child: const Text("Log Out Everywhere",
                          style: TextStyle(color: NetraColors.errorRed)),
                    ),
                  ],
                ),
              );

              if (confirm == true && context.mounted) {
                await authController?.logoutAll();
                if (context.mounted) {
                  Navigator.of(context).pushAndRemoveUntil(
                    MaterialPageRoute(builder: (_) => const LoginScreen()),
                    (route) => false,
                  );
                }
              }
            },
            icon: const Icon(Icons.security_rounded,
                size: 16, color: NetraColors.textSecondary),
            label: Text(
              "Log out from all devices",
              style: NetraTypography.bodySmall
                  .copyWith(color: NetraColors.textSecondary),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildDetailRow(String label, String value, {String? note}) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 110,
            child: Text(label,
                style: NetraTypography.bodySmall
                    .copyWith(color: NetraColors.textSecondary)),
          ),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(value,
                    style: NetraTypography.bodyMedium
                        .copyWith(fontWeight: FontWeight.w500)),
                if (note != null) ...[
                  NetraSpacing.gapH2,
                  Text(note,
                      style: NetraTypography.labelSmall
                          .copyWith(color: NetraColors.textSecondary)),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }
}
