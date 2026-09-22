import 'package:flutter/material.dart';
import '../services/notification_api_service.dart';
import '../screens/notification_list_screen.dart';

class NotificationBellIcon extends StatefulWidget {
  final NotificationApiService? apiService;
  final int? initialUnreadCount;

  const NotificationBellIcon({
    super.key,
    this.apiService,
    this.initialUnreadCount,
  });

  @override
  State<NotificationBellIcon> createState() => _NotificationBellIconState();
}

class _NotificationBellIconState extends State<NotificationBellIcon> {
  late final NotificationApiService _apiService;
  int _unreadCount = 0;

  @override
  void initState() {
    super.initState();
    _unreadCount = widget.initialUnreadCount ?? 0;
    _apiService = widget.apiService ?? NotificationApiService();
    if (widget.initialUnreadCount == null) {
      _fetchUnreadCount();
    }
  }

  Future<void> _fetchUnreadCount() async {
    try {
      final count = await _apiService.getUnreadCount();
      if (mounted) {
        setState(() => _unreadCount = count);
      }
    } catch (_) {
      // Graceful silence on network failure
    }
  }

  void _openNotifications() async {
    await Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => NotificationListScreen(apiService: _apiService),
      ),
    );
    _fetchUnreadCount();
  }

  @override
  Widget build(BuildContext context) {
    return IconButton(
      icon: Stack(
        clipBehavior: Clip.none,
        children: [
          const Icon(Icons.notifications_outlined, size: 24),
          if (_unreadCount > 0)
            Positioned(
              right: -2,
              top: -2,
              child: Container(
                padding: const EdgeInsets.all(3),
                decoration: const BoxDecoration(
                  color: Color(0xFFDC2626),
                  shape: BoxShape.circle,
                ),
                constraints: const BoxConstraints(minWidth: 16, minHeight: 16),
                child: Text(
                  _unreadCount > 99 ? '99+' : '$_unreadCount',
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 10,
                    fontWeight: FontWeight.bold,
                  ),
                  textAlign: TextAlign.center,
                ),
              ),
            ),
        ],
      ),
      tooltip: 'Notifications',
      onPressed: _openNotifications,
    );
  }
}
