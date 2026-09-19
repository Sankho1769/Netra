import 'package:flutter/material.dart';
import '../../../core/responsive/responsive.dart';
import '../../../core/theme/netra_colors.dart';
import '../../../core/theme/netra_spacing.dart';
import '../../../core/theme/netra_typography.dart';
import '../../../common/widgets/common_widgets.dart';
import '../../eligibility/screens/eligibility_intro_screen.dart';
import '../../donor_response/screens/donor_incoming_matches_screen.dart';
import '../models/donor_profile.dart';
import '../state/donor_controller.dart';
import 'edit_donor_profile_screen.dart';

class DonorProfileScreen extends StatelessWidget {
  final DonorProfile? donorProfile;
  final DonorController controller;

  const DonorProfileScreen({
    super.key,
    required this.donorProfile,
    required this.controller,
  });

  @override
  Widget build(BuildContext context) {
    final profile = donorProfile;

    return Scaffold(
      backgroundColor: NetraColors.backgroundGray,
      appBar: const NetraAppBar(title: "Donor Profile"),
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: context.screenGutter,
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 600),
              child: profile == null
                  ? _buildEmptyState(context)
                  : _buildProfileContent(context, profile),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildEmptyState(BuildContext context) {
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
        padding: const EdgeInsets.symmetric(
          horizontal: NetraSpacing.xl,
          vertical: NetraSpacing.xxl,
        ),
        child: Column(
          children: [
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: NetraColors.backgroundRed,
                shape: BoxShape.circle,
              ),
              child: const Icon(Icons.volunteer_activism_rounded, color: NetraColors.primaryRed, size: 40),
            ),
            NetraSpacing.gapH20,
            Text("No Donor Profile Found", style: NetraTypography.headlineSmall),
            NetraSpacing.gapH8,
            Text(
              "Set up your blood group and donation availability to join the lifesaving community.",
              textAlign: TextAlign.center,
              style: NetraTypography.bodyMedium.copyWith(color: NetraColors.textSecondary),
            ),
            NetraSpacing.gapH24,
            NetraButton(
              text: "Setup Donor Profile",
              onPressed: () {
                Navigator.of(context).push(
                  MaterialPageRoute(
                    builder: (_) => EditDonorProfileScreen(
                      currentProfile: null,
                      controller: controller,
                    ),
                  ),
                );
              },
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildProfileContent(BuildContext context, DonorProfile profile) {
    final isVerified = profile.isVerified;
    final isAvailable = profile.isAvailable;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        // Top Summary Card
        Card(
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
                // Blood Group Badge
                Container(
                  width: 72,
                  height: 72,
                  decoration: BoxDecoration(
                    color: NetraColors.backgroundRed,
                    borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
                    border: BorderSide(color: NetraColors.primaryRed.withOpacity(0.3), width: 1.5),
                  ),
                  alignment: Alignment.center,
                  child: Text(
                    profile.bloodGroup,
                    style: NetraTypography.headlineMedium.copyWith(
                      color: NetraColors.primaryRed,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
                NetraSpacing.gapW20,
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text("Blood Donor", style: NetraTypography.titleLarge),
                      NetraSpacing.gapH6,
                      Wrap(
                        spacing: 8,
                        runSpacing: 4,
                        children: [
                          NetraChip(
                            label: isVerified ? "Clinically Verified" : "Self-reported",
                            type: isVerified ? NetraChipType.statusSuccess : NetraChipType.outline,
                          ),
                          NetraChip(
                            label: profile.availabilityStatus,
                            type: isAvailable ? NetraChipType.statusSuccess : NetraChipType.neutral,
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
        NetraSpacing.gapH16,

        // Clinical Notice
        Container(
          padding: const EdgeInsets.all(NetraSpacing.md),
          decoration: BoxDecoration(
            color: NetraColors.backgroundGray,
            borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
            border: const BorderSide(color: NetraColors.borderGray),
          ),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Icon(Icons.shield_outlined, color: NetraColors.textSecondary, size: 20),
              NetraSpacing.gapW12,
              Expanded(
                child: Text(
                  "Blood group verification is completed by authorized clinical staff during donation screening. Self-reported records are clearly flagged for clinical safety.",
                  style: NetraTypography.bodySmall.copyWith(color: NetraColors.textSecondary),
                ),
              ),
            ],
          ),
        ),
        NetraSpacing.gapH20,

        // Eligibility Pre-Screening Section
        Card(
          elevation: 0,
          color: NetraColors.surfaceWhite,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(NetraSpacing.radiusLg),
            side: const BorderSide(color: NetraColors.borderGray),
          ),
          child: Padding(
            padding: const EdgeInsets.all(NetraSpacing.lg),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    const Icon(Icons.fact_check_outlined, color: NetraColors.primaryRed, size: 20),
                    NetraSpacing.gapW12,
                    Text("Pre-Donation Screening", style: NetraTypography.titleMedium),
                  ],
                ),
                NetraSpacing.gapH8,
                Text(
                  "Eligibility self-check is a confidential pre-screening tool based on national standards. Results are temporary and do not constitute permanent clinical clearance.",
                  style: NetraTypography.bodySmall.copyWith(color: NetraColors.textSecondary),
                ),
                NetraSpacing.gapH16,
                OutlinedButton.icon(
                  onPressed: () {
                    Navigator.of(context).push(
                      MaterialPageRoute(builder: (_) => const EligibilityIntroScreen()),
                    );
                  },
                  icon: const Icon(Icons.play_arrow_rounded, size: 18),
                  label: const Text("Take Eligibility Self-Check"),
                  style: OutlinedButton.styleFrom(
                    foregroundColor: NetraColors.primaryRed,
                    side: const BorderSide(color: NetraColors.primaryRed),
                  ),
                ),
              ],
            ),
          ),
        ),
        NetraSpacing.gapH16,

        // Incoming Match Requests Section
        Card(
          elevation: 0,
          color: NetraColors.surfaceWhite,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(NetraSpacing.radiusLg),
            side: const BorderSide(color: NetraColors.borderGray),
          ),
          child: Padding(
            padding: const EdgeInsets.all(NetraSpacing.lg),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    const Icon(Icons.mark_email_unread_outlined, color: NetraColors.primaryRed, size: 20),
                    NetraSpacing.gapW12,
                    Text("Blood Request Matches", style: NetraTypography.titleMedium),
                  ],
                ),
                NetraSpacing.gapH8,
                Text(
                  "Review blood requests matching your profile and respond with acceptance or decline.",
                  style: NetraTypography.bodySmall.copyWith(color: NetraColors.textSecondary),
                ),
                NetraSpacing.gapH16,
                OutlinedButton.icon(
                  onPressed: () {
                    Navigator.of(context).push(
                      MaterialPageRoute(builder: (_) => const DonorIncomingMatchesScreen()),
                    );
                  },
                  icon: const Icon(Icons.assignment_turned_in_outlined, size: 18),
                  label: const Text("View Matched Requests"),
                  style: OutlinedButton.styleFrom(
                    foregroundColor: NetraColors.primaryRed,
                    side: const BorderSide(color: NetraColors.primaryRed),
                  ),
                ),
              ],
            ),
          ),
        ),
        NetraSpacing.gapH16,
        Card(
          elevation: 0,
          color: NetraColors.surfaceWhite,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(NetraSpacing.radiusLg),
            side: const BorderSide(color: NetraColors.borderGray),
          ),
          child: Padding(
            padding: const EdgeInsets.all(NetraSpacing.lg),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    const Icon(Icons.history_edu_outlined, color: NetraColors.textSecondary, size: 20),
                    NetraSpacing.gapW12,
                    Text("Last Verified Donation", style: NetraTypography.titleMedium),
                  ],
                ),
                NetraSpacing.gapH8,
                Text(
                  profile.lastDonationDate != null
                      ? "${profile.lastDonationDate!.year}-${profile.lastDonationDate!.month.toString().padLeft(2, '0')}-${profile.lastDonationDate!.day.toString().padLeft(2, '0')}"
                      : "Not available (No verified donations recorded yet)",
                  style: NetraTypography.bodyMedium.copyWith(
                    fontWeight: FontWeight.w600,
                    color: profile.lastDonationDate != null ? NetraColors.textPrimary : NetraColors.textSecondary,
                  ),
                ),
                NetraSpacing.gapH4,
                Text(
                  "Verified donation dates are recorded directly by blood bank staff upon donation completion.",
                  style: NetraTypography.bodySmall.copyWith(color: NetraColors.textSecondary),
                ),
              ],
            ),
          ),
        ),
        NetraSpacing.gapH24,

        // Edit Action Button
        NetraButton(
          text: "Edit Donor Preferences",
          icon: Icons.edit_outlined,
          onPressed: () {
            Navigator.of(context).push(
              MaterialPageRoute(
                builder: (_) => EditDonorProfileScreen(
                  currentProfile: profile,
                  controller: controller,
                ),
              ),
            );
          },
        ),
      ],
    );
  }
}
