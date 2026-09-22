import 'package:flutter/material.dart';
import '../models/notification_model.dart';
import '../services/push_notification_service.dart';

class NotificationItemCard extends StatelessWidget {
  final AppNotification notification;
  final VoidCallback onTap;

  const NotificationItemCard({
    super.key,
    required this.notification,
    required this.onTap,
  });

  IconData _iconForType(NotificationType type) {
    switch (type) {
      case NotificationType.matchCreated:
        return Icons.bloodtype;
      case NotificationType.matchAccepted:
        return Icons.check_circle_outline;
      case NotificationType.matchDeclined:
        return Icons.cancel_outlined;
      case NotificationType.matchExpired:
        return Icons.timer_off_outlined;
      case NotificationType.bloodRequestCancelled:
        return Icons.block_outlined;
      case NotificationType.emergencyRequestCreated:
        return Icons.warning_amber_rounded;
      case NotificationType.unknown:
        return Icons.notifications_outlined;
    }
  }

  Color _colorForType(NotificationType type) {
    switch (type) {
      case NotificationType.matchCreated:
        return const Color(0xFFDC2626); // Primary Red
      case NotificationType.matchAccepted:
        return const Color(0xFF16A34A); // Green
      case NotificationType.matchDeclined:
        return const Color(0xFF6B7280); // Gray
      case NotificationType.matchExpired:
        return const Color(0xFFD97706); // Amber
      case NotificationType.bloodRequestCancelled:
        return const Color(0xFFDC2626); // Red
      case NotificationType.emergencyRequestCreated:
        return const Color(0xFFE11D48); // Rose
      case NotificationType.unknown:
        return const Color(0xFF2563EB); // Blue
    }
  }

  @override
  Widget build(BuildContext context) {
    final typeColor = _colorForType(notification.type);
    final relativeTime =
        PushNotificationService.formatRelativeTime(notification.createdAt);

    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
      elevation: notification.isRead ? 0.5 : 2.0,
      color: notification.isRead ? Colors.white : const Color(0xFFF8FAFC),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: BorderSide(
          color: notification.isRead
              ? Colors.grey.shade200
              : const Color(0xFFE2E8F0),
          width: 1.0,
        ),
      ),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Type Icon
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: typeColor.withOpacity(0.12),
                  shape: BoxShape.circle,
                ),
                child: Icon(
                  _iconForType(notification.type),
                  color: typeColor,
                  size: 20,
                ),
              ),
              const SizedBox(width: 14),

              // Notification Content
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: Text(
                            notification.title,
                            style: TextStyle(
                              fontSize: 15,
                              fontWeight: notification.isRead
                                  ? FontWeight.w600
                                  : FontWeight.bold,
                              color: const Color(0xFF0F172A),
                            ),
                          ),
                        ),
                        if (!notification.isRead) ...[
                          const SizedBox(width: 8),
                          Container(
                            width: 8,
                            height: 8,
                            decoration: const BoxDecoration(
                              color: Color(0xFF2563EB),
                              shape: BoxShape.circle,
                            ),
                          ),
                        ],
                      ],
                    ),
                    const SizedBox(height: 4),
                    Text(
                      notification.body,
                      style: TextStyle(
                        fontSize: 13,
                        color: Colors.grey.shade700,
                        height: 1.35,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text(
                          relativeTime,
                          style: TextStyle(
                            fontSize: 12,
                            color: Colors.grey.shade500,
                          ),
                        ),
                        if (notification.referenceType != null &&
                            notification.referenceId != null)
                          Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Text(
                                'View details',
                                style: TextStyle(
                                  fontSize: 12,
                                  fontWeight: FontWeight.w600,
                                  color: typeColor,
                                ),
                              ),
                              const SizedBox(width: 2),
                              Icon(
                                Icons.chevron_right_rounded,
                                size: 16,
                                color: typeColor,
                              ),
                            ],
                          ),
                      ],
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
