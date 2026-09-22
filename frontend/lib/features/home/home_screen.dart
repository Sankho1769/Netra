import 'package:flutter/material.dart';
import '../../core/responsive/responsive.dart';
import '../../core/theme/netra_colors.dart';
import '../../core/theme/netra_spacing.dart';
import '../../core/theme/netra_typography.dart';
import '../../common/widgets/common_widgets.dart';
import '../eligibility/screens/eligibility_intro_screen.dart';
import '../bloodbank/screens/nearby_blood_banks_screen.dart';
import '../events/screens/donation_event_list_screen.dart';
import '../blood_request/screens/blood_request_list_screen.dart';
import '../emergency/screens/emergency_mode_screen.dart';
import '../auth/state/auth_scope.dart';
import '../auth/screens/login_screen.dart';
import '../profile/screens/profile_screen.dart';
import '../notification/widgets/notification_bell_icon.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _selectedTab = 0;

  @override
  Widget build(BuildContext context) {
    return ResponsiveScaffold(
      selectedIndex: _selectedTab,
      onDestinationSelected: (idx) => setState(() => _selectedTab = idx),
      destinations: const [
        ResponsiveNavigationDestination(
          icon: Icons.home_outlined,
          selectedIcon: Icons.home_rounded,
          label: "Home",
          tooltip: "Home Dashboard",
        ),
        ResponsiveNavigationDestination(
          icon: Icons.person_outline_rounded,
          selectedIcon: Icons.person_rounded,
          label: "Profile",
          tooltip: "Donor Profile",
        ),
      ],
      appBar: NetraAppBar(
        title: "NETRA",
        leading: Padding(
          padding: const EdgeInsets.only(left: NetraSpacing.md),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Container(
                padding: const EdgeInsets.all(6),
                decoration: BoxDecoration(
                  color: NetraColors.primaryRed,
                  borderRadius: BorderRadius.circular(NetraSpacing.radiusSm),
                ),
                child: const Icon(Icons.water_drop_rounded,
                    color: NetraColors.surfaceWhite, size: 18),
              ),
            ],
          ),
        ),
        actions: const [
          NotificationBellIcon(),
        ],
        showBackButton: false,
      ),
      body: _selectedTab == 0
          ? _buildHomeTab(context)
          : _buildProfileTab(context),
    );
  }

  Widget _buildHomeTab(BuildContext context) {
    final authController = AuthScope.maybeOf(context);
    final user = authController?.currentUser;
    final displayName = user != null ? user.fullName.split(' ').first : "Donor";

    return ResponsiveContainer.standard(
      scrollable: true,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Greeting & Status
          Text(
            "Welcome back, $displayName",
            style: NetraTypography.headlineMedium,
          ),
          NetraSpacing.gapH4,
          Text(
            "Every drop saves a life. Check your eligibility before donating.",
            style: NetraTypography.bodyMedium,
          ),
          NetraSpacing.gapH20,

          // Primary Feature Banner Card: Donate Blood -> Check Eligibility
          Container(
            padding: NetraSpacing.cardPaddingSpacious,
            decoration: BoxDecoration(
              gradient: const LinearGradient(
                colors: [NetraColors.primaryRed, NetraColors.darkRed],
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
              ),
              borderRadius: BorderRadius.circular(NetraSpacing.radiusLg),
              boxShadow: [
                BoxShadow(
                  color: NetraColors.primaryRed.withOpacity(0.25),
                  blurRadius: 12,
                  offset: const Offset(0, 4),
                ),
              ],
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Container(
                      padding: NetraSpacing.paddingSm,
                      decoration: BoxDecoration(
                        color: Colors.white.withOpacity(0.2),
                        borderRadius:
                            BorderRadius.circular(NetraSpacing.radiusMd),
                      ),
                      child: const Icon(Icons.volunteer_activism_rounded,
                          color: Colors.white, size: 24),
                    ),
                    NetraSpacing.gapW12,
                    Text(
                      "Donate Blood",
                      style: NetraTypography.headlineSmall
                          .copyWith(color: NetraColors.surfaceWhite),
                    ),
                  ],
                ),
                NetraSpacing.gapH12,
                Text(
                  "Thinking of donating? Take a confidential 3-minute self-check to see if you meet general donor criteria.",
                  style: NetraTypography.bodyMedium
                      .copyWith(color: NetraColors.surfaceWhite),
                ),
                NetraSpacing.gapH20,
                ElevatedButton(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: NetraColors.surfaceWhite,
                    foregroundColor: NetraColors.primaryRed,
                    minimumSize: const Size(double.infinity, 48),
                    shape: RoundedRectangleBorder(
                      borderRadius:
                          BorderRadius.circular(NetraSpacing.radiusMd),
                    ),
                  ),
                  onPressed: () {
                    Navigator.of(context).push(
                      MaterialPageRoute(
                          builder: (context) => const EligibilityIntroScreen()),
                    );
                  },
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(
                        "Check Eligibility",
                        style: NetraTypography.labelLarge
                            .copyWith(color: NetraColors.primaryRed),
                      ),
                      NetraSpacing.gapW8,
                      const Icon(Icons.arrow_forward_rounded, size: 16),
                    ],
                  ),
                ),
              ],
            ),
          ),
          NetraSpacing.gapH16,

          // Emergency Mode Urgent Action Card
          InkWell(
            onTap: () {
              Navigator.of(context).push(
                MaterialPageRoute(
                    builder: (context) => const EmergencyModeScreen()),
              );
            },
            borderRadius: BorderRadius.circular(NetraSpacing.radiusLg),
            child: Container(
              padding: NetraSpacing.cardPaddingStandard,
              decoration: BoxDecoration(
                color: const Color(0xFFFEF2F2),
                borderRadius: BorderRadius.circular(NetraSpacing.radiusLg),
                border: Border.all(color: const Color(0xFFFCA5A5), width: 1.5),
                boxShadow: [
                  BoxShadow(
                    color: const Color(0xFFDC2626).withOpacity(0.08),
                    blurRadius: 8,
                    offset: const Offset(0, 2),
                  ),
                ],
              ),
              child: Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: const Color(0xFFDC2626),
                      borderRadius:
                          BorderRadius.circular(NetraSpacing.radiusMd),
                    ),
                    child: const Icon(
                      Icons.emergency_outlined,
                      color: NetraColors.surfaceWhite,
                      size: 26,
                    ),
                  ),
                  NetraSpacing.gapW16,
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            Text(
                              "Emergency Mode",
                              style: NetraTypography.titleMedium.copyWith(
                                color: const Color(0xFF991B1B),
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                            NetraSpacing.gapW8,
                            Container(
                              padding: const EdgeInsets.symmetric(
                                  horizontal: 6, vertical: 2),
                              decoration: BoxDecoration(
                                color: const Color(0xFFDC2626),
                                borderRadius: BorderRadius.circular(
                                    NetraSpacing.radiusSm),
                              ),
                              child: Text(
                                "CRITICAL",
                                style: NetraTypography.labelSmall.copyWith(
                                  color: NetraColors.surfaceWhite,
                                  fontWeight: FontWeight.bold,
                                  fontSize: 10,
                                ),
                              ),
                            ),
                          ],
                        ),
                        NetraSpacing.gapH4,
                        Text(
                          "Immediate blood assistance & rapid nearby centre lookup",
                          style: NetraTypography.bodySmall.copyWith(
                            color: const Color(0xFF7F1D1D),
                          ),
                        ),
                      ],
                    ),
                  ),
                  const Icon(Icons.arrow_forward_ios_rounded,
                      color: Color(0xFFDC2626), size: 16),
                ],
              ),
            ),
          ),
          NetraSpacing.gapH24,

          // Quick Actions
          Text(
            "Quick Actions",
            style: NetraTypography.titleLarge,
          ),
          NetraSpacing.gapH12,
          Row(
            children: [
              Expanded(
                child: _buildQuickActionCard(
                  icon: Icons.event_available_rounded,
                  title: "Donation Camps",
                  subtitle: "Find & register",
                  bgColor: NetraColors.backgroundRed,
                  iconColor: NetraColors.primaryRed,
                  onTap: () {
                    Navigator.of(context).push(
                      MaterialPageRoute(
                          builder: (context) =>
                              const DonationEventListScreen()),
                    );
                  },
                ),
              ),
              NetraSpacing.gapW12,
              Expanded(
                child: _buildQuickActionCard(
                  icon: Icons.bloodtype_outlined,
                  title: "Blood Requests",
                  subtitle: "Find & request blood",
                  bgColor: const Color(0xFFFEF2F2),
                  iconColor: const Color(0xFFDC2626),
                  onTap: () {
                    Navigator.of(context).push(
                      MaterialPageRoute(
                          builder: (context) => const BloodRequestListScreen()),
                    );
                  },
                ),
              ),
            ],
          ),
          NetraSpacing.gapH12,
          Row(
            children: [
              Expanded(
                child: _buildQuickActionCard(
                  icon: Icons.location_on_outlined,
                  title: "Blood Centres",
                  subtitle: "Authorized centres",
                  bgColor: NetraColors.backgroundGray,
                  iconColor: NetraColors.textPrimary,
                  onTap: () {
                    Navigator.of(context).push(
                      MaterialPageRoute(
                          builder: (context) => const NearbyBloodBanksScreen()),
                    );
                  },
                ),
              ),
            ],
          ),
          NetraSpacing.gapH24,
        ],
      ),
    );
  }

  Widget _buildProfileTab(BuildContext context) {
    final authController = AuthScope.maybeOf(context);
    final user = authController?.currentUser;

    return ResponsiveContainer.standard(
      scrollable: true,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            "Account & Profile",
            style: NetraTypography.headlineMedium,
          ),
          NetraSpacing.gapH16,

          // Profile Summary Card
          if (user != null) ...[
            NetraCard.outlined(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      CircleAvatar(
                        radius: 28,
                        backgroundColor: NetraColors.backgroundRed,
                        child: Text(
                          user.fullName.isNotEmpty
                              ? user.fullName[0].toUpperCase()
                              : 'U',
                          style: NetraTypography.headlineSmall.copyWith(
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
                            Text(user.fullName,
                                style: NetraTypography.titleMedium),
                            NetraSpacing.gapH4,
                            Text(user.email, style: NetraTypography.bodySmall),
                            if (user.phone != null &&
                                user.phone!.isNotEmpty) ...[
                              NetraSpacing.gapH2,
                              Text(user.phone!,
                                  style: NetraTypography.bodySmall.copyWith(
                                      color: NetraColors.textSecondary)),
                            ],
                          ],
                        ),
                      ),
                    ],
                  ),
                  NetraSpacing.gapH16,
                  Wrap(
                    spacing: 8,
                    runSpacing: 8,
                    children: [
                      ...user.roles.map((r) => NetraChip(
                            label: r.replaceFirst('ROLE_', ''),
                            type: NetraChipType.outline,
                          )),
                      NetraChip(
                        label: user.status,
                        type: user.status == 'ACTIVE'
                            ? NetraChipType.statusSuccess
                            : NetraChipType.neutral,
                      ),
                    ],
                  ),
                  NetraSpacing.gapH16,
                  NetraButton.outlined(
                    text: "View Full Profile & Donor Settings",
                    icon: Icons.manage_accounts_outlined,
                    onPressed: () {
                      Navigator.of(context).push(
                        MaterialPageRoute(
                            builder: (_) => const ProfileScreen()),
                      );
                    },
                  ),
                ],
              ),
            ),
          ] else ...[
            NetraCard.outlined(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      const CircleAvatar(
                        radius: 24,
                        backgroundColor: NetraColors.backgroundGray,
                        child: Icon(Icons.person_outline_rounded,
                            color: NetraColors.textSecondary),
                      ),
                      NetraSpacing.gapW16,
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text("Guest Mode",
                                style: NetraTypography.titleMedium),
                            NetraSpacing.gapH4,
                            Text(
                              "Sign in to track donations and access your profile.",
                              style: NetraTypography.bodySmall
                                  .copyWith(color: NetraColors.textSecondary),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                  NetraSpacing.gapH16,
                  NetraButton(
                    text: "Sign In / Register",
                    icon: Icons.login_rounded,
                    onPressed: () {
                      Navigator.of(context).push(
                        MaterialPageRoute(builder: (_) => const LoginScreen()),
                      );
                    },
                  ),
                ],
              ),
            ),
          ],
          NetraSpacing.gapH24,

          Text(
            "Donation Services",
            style: NetraTypography.titleLarge,
          ),
          NetraSpacing.gapH12,

          // Entry Point: Profile -> Eligibility Check
          NetraCard.outlined(
            padding: EdgeInsets.zero,
            child: ListTile(
              leading: Container(
                padding: NetraSpacing.paddingSm,
                decoration: BoxDecoration(
                  color: NetraColors.backgroundRed,
                  borderRadius: BorderRadius.circular(NetraSpacing.radiusSm),
                ),
                child: const Icon(Icons.fact_check_outlined,
                    color: NetraColors.primaryRed, size: 20),
              ),
              title:
                  Text("Eligibility Check", style: NetraTypography.titleSmall),
              subtitle: Text("Verify pre-donation suitability",
                  style: NetraTypography.bodySmall),
              trailing: const Icon(Icons.chevron_right_rounded,
                  color: NetraColors.textSecondary),
              onTap: () {
                Navigator.of(context).push(
                  MaterialPageRoute(
                      builder: (context) => const EligibilityIntroScreen()),
                );
              },
            ),
          ),
          NetraSpacing.gapH12,
          NetraCard.outlined(
            padding: EdgeInsets.zero,
            child: ListTile(
              leading: Container(
                padding: NetraSpacing.paddingSm,
                decoration: BoxDecoration(
                  color: NetraColors.backgroundGray,
                  borderRadius: BorderRadius.circular(NetraSpacing.radiusSm),
                ),
                child: const Icon(Icons.history_rounded,
                    color: NetraColors.textSecondary, size: 20),
              ),
              title:
                  Text("Donation History", style: NetraTypography.titleSmall),
              subtitle: Text("Past contributions and certificates",
                  style: NetraTypography.bodySmall),
              trailing: const Icon(Icons.chevron_right_rounded,
                  color: NetraColors.textSecondary),
              onTap: () {},
            ),
          ),

          if (user != null) ...[
            NetraSpacing.gapH24,
            Text(
              "Account Security",
              style: NetraTypography.titleLarge,
            ),
            NetraSpacing.gapH12,
            NetraButton.outlined(
              text: "Log Out",
              icon: Icons.logout_rounded,
              onPressed: () async {
                await authController?.logout();
                if (context.mounted) {
                  Navigator.of(context).pushReplacement(
                    MaterialPageRoute(builder: (_) => const LoginScreen()),
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
                      Navigator.of(context).pushReplacement(
                        MaterialPageRoute(builder: (_) => const LoginScreen()),
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
          NetraSpacing.gapH24,
        ],
      ),
    );
  }

  Widget _buildQuickActionCard({
    required IconData icon,
    required String title,
    required String subtitle,
    required Color bgColor,
    required Color iconColor,
    required VoidCallback onTap,
  }) {
    return NetraCard.outlined(
      onTap: onTap,
      padding: NetraSpacing.cardPadding,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            padding: NetraSpacing.paddingSm,
            decoration: BoxDecoration(
                color: bgColor,
                borderRadius: BorderRadius.circular(NetraSpacing.radiusSm)),
            child: Icon(icon, color: iconColor, size: 20),
          ),
          NetraSpacing.gapH12,
          Text(title, style: NetraTypography.titleSmall),
          NetraSpacing.gapH4,
          Text(subtitle, style: NetraTypography.bodySmall),
        ],
      ),
    );
  }
}
