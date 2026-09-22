import 'package:flutter/material.dart';
import '../models/blood_request.dart';

class UrgencyBadge extends StatelessWidget {
  final BloodRequestUrgency urgency;

  const UrgencyBadge({super.key, required this.urgency});

  @override
  Widget build(BuildContext context) {
    Color bg;
    Color fg;
    IconData icon;

    switch (urgency) {
      case BloodRequestUrgency.critical:
        bg = const Color(0xFFFEE2E2);
        fg = const Color(0xFFDC2626);
        icon = Icons.warning_amber_rounded;
        break;
      case BloodRequestUrgency.urgent:
        bg = const Color(0xFFFEF3C7);
        fg = const Color(0xFFD97706);
        icon = Icons.priority_high_rounded;
        break;
      case BloodRequestUrgency.normal:
        bg = const Color(0xFFDBEAFE);
        fg = const Color(0xFF2563EB);
        icon = Icons.info_outline_rounded;
        break;
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: fg.withOpacity(0.3)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 14, color: fg),
          const SizedBox(width: 4),
          Text(
            urgency.displayName.toUpperCase(),
            style: TextStyle(
              fontSize: 11,
              fontWeight: FontWeight.w700,
              color: fg,
              letterSpacing: 0.5,
            ),
          ),
        ],
      ),
    );
  }
}
