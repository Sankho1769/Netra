import 'package:flutter/material.dart';
import '../../../core/responsive/responsive_scaffold.dart';
import '../../../common/widgets/netra_loading_indicator.dart';
import '../../../common/widgets/netra_error_view.dart';
import '../../../common/widgets/netra_empty_view.dart';
import '../models/event_registration.dart';
import '../state/donation_event_controller.dart';
import 'donation_event_details_screen.dart';

class MyEventRegistrationsScreen extends StatefulWidget {
  final DonationEventController? controller;

  const MyEventRegistrationsScreen({super.key, this.controller});

  @override
  State<MyEventRegistrationsScreen> createState() => _MyEventRegistrationsScreenState();
}

class _MyEventRegistrationsScreenState extends State<MyEventRegistrationsScreen> {
  late final DonationEventController _controller;

  @override
  void initState() {
    super.initState();
    _controller = widget.controller ?? DonationEventController();
    _controller.loadMyRegistrations();
  }

  @override
  void dispose() {
    if (widget.controller == null) {
      _controller.dispose();
    }
    super.dispose();
  }

  String _formatDate(DateTime dt) {
    const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    return '${dt.day} ${months[dt.month - 1]} ${dt.year}';
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _controller,
      builder: (context, _) {
        return ResponsiveScaffold(
          title: 'My Camp Registrations',
          body: _buildBody(),
        );
      },
    );
  }

  Widget _buildBody() {
    if (_controller.isLoading) {
      return const Center(child: NetraLoadingIndicator(message: 'Loading your camp registrations...'));
    }

    if (_controller.errorMessage != null) {
      return NetraErrorView(
        message: _controller.errorMessage!,
        onRetry: () => _controller.loadMyRegistrations(),
      );
    }

    if (_controller.myRegistrations.isEmpty) {
      return NetraEmptyView(
        title: 'No Registrations',
        message: "You haven't registered for any blood donation camps yet.",
        icon: Icons.event_note,
        actionLabel: 'Discover Donation Camps',
        onAction: () => Navigator.pop(context),
      );
    }

    return ListView.builder(
      padding: const EdgeInsets.all(12),
      itemCount: _controller.myRegistrations.length,
      itemBuilder: (context, index) {
        final reg = _controller.myRegistrations[index];
        return Card(
          margin: const EdgeInsets.symmetric(vertical: 6),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
            side: BorderSide(color: Colors.grey.shade200),
          ),
          child: ListTile(
            contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            title: Text(
              reg.eventTitle,
              style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15),
            ),
            subtitle: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const SizedBox(height: 4),
                Text('Registered on: ${_formatDate(reg.registeredAt.toLocal())}'),
                const SizedBox(height: 6),
                _buildStatusChip(reg.status),
              ],
            ),
            trailing: const Icon(Icons.chevron_right),
            onTap: () {
              Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (_) => DonationEventDetailsScreen(eventId: reg.eventId),
                ),
              );
            },
          ),
        );
      },
    );
  }

  Widget _buildStatusChip(EventRegistrationStatus status) {
    Color bg;
    Color fg;

    switch (status) {
      case EventRegistrationStatus.registered:
        bg = Colors.green.shade50;
        fg = Colors.green.shade800;
        break;
      case EventRegistrationStatus.checkedIn:
        bg = Colors.blue.shade50;
        fg = Colors.blue.shade800;
        break;
      case EventRegistrationStatus.completed:
        bg = Colors.teal.shade50;
        fg = Colors.teal.shade800;
        break;
      case EventRegistrationStatus.cancelled:
        bg = Colors.grey.shade100;
        fg = Colors.grey.shade700;
        break;
      case EventRegistrationStatus.waitlisted:
        bg = Colors.amber.shade50;
        fg = Colors.amber.shade900;
        break;
      case EventRegistrationStatus.noShow:
      case EventRegistrationStatus.rejected:
        bg = Colors.red.shade50;
        fg = Colors.red.shade800;
        break;
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(8),
      ),
      child: Text(
        status.displayName,
        style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: fg),
      ),
    );
  }
}
