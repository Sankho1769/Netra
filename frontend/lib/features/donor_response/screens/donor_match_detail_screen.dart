import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../models/donor_match_response_model.dart';
import '../services/donor_response_api_service.dart';
import '../widgets/match_status_badge.dart';

/// Detail screen presenting a match to the authenticated donor with accept/decline actions.
class DonorMatchDetailScreen extends StatefulWidget {
  final String matchId;
  final DonorResponseApiService? apiService;

  const DonorMatchDetailScreen({
    super.key,
    required this.matchId,
    this.apiService,
  });

  @override
  State<DonorMatchDetailScreen> createState() => _DonorMatchDetailScreenState();
}

class _DonorMatchDetailScreenState extends State<DonorMatchDetailScreen> {
  late final DonorResponseApiService _apiService;

  bool _isLoading = true;
  bool _isProcessingAction = false;
  String? _errorMessage;
  DonorMatchDetail? _detail;

  @override
  void initState() {
    super.initState();
    _apiService = widget.apiService ?? DonorResponseApiService();
    _fetchDetail();
  }

  Future<void> _fetchDetail() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final detail = await _apiService.getMatchDetail(widget.matchId);
      if (mounted) {
        setState(() {
          _detail = detail;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _errorMessage =
              e.toString().replaceFirst('ValidationException: ', '');
          _isLoading = false;
        });
      }
    }
  }

  Future<void> _confirmAndAccept() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Accept Blood Match?'),
        content: const Text(
          'Accepting signals your willingness to donate for this blood request.\n\n'
          'Important:\n'
          '• This does NOT confirm medical clearance or complete a donation.\n'
          '• Final screening and crossmatching are performed by qualified blood-bank staff.\n'
          '• Terminal responses cannot be reopened once submitted.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('Cancel'),
          ),
          ElevatedButton(
            onPressed: () => Navigator.of(ctx).pop(true),
            style: ElevatedButton.styleFrom(
              backgroundColor: const Color(0xFF16A34A),
              foregroundColor: Colors.white,
            ),
            child: const Text('Confirm Accept'),
          ),
        ],
      ),
    );

    if (confirmed != true) return;

    setState(() => _isProcessingAction = true);
    try {
      final updated = await _apiService.acceptMatch(widget.matchId);
      if (mounted) {
        setState(() {
          _detail = updated;
          _isProcessingAction = false;
        });
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text(
                'Match accepted successfully. Blood bank staff will coordinate next steps.'),
            backgroundColor: Color(0xFF16A34A),
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isProcessingAction = false);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content:
                Text(e.toString().replaceFirst('ValidationException: ', '')),
            backgroundColor: const Color(0xFFDC2626),
          ),
        );
      }
    }
  }

  Future<void> _confirmAndDecline() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Decline Blood Match?'),
        content: const Text(
          'Are you sure you want to decline this request?\n\n'
          'This response is final and cannot be reopened.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('Back'),
          ),
          ElevatedButton(
            onPressed: () => Navigator.of(ctx).pop(true),
            style: ElevatedButton.styleFrom(
              backgroundColor: const Color(0xFFDC2626),
              foregroundColor: Colors.white,
            ),
            child: const Text('Confirm Decline'),
          ),
        ],
      ),
    );

    if (confirmed != true) return;

    setState(() => _isProcessingAction = true);
    try {
      final updated = await _apiService.declineMatch(widget.matchId);
      if (mounted) {
        setState(() {
          _detail = updated;
          _isProcessingAction = false;
        });
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Match declined.'),
            backgroundColor: Color(0xFF4B5563),
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isProcessingAction = false);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content:
                Text(e.toString().replaceFirst('ValidationException: ', '')),
            backgroundColor: const Color(0xFFDC2626),
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return PopScope<bool>(
      canPop: false,
      onPopInvokedWithResult: (bool didPop, dynamic result) {
        if (didPop) return;
        Navigator.of(context).pop(_detail?.responseStatus.isTerminal == true);
      },
      child: Scaffold(
        appBar: AppBar(
          title: const Text('Match Details'),
          backgroundColor: const Color(0xFFDC2626),
          foregroundColor: Colors.white,
        ),
        body: _isLoading
            ? const Center(
                child: CircularProgressIndicator(color: Color(0xFFDC2626)))
            : _errorMessage != null
                ? _buildErrorState()
                : _buildContent(),
      ),
    );
  }

  Widget _buildContent() {
    final detail = _detail!;
    final bool canAct =
        detail.responseStatus == DonorMatchStatus.matched && !detail.isExpired;

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // Medical Disclaimer
          _buildDisclaimerBanner(detail.disclaimer),
          const SizedBox(height: 16),

          // Status & Clinical Card
          Card(
            elevation: 2,
            shape:
                RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(
                        'Status',
                        style: TextStyle(
                            fontSize: 14,
                            color: Colors.grey.shade600,
                            fontWeight: FontWeight.w500),
                      ),
                      MatchStatusBadge(status: detail.responseStatus),
                    ],
                  ),
                  const Divider(height: 24),
                  Row(
                    children: [
                      Container(
                        padding: const EdgeInsets.all(12),
                        decoration: BoxDecoration(
                          color: const Color(0xFFFEE2E2),
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: Text(
                          detail.bloodGroupRequired,
                          style: const TextStyle(
                            fontSize: 22,
                            fontWeight: FontWeight.bold,
                            color: Color(0xFFDC2626),
                          ),
                        ),
                      ),
                      const SizedBox(width: 16),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              '${detail.unitsRequired} Unit${detail.unitsRequired > 1 ? 's' : ''} Needed',
                              style: const TextStyle(
                                  fontSize: 16, fontWeight: FontWeight.bold),
                            ),
                            const SizedBox(height: 4),
                            Text(
                              'Urgency: ${detail.urgency}',
                              style: TextStyle(
                                fontSize: 13,
                                color: detail.urgency == 'CRITICAL'
                                    ? const Color(0xFFDC2626)
                                    : Colors.grey.shade700,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),

          // Hospital & Location Card
          Card(
            elevation: 2,
            shape:
                RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('Location Details',
                      style:
                          TextStyle(fontSize: 15, fontWeight: FontWeight.bold)),
                  const SizedBox(height: 12),
                  _buildDetailRow(Icons.local_hospital_outlined, 'Hospital',
                      detail.hospitalName),
                  if (detail.city.isNotEmpty || detail.state.isNotEmpty)
                    _buildDetailRow(
                      Icons.location_on_outlined,
                      'City / State',
                      '${detail.city}${detail.city.isNotEmpty && detail.state.isNotEmpty ? ', ' : ''}${detail.state}',
                    ),
                  if (detail.distanceKm != null)
                    _buildDetailRow(
                      Icons.route_outlined,
                      'Approx Distance',
                      '${detail.distanceKm!.toStringAsFixed(1)} km away',
                    ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),

          // Match Timeline Card
          Card(
            elevation: 2,
            shape:
                RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('Timeline & Expiration',
                      style:
                          TextStyle(fontSize: 15, fontWeight: FontWeight.bold)),
                  const SizedBox(height: 12),
                  _buildDetailRow(
                    Icons.schedule_outlined,
                    'Match Created',
                    detail.createdAt.toLocal().toString().substring(0, 16),
                  ),
                  _buildDetailRow(
                    Icons.timer_off_outlined,
                    'Response Deadline',
                    detail.expiresAt.toLocal().toString().substring(0, 16),
                  ),
                  if (detail.requiredBy != null)
                    _buildDetailRow(
                      Icons.event_outlined,
                      'Blood Required By',
                      detail.requiredBy!.toLocal().toString().substring(0, 16),
                    ),
                  if (detail.respondedAt != null)
                    _buildDetailRow(
                      Icons.check_circle_outline,
                      'Responded At',
                      detail.respondedAt!.toLocal().toString().substring(0, 16),
                    ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 24),

          // Action Section
          if (canAct)
            Column(
              children: [
                ElevatedButton.icon(
                  onPressed: _isProcessingAction ? null : _confirmAndAccept,
                  icon: const Icon(Icons.check_circle),
                  label: _isProcessingAction
                      ? const SizedBox(
                          height: 20,
                          width: 20,
                          child: CircularProgressIndicator(
                              color: Colors.white, strokeWidth: 2),
                        )
                      : const Text('Accept Match Assignment'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFF16A34A),
                    foregroundColor: Colors.white,
                    minimumSize: const Size.fromHeight(48),
                    shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(8)),
                  ),
                ),
                const SizedBox(height: 12),
                OutlinedButton.icon(
                  onPressed: _isProcessingAction ? null : _confirmAndDecline,
                  icon: const Icon(Icons.close),
                  label: const Text('Decline Match'),
                  style: OutlinedButton.styleFrom(
                    foregroundColor: const Color(0xFFDC2626),
                    side: const BorderSide(color: Color(0xFFDC2626)),
                    minimumSize: const Size.fromHeight(48),
                    shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(8)),
                  ),
                ),
              ],
            )
          else if (detail.responseStatus == DonorMatchStatus.accepted)
            Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: const Color(0xFFF0FDF4),
                borderRadius: BorderRadius.circular(10),
                border: Border.all(color: const Color(0xFF86EFAC)),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const Row(
                    children: [
                      Icon(Icons.check_circle,
                          color: Color(0xFF16A34A), size: 20),
                      SizedBox(width: 8),
                      Text(
                        'Offer Accepted & Confirmed',
                        style: TextStyle(
                          fontSize: 14,
                          fontWeight: FontWeight.bold,
                          color: Color(0xFF15803D),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  const Text(
                    'The requester has accepted your offer! You can now view their direct contact information and coordinate arrival at the hospital blood bank.',
                    style: TextStyle(
                        fontSize: 12.5,
                        color: Color(0xFF166534),
                        height: 1.35),
                  ),
                  const SizedBox(height: 12),
                  ElevatedButton.icon(
                    style: ElevatedButton.styleFrom(
                      backgroundColor: const Color(0xFF16A34A),
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(vertical: 12),
                      shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(8)),
                    ),
                    icon: const Icon(Icons.contact_phone, size: 18),
                    label: const Text('View Requester Contact & Coordinate',
                        style: TextStyle(fontWeight: FontWeight.bold)),
                    onPressed: _showRequesterContact,
                  ),
                ],
              ),
            )
          else
            Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: Colors.grey.shade100,
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: Colors.grey.shade300),
              ),
              child: Row(
                children: [
                  Icon(Icons.lock_outline,
                      color: Colors.grey.shade600, size: 20),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Text(
                      detail.responseStatus.isTerminal
                          ? 'This match is in a terminal state (${detail.responseStatus.displayName}) and cannot be modified.'
                          : 'This match has expired.',
                      style:
                          TextStyle(color: Colors.grey.shade700, fontSize: 13),
                    ),
                  ),
                ],
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildDetailRow(IconData icon, String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, size: 18, color: Colors.grey.shade600),
          const SizedBox(width: 10),
          SizedBox(
            width: 130,
            child: Text(label,
                style: TextStyle(color: Colors.grey.shade600, fontSize: 13)),
          ),
          Expanded(
            child: Text(
              value,
              style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildDisclaimerBanner(String text) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: const Color(0xFFFEF3C7),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: const Color(0xFFF59E0B), width: 1),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Icon(Icons.shield_outlined, color: Color(0xFFB45309), size: 20),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              text,
              style: const TextStyle(
                fontSize: 12.5,
                color: Color(0xFF78350F),
                height: 1.35,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildErrorState() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.error_outline, size: 48, color: Color(0xFFDC2626)),
            const SizedBox(height: 16),
            Text(
              _errorMessage ?? 'Failed to load match details.',
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 16),
            ElevatedButton(
              onPressed: _fetchDetail,
              child: const Text('Retry'),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _showRequesterContact() async {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (_) => const Center(child: CircularProgressIndicator()),
    );

    try {
      final contact = await _apiService.getMatchContact(widget.matchId);
      if (mounted) {
        Navigator.pop(context);
        _showContactBottomSheet(contact);
      }
    } catch (e) {
      if (mounted) {
        Navigator.pop(context);
        final msg = e
            .toString()
            .replaceFirst('ValidationException: ', '')
            .replaceFirst('Exception: ', '');
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
              content: Text('Failed to load contact: $msg'),
              backgroundColor: Colors.red.shade700),
        );
      }
    }
  }

  void _showContactBottomSheet(MatchContactInfo contact) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (ctx) => Padding(
        padding: EdgeInsets.only(
          left: 20,
          right: 20,
          top: 20,
          bottom: MediaQuery.of(ctx).viewInsets.bottom + 24,
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              children: [
                const Icon(Icons.contact_phone,
                    color: Color(0xFF16A34A), size: 28),
                const SizedBox(width: 12),
                const Expanded(
                  child: Text(
                    'Requester Contact & Coordination',
                    style:
                        TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                  ),
                ),
                IconButton(
                  icon: const Icon(Icons.close),
                  onPressed: () => Navigator.pop(ctx),
                ),
              ],
            ),
            const Divider(height: 24),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: const Color(0xFFF0FDF4),
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: const Color(0xFF86EFAC)),
              ),
              child: Text(
                contact.instructions,
                style: const TextStyle(fontSize: 13, color: Color(0xFF166534)),
              ),
            ),
            const SizedBox(height: 16),
            ListTile(
              contentPadding: EdgeInsets.zero,
              leading: CircleAvatar(
                backgroundColor: const Color(0xFFDCFCE7),
                child: Text(
                  contact.otherPartyName.isNotEmpty
                      ? contact.otherPartyName[0].toUpperCase()
                      : 'R',
                  style: const TextStyle(
                      color: Color(0xFF15803D), fontWeight: FontWeight.bold),
                ),
              ),
              title: Text(
                contact.otherPartyName,
                style:
                    const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
              ),
              subtitle: const Text(
                'Blood Requester / Patient Coordinator',
                style: TextStyle(color: Colors.grey, fontSize: 13),
              ),
            ),
            const SizedBox(height: 12),
            if (contact.otherPartyPhone != null &&
                contact.otherPartyPhone!.isNotEmpty)
              Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                decoration: BoxDecoration(
                  color: Colors.grey.shade100,
                  borderRadius: BorderRadius.circular(10),
                  border: Border.all(color: Colors.grey.shade300),
                ),
                child: Row(
                  children: [
                    const Icon(Icons.phone, color: Color(0xFF16A34A)),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text('Requester Phone Number',
                              style: TextStyle(
                                  fontSize: 11, color: Colors.grey.shade600)),
                          Text(
                            contact.otherPartyPhone!,
                            style: const TextStyle(
                                fontSize: 16,
                                fontWeight: FontWeight.bold,
                                letterSpacing: 0.5),
                          ),
                        ],
                      ),
                    ),
                    IconButton(
                      icon: const Icon(Icons.copy,
                          size: 20, color: Color(0xFF2563EB)),
                      tooltip: 'Copy Number',
                      onPressed: () {
                        Clipboard.setData(
                            ClipboardData(text: contact.otherPartyPhone!));
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(
                              content: Text(
                                  'Requester phone number copied to clipboard.')),
                        );
                      },
                    ),
                  ],
                ),
              ),
            const SizedBox(height: 12),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: Colors.grey.shade50,
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: Colors.grey.shade200),
              ),
              child: Row(
                children: [
                  const Icon(Icons.local_hospital,
                      color: Color(0xFFDC2626), size: 20),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(contact.hospitalName,
                            style: const TextStyle(
                                fontWeight: FontWeight.bold, fontSize: 13)),
                        if (contact.hospitalAddress.isNotEmpty)
                          Text(contact.hospitalAddress,
                              style: TextStyle(
                                  fontSize: 12, color: Colors.grey.shade600)),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFF16A34A),
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(vertical: 12),
                  shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(8)),
                ),
                onPressed: () => Navigator.pop(ctx),
                child: const Text('Close'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
