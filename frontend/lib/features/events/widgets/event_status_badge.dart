import 'package:flutter/material.dart';
import '../models/donation_event.dart';

class EventStatusBadge extends StatelessWidget {
  final DonationEventStatus status;

  const EventStatusBadge({super.key, required this.status});

  @override
  Widget build(BuildContext context) {
    Color bg;
    Color fg;
    IconData icon;

    switch (status) {
      case DonationEventStatus.published:
        bg = Colors.green.shade50;
        fg = Colors.green.shade800;
        icon = Icons.check_circle_outline;
        break;
      case DonationEventStatus.pendingApproval:
        bg = Colors.amber.shade50;
        fg = Colors.amber.shade900;
        icon = Icons.schedule;
        break;
      case DonationEventStatus.draft:
        bg = Colors.grey.shade100;
        fg = Colors.grey.shade700;
        icon = Icons.edit_note;
        break;
      case DonationEventStatus.registrationClosed:
        bg = Colors.orange.shade50;
        fg = Colors.orange.shade800;
        icon = Icons.lock_clock;
        break;
      case DonationEventStatus.ongoing:
        bg = Colors.blue.shade50;
        fg = Colors.blue.shade800;
        icon = Icons.campaign;
        break;
      case DonationEventStatus.completed:
        bg = Colors.teal.shade50;
        fg = Colors.teal.shade800;
        icon = Icons.task_alt;
        break;
      case DonationEventStatus.cancelled:
        bg = Colors.red.shade50;
        fg = Colors.red.shade800;
        icon = Icons.cancel_outlined;
        break;
      case DonationEventStatus.rejected:
        bg = Colors.red.shade100;
        fg = Colors.red.shade900;
        icon = Icons.block;
        break;
    }

    return Semantics(
      label: 'Camp status: ${status.displayName}',
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
        decoration: BoxDecoration(
          color: bg,
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: fg.withValues(alpha: 0.3)),
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
      ),
    );
  }
}
