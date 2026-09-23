import 'package:flutter/material.dart';
import '../models/fulfillment_model.dart';
import '../services/fulfillment_api_service.dart';
import '../widgets/fulfillment_status_badge.dart';

class FulfillmentDetailScreen extends StatefulWidget {
  final String fulfillmentId;
  final FulfillmentApiService? apiService;

  const FulfillmentDetailScreen({
    super.key,
    required this.fulfillmentId,
    this.apiService,
  });

  @override
  State<FulfillmentDetailScreen> createState() =>
      _FulfillmentDetailScreenState();
}

class _FulfillmentDetailScreenState extends State<FulfillmentDetailScreen> {
  late final FulfillmentApiService _apiService;
  FulfillmentModel? _fulfillment;
  bool _isLoading = true;
  String? _errorMessage;
  bool _isActionLoading = false;

  @override
  void initState() {
    super.initState();
    _apiService = widget.apiService ?? FulfillmentApiService();
    _loadDetails();
  }

  Future<void> _loadDetails() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final details =
          await _apiService.getFulfillmentById(widget.fulfillmentId);
      if (mounted) {
        setState(() {
          _fulfillment = details;
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

  Future<void> _handleStart() async {
    setState(() => _isActionLoading = true);
    try {
      await _apiService.startFulfillment(widget.fulfillmentId);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Fulfillment started successfully.')),
        );
        _loadDetails();
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(e.toString().replaceFirst('Exception: ', '')),
            backgroundColor: Colors.red,
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _isActionLoading = false);
    }
  }

  Future<void> _handleComplete() async {
    setState(() => _isActionLoading = true);
    try {
      await _apiService.completeFulfillment(widget.fulfillmentId);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Fulfillment completed successfully!')),
        );
        _loadDetails();
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(e.toString().replaceFirst('Exception: ', '')),
            backgroundColor: Colors.red,
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _isActionLoading = false);
    }
  }

  Future<void> _promptFail() async {
    final reasonController = TextEditingController();
    final notesController = TextEditingController();

    final result = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Fail Fulfillment'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: reasonController,
              decoration: const InputDecoration(
                labelText: 'Failure Reason *',
                hintText: 'e.g. Crossmatch mismatch, spoiled unit',
              ),
            ),
            const SizedBox(height: 8),
            TextField(
              controller: notesController,
              decoration: const InputDecoration(
                labelText: 'Notes (optional)',
              ),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('Back'),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: Colors.red),
            onPressed: () {
              if (reasonController.text.trim().isEmpty) return;
              Navigator.of(ctx).pop(true);
            },
            child: const Text('Fail Fulfillment'),
          ),
        ],
      ),
    );

    if (result == true) {
      setState(() => _isActionLoading = true);
      try {
        await _apiService.failFulfillment(
          widget.fulfillmentId,
          FailFulfillmentDto(
            failureReason: reasonController.text.trim(),
            notes: notesController.text.trim().isEmpty
                ? null
                : notesController.text.trim(),
          ),
        );
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Fulfillment marked as failed.')),
          );
          _loadDetails();
        }
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(e.toString().replaceFirst('Exception: ', '')),
              backgroundColor: Colors.red,
            ),
          );
        }
      } finally {
        if (mounted) setState(() => _isActionLoading = false);
      }
    }
  }

  Future<void> _promptCancel() async {
    final reasonController = TextEditingController();

    final result = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Cancel Fulfillment'),
        content: TextField(
          controller: reasonController,
          decoration: const InputDecoration(
            labelText: 'Cancellation Reason *',
            hintText: 'e.g. Requester no longer needs units',
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('Back'),
          ),
          ElevatedButton(
            onPressed: () {
              if (reasonController.text.trim().isEmpty) return;
              Navigator.of(ctx).pop(true);
            },
            child: const Text('Confirm Cancel'),
          ),
        ],
      ),
    );

    if (result == true) {
      setState(() => _isActionLoading = true);
      try {
        await _apiService.cancelFulfillment(
          widget.fulfillmentId,
          CancelFulfillmentDto(
            cancellationReason: reasonController.text.trim(),
          ),
        );
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Fulfillment cancelled.')),
          );
          _loadDetails();
        }
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(e.toString().replaceFirst('Exception: ', '')),
              backgroundColor: Colors.red,
            ),
          );
        }
      } finally {
        if (mounted) setState(() => _isActionLoading = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Fulfillment Details'),
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _errorMessage != null
              ? Center(
                  child: Padding(
                    padding: const EdgeInsets.all(24.0),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        const Icon(Icons.error_outline,
                            size: 48, color: Colors.red),
                        const SizedBox(height: 12),
                        Text(_errorMessage!, textAlign: TextAlign.center),
                        const SizedBox(height: 16),
                        ElevatedButton(
                          onPressed: _loadDetails,
                          child: const Text('Retry'),
                        ),
                      ],
                    ),
                  ),
                )
              : _buildContent(),
    );
  }

  Widget _buildContent() {
    final item = _fulfillment!;
    final theme = Theme.of(context);

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Expanded(
                      child: Text(
                        item.hospitalName ?? 'Blood Request Fulfillment',
                        style: theme.textTheme.titleLarge?.copyWith(
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                    FulfillmentStatusBadge(status: item.status),
                  ],
                ),
                const Divider(height: 24),
                _buildInfoRow('Units to Fulfill', '${item.units} unit(s)'),
                if (item.requestedBloodGroup != null)
                  _buildInfoRow(
                      'Requested Blood Group', item.requestedBloodGroup!),
                if (item.donorBloodGroup != null)
                  _buildInfoRow('Donor Blood Group', item.donorBloodGroup!),
                if (item.unitsRequired != null)
                  _buildInfoRow(
                      'Total Units Required', '${item.unitsRequired}'),
                if (item.unitsFulfilled != null)
                  _buildInfoRow(
                      'Total Units Fulfilled', '${item.unitsFulfilled}'),
                if (item.remainingUnits != null)
                  _buildInfoRow(
                      'Remaining Units Needed', '${item.remainingUnits}'),
                if (item.donationDate != null)
                  _buildInfoRow('Donation Date', item.donationDate!),
                if (item.hospitalAddress != null)
                  _buildInfoRow('Hospital Address',
                      '${item.hospitalAddress}, ${item.city ?? ''}'),
                if (item.failureReason != null)
                  _buildInfoRow('Failure Reason', item.failureReason!,
                      textColor: Colors.red),
                if (item.cancellationReason != null)
                  _buildInfoRow(
                      'Cancellation Reason', item.cancellationReason!),
                if (item.notes != null && item.notes!.isNotEmpty)
                  _buildInfoRow('Notes', item.notes!),
              ],
            ),
          ),
        ),
        const SizedBox(height: 16),
        _buildActionButtons(item),
      ],
    );
  }

  Widget _buildInfoRow(String label, String value, {Color? textColor}) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 150,
            child: Text(
              label,
              style: const TextStyle(
                fontWeight: FontWeight.w500,
                color: Colors.grey,
              ),
            ),
          ),
          Expanded(
            child: Text(
              value,
              style: TextStyle(
                fontWeight: FontWeight.w600,
                color: textColor,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildActionButtons(FulfillmentModel item) {
    if (_isActionLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    final buttons = <Widget>[];

    // Staff actions: Start, Complete, Fail
    if (item.canManage) {
      if (item.status == FulfillmentStatus.ready) {
        buttons.add(
          ElevatedButton.icon(
            icon: const Icon(Icons.play_arrow),
            label: const Text('Start Fulfillment'),
            onPressed: _handleStart,
          ),
        );
      } else if (item.status == FulfillmentStatus.inProgress) {
        buttons.add(
          ElevatedButton.icon(
            icon: const Icon(Icons.check_circle),
            label: const Text('Complete Fulfillment'),
            style: ElevatedButton.styleFrom(backgroundColor: Colors.green),
            onPressed: _handleComplete,
          ),
        );
        buttons.add(const SizedBox(height: 8));
        buttons.add(
          OutlinedButton.icon(
            icon: const Icon(Icons.error_outline),
            label: const Text('Fail Fulfillment'),
            style: OutlinedButton.styleFrom(foregroundColor: Colors.red),
            onPressed: _promptFail,
          ),
        );
      }
    }

    // Cancellation: available in READY status for creator, requester, or staff
    if (item.status == FulfillmentStatus.ready &&
        (item.canManage || item.isRequester)) {
      if (buttons.isNotEmpty) {
        buttons.add(const SizedBox(height: 8));
      }
      buttons.add(
        OutlinedButton.icon(
          icon: const Icon(Icons.cancel_outlined),
          label: const Text('Cancel Fulfillment'),
          onPressed: _promptCancel,
        ),
      );
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: buttons,
    );
  }
}
