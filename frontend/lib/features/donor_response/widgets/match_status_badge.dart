import 'package:flutter/material.dart';
import '../models/donor_match_response_model.dart';

/// Presentation badge for donor match response statuses.
class MatchStatusBadge extends StatelessWidget {
  final DonorMatchStatus status;

  const MatchStatusBadge({super.key, required this.status});

  @override
  Widget build(BuildContext context) {
    Color bg;
    Color fg;
    IconData icon;

    switch (status) {
      case DonorMatchStatus.matched:
        bg = Colors.amber.shade100;
        fg = Colors.amber.shade900;
        icon = Icons.hourglass_top_rounded;
        break;
      case DonorMatchStatus.accepted:
        bg = Colors.green.shade100;
        fg = Colors.green.shade900;
        icon = Icons.check_circle_rounded;
        break;
      case DonorMatchStatus.declined:
        bg = Colors.red.shade100;
        fg = Colors.red.shade900;
        icon = Icons.cancel_rounded;
        break;
      case DonorMatchStatus.expired:
        bg = Colors.grey.shade200;
        fg = Colors.grey.shade700;
        icon = Icons.timer_off_rounded;
        break;
      case DonorMatchStatus.cancelled:
        bg = Colors.blueGrey.shade100;
        fg = Colors.blueGrey.shade800;
        icon = Icons.block_rounded;
        break;
      case DonorMatchStatus.unknown:
        bg = Colors.grey.shade100;
        fg = Colors.grey.shade600;
        icon = Icons.help_outline_rounded;
        break;
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(16),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 14, color: fg),
          const SizedBox(width: 4),
          Text(
            status.displayName,
            style: TextStyle(
              color: fg,
              fontSize: 12,
              fontWeight: FontWeight.w600,
            ),
          ),
        ],
      ),
    );
  }
}
