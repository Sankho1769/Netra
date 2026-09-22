import 'package:flutter/material.dart';
import '../models/donation_model.dart';

/// Presentation badge for verified donation statuses.
class DonationStatusBadge extends StatelessWidget {
  final DonationVerificationStatus status;

  const DonationStatusBadge({super.key, required this.status});

  @override
  Widget build(BuildContext context) {
    Color bg;
    Color fg;
    IconData icon;

    switch (status) {
      case DonationVerificationStatus.pendingVerification:
        bg = Colors.amber.shade100;
        fg = Colors.amber.shade900;
        icon = Icons.hourglass_top_rounded;
        break;
      case DonationVerificationStatus.verified:
        bg = Colors.green.shade100;
        fg = Colors.green.shade900;
        icon = Icons.verified_rounded;
        break;
      case DonationVerificationStatus.rejected:
        bg = Colors.red.shade100;
        fg = Colors.red.shade900;
        icon = Icons.cancel_rounded;
        break;
      case DonationVerificationStatus.cancelled:
        bg = Colors.grey.shade200;
        fg = Colors.grey.shade700;
        icon = Icons.block_rounded;
        break;
      case DonationVerificationStatus.unknown:
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
            status.label,
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
