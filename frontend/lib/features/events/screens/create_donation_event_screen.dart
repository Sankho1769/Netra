import 'package:flutter/material.dart';
import '../../../core/location/location_service.dart';
import '../../../core/responsive/responsive_breakpoints.dart';
import '../../../core/responsive/responsive_scaffold.dart';
import '../../../core/theme/netra_colors.dart';
import '../../../core/theme/netra_spacing.dart';
import '../../../core/theme/netra_typography.dart';
import '../../../common/widgets/netra_button.dart';
import '../../../common/widgets/netra_text_field.dart';
import '../../blood_request/widgets/hospital_search_field.dart';
import '../state/donation_event_controller.dart';
import 'donation_event_details_screen.dart';

class CreateDonationEventScreen extends StatefulWidget {
  final String? initialBloodBankId;
  final DonationEventController? controller;

  const CreateDonationEventScreen({
    super.key,
    this.initialBloodBankId,
    this.controller,
  });

  @override
  State<CreateDonationEventScreen> createState() =>
      _CreateDonationEventScreenState();
}

class _CreateDonationEventScreenState extends State<CreateDonationEventScreen> {
  final _formKey = GlobalKey<FormState>();
  late final DonationEventController _controller;

  // Form field controllers
  late final TextEditingController _bloodBankIdController;
  final TextEditingController _titleController = TextEditingController();
  final TextEditingController _descriptionController = TextEditingController();
  final TextEditingController _venueNameController = TextEditingController();
  final TextEditingController _addressController = TextEditingController();
  final TextEditingController _cityController = TextEditingController();
  final TextEditingController _stateController = TextEditingController();
  final TextEditingController _postalCodeController = TextEditingController();
  final TextEditingController _capacityController =
      TextEditingController(text: '50');

  // Coordinated venue location
  double? _latitude;
  double? _longitude;
  bool _isLocating = false;
  final LocationService _locationService = DefaultLocationService();

  // Date and time state
  DateTime _startAt = DateTime.now().add(const Duration(days: 7, hours: 9));
  DateTime _endAt = DateTime.now().add(const Duration(days: 7, hours: 17));
  DateTime _regOpenAt = DateTime.now();
  DateTime _regCloseAt = DateTime.now().add(const Duration(days: 7, hours: 8));

  bool _submitForReviewImmediately = false;

  @override
  void initState() {
    super.initState();
    _controller = widget.controller ?? DonationEventController();
    _bloodBankIdController =
        TextEditingController(text: widget.initialBloodBankId ?? '');
  }

  @override
  void dispose() {
    _bloodBankIdController.dispose();
    _titleController.dispose();
    _descriptionController.dispose();
    _venueNameController.dispose();
    _addressController.dispose();
    _cityController.dispose();
    _stateController.dispose();
    _postalCodeController.dispose();
    _capacityController.dispose();
    if (widget.controller == null) {
      _controller.dispose();
    }
    super.dispose();
  }

  Future<DateTime?> _pickDateTime(DateTime initialDate) async {
    final date = await showDatePicker(
      context: context,
      initialDate: initialDate,
      firstDate: DateTime.now().subtract(const Duration(days: 1)),
      lastDate: DateTime.now().add(const Duration(days: 365)),
    );
    if (date == null || !mounted) return null;

    final time = await showTimePicker(
      context: context,
      initialTime: TimeOfDay.fromDateTime(initialDate),
    );
    if (time == null) return null;

    return DateTime(date.year, date.month, date.day, time.hour, time.minute);
  }

  Future<void> _detectLocation() async {
    setState(() => _isLocating = true);
    try {
      final result =
          await _locationService.getDetailedLocation(approximateOnly: false);
      if (result.isSuccess && result.location != null && mounted) {
        final loc = result.location!;
        setState(() {
          if (loc.city != null && _cityController.text.trim().isEmpty) {
            _cityController.text = loc.city!;
          }
          if (loc.state != null && _stateController.text.trim().isEmpty) {
            _stateController.text = loc.state!;
          }
          if (loc.postalCode != null &&
              _postalCodeController.text.trim().isEmpty) {
            _postalCodeController.text = loc.postalCode!;
          }
          if (loc.latitude != null) _latitude = loc.latitude;
          if (loc.longitude != null) _longitude = loc.longitude;
        });

        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Venue location coordinates detected successfully.'),
            backgroundColor: Color(0xFF16A34A),
          ),
        );
      } else if (mounted) {
        if (result.isPermissionPermanentlyDenied) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: const Text(
                  'Location permission permanently denied. Please grant permission in App Settings.'),
              backgroundColor: const Color(0xFFDC2626),
              action: SnackBarAction(
                label: 'Settings',
                textColor: Colors.white,
                onPressed: () => _locationService.openAppSettings(),
              ),
            ),
          );
        } else if (result.isServiceDisabled) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: const Text(
                  'Device location is turned off. Please enable GPS in device settings.'),
              backgroundColor: const Color(0xFFD97706),
              action: SnackBarAction(
                label: 'Turn On',
                textColor: Colors.white,
                onPressed: () => _locationService.openLocationSettings(),
              ),
            ),
          );
        } else if (result.isPermissionDenied) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: const Text(
                  'Location permission was denied. Tap Retry to request again.'),
              backgroundColor: const Color(0xFFD97706),
              action: SnackBarAction(
                label: 'Retry',
                textColor: Colors.white,
                onPressed: _detectLocation,
              ),
            ),
          );
        } else {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(result.errorMessage ??
                  'Location unavailable. Please select a verified venue.'),
              backgroundColor: const Color(0xFFD97706),
              action: SnackBarAction(
                label: 'Retry',
                textColor: Colors.white,
                onPressed: _detectLocation,
              ),
            ),
          );
        }
      }
    } finally {
      if (mounted) setState(() => _isLocating = false);
    }
  }

  Future<void> _handleCreate() async {
    _controller.clearMessages();

    if (_bloodBankIdController.text.trim().isEmpty) {
      _showError('Blood Bank ID is required');
      return;
    }
    if (_titleController.text.trim().isEmpty) {
      _showError('Camp Title is required');
      return;
    }
    if (_venueNameController.text.trim().isEmpty) {
      _showError('Venue Name is required');
      return;
    }
    if (_addressController.text.trim().isEmpty) {
      _showError('Address is required');
      return;
    }
    if (_cityController.text.trim().isEmpty) {
      _showError('City is required');
      return;
    }
    if (_stateController.text.trim().isEmpty) {
      _showError('State is required');
      return;
    }
    if (_postalCodeController.text.trim().isEmpty) {
      _showError('Postal code is required');
      return;
    }

    if (_latitude == null || _longitude == null) {
      _showError(
          'Please select a verified venue or tap "Detect Location" to establish venue coordinates');
      return;
    }

    final capacity = int.tryParse(_capacityController.text.trim());
    if (capacity == null || capacity < 1) {
      _showError('Capacity must be at least 1');
      return;
    }

    // Time validation
    if (!_startAt.isBefore(_endAt)) {
      _showError('Event start time must be before event end time');
      return;
    }
    if (!_regOpenAt.isBefore(_regCloseAt)) {
      _showError(
          'Registration open time must be before registration close time');
      return;
    }
    if (_regCloseAt.isAfter(_endAt)) {
      _showError('Registration cannot close after the event ends');
      return;
    }

    final payload = {
      'bloodBankId': _bloodBankIdController.text.trim(),
      'title': _titleController.text.trim(),
      'description': _descriptionController.text.trim().isNotEmpty
          ? _descriptionController.text.trim()
          : null,
      'venueName': _venueNameController.text.trim(),
      'address': _addressController.text.trim(),
      'city': _cityController.text.trim(),
      'state': _stateController.text.trim(),
      'postalCode': _postalCodeController.text.trim(),
      'latitude': _latitude!,
      'longitude': _longitude!,
      'startAt': _startAt.toUtc().toIso8601String(),
      'endAt': _endAt.toUtc().toIso8601String(),
      'registrationOpenAt': _regOpenAt.toUtc().toIso8601String(),
      'registrationCloseAt': _regCloseAt.toUtc().toIso8601String(),
      'donorCapacity': capacity,
    };

    final created = await _controller.createEvent(payload);
    if (!mounted) return;

    if (created != null) {
      if (_submitForReviewImmediately) {
        await _controller.submitEvent(created.id);
      }

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              _submitForReviewImmediately
                  ? 'Camp created and submitted for admin review!'
                  : 'Donation camp draft created successfully!',
            ),
            backgroundColor: NetraColors.successGreen,
          ),
        );

        Navigator.pushReplacement(
          context,
          MaterialPageRoute(
            builder: (_) => DonationEventDetailsScreen(eventId: created.id),
          ),
        );
      }
    } else {
      _showError(_controller.errorMessage ?? 'Failed to create camp');
    }
  }

  void _showError(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: NetraColors.errorRed,
      ),
    );
  }

  String _formatDateTime(DateTime dt) {
    return '${dt.day.toString().padLeft(2, '0')}/${dt.month.toString().padLeft(2, '0')}/${dt.year} '
        '${dt.hour.toString().padLeft(2, '0')}:${dt.minute.toString().padLeft(2, '0')}';
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _controller,
      builder: (context, _) {
        final screenWidth = MediaQuery.of(context).size.width;
        final isDesktop = screenWidth >= ResponsiveBreakpoints.tablet;

        return ResponsiveScaffold(
          title: 'Create Donation Camp',
          body: SingleChildScrollView(
            padding: EdgeInsets.symmetric(
              horizontal: isDesktop ? 48.0 : 16.0,
              vertical: 24.0,
            ),
            child: Center(
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 720),
                child: Card(
                  elevation: 2,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(NetraSpacing.radiusLg),
                    side: const BorderSide(color: NetraColors.borderGray),
                  ),
                  child: Padding(
                    padding: const EdgeInsets.all(24.0),
                    child: Form(
                      key: _formKey,
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          Text(
                            'Organize a Blood Donation Camp',
                            style: NetraTypography.headlineSmall.copyWith(
                              color: NetraColors.textPrimary,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                          const SizedBox(height: 6),
                          Text(
                            'Enter the camp schedule, venue, and capacity details. Once submitted, '
                            'administrators review and publish camps for donor discovery.',
                            style: NetraTypography.bodyMedium.copyWith(
                              color: NetraColors.textSecondary,
                            ),
                          ),
                          const Divider(height: 32),

                          // Blood Bank ID
                          NetraTextField(
                            label: 'Blood Bank ID *',
                            hint: 'e.g. 550e8400-e29b-41d4-a716-446655440000',
                            controller: _bloodBankIdController,
                            helperText: 'UUID of your authorized blood bank',
                          ),
                          const SizedBox(height: 16),

                          // Camp Title
                          NetraTextField(
                            label: 'Camp Title *',
                            hint: 'e.g. Annual Community Blood Donation Drive',
                            controller: _titleController,
                          ),
                          const SizedBox(height: 16),

                          // Description
                          NetraTextField(
                            label: 'Description',
                            hint:
                                'Information about the drive, partners, amenities provided...',
                            controller: _descriptionController,
                            maxLines: 3,
                          ),
                          const SizedBox(height: 24),

                          // Venue Details
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Text(
                                'Venue & Location',
                                style: NetraTypography.titleMedium.copyWith(
                                  fontWeight: FontWeight.bold,
                                  color: NetraColors.primaryRed,
                                ),
                              ),
                              TextButton.icon(
                                onPressed: _isLocating ? null : _detectLocation,
                                icon: _isLocating
                                    ? const SizedBox(
                                        width: 14,
                                        height: 14,
                                        child: CircularProgressIndicator(
                                            strokeWidth: 2),
                                      )
                                    : const Icon(Icons.my_location_rounded,
                                        size: 16),
                                label: const Text('Detect Location',
                                    style: TextStyle(fontSize: 12)),
                              ),
                            ],
                          ),
                          const SizedBox(height: 12),

                          HospitalSearchField(
                            controller: _venueNameController,
                            onHospitalSelected: (hospital) {
                              setState(() {
                                if (hospital != null) {
                                  _venueNameController.text = hospital.name;
                                  _addressController.text = hospital.address;
                                  _cityController.text = hospital.city;
                                  _stateController.text = hospital.state;
                                  if (hospital.postalCode != null) {
                                    _postalCodeController.text =
                                        hospital.postalCode!;
                                  }
                                  _latitude = hospital.latitude;
                                  _longitude = hospital.longitude;
                                } else {
                                  _latitude = null;
                                  _longitude = null;
                                }
                              });
                            },
                          ),
                          const SizedBox(height: 16),

                          NetraTextField(
                            label: 'Street Address *',
                            hint: 'e.g. 102 Sector 4, Hospital Road',
                            controller: _addressController,
                          ),
                          const SizedBox(height: 16),

                          Row(
                            children: [
                              Expanded(
                                flex: 2,
                                child: NetraTextField(
                                  label: 'City *',
                                  hint: 'e.g. Kolkata, Delhi',
                                  controller: _cityController,
                                ),
                              ),
                              const SizedBox(width: 12),
                              Expanded(
                                flex: 2,
                                child: NetraTextField(
                                  label: 'State *',
                                  hint: 'e.g. West Bengal',
                                  controller: _stateController,
                                ),
                              ),
                              const SizedBox(width: 12),
                              Expanded(
                                flex: 1,
                                child: NetraTextField(
                                  label: 'PIN *',
                                  hint: 'e.g. 700020',
                                  controller: _postalCodeController,
                                  keyboardType: TextInputType.number,
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 12),

                          // Verified Coordinates Status Badge (Zero manual typing)
                          Container(
                            padding: const EdgeInsets.symmetric(
                                horizontal: 12, vertical: 8),
                            decoration: BoxDecoration(
                              color: _latitude != null && _longitude != null
                                  ? const Color(0xFFF0FDF4)
                                  : const Color(0xFFF8FAFC),
                              borderRadius: BorderRadius.circular(8),
                              border: Border.all(
                                color: _latitude != null && _longitude != null
                                    ? const Color(0xFF86EFAC)
                                    : const Color(0xFFCBD5E1),
                              ),
                            ),
                            child: Row(
                              children: [
                                Icon(
                                  _latitude != null && _longitude != null
                                      ? Icons.check_circle_outline
                                      : Icons.info_outline,
                                  size: 16,
                                  color: _latitude != null && _longitude != null
                                      ? const Color(0xFF16A34A)
                                      : const Color(0xFF64748B),
                                ),
                                const SizedBox(width: 8),
                                Expanded(
                                  child: Text(
                                    _latitude != null && _longitude != null
                                        ? 'Verified coordinates attached (${_latitude!.toStringAsFixed(3)}, ${_longitude!.toStringAsFixed(3)})'
                                        : 'Search a verified venue above or tap "Detect Location" to attach GPS coordinates.',
                                    style: TextStyle(
                                      fontSize: 12,
                                      color: _latitude != null &&
                                              _longitude != null
                                          ? const Color(0xFF16A34A)
                                          : const Color(0xFF64748B),
                                    ),
                                  ),
                                ),
                                if (_latitude != null && _longitude != null)
                                  GestureDetector(
                                    onTap: () => setState(() {
                                      _latitude = null;
                                      _longitude = null;
                                    }),
                                    child: const Icon(Icons.close,
                                        size: 14, color: Color(0xFF64748B)),
                                  ),
                              ],
                            ),
                          ),
                          const SizedBox(height: 24),

                          // Schedule & Capacity
                          Text(
                            'Schedule & Capacity',
                            style: NetraTypography.titleMedium.copyWith(
                              fontWeight: FontWeight.bold,
                              color: NetraColors.primaryRed,
                            ),
                          ),
                          const SizedBox(height: 12),

                          // Event Start / End Pickers
                          Row(
                            children: [
                              Expanded(
                                child: _buildDateTimePickerTile(
                                  label: 'Camp Starts',
                                  value: _startAt,
                                  onTap: () async {
                                    final picked =
                                        await _pickDateTime(_startAt);
                                    if (picked != null)
                                      setState(() => _startAt = picked);
                                  },
                                ),
                              ),
                              const SizedBox(width: 12),
                              Expanded(
                                child: _buildDateTimePickerTile(
                                  label: 'Camp Ends',
                                  value: _endAt,
                                  onTap: () async {
                                    final picked = await _pickDateTime(_endAt);
                                    if (picked != null)
                                      setState(() => _endAt = picked);
                                  },
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 12),

                          // Reg Open / Close Pickers
                          Row(
                            children: [
                              Expanded(
                                child: _buildDateTimePickerTile(
                                  label: 'Registration Opens',
                                  value: _regOpenAt,
                                  onTap: () async {
                                    final picked =
                                        await _pickDateTime(_regOpenAt);
                                    if (picked != null)
                                      setState(() => _regOpenAt = picked);
                                  },
                                ),
                              ),
                              const SizedBox(width: 12),
                              Expanded(
                                child: _buildDateTimePickerTile(
                                  label: 'Registration Closes',
                                  value: _regCloseAt,
                                  onTap: () async {
                                    final picked =
                                        await _pickDateTime(_regCloseAt);
                                    if (picked != null)
                                      setState(() => _regCloseAt = picked);
                                  },
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 16),

                          NetraTextField(
                            label: 'Donor Capacity (Max Slots) *',
                            controller: _capacityController,
                            keyboardType: TextInputType.number,
                            helperText:
                                'Registration stops automatically when capacity is reached',
                          ),
                          const SizedBox(height: 16),

                          // Submit immediately option
                          CheckboxListTile(
                            contentPadding: EdgeInsets.zero,
                            title: const Text(
                                'Submit for admin review immediately after creation'),
                            subtitle: const Text(
                                'If unchecked, camp remains in DRAFT state'),
                            value: _submitForReviewImmediately,
                            onChanged: (val) {
                              setState(() =>
                                  _submitForReviewImmediately = val ?? false);
                            },
                          ),
                          const SizedBox(height: 24),

                          NetraButton(
                            text: 'Create Camp',
                            isLoading: _controller.isActionLoading,
                            icon: Icons.add_circle_outline,
                            onPressed: _controller.isActionLoading
                                ? null
                                : _handleCreate,
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ),
        );
      },
    );
  }

  Widget _buildDateTimePickerTile({
    required String label,
    required DateTime value,
    required VoidCallback onTap,
  }) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
        decoration: BoxDecoration(
          color: NetraColors.surfaceWhite,
          border: Border.all(color: NetraColors.borderGray),
          borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              label,
              style: NetraTypography.labelSmall
                  .copyWith(color: NetraColors.textSecondary),
            ),
            const SizedBox(height: 4),
            Row(
              children: [
                const Icon(Icons.calendar_today,
                    size: 16, color: NetraColors.primaryRed),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    _formatDateTime(value),
                    style: NetraTypography.bodyMedium
                        .copyWith(fontWeight: FontWeight.w600),
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
