import 'package:flutter/material.dart';
import '../../core/theme/netra_colors.dart';
import '../../core/theme/netra_spacing.dart';
import '../../core/theme/netra_typography.dart';
import '../../features/eligibility/screens/eligibility_intro_screen.dart';
import '../../features/emergency/screens/emergency_mode_screen.dart';
import '../../features/bloodbank/screens/nearby_blood_banks_screen.dart';

class BloodActionSheet extends StatelessWidget {
  const BloodActionSheet({super.key});

  static Future<void> show(BuildContext context) {
    return showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (_) => const BloodActionSheet(),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(20, 12, 20, 28),
      decoration: const BoxDecoration(
        color: NetraColors.surfaceWhite,
        borderRadius: BorderRadius.vertical(top: Radius.circular(28)),
      ),
      child: SafeArea(
        top: false,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            // Drag handle
            Container(
              width: 44,
              height: 4,
              decoration: BoxDecoration(
                color: const Color(0xFFCBD5E1),
                borderRadius: BorderRadius.circular(2),
              ),
            ),
            NetraSpacing.gapH16,

            // Header
            Text(
              "What do you need?",
              style: NetraTypography.headlineMedium.copyWith(
                fontWeight: FontWeight.w800,
              ),
            ),
            NetraSpacing.gapH4,
            Text(
              "Choose an option to get started",
              style: NetraTypography.bodyMedium.copyWith(
                color: NetraColors.textSecondary,
              ),
            ),
            NetraSpacing.gapH20,

            // GIVE BLOOD Card
            _buildActionCard(
              context: context,
              title: "GIVE BLOOD",
              subtitle: "Check eligibility, donate blood & save lives",
              buttonText: "DONATE NOW",
              icon: Icons.volunteer_activism_rounded,
              gradient: const LinearGradient(
                colors: [Color(0xFFFB7185), Color(0xFFDC2626)],
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
              ),
              shadowColor: const Color(0xFFDC2626).withValues(alpha: 0.35),
              onTap: () {
                Navigator.of(context).pop();
                Navigator.of(context).push(
                  MaterialPageRoute(
                    builder: (_) => const EligibilityIntroScreen(),
                  ),
                );
              },
            ),

            // OR divider
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 14),
              child: Row(
                children: [
                  const Expanded(child: Divider(color: Color(0xFFE2E8F0))),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 14),
                    child: Text(
                      "OR",
                      style: TextStyle(
                        fontSize: 11,
                        fontWeight: FontWeight.w800,
                        color: Colors.grey.shade400,
                        letterSpacing: 1.5,
                      ),
                    ),
                  ),
                  const Expanded(child: Divider(color: Color(0xFFE2E8F0))),
                ],
              ),
            ),

            // NEED BLOOD Card
            _buildActionCard(
              context: context,
              title: "NEED BLOOD",
              subtitle: "Urgent emergency request & nearby donor lookup",
              buttonText: "REQUEST BLOOD",
              icon: Icons.emergency_rounded,
              gradient: const LinearGradient(
                colors: [Color(0xFF38BDF8), Color(0xFF4F46E5)],
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
              ),
              shadowColor: const Color(0xFF4F46E5).withValues(alpha: 0.35),
              onTap: () {
                Navigator.of(context).pop();
                Navigator.of(context).push(
                  MaterialPageRoute(
                    builder: (_) => const EmergencyModeScreen(),
                  ),
                );
              },
            ),

            NetraSpacing.gapH16,

            // Nearby Blood Banks Link
            TextButton.icon(
              onPressed: () {
                Navigator.of(context).pop();
                Navigator.of(context).push(
                  MaterialPageRoute(
                    builder: (_) => const NearbyBloodBanksScreen(),
                  ),
                );
              },
              icon: const Icon(Icons.local_hospital_outlined,
                  size: 18, color: NetraColors.textPrimary),
              label: Text(
                "Find Authorized Blood Centres",
                style: NetraTypography.labelMedium.copyWith(
                  color: NetraColors.textPrimary,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildActionCard({
    required BuildContext context,
    required String title,
    required String subtitle,
    required String buttonText,
    required IconData icon,
    required LinearGradient gradient,
    required Color shadowColor,
    required VoidCallback onTap,
  }) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(20),
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          gradient: gradient,
          borderRadius: BorderRadius.circular(20),
          boxShadow: [
            BoxShadow(
              color: shadowColor,
              blurRadius: 14,
              offset: const Offset(0, 6),
            ),
          ],
        ),
        child: Row(
          children: [
            Container(
              width: 52,
              height: 52,
              decoration: BoxDecoration(
                color: Colors.white.withValues(alpha: 0.2),
                shape: BoxShape.circle,
              ),
              child: Icon(icon, color: Colors.white, size: 28),
            ),
            NetraSpacing.gapW16,
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: const TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.w900,
                      color: Colors.white,
                      letterSpacing: 0.5,
                    ),
                  ),
                  const SizedBox(height: 3),
                  Text(
                    subtitle,
                    style: TextStyle(
                      fontSize: 12,
                      color: Colors.white.withValues(alpha: 0.9),
                    ),
                  ),
                  const SizedBox(height: 10),
                  Container(
                    padding:
                        const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: Colors.white.withValues(alpha: 0.25),
                      borderRadius: BorderRadius.circular(20),
                      border: Border.all(
                        color: Colors.white.withValues(alpha: 0.4),
                      ),
                    ),
                    child: Text(
                      buttonText,
                      style: const TextStyle(
                        fontSize: 10,
                        fontWeight: FontWeight.bold,
                        color: Colors.white,
                        letterSpacing: 0.8,
                      ),
                    ),
                  ),
                ],
              ),
            ),
            const Icon(Icons.arrow_forward_ios_rounded,
                color: Colors.white, size: 16),
          ],
        ),
      ),
    );
  }
}
