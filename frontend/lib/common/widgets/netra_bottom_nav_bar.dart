import 'dart:ui';
import 'package:flutter/material.dart';
import '../../core/theme/netra_colors.dart';
import '../../core/theme/clay_glass_theme.dart';

class NetraBottomNavBar extends StatelessWidget {
  final int selectedIndex;
  final ValueChanged<int> onItemSelected;
  final VoidCallback onCenterActionTap;

  const NetraBottomNavBar({
    super.key,
    required this.selectedIndex,
    required this.onItemSelected,
    required this.onCenterActionTap,
  });

  @override
  Widget build(BuildContext context) {
    final bottomInset = MediaQuery.viewPaddingOf(context).bottom;
    final effectiveBottom = bottomInset > 0 ? bottomInset : 6.0;
    const double barHeight = 60.0;
    final double totalBarHeight = barHeight + effectiveBottom;

    return SizedBox(
      height: totalBarHeight + 16,
      child: Stack(
        clipBehavior: Clip.none,
        alignment: Alignment.bottomCenter,
        children: [
          // 1. Frosted Glass Background Bar
          Positioned(
            left: 0,
            right: 0,
            bottom: 0,
            height: totalBarHeight,
            child: Container(
              decoration: BoxDecoration(
                borderRadius:
                    const BorderRadius.vertical(top: Radius.circular(24)),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withValues(alpha: 0.07),
                    blurRadius: 20,
                    offset: const Offset(0, -4),
                  ),
                  BoxShadow(
                    color: Colors.white.withValues(alpha: 0.85),
                    blurRadius: 6,
                    offset: const Offset(0, -1),
                  ),
                ],
              ),
              child: ClipRRect(
                borderRadius:
                    const BorderRadius.vertical(top: Radius.circular(24)),
                child: BackdropFilter(
                  filter: ImageFilter.blur(sigmaX: 16, sigmaY: 16),
                  child: Container(
                    decoration: BoxDecoration(
                      color: Colors.white.withValues(alpha: 0.92),
                      borderRadius: const BorderRadius.vertical(
                          top: Radius.circular(24)),
                      border: Border(
                        top: BorderSide(
                          color: Colors.white.withValues(alpha: 0.95),
                          width: 1.5,
                        ),
                      ),
                    ),
                    padding: EdgeInsets.only(
                      bottom: effectiveBottom,
                      top: 4,
                    ),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceAround,
                      children: [
                        // 0: Home
                        _buildNavItem(
                          context: context,
                          index: 0,
                          icon: Icons.home_rounded,
                          unselectedIcon: Icons.home_outlined,
                          label: 'Home',
                        ),
                        // 1: Events
                        _buildNavItem(
                          context: context,
                          index: 1,
                          icon: Icons.calendar_today_rounded,
                          unselectedIcon: Icons.calendar_today_outlined,
                          label: 'Events',
                        ),
                        // 2: Placeholder for Floating Action Button
                        const SizedBox(width: 68),
                        // 3: About
                        _buildNavItem(
                          context: context,
                          index: 3,
                          icon: Icons.article_rounded,
                          unselectedIcon: Icons.article_outlined,
                          label: 'About',
                        ),
                        // 4: Profile
                        _buildNavItem(
                          context: context,
                          index: 4,
                          icon: Icons.person_rounded,
                          unselectedIcon: Icons.person_outline_rounded,
                          label: 'Profile',
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ),

          // 2. Floating 3D Crimson Clay Blood-Drop Button (Never Clipped!)
          Positioned(
            bottom: effectiveBottom + 12,
            child: GestureDetector(
              behavior: HitTestBehavior.opaque,
              onTap: onCenterActionTap,
              child: Container(
                width: 58,
                height: 58,
                decoration: BoxDecoration(
                  gradient: const LinearGradient(
                    colors: [Color(0xFFEF4444), Color(0xFFDC2626)],
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                  ),
                  shape: BoxShape.circle,
                  border: Border.all(
                    color: Colors.white,
                    width: 3.5,
                  ),
                  boxShadow: ClayGlassTheme.crimsonClayShadow(depth: 6),
                ),
                child: const Center(
                  child: Icon(
                    Icons.water_drop_rounded,
                    color: Colors.white,
                    size: 28,
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildNavItem({
    required BuildContext context,
    required int index,
    required IconData icon,
    required IconData unselectedIcon,
    required String label,
  }) {
    final isSelected = selectedIndex == index;
    // WCAG AA compliant text colors (contrast ratio > 5:1 on light surface)
    final color = isSelected ? NetraColors.primaryRed : const Color(0xFF475569);

    return Expanded(
      child: InkWell(
        onTap: () => onItemSelected(index),
        borderRadius: BorderRadius.circular(16),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          mainAxisSize: MainAxisSize.min,
          children: [
            AnimatedContainer(
              duration: const Duration(milliseconds: 180),
              padding: EdgeInsets.symmetric(
                horizontal: isSelected ? 12 : 0,
                vertical: isSelected ? 3 : 0,
              ),
              decoration: BoxDecoration(
                color: isSelected
                    ? const Color(0xFFFFEBEE).withValues(alpha: 0.85)
                    : Colors.transparent,
                borderRadius: BorderRadius.circular(12),
                boxShadow: isSelected
                    ? [
                        BoxShadow(
                          color: NetraColors.primaryRed.withValues(alpha: 0.12),
                          blurRadius: 4,
                          offset: const Offset(0, 1),
                        ),
                      ]
                    : null,
              ),
              child: Icon(
                isSelected ? icon : unselectedIcon,
                color: color,
                size: 23,
              ),
            ),
            const SizedBox(height: 3),
            Text(
              label,
              style: TextStyle(
                fontSize: 11,
                fontWeight: isSelected ? FontWeight.w700 : FontWeight.w500,
                color: color,
                letterSpacing: 0.1,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
