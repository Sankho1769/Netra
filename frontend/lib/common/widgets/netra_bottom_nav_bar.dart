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
    return Container(
      decoration: BoxDecoration(
        borderRadius: const BorderRadius.vertical(top: Radius.circular(28)),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.08),
            blurRadius: 24,
            offset: const Offset(0, -6),
            spreadRadius: 0,
          ),
          BoxShadow(
            color: Colors.white.withValues(alpha: 0.9),
            blurRadius: 8,
            offset: const Offset(0, -2),
            spreadRadius: 0,
          ),
        ],
      ),
      child: ClipRRect(
        borderRadius: const BorderRadius.vertical(top: Radius.circular(28)),
        child: BackdropFilter(
          filter: ImageFilter.blur(sigmaX: 18, sigmaY: 18),
          child: Container(
            decoration: BoxDecoration(
              color: Colors.white.withValues(alpha: 0.88),
              borderRadius: const BorderRadius.vertical(top: Radius.circular(28)),
              border: Border(
                top: BorderSide(
                  color: Colors.white.withValues(alpha: 0.95),
                  width: 1.5,
                ),
              ),
            ),
            child: SafeArea(
              top: false,
              child: SizedBox(
                height: 70,
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
                    // 2: Center 3D Clay Crimson Blood-Drop
                    _buildCenterActionButton(context),
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
    final color = isSelected ? NetraColors.primaryRed : const Color(0xFF94A3B8);

    return Expanded(
      child: InkWell(
        onTap: () => onItemSelected(index),
        borderRadius: BorderRadius.circular(20),
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 200),
          padding: const EdgeInsets.symmetric(vertical: 4),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              AnimatedContainer(
                duration: const Duration(milliseconds: 200),
                padding: EdgeInsets.symmetric(
                  horizontal: isSelected ? 12 : 0,
                  vertical: isSelected ? 4 : 0,
                ),
                decoration: BoxDecoration(
                  color: isSelected
                      ? const Color(0xFFFFEBEE).withValues(alpha: 0.8)
                      : Colors.transparent,
                  borderRadius: BorderRadius.circular(14),
                  boxShadow: isSelected
                      ? [
                          BoxShadow(
                            color: NetraColors.primaryRed.withValues(alpha: 0.12),
                            blurRadius: 6,
                            offset: const Offset(0, 2),
                          ),
                        ]
                      : null,
                ),
                child: Icon(
                  isSelected ? icon : unselectedIcon,
                  color: color,
                  size: 24,
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
      ),
    );
  }

  Widget _buildCenterActionButton(BuildContext context) {
    return SizedBox(
      width: 76,
      child: Center(
        child: Transform.translate(
          offset: const Offset(0, -14),
          child: GestureDetector(
            behavior: HitTestBehavior.opaque,
            onTap: onCenterActionTap,
            child: Container(
              width: 64,
              height: 64,
              decoration: BoxDecoration(
                gradient: const LinearGradient(
                  colors: [Color(0xFFEF4444), Color(0xFFB91C1C)],
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
                shape: BoxShape.circle,
                border: Border.all(
                  color: Colors.white,
                  width: 4.5,
                ),
                boxShadow: ClayGlassTheme.crimsonClayShadow(depth: 8),
              ),
              child: const Center(
                child: Icon(
                  Icons.water_drop_rounded,
                  color: Colors.white,
                  size: 30,
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
