import 'package:flutter/material.dart';
import '../../../core/location/location_service.dart';
import '../../../core/responsive/responsive_breakpoints.dart';
import '../../../core/responsive/responsive_scaffold.dart';
import '../../../core/theme/netra_colors.dart';
import '../../../core/theme/netra_spacing.dart';
import '../../../core/theme/netra_typography.dart';
import '../../../common/widgets/netra_button.dart';
import '../../../common/widgets/netra_text_field.dart';
import '../../bloodbank/models/blood_bank.dart';
import '../../bloodbank/services/bloodbank_api_service.dart';
import '../../auth/state/auth_scope.dart';
import '../state/donation_event_controller.dart';
import 'donation_event_details_screen.dart';

enum LocationSource {
  none,
  deviceGps,
  verifiedBloodCenter,
  manual,
}

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

  // Form field controllers - Start completely empty (No fake/default business data)
  late final TextEditingController _bloodBankIdController;
  final TextEditingController _titleController = TextEditingController();
  final TextEditingController _descriptionController = TextEditingController();
  final TextEditingController _venueNameController = TextEditingController();
  final TextEditingController _addressController = TextEditingController();
  final TextEditingController _cityController = TextEditingController();
  final TextEditingController _stateController = TextEditingController();
  final TextEditingController _postalCodeController = TextEditingController();
  final TextEditingController _capacityController = TextEditingController();

  // Coordinated venue location
  double? _latitude;
  double? _longitude;
  LocationSource _locationSource = LocationSource.none;
  bool _isLocating = false;
  final LocationService _locationService = DefaultLocationService();

  // Date and time state - Explicitly chosen by user (starts null)
  DateTime? _startAt;
  DateTime? _endAt;
  DateTime? _regOpenAt;
  DateTime? _regCloseAt;

  bool _submitForReviewImmediately = false;

  List<BloodBankSummary> _availableBloodBanks = [];
  bool _isLoadingBloodBanks = true;
  BloodBankSummary? _selectedBloodBank;

  @override
  void initState() {
    super.initState();
    _controller = widget.controller ?? DonationEventController();
    _bloodBankIdController =
        TextEditingController(text: widget.initialBloodBankId ?? '');
    _titleController.addListener(_onFormFieldChanged);
    _venueNameController.addListener(_onFormFieldChanged);
    _addressController.addListener(_onFormFieldChanged);
    _cityController.addListener(_onFormFieldChanged);
    _stateController.addListener(_onFormFieldChanged);
    _postalCodeController.addListener(_onFormFieldChanged);
    _capacityController.addListener(_onFormFieldChanged);
    _loadAvailableBloodBanks();
  }

  void _onFormFieldChanged() {
    if (mounted) setState(() {});
  }

  Future<void> _loadAvailableBloodBanks() async {
    try {
      final api = BloodBankApiService();
      List<BloodBankSummary> banks = [];
      try {
        banks = await api.getManagedBloodBanks();
      } catch (_) {
        // Fallback for admin or unlinked
      }
      if (banks.isEmpty) {
        try {
          banks = await api.discoverBloodBanks(size: 50);
        } catch (_) {}
      }

      if (mounted) {
        final uniqueBanks = {for (final b in banks) b.id: b}.values.toList();
        setState(() {
          _availableBloodBanks = uniqueBanks;
          _isLoadingBloodBanks = false;
          if (_bloodBankIdController.text.isNotEmpty) {
            final match = uniqueBanks.where((b) => b.id == _bloodBankIdController.text);
            if (match.isNotEmpty) {
              _selectedBloodBank = match.first;
            }
          }
          // Intentionally DO NOT auto-fill title, venue, address, or dates!
          // Fresh form starts completely clean.
        });
      }
    } catch (_) {
      if (mounted) setState(() => _isLoadingBloodBanks = false);
    }
  }

  @override
  void dispose() {
    _titleController.removeListener(_onFormFieldChanged);
    _venueNameController.removeListener(_onFormFieldChanged);
    _addressController.removeListener(_onFormFieldChanged);
    _cityController.removeListener(_onFormFieldChanged);
    _stateController.removeListener(_onFormFieldChanged);
    _postalCodeController.removeListener(_onFormFieldChanged);
    _capacityController.removeListener(_onFormFieldChanged);
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

  Future<DateTime?> _pickDateTime(DateTime? currentValue) async {
    final now = DateTime.now();
    final initialDate = currentValue ?? now;
    final date = await showDatePicker(
      context: context,
      initialDate: initialDate.isBefore(now) ? now : initialDate,
      firstDate: now.subtract(const Duration(days: 1)),
      lastDate: now.add(const Duration(days: 365)),
    );
    if (date == null || !mounted) return null;

    final initialTime = currentValue != null
        ? TimeOfDay.fromDateTime(currentValue)
        : const TimeOfDay(hour: 9, minute: 0);

    final time = await showTimePicker(
      context: context,
      initialTime: initialTime,
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
          if (loc.address != null && loc.address!.trim().isNotEmpty) {
            _addressController.text = loc.address!.trim();
          }
          if (loc.city != null && loc.city!.trim().isNotEmpty) {
            _cityController.text = loc.city!.trim();
          }
          if (loc.state != null && loc.state!.trim().isNotEmpty) {
            _stateController.text = loc.state!.trim();
          }
          if (loc.postalCode != null && loc.postalCode!.trim().isNotEmpty) {
            _postalCodeController.text = loc.postalCode!.trim();
          }
          _latitude = loc.latitude;
          _longitude = loc.longitude;
          _locationSource = LocationSource.deviceGps;
        });

        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Row(
              children: [
                Icon(Icons.check_circle, color: Colors.white, size: 18),
                SizedBox(width: 8),
                Text('Location detected from device GPS.'),
              ],
            ),
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
                  'Device GPS is turned off. Please enable location services in device settings.'),
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
                  'Location unavailable. Please enter address manually.'),
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

  void _applyVerifiedBloodCenterLocation(BloodBankSummary bank) {
    setState(() {
      _selectedBloodBank = bank;
      _bloodBankIdController.text = bank.id;
      _venueNameController.text = bank.name;
      _addressController.text = bank.address;
      _cityController.text = bank.city;
      _stateController.text = bank.state;
      _postalCodeController.text = bank.postalCode;
      _latitude = bank.latitude;
      _longitude = bank.longitude;
      _locationSource = LocationSource.verifiedBloodCenter;
    });

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('Applied verified address and coordinates for ${bank.name}.'),
        backgroundColor: const Color(0xFF2563EB),
      ),
    );
  }

  Future<void> _handleCreate() async {
    _controller.clearMessages();

    final user = AuthScope.maybeOf(context)?.currentUser;
    if (user != null && !user.isAdmin && !user.isBloodBank) {
      _showError('Only authorized blood-bank organizations and approved administrators can create donation camps.');
      return;
    }

    if (_bloodBankIdController.text.trim().isEmpty) {
      _showError('Please select an authorized partner blood centre.');
      return;
    }
    if (_titleController.text.trim().isEmpty) {
      _showError('Camp title is required.');
      return;
    }
    if (_venueNameController.text.trim().isEmpty) {
      _showError('Venue name is required.');
      return;
    }
    if (_addressController.text.trim().isEmpty) {
      _showError('Street address is required.');
      return;
    }
    if (_cityController.text.trim().isEmpty) {
      _showError('City is required.');
      return;
    }
    if (_stateController.text.trim().isEmpty) {
      _showError('State is required.');
      return;
    }
    if (_postalCodeController.text.trim().isEmpty) {
      _showError('Postal code (PIN) is required.');
      return;
    }

    if (_latitude == null || _longitude == null) {
      _showError('Venue coordinates are required. Tap "Detect Location" to attach GPS or select a verified blood centre.');
      return;
    }

    if (_capacityController.text.trim().isEmpty) {
      _showError('Please enter maximum donor capacity.');
      return;
    }
    final capacity = int.tryParse(_capacityController.text.trim());
    if (capacity == null || capacity < 1) {
      _showError('Donor capacity must be a positive number of at least 1.');
      return;
    }

    // Explicit date selection validations
    if (_regOpenAt == null) {
      _showError('Please select Registration Open date and time.');
      return;
    }
    if (_regCloseAt == null) {
      _showError('Please select Registration Close date and time.');
      return;
    }
    if (_startAt == null) {
      _showError('Please select Camp Start date and time.');
      return;
    }
    if (_endAt == null) {
      _showError('Please select Camp End date and time.');
      return;
    }

    if (!_regOpenAt!.isBefore(_regCloseAt!)) {
      _showError('Registration open time must be before registration close time.');
      return;
    }
    if (_regCloseAt!.isAfter(_startAt!)) {
      _showError('Registration must close before or when the camp starts.');
      return;
    }
    if (!_startAt!.isBefore(_endAt!)) {
      _showError('Camp start time must be before camp end time.');
      return;
    }
    if (_endAt!.isBefore(DateTime.now())) {
      _showError('Camp end time cannot be in the past.');
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
      'startAt': _startAt!.toUtc().toIso8601String(),
      'endAt': _endAt!.toUtc().toIso8601String(),
      'registrationOpenAt': _regOpenAt!.toUtc().toIso8601String(),
      'registrationCloseAt': _regCloseAt!.toUtc().toIso8601String(),
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
    String displayMsg = message;
    if (message.contains('403') ||
        message.toLowerCase().contains('forbidden') ||
        message.toLowerCase().contains('access denied')) {
      displayMsg =
          'Permission Denied (403): Only licensed blood banks and administrators are authorized to publish donation camps under NBTC guidelines.';
    }
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(displayMsg),
        backgroundColor: NetraColors.errorRed,
      ),
    );
  }

  String _formatDateTime(DateTime dt) {
    return '${dt.day.toString().padLeft(2, '0')}/${dt.month.toString().padLeft(2, '0')}/${dt.year} '
        '${dt.hour.toString().padLeft(2, '0')}:${dt.minute.toString().padLeft(2, '0')}';
  }

  void _showSupportInfo(BuildContext context) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Row(
          children: [
            Icon(Icons.support_agent, color: NetraColors.primaryRed),
            SizedBox(width: 8),
            Text('Organizer Support', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          ],
        ),
        content: const Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Are you a licensed blood bank representative or organizing an institutional blood drive?',
              style: TextStyle(fontSize: 13, height: 1.4),
            ),
            SizedBox(height: 12),
            Text('• Email: support@netra.org', style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600)),
            SizedBox(height: 4),
            Text('• Verification Desk: 1800-NETRA-HELP', style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600)),
            SizedBox(height: 8),
            Text(
              'Under NBTC clinical standards, all drives require accredited oversight before scheduling.',
              style: TextStyle(fontSize: 12, color: Color(0xFF64748B)),
            ),
          ],
        ),
        actions: [
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: NetraColors.primaryRed),
            onPressed: () => Navigator.pop(ctx),
            child: const Text('Close', style: TextStyle(color: Colors.white)),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final user = AuthScope.maybeOf(context)?.currentUser;
    final bool isAuthorized = user != null && (user.isAdmin || user.isBloodBank);

    if (!isAuthorized) {
      return _buildUnauthorizedScreen(context);
    }

    final screenWidth = MediaQuery.of(context).size.width;
    final isDesktop = screenWidth >= ResponsiveBreakpoints.tablet;

    return ResponsiveScaffold(
      title: 'Create Donation Camp',
      body: SingleChildScrollView(
        padding: EdgeInsets.symmetric(
          horizontal: isDesktop ? 48.0 : 16.0,
          vertical: 20.0,
        ),
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

                          // Authorization Badge
                          Container(
                            margin: const EdgeInsets.only(bottom: 20),
                            padding: const EdgeInsets.all(12),
                            decoration: BoxDecoration(
                              color: const Color(0xFFF0FDF4),
                              borderRadius: BorderRadius.circular(10),
                              border: Border.all(color: const Color(0xFF86EFAC)),
                            ),
                            child: Row(
                              children: [
                                const Icon(Icons.verified_user, color: Color(0xFF16A34A), size: 20),
                                const SizedBox(width: 10),
                                Expanded(
                                  child: Text(
                                    'Authorized Host: ${user.fullName.isNotEmpty ? user.fullName : user.email} (${user.roles.where((r) => r.contains('ADMIN') || r.contains('BLOODBANK')).join(', ')})',
                                    style: const TextStyle(
                                      color: Color(0xFF166534),
                                      fontSize: 13,
                                      fontWeight: FontWeight.w600,
                                    ),
                                  ),
                                ),
                              ],
                            ),
                          ),

                          // 1. Blood Center Selection
                          if (_isLoadingBloodBanks) ...[
                            const Padding(
                              padding: EdgeInsets.symmetric(vertical: 12),
                              child: Row(
                                children: [
                                  SizedBox(
                                    width: 18,
                                    height: 18,
                                    child: CircularProgressIndicator(strokeWidth: 2),
                                  ),
                                  SizedBox(width: 12),
                                  Text('Loading verified blood centres...'),
                                ],
                              ),
                            ),
                          ] else if (_availableBloodBanks.isNotEmpty) ...[
                            Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  'Partner Blood Centre *',
                                  style: NetraTypography.labelMedium.copyWith(
                                    color: NetraColors.textPrimary,
                                    fontWeight: FontWeight.w600,
                                  ),
                                ),
                                const SizedBox(height: 6),
                                Container(
                                  padding: const EdgeInsets.symmetric(horizontal: 14),
                                  decoration: BoxDecoration(
                                    color: NetraColors.surfaceWhite,
                                    border: Border.all(color: NetraColors.borderGray),
                                    borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
                                  ),
                                  child: DropdownButtonHideUnderline(
                                    child: DropdownButton<String>(
                                      value: _availableBloodBanks.any((b) => b.id == _selectedBloodBank?.id)
                                          ? _selectedBloodBank?.id
                                          : null,
                                      isExpanded: true,
                                      hint: const Text('Select verified partner blood centre'),
                                      items: _availableBloodBanks.map((bank) {
                                        return DropdownMenuItem<String>(
                                          value: bank.id,
                                          child: Text(
                                            '${bank.name} (${bank.city})',
                                            style: NetraTypography.bodyMedium,
                                            overflow: TextOverflow.ellipsis,
                                          ),
                                        );
                                      }).toList(),
                                      onChanged: (selectedId) {
                                        if (selectedId != null) {
                                          final bank = _availableBloodBanks.firstWhere(
                                            (b) => b.id == selectedId,
                                            orElse: () => _availableBloodBanks.first,
                                          );
                                          setState(() {
                                            _selectedBloodBank = bank;
                                            _bloodBankIdController.text = bank.id;
                                          });
                                        }
                                      },
                                    ),
                                  ),
                                ),
                                const SizedBox(height: 6),
                                if (_selectedBloodBank != null) ...[
                                  Align(
                                    alignment: Alignment.centerLeft,
                                    child: TextButton.icon(
                                      style: TextButton.styleFrom(
                                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                                        visualDensity: VisualDensity.compact,
                                      ),
                                      icon: const Icon(Icons.location_city, size: 16, color: Color(0xFF2563EB)),
                                      label: const Text(
                                        'Use Blood Center Location & Venue',
                                        style: TextStyle(fontSize: 12, color: Color(0xFF2563EB), fontWeight: FontWeight.w600),
                                      ),
                                      onPressed: () => _applyVerifiedBloodCenterLocation(_selectedBloodBank!),
                                    ),
                                  ),
                                ],
                                Text(
                                  'Licensed centre overseeing clinical collection and safety standards.',
                                  style: NetraTypography.bodySmall.copyWith(
                                    color: NetraColors.textSecondary,
                                    fontSize: 11,
                                  ),
                                ),
                              ],
                            ),
                          ] else ...[
                            NetraTextField(
                              label: 'Blood Bank ID *',
                              hint: 'Enter authorized blood bank UUID',
                              controller: _bloodBankIdController,
                              helperText: 'UUID of your authorized blood bank',
                            ),
                          ],
                          const SizedBox(height: 18),

                          // 2. Camp Title
                          NetraTextField(
                            label: 'Camp Title *',
                            hint: 'Enter camp name (e.g. Annual Community Blood Drive)',
                            controller: _titleController,
                          ),
                          const SizedBox(height: 16),

                          // 3. Description
                          NetraTextField(
                            label: 'Description',
                            hint: 'Information about the drive, facilities, special instructions...',
                            controller: _descriptionController,
                            maxLines: 3,
                          ),
                          const SizedBox(height: 24),

                          // 4. Venue & Location
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
                              OutlinedButton.icon(
                                style: OutlinedButton.styleFrom(
                                  minimumSize: const Size(0, 36),
                                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                                  visualDensity: VisualDensity.compact,
                                  side: const BorderSide(color: Color(0xFF16A34A)),
                                  foregroundColor: const Color(0xFF16A34A),
                                ),
                                onPressed: _isLocating ? null : _detectLocation,
                                icon: _isLocating
                                    ? const SizedBox(
                                        width: 14,
                                        height: 14,
                                        child: CircularProgressIndicator(
                                          strokeWidth: 2,
                                          valueColor: AlwaysStoppedAnimation<Color>(Color(0xFF16A34A)),
                                        ),
                                      )
                                    : const Icon(Icons.my_location_rounded, size: 16),
                                label: Text(
                                  _isLocating ? 'Detecting...' : 'Detect Location',
                                  style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w600),
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 8),

                          // Location Source Indicator Banner
                          _buildLocationSourceBanner(),
                          const SizedBox(height: 12),

                          NetraTextField(
                            label: 'Venue Name *',
                            hint: 'Enter venue name (e.g. Community Centre / Town Hall)',
                            controller: _venueNameController,
                            helperText: 'GPS detects coordinates; organizer enters exact venue name',
                          ),
                          const SizedBox(height: 16),

                          NetraTextField(
                            label: 'Street Address *',
                            hint: 'Enter street address',
                            controller: _addressController,
                          ),
                          const SizedBox(height: 16),

                          Row(
                            children: [
                              Expanded(
                                flex: 2,
                                child: NetraTextField(
                                  label: 'City *',
                                  hint: 'Enter city',
                                  controller: _cityController,
                                ),
                              ),
                              const SizedBox(width: 12),
                              Expanded(
                                flex: 2,
                                child: NetraTextField(
                                  label: 'State *',
                                  hint: 'Enter state',
                                  controller: _stateController,
                                ),
                              ),
                              const SizedBox(width: 12),
                              Expanded(
                                flex: 1,
                                child: NetraTextField(
                                  label: 'PIN *',
                                  hint: 'Postal code',
                                  controller: _postalCodeController,
                                  keyboardType: TextInputType.number,
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 24),

                          // 5. Schedule & Capacity
                          Text(
                            'Schedule & Capacity',
                            style: NetraTypography.titleMedium.copyWith(
                              fontWeight: FontWeight.bold,
                              color: NetraColors.primaryRed,
                            ),
                          ),
                          const SizedBox(height: 6),
                          Text(
                            'Under NBTC rules: Registration Open < Registration Close ≤ Camp Start < Camp End.',
                            style: NetraTypography.bodySmall.copyWith(
                              color: NetraColors.textSecondary,
                              fontSize: 11,
                            ),
                          ),
                          const SizedBox(height: 12),

                          // Event Start / End Pickers
                          Row(
                            children: [
                              Expanded(
                                child: _buildDateTimePickerTile(
                                  label: 'Camp Starts *',
                                  value: _startAt,
                                  placeholder: 'Select camp start',
                                  onTap: () async {
                                    final picked = await _pickDateTime(_startAt);
                                    if (picked != null) {
                                      setState(() => _startAt = picked);
                                    }
                                  },
                                ),
                              ),
                              const SizedBox(width: 12),
                              Expanded(
                                child: _buildDateTimePickerTile(
                                  label: 'Camp Ends *',
                                  value: _endAt,
                                  placeholder: 'Select camp end',
                                  onTap: () async {
                                    final picked = await _pickDateTime(_endAt);
                                    if (picked != null) {
                                      setState(() => _endAt = picked);
                                    }
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
                                  label: 'Registration Opens *',
                                  value: _regOpenAt,
                                  placeholder: 'Select registration open',
                                  onTap: () async {
                                    final picked = await _pickDateTime(_regOpenAt);
                                    if (picked != null) {
                                      setState(() => _regOpenAt = picked);
                                    }
                                  },
                                ),
                              ),
                              const SizedBox(width: 12),
                              Expanded(
                                child: _buildDateTimePickerTile(
                                  label: 'Registration Closes *',
                                  value: _regCloseAt,
                                  placeholder: 'Select registration close',
                                  onTap: () async {
                                    final picked = await _pickDateTime(_regCloseAt);
                                    if (picked != null) {
                                      setState(() => _regCloseAt = picked);
                                    }
                                  },
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 16),

                          NetraTextField(
                            label: 'Donor Capacity (Max Slots) *',
                            hint: 'Enter maximum donor capacity (e.g. 50, 100)',
                            controller: _capacityController,
                            keyboardType: TextInputType.number,
                            helperText:
                                'Registration stops automatically when capacity is reached.',
                          ),
                          const SizedBox(height: 16),

                          // Submit immediately option
                          CheckboxListTile(
                            contentPadding: EdgeInsets.zero,
                            title: const Text(
                              'Submit for admin review immediately after creation',
                              style: TextStyle(fontSize: 14, fontWeight: FontWeight.w600),
                            ),
                            subtitle: const Text(
                              'If unchecked, camp remains in DRAFT state.',
                              style: TextStyle(fontSize: 12),
                            ),
                            value: _submitForReviewImmediately,
                            onChanged: (val) {
                              setState(() =>
                                  _submitForReviewImmediately = val ?? false);
                            },
                          ),
                          const SizedBox(height: 24),

                          // 6. Review Summary Card Before Submit
                          _buildReviewSummaryCard(),
                          const SizedBox(height: 24),

                          AnimatedBuilder(
                            animation: _controller,
                            builder: (context, _) => NetraButton(
                              text: 'Create Camp',
                              isLoading: _controller.isActionLoading,
                              icon: Icons.add_circle_outline,
                              onPressed: _controller.isActionLoading
                                  ? null
                                  : _handleCreate,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
        );
  }

  Widget _buildUnauthorizedScreen(BuildContext context) {
    return ResponsiveScaffold(
      title: 'Create Donation Camp',
      body: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: 24.0, vertical: 32.0),
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 580),
            child: Card(
              elevation: 2,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(NetraSpacing.radiusLg),
                side: const BorderSide(color: NetraColors.borderGray),
              ),
              child: Padding(
                padding: const EdgeInsets.all(32.0),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Container(
                      padding: const EdgeInsets.all(16),
                      decoration: const BoxDecoration(
                        color: Color(0xFFFEF2F2),
                        shape: BoxShape.circle,
                      ),
                      child: const Icon(
                        Icons.admin_panel_settings_outlined,
                        color: NetraColors.primaryRed,
                        size: 48,
                      ),
                    ),
                    const SizedBox(height: 20),
                    Text(
                      'Create Donation Camp',
                      style: NetraTypography.headlineSmall.copyWith(
                        fontWeight: FontWeight.bold,
                        color: NetraColors.textPrimary,
                      ),
                      textAlign: TextAlign.center,
                    ),
                    const SizedBox(height: 12),
                    Text(
                      'Only authorized blood-bank organizations and approved administrators can create donation camps.',
                      style: NetraTypography.bodyLarge.copyWith(
                        color: NetraColors.textPrimary,
                        fontWeight: FontWeight.w600,
                      ),
                      textAlign: TextAlign.center,
                    ),
                    const SizedBox(height: 12),
                    Text(
                      'Under National Blood Transfusion Council (NBTC) regulations, blood donation drives require licensed clinical supervision and certified equipment. Regular donors and volunteer organizers cannot create camps directly.',
                      style: NetraTypography.bodyMedium.copyWith(
                        color: NetraColors.textSecondary,
                        height: 1.4,
                      ),
                      textAlign: TextAlign.center,
                    ),
                    const SizedBox(height: 28),
                    NetraButton(
                      text: 'Find Active Camps',
                      icon: Icons.event_available,
                      onPressed: () {
                        Navigator.pop(context);
                      },
                    ),
                    const SizedBox(height: 12),
                    NetraButton.outlined(
                      text: 'Contact Support',
                      icon: Icons.help_outline,
                      onPressed: () => _showSupportInfo(context),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildLocationSourceBanner() {
    final hasCoords = _latitude != null && _longitude != null;
    Color bgColor;
    Color borderColor;
    Color textColor;
    IconData icon;
    String label;

    switch (_locationSource) {
      case LocationSource.deviceGps:
        bgColor = const Color(0xFFF0FDF4);
        borderColor = const Color(0xFF86EFAC);
        textColor = const Color(0xFF16A34A);
        icon = Icons.my_location_rounded;
        label = 'Location detected from device (${_latitude!.toStringAsFixed(4)}, ${_longitude!.toStringAsFixed(4)})';
        break;
      case LocationSource.verifiedBloodCenter:
        bgColor = const Color(0xFFEFF6FF);
        borderColor = const Color(0xFF93C5FD);
        textColor = const Color(0xFF2563EB);
        icon = Icons.verified_rounded;
        label = 'Verified Blood Center location attached (${_latitude!.toStringAsFixed(4)}, ${_longitude!.toStringAsFixed(4)})';
        break;
      default:
        if (hasCoords) {
          bgColor = const Color(0xFFF0FDF4);
          borderColor = const Color(0xFF86EFAC);
          textColor = const Color(0xFF16A34A);
          icon = Icons.pin_drop_outlined;
          label = 'Coordinates attached (${_latitude!.toStringAsFixed(4)}, ${_longitude!.toStringAsFixed(4)})';
        } else {
          bgColor = const Color(0xFFFFFBEB);
          borderColor = const Color(0xFFFDE68A);
          textColor = const Color(0xFFD97706);
          icon = Icons.location_off_outlined;
          label = 'No GPS coordinates attached. Tap "Detect Location" or choose a verified centre.';
        }
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: BoxDecoration(
        color: bgColor,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: borderColor),
      ),
      child: Row(
        children: [
          Icon(icon, size: 16, color: textColor),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              label,
              style: TextStyle(
                fontSize: 12,
                color: textColor,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
          if (hasCoords)
            GestureDetector(
              onTap: () => setState(() {
                _latitude = null;
                _longitude = null;
                _locationSource = LocationSource.none;
              }),
              child: const Icon(Icons.close, size: 14, color: Color(0xFF64748B)),
            ),
        ],
      ),
    );
  }

  Widget _buildReviewSummaryCard() {
    final title = _titleController.text.trim();
    final venue = _venueNameController.text.trim();
    final address = _addressController.text.trim();
    final city = _cityController.text.trim();
    final state = _stateController.text.trim();
    final pin = _postalCodeController.text.trim();
    final capacity = _capacityController.text.trim();

    String sourceLabel = 'Not Attached';
    Color sourceColor = const Color(0xFFD97706);
    if (_locationSource == LocationSource.deviceGps) {
      sourceLabel = 'Device GPS';
      sourceColor = const Color(0xFF16A34A);
    } else if (_locationSource == LocationSource.verifiedBloodCenter) {
      sourceLabel = 'Verified Blood Center';
      sourceColor = const Color(0xFF2563EB);
    } else if (_latitude != null && _longitude != null) {
      sourceLabel = 'Custom Coordinates';
      sourceColor = const Color(0xFF64748B);
    }

    final fullAddress = [
      if (address.isNotEmpty) address,
      if (city.isNotEmpty) city,
      if (state.isNotEmpty) state,
      if (pin.isNotEmpty) pin,
    ].join(', ');

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFFF8FAFC),
        borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
        border: Border.all(color: const Color(0xFFE2E8F0)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Icons.rate_review_outlined, color: NetraColors.primaryRed, size: 18),
              const SizedBox(width: 8),
              Text(
                'Camp Summary Review',
                style: NetraTypography.titleMedium.copyWith(
                  fontWeight: FontWeight.bold,
                  color: NetraColors.textPrimary,
                  fontSize: 14,
                ),
              ),
              const Spacer(),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                decoration: BoxDecoration(
                  color: sourceColor.withValues(alpha: 0.1),
                  borderRadius: BorderRadius.circular(4),
                  border: Border.all(color: sourceColor.withValues(alpha: 0.3)),
                ),
                child: Text(
                  sourceLabel,
                  style: TextStyle(
                    fontSize: 11,
                    fontWeight: FontWeight.w600,
                    color: sourceColor,
                  ),
                ),
              ),
            ],
          ),
          const Divider(height: 20),
          _buildReviewRow('Camp Title', title.isNotEmpty ? title : '—'),
          _buildReviewRow('Partner Centre', _selectedBloodBank?.name ?? (_bloodBankIdController.text.isNotEmpty ? _bloodBankIdController.text : '—')),
          _buildReviewRow('Venue', venue.isNotEmpty ? venue : '—'),
          _buildReviewRow('Address', fullAddress.isNotEmpty ? fullAddress : '—'),
          _buildReviewRow(
            'Schedule',
            _startAt != null && _endAt != null
                ? '${_formatDateTime(_startAt!)} → ${_formatDateTime(_endAt!)}'
                : 'Not selected',
          ),
          _buildReviewRow(
            'Registration',
            _regOpenAt != null && _regCloseAt != null
                ? '${_formatDateTime(_regOpenAt!)} → ${_formatDateTime(_regCloseAt!)}'
                : 'Not selected',
          ),
          _buildReviewRow(
            'Donor Capacity',
            capacity.isNotEmpty ? '$capacity slots' : '—',
          ),
        ],
      ),
    );
  }

  Widget _buildReviewRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 3),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 110,
            child: Text(
              label,
              style: const TextStyle(
                fontSize: 12,
                color: Color(0xFF64748B),
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              value,
              style: const TextStyle(
                fontSize: 12,
                color: Color(0xFF1E293B),
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildDateTimePickerTile({
    required String label,
    required DateTime? value,
    required String placeholder,
    required VoidCallback onTap,
  }) {
    final hasValue = value != null;
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
        decoration: BoxDecoration(
          color: NetraColors.surfaceWhite,
          border: Border.all(
            color: hasValue ? NetraColors.primaryRed.withValues(alpha: 0.5) : NetraColors.borderGray,
          ),
          borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              label,
              style: NetraTypography.labelSmall.copyWith(
                color: hasValue ? NetraColors.textPrimary : NetraColors.textSecondary,
                fontWeight: hasValue ? FontWeight.w600 : FontWeight.normal,
              ),
            ),
            const SizedBox(height: 4),
            Row(
              children: [
                Icon(
                  Icons.calendar_today,
                  size: 16,
                  color: hasValue ? NetraColors.primaryRed : const Color(0xFF94A3B8),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    hasValue ? _formatDateTime(value) : placeholder,
                    style: NetraTypography.bodyMedium.copyWith(
                      fontWeight: hasValue ? FontWeight.w600 : FontWeight.normal,
                      color: hasValue ? NetraColors.textPrimary : const Color(0xFF94A3B8),
                    ),
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
