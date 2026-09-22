import 'package:flutter/material.dart';
import '../models/notification_model.dart';
import '../services/notification_api_service.dart';
import '../widgets/notification_item_card.dart';
import '../../donor_response/screens/donor_match_detail_screen.dart';
import '../../blood_request/screens/blood_request_details_screen.dart';

class NotificationListScreen extends StatefulWidget {
  final NotificationApiService? apiService;

  const NotificationListScreen({super.key, this.apiService});

  @override
  State<NotificationListScreen> createState() => _NotificationListScreenState();
}

class _NotificationListScreenState extends State<NotificationListScreen> {
  late final NotificationApiService _apiService;
  List<AppNotification> _notifications = [];
  bool _isLoading = true;
  String? _errorMessage;
  bool _unreadOnly = false;
  int _unreadCount = 0;

  @override
  void initState() {
    super.initState();
    _apiService = widget.apiService ?? NotificationApiService();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final results = await Future.wait([
        _apiService.getNotifications(unreadOnly: _unreadOnly),
        _apiService.getUnreadCount(),
      ]);

      if (mounted) {
        setState(() {
          _notifications = results[0] as List<AppNotification>;
          _unreadCount = results[1] as int;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _errorMessage = e.toString().replaceFirst('Exception: ', '');
          _isLoading = false;
        });
      }
    }
  }

  Future<void> _markAllAsRead() async {
    try {
      final updated = await _apiService.markAllAsRead();
      if (mounted) {
        setState(() {
          _notifications = _notifications
              .map((n) => n.copyWith(isRead: true, readAt: DateTime.now()))
              .toList();
          _unreadCount = 0;
        });
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Marked $updated notifications as read'),
            duration: const Duration(seconds: 2),
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Failed to mark all as read: $e'),
            backgroundColor: Colors.red.shade700,
          ),
        );
      }
    }
  }

  Future<void> _handleNotificationTap(AppNotification notification) async {
    // Mark as read locally and on server if not read yet
    if (!notification.isRead) {
      try {
        _apiService.markAsRead(notification.id);
        setState(() {
          final idx = _notifications.indexWhere((n) => n.id == notification.id);
          if (idx != -1) {
            _notifications[idx] =
                notification.copyWith(isRead: true, readAt: DateTime.now());
          }
          if (_unreadCount > 0) _unreadCount--;
        });
      } catch (_) {}
    }

    // Safe navigation based on referenceType
    if (notification.referenceType == NotificationReferenceType.donorMatch &&
        notification.referenceId != null) {
      if (!mounted) return;
      Navigator.of(context).push(
        MaterialPageRoute(
          builder: (_) =>
              DonorMatchDetailScreen(matchId: notification.referenceId!),
        ),
      );
    } else if (notification.referenceType ==
            NotificationReferenceType.bloodRequest &&
        notification.referenceId != null) {
      if (!mounted) return;
      Navigator.of(context).push(
        MaterialPageRoute(
          builder: (_) =>
              BloodRequestDetailsScreen(requestId: notification.referenceId!),
        ),
      );
    } else {
      // General detail dialog
      _showDetailDialog(notification);
    }
  }

  void _showDetailDialog(AppNotification notification) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: Text(notification.title,
            style: const TextStyle(fontWeight: FontWeight.bold)),
        content: Text(notification.body),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(),
            child: const Text('Close'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final screenWidth = MediaQuery.of(context).size.width;
    final maxContentWidth = screenWidth >= 1024 ? 800.0 : double.infinity;

    return Scaffold(
      backgroundColor: const Color(0xFFF1F5F9),
      appBar: AppBar(
        title: const Text('Notifications',
            style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
        backgroundColor: Colors.white,
        foregroundColor: const Color(0xFF0F172A),
        elevation: 0,
        actions: [
          if (_unreadCount > 0)
            TextButton.icon(
              onPressed: _markAllAsRead,
              icon: const Icon(Icons.done_all, size: 18),
              label: const Text('Mark all read'),
            ),
        ],
      ),
      body: Center(
        child: ConstrainedBox(
          constraints: BoxConstraints(maxWidth: maxContentWidth),
          child: Column(
            children: [
              // Filter Chips Row
              Container(
                color: Colors.white,
                padding:
                    const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                child: Row(
                  children: [
                    FilterChip(
                      label: const Text('All'),
                      selected: !_unreadOnly,
                      onSelected: (val) {
                        if (_unreadOnly) {
                          setState(() => _unreadOnly = false);
                          _loadData();
                        }
                      },
                      selectedColor: const Color(0xFFDC2626).withOpacity(0.15),
                      checkmarkColor: const Color(0xFFDC2626),
                    ),
                    const SizedBox(width: 8),
                    FilterChip(
                      label: Text('Unread (${_unreadCount})'),
                      selected: _unreadOnly,
                      onSelected: (val) {
                        if (!_unreadOnly) {
                          setState(() => _unreadOnly = true);
                          _loadData();
                        }
                      },
                      selectedColor: const Color(0xFFDC2626).withOpacity(0.15),
                      checkmarkColor: const Color(0xFFDC2626),
                    ),
                  ],
                ),
              ),

              // Content Body
              Expanded(
                child: _buildBody(),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildBody() {
    if (_isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_errorMessage != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(Icons.error_outline, size: 48, color: Colors.red.shade400),
              const SizedBox(height: 12),
              Text(
                _errorMessage!,
                textAlign: TextAlign.center,
                style: const TextStyle(fontSize: 15, color: Color(0xFF334155)),
              ),
              const SizedBox(height: 16),
              ElevatedButton.icon(
                onPressed: _loadData,
                icon: const Icon(Icons.refresh),
                label: const Text('Retry'),
              ),
            ],
          ),
        ),
      );
    }

    if (_notifications.isEmpty) {
      return RefreshIndicator(
        onRefresh: _loadData,
        child: ListView(
          physics: const AlwaysScrollableScrollPhysics(),
          children: [
            SizedBox(height: MediaQuery.of(context).size.height * 0.2),
            Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.notifications_none_rounded,
                      size: 64, color: Colors.grey.shade400),
                  const SizedBox(height: 16),
                  Text(
                    _unreadOnly
                        ? 'No unread notifications'
                        : 'No notifications yet',
                    style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w600,
                        color: Colors.grey.shade600),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    _unreadOnly
                        ? 'You are all caught up!'
                        : 'Updates regarding blood requests and matches will appear here.',
                    style: TextStyle(fontSize: 13, color: Colors.grey.shade500),
                    textAlign: TextAlign.center,
                  ),
                ],
              ),
            ),
          ],
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: _loadData,
      child: ListView.builder(
        padding: const EdgeInsets.symmetric(vertical: 8),
        itemCount: _notifications.length,
        itemBuilder: (context, index) {
          final notification = _notifications[index];
          return NotificationItemCard(
            notification: notification,
            onTap: () => _handleNotificationTap(notification),
          );
        },
      ),
    );
  }
}
