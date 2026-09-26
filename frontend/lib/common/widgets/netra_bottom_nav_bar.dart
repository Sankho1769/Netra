import 'package:flutter/material.dart';
import '../../core/theme/netra_colors.dart';

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
        color: NetraColors.surfaceWhite,
        borderRadius: const BorderRadius.vertical(top: Radius.circular(24)),
        border: const Border(
          top: BorderSide(color: Color(0xFFE2E8F0), width: 1),
        ),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.06),
            blurRadius: 20,
            offset: const Offset(0, -6),
          ),
        ],
      ),
      child: SafeArea(
        top: false,
        child: SizedBox(
          height: 68,
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
              // 2: Center Elevated Blood-Drop Action Button
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
        borderRadius: BorderRadius.circular(16),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              isSelected ? icon : unselectedIcon,
              color: color,
              size: 24,
            ),
            const SizedBox(height: 3),
            Text(
              label,
              style: TextStyle(
                fontSize: 10,
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

  Widget _buildCenterActionButton(BuildContext context) {
    return SizedBox(
      width: 76,
      child: Center(
        child: Transform.translate(
          offset: const Offset(0, -14),
          child: GestureDetector(
            onTap: onCenterActionTap,
            child: Container(
              width: 62,
              height: 62,
              decoration: BoxDecoration(
                color: NetraColors.primaryRed,
                shape: BoxShape.circle,
                border: Border.all(
                  color: NetraColors.surfaceWhite,
                  width: 5,
                ),
                boxShadow: [
                  BoxShadow(
                    color: NetraColors.primaryRed.withValues(alpha: 0.45),
                    blurRadius: 16,
                    offset: const Offset(0, 6),
                  ),
                ],
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
      ),
    );
  }
}
