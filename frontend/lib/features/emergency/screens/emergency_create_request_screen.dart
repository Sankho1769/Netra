import 'package:flutter/material.dart';
import '../../../common/widgets/common_widgets.dart';
import '../../../core/location/location_service.dart';
import '../../../core/network/network_exception.dart';
import '../../../core/responsive/responsive.dart';
import '../../../core/theme/netra_colors.dart';
import '../../../core/theme/netra_spacing.dart';
import '../../../core/theme/netra_typography.dart';
import '../services/emergency_api_service.dart';
import 'emergency_request_created_screen.dart';

class EmergencyCreateRequestScreen extends StatefulWidget {
  final EmergencyApiService? apiService;
  final LocationService? locationService;

  const EmergencyCreateRequestScreen({
    super.key,
    this.apiService,
    this.locationService,
  });

  @override
  State<EmergencyCreateRequestScreen> createState() =>
      _EmergencyCreateRequestScreenState();
}

class _EmergencyCreateRequestScreenState
    extends State<EmergencyCreateRequestScreen> {
  late final EmergencyApiService _apiService;
  late final LocationService _locationService;
  final _formKey = GlobalKey<FormState>();

  String _selectedBloodGroup = 'O+';
  int _unitsRequired = 2;
  Duration _selectedDuration = const Duration(hours: 6);
  DateTime? _customRequiredBy;

  final TextEditingController _hospitalNameController = TextEditingController();
  final TextEditingController _hospitalAddressController =
      TextEditingController();
  final TextEditingController _cityController = TextEditingController();
  final TextEditingController _stateController = TextEditingController();
  final TextEditingController _postalCodeController = TextEditingController();
  final TextEditingController _latitudeController = TextEditingController();
  final TextEditingController _longitudeController = TextEditingController();
  final TextEditingController _descriptionController = TextEditingController();

  bool _isSubmitting = false;
  bool _isDetectingLocation = false;
  String? _errorMessage;
  String? _currentSubmissionIdempotencyKey;

  final List<String> _bloodGroups = [
    'A+',
    'A-',
    'B+',
    'B-',
    'O+',
    'O-',
    'AB+',
    'AB-'
  ];

  final List<int> _quickUnits = [1, 2, 3, 4, 5];

  final List<Map<String, dynamic>> _quickDeadlines = [
    {'label': 'Within 2 Hours', 'duration': const Duration(hours: 2)},
    {'label': 'Within 6 Hours', 'duration': const Duration(hours: 6)},
    {'label': 'Within 12 Hours', 'duration': const Duration(hours: 12)},
    {'label': 'Within 24 Hours', 'duration': const Duration(hours: 24)},
    {'label': 'Within 48 Hours', 'duration': const Duration(hours: 48)},
  ];

  @override
  void initState() {
    super.initState();
    _apiService = widget.apiService ?? EmergencyApiService();
    _locationService = widget.locationService ?? DefaultLocationService();

    _hospitalNameController.addListener(_onInputChanged);
    _hospitalAddressController.addListener(_onInputChanged);
    _cityController.addListener(_onInputChanged);
    _stateController.addListener(_onInputChanged);
    _postalCodeController.addListener(_onInputChanged);
    _latitudeController.addListener(_onInputChanged);
    _longitudeController.addListener(_onInputChanged);
    _descriptionController.addListener(_onInputChanged);
  }

  void _onInputChanged() {
    if (_currentSubmissionIdempotencyKey != null) {
      _currentSubmissionIdempotencyKey = null;
    }
  }

  @override
  void dispose() {
    _hospitalNameController.removeListener(_onInputChanged);
    _hospitalAddressController.removeListener(_onInputChanged);
    _cityController.removeListener(_onInputChanged);
    _stateController.removeListener(_onInputChanged);
    _postalCodeController.removeListener(_onInputChanged);
    _latitudeController.removeListener(_onInputChanged);
    _longitudeController.removeListener(_onInputChanged);
    _descriptionController.removeListener(_onInputChanged);

    _hospitalNameController.dispose();
    _hospitalAddressController.dispose();
    _cityController.dispose();
    _stateController.dispose();
    _postalCodeController.dispose();
    _latitudeController.dispose();
    _longitudeController.dispose();
    _descriptionController.dispose();
    super.dispose();
  }

  Future<void> _detectLocation() async {
    setState(() {
      _isDetectingLocation = true;
    });

    try {
      final loc =
          await _locationService.getCurrentLocation(approximateOnly: false);
      if (loc != null && mounted) {
        setState(() {
          if (loc.city != null) _cityController.text = loc.city!;
          if (loc.state != null) _stateController.text = loc.state!;
          if (loc.postalCode != null)
            _postalCodeController.text = loc.postalCode!;
          if (loc.latitude != null)
            _latitudeController.text = loc.latitude!.toStringAsFixed(4);
          if (loc.longitude != null)
            _longitudeController.text = loc.longitude!.toStringAsFixed(4);
        });

        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Location detected successfully.'),
            backgroundColor: Color(0xFF16A34A),
          ),
        );
      } else if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text(
                'Location permission denied or unavailable. Please enter details manually.'),
            backgroundColor: Color(0xFFD97706),
          ),
        );
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content:
                Text('Could not auto-detect location. Please fill manually.'),
            backgroundColor: Color(0xFFD97706),
          ),
        );
      }
    } finally {
      if (mounted) {
        setState(() {
          _isDetectingLocation = false;
        });
      }
    }
  }

  DateTime _calculateDeadline() {
    if (_customRequiredBy != null) {
      return _customRequiredBy!;
    }
    return DateTime.now().add(_selectedDuration);
  }

  Future<void> _submitEmergencyRequest() async {
    if (_isSubmitting) return; // double-tap safety

    if (!_formKey.currentState!.validate()) return;

    final lat = double.tryParse(_latitudeController.text.trim());
    final lng = double.tryParse(_longitudeController.text.trim());

    if (lat == null || lat < -90.0 || lat > 90.0) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content:
              Text('Please provide a valid latitude between -90.0 and 90.0.'),
          backgroundColor: Colors.red,
        ),
      );
      return;
    }

    if (lng == null || lng < -180.0 || lng > 180.0) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text(
              'Please provide a valid longitude between -180.0 and 180.0.'),
          backgroundColor: Colors.red,
        ),
      );
      return;
    }

    final deadline = _calculateDeadline();
    final now = DateTime.now();
    if (!deadline.isAfter(now)) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Deadline must be in the future.'),
          backgroundColor: Colors.red,
        ),
      );
      return;
    }

    if (deadline.isAfter(now.add(const Duration(hours: 72)))) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Emergency deadline must be within 72 hours.'),
          backgroundColor: Colors.red,
        ),
      );
      return;
    }

    setState(() {
      _isSubmitting = true;
      _errorMessage = null;
    });

    final payload = <String, dynamic>{
      'bloodGroup': _selectedBloodGroup,
      'unitsRequired': _unitsRequired,
      'hospitalName': _hospitalNameController.text.trim(),
      'hospitalAddress': _hospitalAddressController.text.trim(),
      'city': _cityController.text.trim(),
      'state': _stateController.text.trim(),
      'postalCode': _postalCodeController.text.trim(),
      'latitude': lat,
      'longitude': lng,
      'requiredBy': deadline.toUtc().toIso8601String(),
    };

    final desc = _descriptionController.text.trim();
    if (desc.isNotEmpty) {
      payload['description'] = desc;
    }

    _currentSubmissionIdempotencyKey ??=
        EmergencyApiService.generateIdempotencyKey();

    try {
      final created = await _apiService.createEmergencyRequest(
        payload,
        idempotencyKey: _currentSubmissionIdempotencyKey!,
      );
      if (!mounted) return;

      _currentSubmissionIdempotencyKey = null;

      Navigator.of(context).pushReplacement(
        MaterialPageRoute(
          builder: (context) => EmergencyRequestCreatedScreen(request: created),
        ),
      );
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _isSubmitting = false;
        _errorMessage = e is NetworkException
            ? e.message
            : 'Failed to create emergency blood request. Please try again.';
      });
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(_errorMessage!),
          backgroundColor: Colors.red,
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return ResponsiveScaffold(
      appBar: NetraAppBar(
        title: "Create Emergency Request",
        showBackButton: true,
      ),
      body: ResponsiveContainer.standard(
        scrollable: true,
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Critical Alert Warning
              Container(
                padding: NetraSpacing.cardPaddingStandard,
                decoration: BoxDecoration(
                  color: const Color(0xFFFEF2F2),
                  borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
                  border: Border.all(color: const Color(0xFFFCA5A5)),
                ),
                child: Row(
                  children: [
                    const Icon(Icons.warning_amber_rounded,
                        color: Color(0xFFDC2626), size: 24),
                    NetraSpacing.gapW12,
                    Expanded(
                      child: Text(
                        "This creates a CRITICAL blood request that becomes available through NETRA's supported emergency and discovery workflows.",
                        style: NetraTypography.bodySmall.copyWith(
                          color: const Color(0xFF991B1B),
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              NetraSpacing.gapH20,

              // 1. Blood Group Selection (1-tap chips)
              Text(
                "Blood Group Needed *",
                style: NetraTypography.titleMedium
                    .copyWith(fontWeight: FontWeight.bold),
              ),
              NetraSpacing.gapH8,
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: _bloodGroups.map((group) {
                  final isSelected = _selectedBloodGroup == group;
                  return ChoiceChip(
                    label: Text(
                      group,
                      style: NetraTypography.labelLarge.copyWith(
                        color: isSelected
                            ? NetraColors.surfaceWhite
                            : NetraColors.textPrimary,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    selected: isSelected,
                    selectedColor: const Color(0xFFDC2626),
                    backgroundColor: NetraColors.backgroundGray,
                    shape: RoundedRectangleBorder(
                      borderRadius:
                          BorderRadius.circular(NetraSpacing.radiusMd),
                    ),
                    onSelected: (selected) {
                      if (selected) {
                        setState(() {
                          _selectedBloodGroup = group;
                          _currentSubmissionIdempotencyKey = null;
                        });
                      }
                    },
                  );
                }).toList(),
              ),
              NetraSpacing.gapH20,

              // 2. Units Required (Quick Selector)
              Text(
                "Units Required (1-50) *",
                style: NetraTypography.titleMedium
                    .copyWith(fontWeight: FontWeight.bold),
              ),
              NetraSpacing.gapH8,
              Row(
                children: _quickUnits.map((u) {
                  final isSelected = _unitsRequired == u;
                  return Padding(
                    padding: const EdgeInsets.only(right: 8.0),
                    child: OutlinedButton(
                      style: OutlinedButton.styleFrom(
                        backgroundColor: isSelected
                            ? const Color(0xFFDC2626)
                            : Colors.transparent,
                        foregroundColor: isSelected
                            ? NetraColors.surfaceWhite
                            : NetraColors.textPrimary,
                        side: BorderSide(
                          color: isSelected
                              ? const Color(0xFFDC2626)
                              : NetraColors.borderSubtle,
                        ),
                        minimumSize: const Size(48, 44),
                        shape: RoundedRectangleBorder(
                          borderRadius:
                              BorderRadius.circular(NetraSpacing.radiusMd),
                        ),
                      ),
                      onPressed: () {
                        setState(() {
                          _unitsRequired = u;
                          _currentSubmissionIdempotencyKey = null;
                        });
                      },
                      child: Text(
                        "$u",
                        style: NetraTypography.labelLarge.copyWith(
                          color: isSelected
                              ? NetraColors.surfaceWhite
                              : NetraColors.textPrimary,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                  );
                }).toList(),
              ),
              NetraSpacing.gapH20,

              // 3. Emergency Deadline Selection (within 72h)
              Text(
                "Required Within *",
                style: NetraTypography.titleMedium
                    .copyWith(fontWeight: FontWeight.bold),
              ),
              NetraSpacing.gapH8,
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: _quickDeadlines.map((item) {
                  final duration = item['duration'] as Duration;
                  final label = item['label'] as String;
                  final isSelected = _customRequiredBy == null &&
                      _selectedDuration == duration;
                  return ChoiceChip(
                    label: Text(
                      label,
                      style: NetraTypography.bodySmall.copyWith(
                        color: isSelected
                            ? NetraColors.surfaceWhite
                            : NetraColors.textPrimary,
                        fontWeight:
                            isSelected ? FontWeight.bold : FontWeight.normal,
                      ),
                    ),
                    selected: isSelected,
                    selectedColor: const Color(0xFFDC2626),
                    backgroundColor: NetraColors.backgroundGray,
                    shape: RoundedRectangleBorder(
                      borderRadius:
                          BorderRadius.circular(NetraSpacing.radiusMd),
                    ),
                    onSelected: (selected) {
                      if (selected) {
                        setState(() {
                          _selectedDuration = duration;
                          _customRequiredBy = null;
                          _currentSubmissionIdempotencyKey = null;
                        });
                      }
                    },
                  );
                }).toList(),
              ),
              NetraSpacing.gapH24,

              // 4. Hospital & Location Information
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    "Hospital & Location *",
                    style: NetraTypography.titleMedium
                        .copyWith(fontWeight: FontWeight.bold),
                  ),
                  TextButton.icon(
                    onPressed: _isDetectingLocation ? null : _detectLocation,
                    icon: _isDetectingLocation
                        ? const SizedBox(
                            width: 14,
                            height: 14,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Icon(Icons.my_location_rounded, size: 16),
                    label: const Text("Detect Location"),
                  ),
                ],
              ),
              NetraSpacing.gapH8,
              NetraTextField(
                controller: _hospitalNameController,
                label: "Hospital / Medical Center Name",
                hint: "e.g. KEM Hospital, Lilavati Hospital",
                prefixIcon: const Icon(Icons.local_hospital_outlined),
                validator: (val) {
                  if (val == null || val.trim().isEmpty) {
                    return "Hospital name is required.";
                  }
                  return null;
                },
              ),
              NetraSpacing.gapH12,
              NetraTextField(
                controller: _hospitalAddressController,
                label: "Hospital Address / Ward",
                hint: "e.g. Acharya Donde Marg, Parel, Ward 4",
                prefixIcon: const Icon(Icons.place_outlined),
                validator: (val) {
                  if (val == null || val.trim().isEmpty) {
                    return "Hospital address is required.";
                  }
                  return null;
                },
              ),
              NetraSpacing.gapH12,
              Row(
                children: [
                  Expanded(
                    child: NetraTextField(
                      controller: _cityController,
                      label: "City",
                      validator: (val) => (val == null || val.trim().isEmpty)
                          ? "City required"
                          : null,
                    ),
                  ),
                  NetraSpacing.gapW12,
                  Expanded(
                    child: NetraTextField(
                      controller: _stateController,
                      label: "State",
                      validator: (val) => (val == null || val.trim().isEmpty)
                          ? "State required"
                          : null,
                    ),
                  ),
                ],
              ),
              NetraSpacing.gapH12,
              Row(
                children: [
                  Expanded(
                    child: NetraTextField(
                      controller: _postalCodeController,
                      label: "Postal Code",
                      validator: (val) => (val == null || val.trim().isEmpty)
                          ? "PIN required"
                          : null,
                    ),
                  ),
                  NetraSpacing.gapW12,
                  Expanded(
                    child: NetraTextField(
                      controller: _latitudeController,
                      label: "Latitude",
                      keyboardType:
                          const TextInputType.numberWithOptions(decimal: true),
                      validator: (val) => (val == null || val.trim().isEmpty)
                          ? "Latitude required"
                          : null,
                    ),
                  ),
                  NetraSpacing.gapW12,
                  Expanded(
                    child: NetraTextField(
                      controller: _longitudeController,
                      label: "Longitude",
                      keyboardType:
                          const TextInputType.numberWithOptions(decimal: true),
                      validator: (val) => (val == null || val.trim().isEmpty)
                          ? "Longitude required"
                          : null,
                    ),
                  ),
                ],
              ),
              NetraSpacing.gapH20,

              // 5. Short Medical Note (Optional)
              Text(
                "Short Note (Optional)",
                style: NetraTypography.titleMedium
                    .copyWith(fontWeight: FontWeight.bold),
              ),
              NetraSpacing.gapH8,
              NetraTextField(
                controller: _descriptionController,
                label: "Emergency Context",
                hint:
                    "e.g. Emergency surgery, ICU patient. Keep confidential details private.",
                maxLines: 2,
                prefixIcon: const Icon(Icons.notes_rounded),
              ),
              NetraSpacing.gapH20,

              // Truthful Emergency Privacy Notice
              Container(
                padding: NetraSpacing.cardPaddingStandard,
                decoration: BoxDecoration(
                  color: NetraColors.backgroundGray,
                  borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
                  border: Border.all(color: NetraColors.borderSubtle),
                ),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Icon(Icons.shield_outlined,
                        color: NetraColors.primaryRed, size: 20),
                    NetraSpacing.gapW12,
                    Expanded(
                      child: Text(
                        "NETRA does not expose donor personal contact information. Available contact and fulfillment actions are handled through the supported NETRA workflow and verified blood-bank processes.",
                        style: NetraTypography.bodySmall
                            .copyWith(color: NetraColors.textSecondary),
                      ),
                    ),
                  ],
                ),
              ),
              NetraSpacing.gapH24,

              // Submit Button
              SizedBox(
                width: double.infinity,
                height: 52,
                child: ElevatedButton(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFFDC2626),
                    foregroundColor: NetraColors.surfaceWhite,
                    shape: RoundedRectangleBorder(
                      borderRadius:
                          BorderRadius.circular(NetraSpacing.radiusMd),
                    ),
                    elevation: 2,
                  ),
                  onPressed: _isSubmitting ? null : _submitEmergencyRequest,
                  child: _isSubmitting
                      ? const Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            SizedBox(
                              width: 20,
                              height: 20,
                              child: CircularProgressIndicator(
                                strokeWidth: 2.5,
                                color: NetraColors.surfaceWhite,
                              ),
                            ),
                            SizedBox(width: 12),
                            Text(
                              "Submitting Request...",
                              style: TextStyle(
                                  color: NetraColors.surfaceWhite,
                                  fontWeight: FontWeight.bold),
                            ),
                          ],
                        )
                      : Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            const Icon(Icons.emergency_share_rounded, size: 20),
                            NetraSpacing.gapW8,
                            Text(
                              "Submit Emergency Request",
                              style: NetraTypography.labelLarge.copyWith(
                                color: NetraColors.surfaceWhite,
                                fontWeight: FontWeight.bold,
                                fontSize: 16,
                              ),
                            ),
                          ],
                        ),
                ),
              ),
              NetraSpacing.gapH32,
            ],
          ),
        ),
      ),
    );
  }
}
