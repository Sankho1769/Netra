import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../models/blood_request.dart';
import '../state/blood_request_controller.dart';
import '../widgets/urgency_badge.dart';
import '../widgets/request_status_badge.dart';
import '../../matching/screens/donor_matches_screen.dart';
import '../../donor_response/screens/requester_match_list_screen.dart';
import '../../donor_response/services/donor_response_api_service.dart';
import '../../donor_response/models/donor_match_response_model.dart';

class BloodRequestDetailsScreen extends StatefulWidget {
  final String requestId;
  final BloodRequestController? controller;

  const BloodRequestDetailsScreen({
    super.key,
    required this.requestId,
    this.controller,
  });

  @override
  State<BloodRequestDetailsScreen> createState() =>
      _BloodRequestDetailsScreenState();
}

class _BloodRequestDetailsScreenState extends State<BloodRequestDetailsScreen> {
  late final BloodRequestController _controller;
  late final DonorResponseApiService _donorResponseApiService;
  BloodRequestDetail? _detail;

  bool _isRaisingHand = false;
  bool _hasRaisedHand = false;
  DonorMatchStatus? _myMatchStatus;
  String? _myMatchId;

  @override
  void initState() {
    super.initState();
    _controller = widget.controller ?? BloodRequestController();
    _donorResponseApiService = DonorResponseApiService();
    _loadDetails();
  }

  Future<void> _checkMyOfferStatus() async {
    try {
      final myMatches = await _donorResponseApiService.getMyMatches();
      for (final m in myMatches) {
        if (m.bloodRequestId == widget.requestId) {
          if (mounted) {
            setState(() {
              _hasRaisedHand = true;
              _myMatchId = m.matchId;
              _myMatchStatus = m.responseStatus;
            });
          }
          break;
        }
      }
    } catch (_) {}
  }

  Future<void> _loadDetails() async {
    final detail = await _controller.loadRequestDetails(widget.requestId);
    if (mounted) {
      setState(() {
        _detail = detail;
      });
      _checkMyOfferStatus();
    }
  }

  Future<void> _showCancelDialog() async {
    final reasonController = TextEditingController();
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Cancel Blood Request'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Are you sure you want to cancel this blood request? This action cannot be undone.',
              style: TextStyle(fontSize: 14),
            ),
            const SizedBox(height: 16),
            TextField(
              controller: reasonController,
              decoration: const InputDecoration(
                labelText: 'Reason for cancellation (optional)',
                hintText: 'e.g. Requirement fulfilled through family',
                border: OutlineInputBorder(),
              ),
              maxLines: 2,
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('Keep Active'),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFFDC2626)),
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('Confirm Cancel',
                style: TextStyle(color: Colors.white)),
          ),
        ],
      ),
    );

    if (confirmed == true && mounted) {
      final success = await _controller.cancelRequest(
        widget.requestId,
        reason: reasonController.text.trim().isEmpty
            ? null
            : reasonController.text.trim(),
      );
      if (success) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
              content: Text('Blood request cancelled successfully.')),
        );
        _loadDetails();
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content:
                Text(_controller.errorMessage ?? 'Failed to cancel request.'),
            backgroundColor: Colors.red,
          ),
        );
      }
    }
  }

  Future<void> _showEditDialog() async {
    if (_detail == null) return;

    final unitsController =
        TextEditingController(text: _detail!.unitsRequired.toString());
    BloodRequestUrgency selectedUrgency = _detail!.urgency;
    final hospitalNameController =
        TextEditingController(text: _detail!.hospitalName);
    final hospitalAddressController =
        TextEditingController(text: _detail!.hospitalAddress);
    final descriptionController =
        TextEditingController(text: _detail!.description ?? '');

    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setDialogState) => AlertDialog(
          title: const Text('Edit Blood Request'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: unitsController,
                  decoration:
                      const InputDecoration(labelText: 'Units Required (1-50)'),
                  keyboardType: TextInputType.number,
                ),
                const SizedBox(height: 12),
                DropdownButtonFormField<BloodRequestUrgency>(
                  initialValue: selectedUrgency,
                  decoration: const InputDecoration(labelText: 'Urgency'),
                  items: BloodRequestUrgency.values.map((u) {
                    return DropdownMenuItem(
                      value: u,
                      child: Text(u.displayName),
                    );
                  }).toList(),
                  onChanged: (val) {
                    if (val != null) {
                      setDialogState(() => selectedUrgency = val);
                    }
                  },
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: hospitalNameController,
                  decoration: const InputDecoration(labelText: 'Hospital Name'),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: hospitalAddressController,
                  decoration:
                      const InputDecoration(labelText: 'Hospital Address'),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: descriptionController,
                  decoration:
                      const InputDecoration(labelText: 'Description / Notes'),
                  maxLines: 2,
                ),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx, false),
              child: const Text('Cancel'),
            ),
            ElevatedButton(
              onPressed: () => Navigator.pop(ctx, true),
              child: const Text('Save Changes'),
            ),
          ],
        ),
      ),
    );

    if (confirmed == true && mounted) {
      final units =
          int.tryParse(unitsController.text.trim()) ?? _detail!.unitsRequired;
      final payload = <String, dynamic>{
        'unitsRequired': units,
        'urgency': selectedUrgency.name.toUpperCase(),
        'hospitalName': hospitalNameController.text.trim(),
        'hospitalAddress': hospitalAddressController.text.trim(),
        if (descriptionController.text.trim().isNotEmpty)
          'description': descriptionController.text.trim(),
      };

      final updated =
          await _controller.updateRequest(widget.requestId, payload);
      if (updated != null && mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Blood request updated successfully.')),
        );
        setState(() {
          _detail = updated;
        });
      } else if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content:
                Text(_controller.errorMessage ?? 'Failed to update request.'),
            backgroundColor: Colors.red,
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_controller.isLoading && _detail == null) {
      return Scaffold(
        appBar: AppBar(title: const Text('Blood Request Details')),
        body: const Center(child: CircularProgressIndicator()),
      );
    }

    if (_detail == null) {
      return Scaffold(
        appBar: AppBar(title: const Text('Blood Request Details')),
        body: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                _controller.errorMessage ?? 'Failed to load request details.',
                style: const TextStyle(color: Colors.red),
              ),
              const SizedBox(height: 16),
              ElevatedButton(
                onPressed: _loadDetails,
                child: const Text('Retry'),
              ),
            ],
          ),
        ),
      );
    }

    final d = _detail!;
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Blood Request Details'),
        actions: [
          if (d.canManage && d.status == BloodRequestStatus.open) ...[
            IconButton(
              icon: const Icon(Icons.edit_outlined),
              tooltip: 'Edit Request',
              onPressed: _showEditDialog,
            ),
            IconButton(
              icon: const Icon(Icons.cancel_outlined, color: Colors.red),
              tooltip: 'Cancel Request',
              onPressed: _showCancelDialog,
            ),
          ],
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Prominent banner: Blood group + units + status
            Card(
              elevation: 0,
              color: const Color(0xFFFEF2F2),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(16),
                side: const BorderSide(color: Color(0xFFFCA5A5)),
              ),
              child: Padding(
                padding: const EdgeInsets.all(20),
                child: Row(
                  children: [
                    Container(
                      width: 64,
                      height: 64,
                      decoration: BoxDecoration(
                        color: const Color(0xFFDC2626),
                        borderRadius: BorderRadius.circular(16),
                      ),
                      alignment: Alignment.center,
                      child: Text(
                        d.bloodGroup,
                        style: const TextStyle(
                          fontSize: 26,
                          fontWeight: FontWeight.w900,
                          color: Colors.white,
                        ),
                      ),
                    ),
                    const SizedBox(width: 16),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            '${d.unitsRequired} ${d.unitsRequired == 1 ? "Unit" : "Units"} Required',
                            style: theme.textTheme.titleLarge?.copyWith(
                              fontWeight: FontWeight.w800,
                              color: const Color(0xFF991B1B),
                            ),
                          ),
                          const SizedBox(height: 4),
                          Text(
                            '${d.unitsFulfilled} of ${d.unitsRequired} Units Fulfilled',
                            style: TextStyle(
                              fontSize: 13,
                              fontWeight: FontWeight.w700,
                              color: d.isFullyFulfilled
                                  ? const Color(0xFF16A34A)
                                  : const Color(0xFF92400E),
                            ),
                          ),
                          if (d.helperCount > 0) ...[
                            const SizedBox(height: 2),
                            Text(
                              '${d.helperCount} ${d.helperCount == 1 ? "person has" : "people have"} offered to help',
                              style: const TextStyle(
                                fontSize: 12.5,
                                fontWeight: FontWeight.w600,
                                color: Color(0xFF1D4ED8),
                              ),
                            ),
                          ],
                          const SizedBox(height: 6),
                          Row(
                            children: [
                              UrgencyBadge(urgency: d.urgency),
                              const SizedBox(width: 8),
                              RequestStatusBadge(status: d.status),
                            ],
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 20),

            // Hospital & Operational Location
            _buildSectionHeader(
                context, 'Hospital & Location', Icons.local_hospital_outlined),
            Card(
              shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12)),
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  children: [
                    _buildInfoRow('Hospital', d.hospitalName, isBold: true),
                    const Divider(height: 16),
                    _buildInfoRow('Address', d.hospitalAddress),
                    const Divider(height: 16),
                    _buildInfoRow('City / State',
                        '${d.city}, ${d.state} (${d.postalCode})'),
                    if (d.canManage &&
                        d.latitude != null &&
                        d.longitude != null) ...[
                      const Divider(height: 16),
                      _buildInfoRow('GPS Coordinates',
                          '${d.latitude!.toStringAsFixed(4)}, ${d.longitude!.toStringAsFixed(4)}'),
                    ],
                    if (d.distanceKm != null) ...[
                      const Divider(height: 16),
                      _buildInfoRow(
                          'Proximity Distance', '${d.distanceKm} km away'),
                    ],
                  ],
                ),
              ),
            ),
            const SizedBox(height: 20),

            // Timelines
            _buildSectionHeader(
                context, 'Timelines & Deadlines', Icons.schedule_outlined),
            Card(
              shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12)),
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  children: [
                    _buildInfoRow(
                      'Required By Deadline',
                      _formatDateTime(d.requiredBy),
                      isUrgent: d.urgency == BloodRequestUrgency.critical,
                    ),
                    const Divider(height: 16),
                    _buildInfoRow('Created At', _formatDateTime(d.createdAt)),
                    if (d.cancelledAt != null) ...[
                      const Divider(height: 16),
                      _buildInfoRow(
                          'Cancelled At', _formatDateTime(d.cancelledAt!)),
                      if (d.cancellationReason != null) ...[
                        const Divider(height: 16),
                        _buildInfoRow(
                            'Cancellation Reason', d.cancellationReason!),
                      ],
                    ],
                  ],
                ),
              ),
            ),
            const SizedBox(height: 20),

            // Clinical Notes / Description
            if (d.description != null && d.description!.isNotEmpty) ...[
              _buildSectionHeader(
                  context, 'Additional Details', Icons.description_outlined),
              Card(
                shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12)),
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Text(
                    d.description!,
                    style: theme.textTheme.bodyMedium?.copyWith(
                      color: const Color(0xFF374151),
                      height: 1.5,
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 20),
            ],

            // Owner-specific section
            if (d.isOwner) ...[
              _buildSectionHeader(context, 'Ownership Actions & Helpers',
                  Icons.verified_user_outlined),
              Card(
                color: Colors.blue.shade50,
                shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12)),
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          const Icon(Icons.check_circle, color: Color(0xFF2563EB)),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Text(
                              'You are verified as the creator of this blood request.',
                              style: TextStyle(
                                fontSize: 13,
                                color: Colors.blue.shade900,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                          ),
                        ],
                      ),
                      if (d.helperCount > 0) ...[
                        const SizedBox(height: 12),
                        Container(
                          padding: const EdgeInsets.all(10),
                          decoration: BoxDecoration(
                            color: Colors.white,
                            borderRadius: BorderRadius.circular(8),
                            border: Border.all(color: Colors.blue.shade200),
                          ),
                          child: Row(
                            children: [
                              const Icon(Icons.volunteer_activism,
                                  color: Color(0xFFDC2626), size: 20),
                              const SizedBox(width: 8),
                              Expanded(
                                child: Text(
                                  '${d.helperCount} ${d.helperCount == 1 ? 'person has' : 'people have'} offered to help!',
                                  style: const TextStyle(
                                    fontWeight: FontWeight.bold,
                                    color: Color(0xFFDC2626),
                                    fontSize: 13,
                                  ),
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 16),
              if (d.helperCount > 0) ...[
                SizedBox(
                  width: double.infinity,
                  child: ElevatedButton.icon(
                    style: ElevatedButton.styleFrom(
                      backgroundColor: const Color(0xFF16A34A),
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(vertical: 14),
                      shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(10)),
                    ),
                    icon: const Icon(Icons.people),
                    label: Text(
                      'View Helpers (${d.helperCount})',
                      style: const TextStyle(
                          fontSize: 15, fontWeight: FontWeight.bold),
                    ),
                    onPressed: () {
                      Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (_) => RequesterMatchListScreen(
                            requestId: d.id,
                            bloodGroup: d.bloodGroup,
                          ),
                        ),
                      ).then((_) => _loadDetails());
                    },
                  ),
                ),
                const SizedBox(height: 10),
              ],
              SizedBox(
                width: double.infinity,
                child: ElevatedButton.icon(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFFDC2626),
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(vertical: 14),
                    shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(10)),
                  ),
                  icon: const Icon(Icons.person_search),
                  label: const Text(
                    'Find Matching Donors',
                    style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold),
                  ),
                  onPressed: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (_) => DonorMatchesScreen(
                          requestId: d.id,
                          targetBloodGroup: d.bloodGroup,
                          hospitalName: d.hospitalName,
                        ),
                      ),
                    );
                  },
                ),
              ),
              const SizedBox(height: 10),
              SizedBox(
                width: double.infinity,
                child: OutlinedButton.icon(
                  style: OutlinedButton.styleFrom(
                    foregroundColor: const Color(0xFFDC2626),
                    side: const BorderSide(color: Color(0xFFDC2626)),
                    padding: const EdgeInsets.symmetric(vertical: 14),
                    shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(10)),
                  ),
                  icon: const Icon(Icons.people_outline),
                  label: Text(
                    d.helperCount > 0
                        ? 'Manage All Matched Donors'
                        : 'View Matched Donors',
                    style: const TextStyle(
                        fontSize: 15, fontWeight: FontWeight.bold),
                  ),
                  onPressed: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (_) => RequesterMatchListScreen(
                          requestId: d.id,
                          bloodGroup: d.bloodGroup,
                        ),
                      ),
                    ).then((_) => _loadDetails());
                  },
                ),
              ),
              const SizedBox(height: 20),
            ] else ...[
              // Non-owner / Donor section
              _buildSectionHeader(
                  context, 'Offer Assistance', Icons.volunteer_activism),
              if (d.status != BloodRequestStatus.open || d.isFullyFulfilled)
                Card(
                  color: Colors.grey.shade100,
                  shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(10)),
                  child: Padding(
                    padding: const EdgeInsets.all(14),
                    child: Row(
                      children: [
                        Icon(Icons.info_outline, color: Colors.grey.shade700),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Text(
                            d.isFullyFulfilled
                                ? 'This request is fully fulfilled (${d.unitsFulfilled}/${d.unitsRequired} units met). Thank you!'
                                : 'This blood request is ${d.status.displayName.toLowerCase()} and is not currently accepting offers.',
                            style: TextStyle(
                                fontSize: 13, color: Colors.grey.shade800),
                          ),
                        ),
                      ],
                    ),
                  ),
                )
              else if (_hasRaisedHand) ...[
                Card(
                  color: _myMatchStatus == DonorMatchStatus.accepted
                      ? const Color(0xFFF0FDF4)
                      : const Color(0xFFFEF3C7),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12),
                    side: BorderSide(
                      color: _myMatchStatus == DonorMatchStatus.accepted
                          ? const Color(0xFF86EFAC)
                          : const Color(0xFFFCD34D),
                    ),
                  ),
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            Icon(
                              _myMatchStatus == DonorMatchStatus.accepted
                                  ? Icons.check_circle
                                  : Icons.hourglass_top,
                              color: _myMatchStatus == DonorMatchStatus.accepted
                                  ? const Color(0xFF16A34A)
                                  : const Color(0xFFD97706),
                            ),
                            const SizedBox(width: 10),
                            Expanded(
                              child: Text(
                                _myMatchStatus == DonorMatchStatus.accepted
                                    ? 'Offer Accepted by Requester!'
                                    : 'You Offered to Help!',
                                style: TextStyle(
                                  fontWeight: FontWeight.bold,
                                  fontSize: 15,
                                  color: _myMatchStatus == DonorMatchStatus.accepted
                                      ? const Color(0xFF15803D)
                                      : const Color(0xFF92400E),
                                ),
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 8),
                        Text(
                          _myMatchStatus == DonorMatchStatus.accepted
                              ? 'The requester has accepted your offer to donate. Please coordinate your arrival and donation details with the requester.'
                              : 'Your offer has been submitted to the requester. When they accept, you will receive their direct contact details.',
                          style: TextStyle(
                            fontSize: 13,
                            color: _myMatchStatus == DonorMatchStatus.accepted
                                ? const Color(0xFF166534)
                                : const Color(0xFF78350F),
                            height: 1.4,
                          ),
                        ),
                        const SizedBox(height: 14),
                        if (_myMatchStatus == DonorMatchStatus.accepted &&
                            _myMatchId != null)
                          SizedBox(
                            width: double.infinity,
                            child: ElevatedButton.icon(
                              style: ElevatedButton.styleFrom(
                                backgroundColor: const Color(0xFF16A34A),
                                foregroundColor: Colors.white,
                                padding:
                                    const EdgeInsets.symmetric(vertical: 12),
                                shape: RoundedRectangleBorder(
                                    borderRadius: BorderRadius.circular(8)),
                              ),
                              icon: const Icon(Icons.contact_phone),
                              label: const Text(
                                'View Requester Contact & Coordinate',
                                style: TextStyle(fontWeight: FontWeight.bold),
                              ),
                              onPressed: () => _showContactDialog(_myMatchId!),
                            ),
                          )
                        else if (_myMatchId != null)
                          SizedBox(
                            width: double.infinity,
                            child: OutlinedButton.icon(
                              style: OutlinedButton.styleFrom(
                                foregroundColor: const Color(0xFFDC2626),
                                side:
                                    const BorderSide(color: Color(0xFFDC2626)),
                                padding:
                                    const EdgeInsets.symmetric(vertical: 12),
                                shape: RoundedRectangleBorder(
                                    borderRadius: BorderRadius.circular(8)),
                              ),
                              icon: const Icon(Icons.cancel_outlined),
                              label: const Text('Withdraw Help Offer'),
                              onPressed: _cancelMyOffer,
                            ),
                          ),
                      ],
                    ),
                  ),
                ),
              ] else
                SizedBox(
                  width: double.infinity,
                  child: ElevatedButton.icon(
                    style: ElevatedButton.styleFrom(
                      backgroundColor: const Color(0xFFDC2626),
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(vertical: 16),
                      shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(10)),
                      elevation: 3,
                    ),
                    icon: _isRaisingHand
                        ? const SizedBox(
                            width: 20,
                            height: 20,
                            child: CircularProgressIndicator(
                                strokeWidth: 2, color: Colors.white),
                          )
                        : const Icon(Icons.volunteer_activism, size: 22),
                    label: Text(
                      _isRaisingHand
                          ? 'Submitting Offer...'
                          : 'RAISE HAND / OFFER HELP',
                      style: const TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.bold,
                          letterSpacing: 0.5),
                    ),
                    onPressed: _isRaisingHand ? null : _handleRaiseHand,
                  ),
                ),
              const SizedBox(height: 20),
            ],

            // Note regarding patient privacy
            Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: Colors.grey.shade100,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: Colors.grey.shade300),
              ),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Icon(Icons.privacy_tip_outlined,
                      size: 20, color: Colors.grey.shade700),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Text(
                      'To protect patient privacy, direct contact numbers and personal identifiers are withheld. Donations are coordinated through registered blood banks and official hospital reception desks.',
                      style: TextStyle(
                        fontSize: 12,
                        color: Colors.grey.shade700,
                        height: 1.4,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSectionHeader(
      BuildContext context, String title, IconData icon) {
    return Padding(
      padding: const EdgeInsets.only(left: 4, bottom: 8),
      child: Row(
        children: [
          Icon(icon, size: 20, color: const Color(0xFF374151)),
          const SizedBox(width: 8),
          Text(
            title,
            style: const TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w700,
              color: Color(0xFF1F2937),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildInfoRow(String label, String value,
      {bool isBold = false, bool isUrgent = false}) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(
          label,
          style: TextStyle(
            fontSize: 14,
            color: Colors.grey.shade600,
            fontWeight: FontWeight.w500,
          ),
        ),
        const SizedBox(width: 16),
        Expanded(
          child: Text(
            value,
            textAlign: TextAlign.end,
            style: TextStyle(
              fontSize: 14,
              fontWeight:
                  isBold || isUrgent ? FontWeight.w700 : FontWeight.w500,
              color:
                  isUrgent ? const Color(0xFFDC2626) : const Color(0xFF1F2937),
            ),
          ),
        ),
      ],
    );
  }

  String _formatDateTime(DateTime dt) {
    final local = dt.toLocal();
    final year = local.year;
    final month = local.month.toString().padLeft(2, '0');
    final day = local.day.toString().padLeft(2, '0');
    final hour = local.hour.toString().padLeft(2, '0');
    final min = local.minute.toString().padLeft(2, '0');
    return '$year-$month-$day $hour:$min';
  }

  Future<void> _handleRaiseHand() async {
    final d = _detail;
    if (d == null) return;

    if (d.isOwner) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
            content: Text('You cannot offer help on your own blood request.')),
      );
      return;
    }

    if (d.status != BloodRequestStatus.open) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
            content: Text(
                'This request is ${d.status.displayName.toLowerCase()} and cannot accept new offers.')),
      );
      return;
    }

    if (d.isFullyFulfilled) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
            content:
                Text('This blood request has already met its required units.')),
      );
      return;
    }

    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Offer to Help as Donor?'),
        content: Text(
          'By offering to help, you signal your readiness to donate ${d.bloodGroup} blood at ${d.hospitalName}.\n\n'
          'The requester will review your offer. Once accepted, both parties will receive secure contact information to coordinate donation.\n\n'
          'Clinical note: Official eligibility screening will be performed at the hospital/blood bank.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('Cancel'),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: const Color(0xFFDC2626),
              foregroundColor: Colors.white,
            ),
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('Confirm & Raise Hand'),
          ),
        ],
      ),
    );

    if (confirmed != true) return;

    setState(() => _isRaisingHand = true);
    try {
      final response =
          await _donorResponseApiService.raiseHand(widget.requestId);
      if (mounted) {
        setState(() {
          _hasRaisedHand = true;
          _myMatchId = response.matchId;
          _myMatchStatus = DonorMatchStatus.matched;
        });
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
              content: Text(
                  'Offer submitted successfully! The requester has been notified.')),
        );
        _loadDetails();
      }
    } catch (e) {
      if (mounted) {
        final msg = e
            .toString()
            .replaceFirst('ValidationException: ', '')
            .replaceFirst('Exception: ', '');
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(msg), backgroundColor: Colors.red.shade700),
        );
        _checkMyOfferStatus();
      }
    } finally {
      if (mounted) {
        setState(() => _isRaisingHand = false);
      }
    }
  }

  Future<void> _cancelMyOffer() async {
    if (_myMatchId == null) return;

    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Withdraw Help Offer?'),
        content: const Text(
          'Are you sure you want to withdraw your offer to help with this blood request?\n\n'
          'The requester will be notified that your help is no longer available.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('Keep Active'),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: const Color(0xFFDC2626),
              foregroundColor: Colors.white,
            ),
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('Withdraw Offer'),
          ),
        ],
      ),
    );

    if (confirmed != true) return;

    try {
      await _donorResponseApiService.cancelCommitment(_myMatchId!,
          reason: 'Donor cancelled offer');
      if (mounted) {
        setState(() {
          _hasRaisedHand = false;
          _myMatchId = null;
          _myMatchStatus = null;
        });
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
              content: Text('Your offer to help has been withdrawn.')),
        );
        _loadDetails();
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to withdraw offer: $e')),
        );
      }
    }
  }

  Future<void> _showContactDialog(String matchId) async {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (_) => const Center(child: CircularProgressIndicator()),
    );

    try {
      final contact = await _donorResponseApiService.getMatchContact(matchId);
      if (mounted) {
        Navigator.pop(context); // Dismiss loading dialog
        _showContactBottomSheet(contact);
      }
    } catch (e) {
      if (mounted) {
        Navigator.pop(context); // Dismiss loading dialog
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
                    color: Color(0xFFDC2626), size: 28),
                const SizedBox(width: 12),
                const Expanded(
                  child: Text(
                    'Direct Coordination Contact',
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
                color: const Color(0xFFEFF6FF),
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: const Color(0xFF93C5FD)),
              ),
              child: Text(
                contact.instructions,
                style: const TextStyle(fontSize: 13, color: Color(0xFF1E3A8A)),
              ),
            ),
            const SizedBox(height: 16),
            ListTile(
              contentPadding: EdgeInsets.zero,
              leading: CircleAvatar(
                backgroundColor: const Color(0xFFFEE2E2),
                child: Text(
                  contact.otherPartyName.isNotEmpty
                      ? contact.otherPartyName[0].toUpperCase()
                      : '?',
                  style: const TextStyle(
                      color: Color(0xFFDC2626), fontWeight: FontWeight.bold),
                ),
              ),
              title: Text(
                contact.otherPartyName,
                style:
                    const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
              ),
              subtitle: Text(
                contact.otherPartyRole == 'DONOR'
                    ? 'Confirmed Blood Donor'
                    : 'Blood Requester',
                style: TextStyle(color: Colors.grey.shade600, fontSize: 13),
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
                          Text('Phone Number',
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
                              content:
                                  Text('Phone number copied to clipboard.')),
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
                  backgroundColor: const Color(0xFFDC2626),
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
