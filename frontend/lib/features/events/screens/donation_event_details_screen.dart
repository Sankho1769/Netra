import 'package:flutter/material.dart';
import '../../../core/responsive/responsive_scaffold.dart';
import '../../../common/widgets/netra_loading_indicator.dart';
import '../../../common/widgets/netra_error_view.dart';
import '../../../common/widgets/netra_button.dart';
import '../../../common/widgets/netra_disclaimer_banner.dart';
import '../models/event_registration.dart';
import '../state/donation_event_controller.dart';
import '../widgets/event_capacity_indicator.dart';
import '../widgets/event_status_badge.dart';
import '../widgets/participant_registration_dialog.dart';
import '../../auth/state/auth_scope.dart';

class DonationEventDetailsScreen extends StatefulWidget {
  final String eventId;
  final DonationEventController? controller;

  const DonationEventDetailsScreen({
    super.key,
    required this.eventId,
    this.controller,
  });

  @override
  State<DonationEventDetailsScreen> createState() =>
      _DonationEventDetailsScreenState();
}

class _DonationEventDetailsScreenState
    extends State<DonationEventDetailsScreen> {
  late final DonationEventController _controller;

  @override
  void initState() {
    super.initState();
    _controller = widget.controller ?? DonationEventController();
    _controller.loadEventDetails(widget.eventId);
  }

  @override
  void dispose() {
    if (widget.controller == null) {
      _controller.dispose();
    }
    super.dispose();
  }

  String _formatDateTime(DateTime dt) {
    const months = [
      'Jan',
      'Feb',
      'Mar',
      'Apr',
      'May',
      'Jun',
      'Jul',
      'Aug',
      'Sep',
      'Oct',
      'Nov',
      'Dec'
    ];
    final month = months[dt.month - 1];
    final hour = dt.hour % 12 == 0 ? 12 : dt.hour % 12;
    final minute = dt.minute.toString().padLeft(2, '0');
    final period = dt.hour >= 12 ? 'PM' : 'AM';
    return '${dt.day} $month ${dt.year}, $hour:$minute $period';
  }

  void _handleRegister() async {
    final authController = AuthScope.of(context);
    final user = authController.currentUser;

    final participantData = await showDialog<ParticipantRegistrationData>(
      context: context,
      barrierDismissible: false,
      builder: (_) => ParticipantRegistrationDialog(
        eventTitle: _controller.currentEvent?.title ?? 'Donation Camp',
        venueName: _controller.currentEvent?.venueName ?? 'Blood Centre',
        initialFullName: user?.fullName,
        initialEmail: user?.email,
        initialPhone: user?.phone,
      ),
    );

    if (participantData == null) return;

    final success =
        await _controller.registerForEvent(widget.eventId, participantData);
    if (!mounted) return;

    if (success) {
      final reg = _controller.currentRegistration;
      await showDialog(
        context: context,
        builder: (ctx) => AlertDialog(
          shape:
              RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
          title: Row(
            children: [
              Container(
                padding: const EdgeInsets.all(6),
                decoration: const BoxDecoration(
                  color: Color(0xFFDCFCE7),
                  shape: BoxShape.circle,
                ),
                child: const Icon(Icons.check_circle_rounded,
                    color: Color(0xFF16A34A), size: 24),
              ),
              const SizedBox(width: 10),
              const Expanded(child: Text('Registration Confirmed!')),
            ],
          ),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                "You are registered for ${_controller.currentEvent?.title ?? 'this donation camp'}.",
                style:
                    const TextStyle(fontWeight: FontWeight.w600, fontSize: 14),
              ),
              const SizedBox(height: 12),
              if (reg != null) ...[
                Container(
                  padding: const EdgeInsets.all(10),
                  decoration: BoxDecoration(
                    color: const Color(0xFFF8FAFC),
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(color: const Color(0xFFE2E8F0)),
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Registration ID: ${reg.id}',
                        style: const TextStyle(
                          fontSize: 11,
                          fontFamily: 'monospace',
                          color: Colors.blueGrey,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        'Participant: ${participantData.fullName} (${participantData.bloodGroup})',
                        style: const TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      Text(
                        'Venue: ${_controller.currentEvent?.venueName ?? ""}',
                        style: const TextStyle(fontSize: 12),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 10),
              ],
              const Text(
                'Please bring government-issued photo ID on the day of the camp. Final medical screening is conducted on-site by clinical staff.',
                style: TextStyle(
                  fontSize: 12,
                  color: Colors.black87,
                  height: 1.35,
                ),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text('OK'),
            ),
          ],
        ),
      );
    } else if (_controller.errorMessage != null) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(_controller.errorMessage!),
          backgroundColor: Colors.red.shade800,
          behavior: SnackBarBehavior.floating,
        ),
      );
    }
  }

  void _handleCancelRegistration() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Cancel Registration?'),
        content: const Text(
            'Are you sure you want to cancel your slot for this donation camp? Your slot will be released to other donors.'),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(ctx, false),
              child: const Text('Keep Registration')),
          TextButton(
            onPressed: () => Navigator.pop(ctx, true),
            style: TextButton.styleFrom(foregroundColor: Colors.red),
            child: const Text('Cancel Slot'),
          ),
        ],
      ),
    );

    if (confirmed == true) {
      final success = await _controller.cancelRegistration(widget.eventId);
      if (!mounted) return;

      if (success) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
                _controller.actionSuccessMessage ?? 'Registration cancelled.'),
            backgroundColor: Colors.grey.shade800,
            behavior: SnackBarBehavior.floating,
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _controller,
      builder: (context, _) {
        if (_controller.isLoading) {
          return const ResponsiveScaffold(
            title: 'Camp Details',
            body: Center(
                child:
                    NetraLoadingIndicator(message: 'Loading camp details...')),
          );
        }

        if (_controller.errorMessage != null &&
            _controller.currentEvent == null) {
          return ResponsiveScaffold(
            title: 'Camp Details',
            body: NetraErrorView(
              message: _controller.errorMessage!,
              onRetry: () => _controller.loadEventDetails(widget.eventId),
            ),
          );
        }

        final event = _controller.currentEvent;
        if (event == null) {
          return const ResponsiveScaffold(
            title: 'Camp Details',
            body: Center(child: Text('Camp details not found.')),
          );
        }

        final reg = _controller.currentRegistration;
        final bool isRegistered =
            reg != null && reg.status == EventRegistrationStatus.registered;

        return ResponsiveScaffold(
          title: event.title,
          body: SingleChildScrollView(
            padding: const EdgeInsets.all(16.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Status Badge & Distance
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    EventStatusBadge(status: event.status),
                    if (event.distanceKm != null)
                      Text(
                        '${event.distanceKm} km away',
                        style: TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w600,
                          color: Colors.blue.shade800,
                        ),
                      ),
                  ],
                ),
                const SizedBox(height: 12),

                // Title
                Text(
                  event.title,
                  style: const TextStyle(
                      fontSize: 22,
                      fontWeight: FontWeight.bold,
                      letterSpacing: -0.3),
                ),
                const SizedBox(height: 6),

                // Blood Bank Info
                Row(
                  children: [
                    const Icon(Icons.local_hospital,
                        size: 16, color: Colors.red),
                    const SizedBox(width: 6),
                    Text(
                      'Organized by ${event.bloodBankName}',
                      style: const TextStyle(
                          fontSize: 14, fontWeight: FontWeight.w600),
                    ),
                  ],
                ),
                const SizedBox(height: 16),

                // Medical Pre-Screening Disclaimer
                const NetraDisclaimerBanner(
                  title: 'Pre-Screening Only',
                  message:
                      'Registration for a donation camp does not guarantee medical eligibility or completed donation. Final medical screening is conducted on-site by blood bank professionals.',
                ),
                const SizedBox(height: 16),

                // Registration Status Card (If already registered)
                if (isRegistered) ...[
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: const Color(0xFFF0FDF4),
                      borderRadius: BorderRadius.circular(14),
                      border: Border.all(color: const Color(0xFF86EFAC)),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            const Icon(Icons.check_circle_rounded,
                                color: Color(0xFF16A34A), size: 22),
                            const SizedBox(width: 8),
                            const Expanded(
                              child: Text(
                                "You're Registered for this Camp",
                                style: TextStyle(
                                  fontSize: 16,
                                  fontWeight: FontWeight.bold,
                                  color: Color(0xFF14532D),
                                ),
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 8),
                        Text(
                          'Registration ID: ${reg.id}',
                          style: TextStyle(
                            fontSize: 11,
                            fontFamily: 'monospace',
                            color: Colors.grey.shade700,
                          ),
                        ),
                        if (reg.participantName != null) ...[
                          const SizedBox(height: 4),
                          Text(
                            'Participant: ${reg.participantName}${reg.participantBloodGroup != null ? " (${reg.participantBloodGroup})" : ""}',
                            style: const TextStyle(
                              fontSize: 13,
                              fontWeight: FontWeight.w600,
                              color: Color(0xFF166534),
                            ),
                          ),
                        ],
                        const SizedBox(height: 4),
                        Text(
                          'Registered on: ${_formatDateTime(reg.registeredAt.toLocal())}',
                          style: TextStyle(
                              fontSize: 12, color: Colors.green.shade800),
                        ),
                        const SizedBox(height: 12),
                        OutlinedButton.icon(
                          onPressed: _controller.isActionLoading
                              ? null
                              : _handleCancelRegistration,
                          icon: const Icon(Icons.cancel_outlined, size: 16),
                          label: const Text('Cancel My Registration'),
                          style: OutlinedButton.styleFrom(
                            foregroundColor: Colors.red.shade800,
                            side: BorderSide(color: Colors.red.shade300),
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 16),
                ],

                // Capacity Card
                Card(
                  elevation: 0,
                  color: Colors.grey.shade50,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12),
                    side: BorderSide(color: Colors.grey.shade200),
                  ),
                  child: Padding(
                    padding: const EdgeInsets.all(16.0),
                    child: EventCapacityIndicator(
                      currentCount: event.currentRegistrationCount,
                      capacity: event.donorCapacity,
                    ),
                  ),
                ),
                const SizedBox(height: 16),

                // Venue & Time Details
                const Text(
                  'Date & Location',
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 8),
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  leading:
                      const Icon(Icons.calendar_today, color: Colors.blueGrey),
                  title: const Text('Camp Date & Time'),
                  subtitle: Text(
                    'Starts: ${_formatDateTime(event.startAt.toLocal())}\nEnds: ${_formatDateTime(event.endAt.toLocal())}',
                  ),
                ),
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  leading: const Icon(Icons.pin_drop, color: Colors.blueGrey),
                  title: Text(event.venueName),
                  subtitle: Text(
                      '${event.address}, ${event.city}, ${event.state} - ${event.postalCode}'),
                ),
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  leading: const Icon(Icons.how_to_reg, color: Colors.blueGrey),
                  title: const Text('Registration Window'),
                  subtitle: Text(
                    'Opens: ${_formatDateTime(event.registrationOpenAt.toLocal())}\nCloses: ${_formatDateTime(event.registrationCloseAt.toLocal())}',
                  ),
                ),

                if (event.description != null &&
                    event.description!.isNotEmpty) ...[
                  const SizedBox(height: 12),
                  const Text(
                    'About this Camp',
                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 6),
                  Text(
                    event.description!,
                    style: TextStyle(
                        fontSize: 14, color: Colors.grey.shade800, height: 1.4),
                  ),
                ],

                const SizedBox(height: 24),

                // Register CTA button / Registered status
                if (isRegistered) ...[
                  SizedBox(
                    width: double.infinity,
                    height: 48,
                    child: OutlinedButton.icon(
                      onPressed: null,
                      icon: const Icon(
                        Icons.check_circle_rounded,
                        color: Color(0xFF16A34A),
                      ),
                      label: const Text(
                        'Registered for Camp',
                        style: TextStyle(
                          fontSize: 15,
                          fontWeight: FontWeight.bold,
                          color: Color(0xFF16A34A),
                        ),
                      ),
                      style: OutlinedButton.styleFrom(
                        side: const BorderSide(
                          color: Color(0xFF86EFAC),
                          width: 1.5,
                        ),
                        backgroundColor: const Color(0xFFF0FDF4),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                      ),
                    ),
                  ),
                ] else ...[
                  SizedBox(
                    width: double.infinity,
                    height: 48,
                    child: NetraButton(
                      text: event.remainingCapacity == 0
                          ? 'Camp Full'
                          : !event.isRegistrationOpen
                              ? 'Registration Closed'
                              : 'Register for Camp',
                      isLoading: _controller.isActionLoading,
                      onPressed: (event.isRegistrationOpen &&
                              event.remainingCapacity > 0 &&
                              !_controller.isActionLoading)
                          ? _handleRegister
                          : null,
                    ),
                  ),
                ],
              ],
            ),
          ),
        );
      },
    );
  }
}
