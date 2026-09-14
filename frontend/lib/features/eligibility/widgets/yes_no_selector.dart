import 'package:flutter/material.dart';
import '../../../core/theme/netra_colors.dart';
import '../../../core/theme/netra_spacing.dart';
import '../../../core/theme/netra_typography.dart';

class YesNoSelector extends StatelessWidget {
  final bool? value;
  final ValueChanged<bool?> onChanged;
  final bool allowUnknown;

  const YesNoSelector({
    super.key,
    required this.value,
    required this.onChanged,
    this.allowUnknown = false,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: _buildChoiceCard(
            label: "Yes",
            icon: Icons.check_circle_outline_rounded,
            isSelected: value == true,
            onTap: () => onChanged(true),
          ),
        ),
        NetraSpacing.gapW12,
        Expanded(
          child: _buildChoiceCard(
            label: "No",
            icon: Icons.cancel_outlined,
            isSelected: value == false,
            onTap: () => onChanged(false),
          ),
        ),
        if (allowUnknown) ...[
          NetraSpacing.gapW12,
          Expanded(
            child: _buildChoiceCard(
              label: "Not Sure",
              icon: Icons.help_outline_rounded,
              isSelected: value == null,
              onTap: () => onChanged(null),
            ),
          ),
        ],
      ],
    );
  }

  Widget _buildChoiceCard({
    required String label,
    required IconData icon,
    required bool isSelected,
    required VoidCallback onTap,
  }) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 180),
        padding: const EdgeInsets.symmetric(vertical: 14),
        decoration: BoxDecoration(
          color: isSelected ? NetraColors.backgroundRed : NetraColors.surfaceWhite,
          borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
          border: Border.all(
            color: isSelected ? NetraColors.primaryRed : NetraColors.borderGray,
            width: isSelected ? 2.0 : 1.0,
          ),
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              icon,
              size: 20,
              color: isSelected ? NetraColors.primaryRed : NetraColors.textSecondary,
            ),
            NetraSpacing.gapW8,
            Text(
              label,
              style: NetraTypography.titleSmall.copyWith(
                fontWeight: isSelected ? FontWeight.w700 : FontWeight.w500,
                color: isSelected ? NetraColors.primaryRed : NetraColors.textPrimary,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
