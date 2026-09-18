import 'package:flutter/material.dart';
import '../../../common/widgets/common_widgets.dart';
import '../../../core/responsive/responsive.dart';
import '../../../core/theme/netra_colors.dart';
import '../../../core/theme/netra_spacing.dart';
import '../../../core/theme/netra_typography.dart';
import 'emergency_create_request_screen.dart';
import 'emergency_nearby_screen.dart';

class EmergencyModeScreen extends StatelessWidget {
  const EmergencyModeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return ResponsiveScaffold(
      appBar: NetraAppBar(
        title: "Emergency Mode",
        showBackButton: true,
      ),
      body: ResponsiveContainer.standard(
        scrollable: true,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // High-contrast Urgent Assistance Header Banner
            Container(
              padding: NetraSpacing.cardPaddingSpacious,
              decoration: BoxDecoration(
                gradient: const LinearGradient(
                  colors: [Color(0xFFDC2626), Color(0xFF991B1B)],
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
                borderRadius: BorderRadius.circular(NetraSpacing.radiusLg),
                boxShadow: [
                  BoxShadow(
                    color: const Color(0xFFDC2626).withOpacity(0.3),
                    blurRadius: 16,
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
                        padding: const EdgeInsets.all(10),
                        decoration: BoxDecoration(
                          color: Colors.white.withOpacity(0.2),
                          shape: BoxShape.circle,
                        ),
                        child: const Icon(
                          Icons.emergency_rounded,
                          color: NetraColors.surfaceWhite,
                          size: 32,
                        ),
                      ),
                      NetraSpacing.gapW16,
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              "Emergency Blood Support",
                              style: NetraTypography.headlineSmall.copyWith(
                                color: NetraColors.surfaceWhite,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                            NetraSpacing.gapH4,
                            Text(
                              "Priority visibility across emergency & discovery workflows",
                              style: NetraTypography.bodySmall.copyWith(
                                color: NetraColors.surfaceWhite.withOpacity(0.9),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                  NetraSpacing.gapH16,
                  Text(
                    "Use Emergency Mode when every minute counts. This creates a CRITICAL blood request that becomes available through NETRA's supported emergency and discovery workflows.",
                    style: NetraTypography.bodyMedium.copyWith(
                      color: NetraColors.surfaceWhite.withOpacity(0.95),
                    ),
                  ),
                ],
              ),
            ),
            NetraSpacing.gapH24,

            // Section: Immediate Actions
            Text(
              "Immediate Actions",
              style: NetraTypography.titleLarge.copyWith(fontWeight: FontWeight.bold),
            ),
            NetraSpacing.gapH12,

            // Action 1: I Need Blood Immediately
            _buildActionCard(
              context,
              icon: Icons.emergency_share_rounded,
              iconBgColor: const Color(0xFFFEE2E2),
              iconColor: const Color(0xFFDC2626),
              title: "I Need Blood Immediately",
              description: "Create a CRITICAL blood request with highest priority across NETRA's emergency discovery workflows.",
              buttonLabel: "Create Emergency Request",
              buttonColor: const Color(0xFFDC2626),
              onTap: () {
                Navigator.of(context).push(
                  MaterialPageRoute(
                    builder: (context) => const EmergencyCreateRequestScreen(),
                  ),
                );
              },
            ),
            NetraSpacing.gapH16,

            // Action 2: Find Nearby Blood Banks
            _buildActionCard(
              context,
              icon: Icons.local_hospital_rounded,
              iconBgColor: const Color(0xFFEFF6FF),
              iconColor: const Color(0xFF2563EB),
              title: "Find Nearby Blood Centres",
              description: "Locate authorized blood banks, view operating hours, and verify stock directly.",
              buttonLabel: "Locate Blood Centres",
              buttonColor: const Color(0xFF2563EB),
              onTap: () {
                Navigator.of(context).push(
                  MaterialPageRoute(
                    builder: (context) => const EmergencyNearbyScreen(initialTabIndex: 0),
                  ),
                );
              },
            ),
            NetraSpacing.gapH16,

            // Action 3: View Nearby Active Requests
            _buildActionCard(
              context,
              icon: Icons.bloodtype_outlined,
              iconBgColor: const Color(0xFFFEF3C7),
              iconColor: const Color(0xFFD97706),
              title: "View Active Requests",
              description: "See nearby open and critical blood requests from patients in urgent need.",
              buttonLabel: "View Nearby Requests",
              buttonColor: const Color(0xFFD97706),
              onTap: () {
                Navigator.of(context).push(
                  MaterialPageRoute(
                    builder: (context) => const EmergencyNearbyScreen(initialTabIndex: 1),
                  ),
                );
              },
            ),
            NetraSpacing.gapH24,

            // Privacy & Patient Safety Notice
            NetraCard.outlined(
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Icon(Icons.shield_outlined, color: NetraColors.primaryRed, size: 24),
                  NetraSpacing.gapW12,
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          "Privacy & Medical Coordination",
                          style: NetraTypography.titleSmall.copyWith(fontWeight: FontWeight.bold),
                        ),
                        NetraSpacing.gapH4,
                        Text(
                          "NETRA does not expose donor personal contact information. Available contact and fulfillment actions are handled through the supported NETRA workflow and verified blood-bank processes.",
                          style: NetraTypography.bodySmall.copyWith(color: NetraColors.textSecondary),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            NetraSpacing.gapH24,
          ],
        ),
      ),
    );
  }

  Widget _buildActionCard(
    BuildContext context, {
    required IconData icon,
    required Color iconBgColor,
    required Color iconColor,
    required String title,
    required String description,
    required String buttonLabel,
    required Color buttonColor,
    required VoidCallback onTap,
  }) {
    return Container(
      decoration: BoxDecoration(
        color: NetraColors.surfaceWhite,
        borderRadius: BorderRadius.circular(NetraSpacing.radiusLg),
        border: Border.all(color: NetraColors.borderSubtle),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.04),
            blurRadius: 8,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      padding: NetraSpacing.cardPaddingStandard,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: iconBgColor,
                  borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
                ),
                child: Icon(icon, color: iconColor, size: 24),
              ),
              NetraSpacing.gapW12,
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: NetraTypography.titleMedium.copyWith(fontWeight: FontWeight.bold),
                    ),
                    NetraSpacing.gapH4,
                    Text(
                      description,
                      style: NetraTypography.bodySmall.copyWith(color: NetraColors.textSecondary),
                    ),
                  ],
                ),
              ),
            ],
          ),
          NetraSpacing.gapH16,
          SizedBox(
            width: double.infinity,
            height: 44,
            child: ElevatedButton(
              style: ElevatedButton.styleFrom(
                backgroundColor: buttonColor,
                foregroundColor: NetraColors.surfaceWhite,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
                ),
                elevation: 0,
              ),
              onPressed: onTap,
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Text(
                    buttonLabel,
                    style: NetraTypography.labelLarge.copyWith(color: NetraColors.surfaceWhite),
                  ),
                  NetraSpacing.gapW8,
                  const Icon(Icons.arrow_forward_rounded, size: 16),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
