import 'package:flutter/material.dart';
import '../../../common/widgets/common_widgets.dart';
import '../../../core/location/location_service.dart';
import '../../../core/network/network_exception.dart';
import '../../../core/responsive/responsive.dart';
import '../../../core/theme/netra_colors.dart';
import '../../../core/theme/netra_spacing.dart';
import '../../../core/theme/netra_typography.dart';
import '../../blood_request/models/verified_hospital_model.dart';
import '../../blood_request/widgets/hospital_search_field.dart';
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
  final TextEditingController _descriptionController = TextEditingController();

  double? _latitude;
  double? _longitude;
  String? _placeId;
  String? _hospitalVerificationStatus;

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
    _descriptionController.removeListener(_onInputChanged);

    _hospitalNameController.dispose();
    _hospitalAddressController.dispose();
    _cityController.dispose();
    _stateController.dispose();
    _postalCodeController.dispose();
    _descriptionController.dispose();
    super.dispose();
  }

  Future<void> _detectLocation() async {
    setState(() {
      _isDetectingLocation = true;
    });

    try {
      final result =
          await _locationService.getDetailedLocation(approximateOnly: false);
      if (result.isSuccess && result.location != null && mounted) {
        final loc = result.location!;
        setState(() {
          if (loc.city != null) _cityController.text = loc.city!;
          if (loc.state != null) _stateController.text = loc.state!;
          if (loc.postalCode != null) {
            _postalCodeController.text = loc.postalCode!;
          }
          if (loc.latitude != null) _latitude = loc.latitude;
          if (loc.longitude != null) _longitude = loc.longitude;
        });

        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Location detected successfully.'),
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
                  'Could not auto-detect location. Please enter details manually or select a hospital.'),
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
      if (_latitude != null) 'latitude': _latitude,
      if (_longitude != null) 'longitude': _longitude,
      if (_placeId != null) 'placeId': _placeId,
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
              HospitalSearchField(
                controller: _hospitalNameController,
                onHospitalSelected: (VerifiedHospitalModel? hospital) {
                  setState(() {
                    if (hospital != null) {
                      _hospitalNameController.text = hospital.name;
                      _hospitalAddressController.text = hospital.address;
                      _cityController.text = hospital.city;
                      _stateController.text = hospital.state;
                      if (hospital.postalCode != null) {
                        _postalCodeController.text = hospital.postalCode!;
                      }
                      _placeId = hospital.placeId;
                      _latitude = hospital.latitude;
                      _longitude = hospital.longitude;
                      _hospitalVerificationStatus =
                          hospital.verificationStatus;
                    } else {
                      _placeId = null;
                      _latitude = null;
                      _longitude = null;
                      _hospitalVerificationStatus = null;
                    }
                    _currentSubmissionIdempotencyKey = null;
                  });
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
              NetraTextField(
                controller: _postalCodeController,
                label: "Postal Code",
                hint: "e.g. 400012",
                validator: (val) => (val == null || val.trim().isEmpty)
                    ? "PIN required"
                    : null,
              ),
              NetraSpacing.gapH12,

              // Location Privacy & Server-Side Verification Badge
              Container(
                width: double.infinity,
                padding: NetraSpacing.cardPaddingStandard,
                decoration: BoxDecoration(
                  color: const Color(0xFFF8FAFC),
                  borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
                  border: Border.all(color: const Color(0xFFE2E8F0)),
                ),
                child: Row(
                  children: [
                    Icon(
                      (_placeId != null ||
                              _hospitalVerificationStatus == 'VERIFIED')
                          ? Icons.verified_user_rounded
                          : Icons.location_on_outlined,
                      size: 20,
                      color: (_placeId != null ||
                              _hospitalVerificationStatus == 'VERIFIED')
                          ? const Color(0xFF16A34A)
                          : NetraColors.textSecondary,
                    ),
                    NetraSpacing.gapW12,
                    Expanded(
                      child: Text(
                        (_placeId != null ||
                                _hospitalVerificationStatus == 'VERIFIED')
                            ? "Verified clinical institution selected. Authoritative coordinates will be attached securely."
                            : "Hospital location coordinates are resolved and verified server-side without manual decimal input.",
                        style: NetraTypography.bodySmall.copyWith(
                          color: (_placeId != null ||
                                  _hospitalVerificationStatus == 'VERIFIED')
                              ? const Color(0xFF15803D)
                              : NetraColors.textSecondary,
                          fontWeight: (_placeId != null ||
                                  _hospitalVerificationStatus == 'VERIFIED')
                              ? FontWeight.w600
                              : FontWeight.normal,
                        ),
                      ),
                    ),
                  ],
                ),
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
