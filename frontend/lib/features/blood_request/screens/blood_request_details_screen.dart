import 'package:flutter/material.dart';
import '../models/blood_request.dart';
import '../state/blood_request_controller.dart';
import '../widgets/urgency_badge.dart';
import '../widgets/request_status_badge.dart';

class BloodRequestDetailsScreen extends StatefulWidget {
  final String requestId;
  final BloodRequestController? controller;

  const BloodRequestDetailsScreen({
    super.key,
    required this.requestId,
    this.controller,
  });

  @override
  State<BloodRequestDetailsScreen> createState() => _BloodRequestDetailsScreenState();
}

class _BloodRequestDetailsScreenState extends State<BloodRequestDetailsScreen> {
  late final BloodRequestController _controller;
  BloodRequestDetail? _detail;

  @override
  void initState() {
    super.initState();
    _controller = widget.controller ?? BloodRequestController();
    _loadDetails();
  }

  Future<void> _loadDetails() async {
    final detail = await _controller.loadRequestDetails(widget.requestId);
    if (mounted) {
      setState(() {
        _detail = detail;
      });
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
            style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFFDC2626)),
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('Confirm Cancel', style: TextStyle(color: Colors.white)),
          ),
        ],
      ),
    );

    if (confirmed == true && mounted) {
      final success = await _controller.cancelRequest(
        widget.requestId,
        reason: reasonController.text.trim().isEmpty ? null : reasonController.text.trim(),
      );
      if (success) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Blood request cancelled successfully.')),
        );
        _loadDetails();
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(_controller.errorMessage ?? 'Failed to cancel request.'),
            backgroundColor: Colors.red,
          ),
        );
      }
    }
  }

  Future<void> _showEditDialog() async {
    if (_detail == null) return;

    final unitsController = TextEditingController(text: _detail!.unitsRequired.toString());
    BloodRequestUrgency selectedUrgency = _detail!.urgency;
    final hospitalNameController = TextEditingController(text: _detail!.hospitalName);
    final hospitalAddressController = TextEditingController(text: _detail!.hospitalAddress);
    final descriptionController = TextEditingController(text: _detail!.description ?? '');

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
                  decoration: const InputDecoration(labelText: 'Units Required (1-50)'),
                  keyboardType: TextInputType.number,
                ),
                const SizedBox(height: 12),
                DropdownButtonFormField<BloodRequestUrgency>(
                  value: selectedUrgency,
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
                  decoration: const InputDecoration(labelText: 'Hospital Address'),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: descriptionController,
                  decoration: const InputDecoration(labelText: 'Description / Notes'),
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
      final units = int.tryParse(unitsController.text.trim()) ?? _detail!.unitsRequired;
      final payload = <String, dynamic>{
        'unitsRequired': units,
        'urgency': selectedUrgency.name.toUpperCase(),
        'hospitalName': hospitalNameController.text.trim(),
        'hospitalAddress': hospitalAddressController.text.trim(),
        if (descriptionController.text.trim().isNotEmpty)
          'description': descriptionController.text.trim(),
      };

      final updated = await _controller.updateRequest(widget.requestId, payload);
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
            content: Text(_controller.errorMessage ?? 'Failed to update request.'),
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
            _buildSectionHeader(context, 'Hospital & Location', Icons.local_hospital_outlined),
            Card(
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  children: [
                    _buildInfoRow('Hospital', d.hospitalName, isBold: true),
                    const Divider(height: 16),
                    _buildInfoRow('Address', d.hospitalAddress),
                    const Divider(height: 16),
                    _buildInfoRow('City / State', '${d.city}, ${d.state} (${d.postalCode})'),
                    if (d.canManage && d.latitude != null && d.longitude != null) ...[
                      const Divider(height: 16),
                      _buildInfoRow('GPS Coordinates', '${d.latitude!.toStringAsFixed(4)}, ${d.longitude!.toStringAsFixed(4)}'),
                    ],
                    if (d.distanceKm != null) ...[
                      const Divider(height: 16),
                      _buildInfoRow('Proximity Distance', '${d.distanceKm} km away'),
                    ],
                  ],
                ),
              ),
            ),
            const SizedBox(height: 20),

            // Timelines
            _buildSectionHeader(context, 'Timelines & Deadlines', Icons.schedule_outlined),
            Card(
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
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
                      _buildInfoRow('Cancelled At', _formatDateTime(d.cancelledAt!)),
                      if (d.cancellationReason != null) ...[
                        const Divider(height: 16),
                        _buildInfoRow('Cancellation Reason', d.cancellationReason!),
                      ],
                    ],
                  ],
                ),
              ),
            ),
            const SizedBox(height: 20),

            // Clinical Notes / Description
            if (d.description != null && d.description!.isNotEmpty) ...[
              _buildSectionHeader(context, 'Additional Details', Icons.description_outlined),
              Card(
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
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
              _buildSectionHeader(context, 'Ownership Verification', Icons.verified_user_outlined),
              Card(
                color: Colors.blue.shade50,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Row(
                    children: [
                      const Icon(Icons.check_circle, color: Color(0xFF2563EB)),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Text(
                          'You are verified as the creator of this blood request. You can edit mutable fields or cancel it while OPEN.',
                          style: TextStyle(
                            fontSize: 13,
                            color: Colors.blue.shade900,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                      ),
                    ],
                  ),
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
                  Icon(Icons.privacy_tip_outlined, size: 20, color: Colors.grey.shade700),
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

  Widget _buildSectionHeader(BuildContext context, String title, IconData icon) {
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

  Widget _buildInfoRow(String label, String value, {bool isBold = false, bool isUrgent = false}) {
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
              fontWeight: isBold || isUrgent ? FontWeight.w700 : FontWeight.w500,
              color: isUrgent ? const Color(0xFFDC2626) : const Color(0xFF1F2937),
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
}
