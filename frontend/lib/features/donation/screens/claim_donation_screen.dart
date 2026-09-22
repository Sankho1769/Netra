import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../models/donation_model.dart';
import '../services/donation_api_service.dart';

/// Screen allowing an authenticated donor to submit a donation claim for verification.
class ClaimDonationScreen extends StatefulWidget {
  final DonationApiService? apiService;
  final String? initialBloodRequestId;
  final String? initialDonationEventId;

  const ClaimDonationScreen({
    super.key,
    this.apiService,
    this.initialBloodRequestId,
    this.initialDonationEventId,
  });

  @override
  State<ClaimDonationScreen> createState() => _ClaimDonationScreenState();
}

class _ClaimDonationScreenState extends State<ClaimDonationScreen> {
  final _formKey = GlobalKey<FormState>();
  late final DonationApiService _apiService;

  late DonationSourceType _sourceType;
  final _referenceIdController = TextEditingController();
  final _notesController = TextEditingController();
  DateTime _selectedDate = DateTime.now();

  bool _isSubmitting = false;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _apiService = widget.apiService ?? DonationApiService();

    if (widget.initialDonationEventId != null) {
      _sourceType = DonationSourceType.donationEvent;
      _referenceIdController.text = widget.initialDonationEventId!;
    } else {
      _sourceType = DonationSourceType.bloodRequest;
      if (widget.initialBloodRequestId != null) {
        _referenceIdController.text = widget.initialBloodRequestId!;
      }
    }
  }

  @override
  void dispose() {
    _referenceIdController.dispose();
    _notesController.dispose();
    super.dispose();
  }

  Future<void> _pickDate() async {
    final now = DateTime.now();
    final firstDate = now.subtract(const Duration(days: 90));

    final picked = await showDatePicker(
      context: context,
      initialDate: _selectedDate.isBefore(firstDate) ? now : _selectedDate,
      firstDate: firstDate,
      lastDate: now,
    );

    if (picked != null) {
      setState(() {
        _selectedDate = picked;
      });
    }
  }

  Future<void> _submitClaim() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() {
      _isSubmitting = true;
      _errorMessage = null;
    });

    final refId = _referenceIdController.text.trim();
    final dto = CreateDonationClaimDto(
      sourceType: _sourceType,
      bloodRequestId:
          _sourceType == DonationSourceType.bloodRequest ? refId : null,
      donationEventId:
          _sourceType == DonationSourceType.donationEvent ? refId : null,
      donationDate: _selectedDate,
      notes: _notesController.text.trim().isNotEmpty
          ? _notesController.text.trim()
          : null,
    );

    try {
      final created = await _apiService.submitClaim(dto);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content:
                Text('Donation claim submitted for clinical verification.'),
            backgroundColor: Colors.green,
          ),
        );
        Navigator.pop(context, created);
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _errorMessage = e.toString().replaceFirst('Exception: ', '');
          _isSubmitting = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final dateFormat = DateFormat('dd MMMM yyyy');

    return Scaffold(
      appBar: AppBar(
        title: const Text('Claim Donation'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              if (_errorMessage != null) ...[
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: Colors.red.shade50,
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(color: Colors.red.shade300),
                  ),
                  child: Row(
                    children: [
                      Icon(Icons.error_outline, color: Colors.red.shade700),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Text(
                          _errorMessage!,
                          style: TextStyle(color: Colors.red.shade900),
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 16),
              ],

              // Section: Source Type
              const Text(
                'Donation Source',
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 8),
              SegmentedButton<DonationSourceType>(
                segments: const [
                  ButtonSegment(
                    value: DonationSourceType.bloodRequest,
                    label: Text('Blood Request'),
                    icon: Icon(Icons.water_drop_outlined),
                  ),
                  ButtonSegment(
                    value: DonationSourceType.donationEvent,
                    label: Text('Donation Camp'),
                    icon: Icon(Icons.campaign_outlined),
                  ),
                ],
                selected: {_sourceType},
                onSelectionChanged: (set) {
                  setState(() {
                    _sourceType = set.first;
                    _referenceIdController.clear();
                  });
                },
              ),
              const SizedBox(height: 20),

              // Section: Reference ID
              Text(
                _sourceType == DonationSourceType.bloodRequest
                    ? 'Blood Request ID'
                    : 'Donation Camp ID',
                style:
                    const TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 4),
              Text(
                _sourceType == DonationSourceType.bloodRequest
                    ? 'Enter the UUID of the fulfilled request you accepted.'
                    : 'Enter the UUID of the camp where you donated.',
                style: TextStyle(fontSize: 12, color: Colors.grey.shade600),
              ),
              const SizedBox(height: 8),
              TextFormField(
                controller: _referenceIdController,
                decoration: InputDecoration(
                  hintText: 'e.g. 550e8400-e29b-41d4-a716-446655440000',
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(10),
                  ),
                  prefixIcon: const Icon(Icons.tag_rounded),
                ),
                validator: (val) {
                  if (val == null || val.trim().isEmpty) {
                    return 'Please enter the reference ID';
                  }
                  return null;
                },
              ),
              const SizedBox(height: 20),

              // Section: Donation Date
              const Text(
                'Donation Date',
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 8),
              InkWell(
                onTap: _pickDate,
                borderRadius: BorderRadius.circular(10),
                child: Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
                  decoration: BoxDecoration(
                    border: Border.all(color: Colors.grey.shade400),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Row(
                        children: [
                          Icon(Icons.calendar_today,
                              size: 18, color: Colors.red.shade700),
                          const SizedBox(width: 10),
                          Text(
                            dateFormat.format(_selectedDate),
                            style: const TextStyle(
                                fontSize: 15, fontWeight: FontWeight.w500),
                          ),
                        ],
                      ),
                      const Icon(Icons.arrow_drop_down),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 20),

              // Section: Optional Notes
              const Text(
                'Notes (Optional)',
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 8),
              TextFormField(
                controller: _notesController,
                maxLines: 3,
                maxLength: 500,
                decoration: InputDecoration(
                  hintText: 'Any additional details about your donation...',
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(10),
                  ),
                ),
              ),
              const SizedBox(height: 24),

              // Submit Button
              SizedBox(
                width: double.infinity,
                height: 48,
                child: ElevatedButton(
                  onPressed: _isSubmitting ? null : _submitClaim,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.red.shade700,
                    foregroundColor: Colors.white,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(10),
                    ),
                  ),
                  child: _isSubmitting
                      ? const SizedBox(
                          height: 20,
                          width: 20,
                          child: CircularProgressIndicator(
                            strokeWidth: 2,
                            valueColor:
                                AlwaysStoppedAnimation<Color>(Colors.white),
                          ),
                        )
                      : const Text(
                          'Submit Claim for Verification',
                          style: TextStyle(
                              fontSize: 16, fontWeight: FontWeight.bold),
                        ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
