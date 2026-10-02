import 'package:flutter/material.dart';
import '../../../../core/theme/netra_colors.dart';
import '../models/event_registration.dart';

class ParticipantRegistrationDialog extends StatefulWidget {
  final String eventTitle;
  final String venueName;
  final String? initialFullName;
  final String? initialEmail;
  final String? initialPhone;

  const ParticipantRegistrationDialog({
    super.key,
    required this.eventTitle,
    required this.venueName,
    this.initialFullName,
    this.initialEmail,
    this.initialPhone,
  });

  @override
  State<ParticipantRegistrationDialog> createState() =>
      _ParticipantRegistrationDialogState();
}

class _ParticipantRegistrationDialogState
    extends State<ParticipantRegistrationDialog> {
  final _formKey = GlobalKey<FormState>();

  late final TextEditingController _nameController;
  late final TextEditingController _emailController;
  late final TextEditingController _phoneController;
  final _addressController = TextEditingController();
  final _cityController = TextEditingController();
  final _emergencyNameController = TextEditingController();
  final _emergencyPhoneController = TextEditingController();

  DateTime? _selectedDob;
  String _selectedBloodGroup = 'O+';

  // 4 NBTC Clinical Pre-Screening Declarations
  bool _declGoodHealth = false;
  bool _declAgeWeight = false;
  bool _declNoRecentSurgery = false;
  bool _declClinicalScreening = false;

  // Final Mandatory Consent
  bool _consentConfirmed = false;

  final List<String> _bloodGroups = [
    'A+',
    'A-',
    'B+',
    'B-',
    'AB+',
    'AB-',
    'O+',
    'O-',
  ];

  @override
  void initState() {
    super.initState();
    _nameController = TextEditingController(text: widget.initialFullName ?? '');
    _emailController = TextEditingController(text: widget.initialEmail ?? '');
    _phoneController = TextEditingController(text: widget.initialPhone ?? '');
    // Default DOB to 20 years ago for convenience
    _selectedDob = DateTime.now().subtract(const Duration(days: 365 * 22));
  }

  @override
  void dispose() {
    _nameController.dispose();
    _emailController.dispose();
    _phoneController.dispose();
    _addressController.dispose();
    _cityController.dispose();
    _emergencyNameController.dispose();
    _emergencyPhoneController.dispose();
    super.dispose();
  }

  int _calculateAge(DateTime dob) {
    final now = DateTime.now();
    int age = now.year - dob.year;
    if (now.month < dob.month ||
        (now.month == dob.month && now.day < dob.day)) {
      age--;
    }
    return age;
  }

  Future<void> _selectDateOfBirth() async {
    final now = DateTime.now();
    final eighteenYearsAgo = DateTime(now.year - 18, now.month, now.day);
    final initialDate = _selectedDob ?? eighteenYearsAgo;

    final picked = await showDatePicker(
      context: context,
      initialDate: initialDate.isAfter(eighteenYearsAgo)
          ? eighteenYearsAgo
          : initialDate,
      firstDate: DateTime(now.year - 70),
      lastDate: now,
      helpText: 'SELECT YOUR DATE OF BIRTH',
    );

    if (picked != null) {
      setState(() {
        _selectedDob = picked;
      });
    }
  }

  void _handleSubmit() {
    if (!_formKey.currentState!.validate()) {
      return;
    }

    if (_selectedDob == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Please select your Date of Birth.'),
          backgroundColor: NetraColors.errorRed,
          behavior: SnackBarBehavior.floating,
        ),
      );
      return;
    }

    final age = _calculateAge(_selectedDob!);
    if (age < 18) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text(
              'Under NBTC guidelines, donors must be at least 18 years of age.'),
          backgroundColor: NetraColors.errorRed,
          behavior: SnackBarBehavior.floating,
        ),
      );
      return;
    }

    if (!_declGoodHealth ||
        !_declAgeWeight ||
        !_declNoRecentSurgery ||
        !_declClinicalScreening) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text(
              'Please review and complete all pre-screening self-declarations.'),
          backgroundColor: NetraColors.errorRed,
          behavior: SnackBarBehavior.floating,
        ),
      );
      return;
    }

    if (!_consentConfirmed) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content:
              Text('Please accept the event consent declaration to continue.'),
          backgroundColor: NetraColors.errorRed,
          behavior: SnackBarBehavior.floating,
        ),
      );
      return;
    }

    // Format phone
    var phone = _phoneController.text.trim();
    if (!phone.startsWith('+')) {
      if (phone.startsWith('91') && phone.length == 12) {
        phone = '+$phone';
      } else {
        phone = '+91$phone';
      }
    }

    var emergencyPhone = _emergencyPhoneController.text.trim();
    if (!emergencyPhone.startsWith('+')) {
      if (emergencyPhone.startsWith('91') && emergencyPhone.length == 12) {
        emergencyPhone = '+$emergencyPhone';
      } else {
        emergencyPhone = '+91$emergencyPhone';
      }
    }

    final data = ParticipantRegistrationData(
      fullName: _nameController.text.trim(),
      dateOfBirth: _selectedDob!,
      phone: phone,
      email: _emailController.text.trim().toLowerCase(),
      bloodGroup: _selectedBloodGroup,
      address: _addressController.text.trim(),
      city: _cityController.text.trim(),
      emergencyContactName: _emergencyNameController.text.trim(),
      emergencyContactPhone: emergencyPhone,
      consentConfirmed: true,
    );

    Navigator.of(context).pop(data);
  }

  @override
  Widget build(BuildContext context) {
    final age = _selectedDob != null ? _calculateAge(_selectedDob!) : null;

    return Dialog(
      insetPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 24),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      backgroundColor: Colors.white,
      clipBehavior: Clip.antiAlias,
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 540, maxHeight: 720),
        child: Column(
          children: [
            // Modal Header
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
              decoration: const BoxDecoration(
                color: Color(0xFFF8FAFC),
                border: Border(
                  bottom: BorderSide(color: Color(0xFFE2E8F0)),
                ),
              ),
              child: Row(
                children: [
                  Container(
                    width: 36,
                    height: 36,
                    decoration: BoxDecoration(
                      color: NetraColors.primaryRed.withValues(alpha: 0.12),
                      shape: BoxShape.circle,
                    ),
                    child: const Icon(
                      Icons.assignment_ind_rounded,
                      color: NetraColors.primaryRed,
                      size: 20,
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text(
                          'Participant Registration',
                          style: TextStyle(
                            fontSize: 17,
                            fontWeight: FontWeight.bold,
                            color: NetraColors.textPrimary,
                          ),
                        ),
                        Text(
                          widget.eventTitle,
                          style: TextStyle(
                            fontSize: 12,
                            color: Colors.grey.shade600,
                          ),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ],
                    ),
                  ),
                  IconButton(
                    icon: const Icon(Icons.close, size: 20),
                    onPressed: () => Navigator.of(context).pop(),
                  ),
                ],
              ),
            ),

            // Scrollable Form Content
            Expanded(
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(20),
                child: Form(
                  key: _formKey,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      // Notice Banner
                      Container(
                        padding: const EdgeInsets.all(12),
                        decoration: BoxDecoration(
                          color: const Color(0xFFEFF6FF),
                          borderRadius: BorderRadius.circular(10),
                          border: Border.all(color: const Color(0xFFBFDBFE)),
                        ),
                        child: Row(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Icon(
                              Icons.info_outline_rounded,
                              color: Color(0xFF1D4ED8),
                              size: 18,
                            ),
                            const SizedBox(width: 8),
                            Expanded(
                              child: Text(
                                'National Blood Transfusion Council (NBTC) regulations require verified participant identification and emergency contacts prior to camp attendance.',
                                style: TextStyle(
                                  fontSize: 12,
                                  color: Colors.blue.shade900,
                                  height: 1.35,
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 18),

                      // Section 1: Basic Information
                      const Text(
                        '1. Donor Participant Details',
                        style: TextStyle(
                          fontSize: 14,
                          fontWeight: FontWeight.bold,
                          color: NetraColors.textPrimary,
                        ),
                      ),
                      const SizedBox(height: 12),

                      // Full Name
                      TextFormField(
                        controller: _nameController,
                        decoration: const InputDecoration(
                          labelText: 'Full Legal Name *',
                          hintText: 'As shown on government ID',
                          prefixIcon: Icon(Icons.person_outline, size: 20),
                          border: OutlineInputBorder(),
                          isDense: true,
                        ),
                        validator: (val) {
                          if (val == null || val.trim().isEmpty) {
                            return 'Participant full name is required';
                          }
                          return null;
                        },
                      ),
                      const SizedBox(height: 12),

                      // DOB and Age selector
                      Row(
                        children: [
                          Expanded(
                            flex: 3,
                            child: InkWell(
                              onTap: _selectDateOfBirth,
                              child: InputDecorator(
                                decoration: const InputDecoration(
                                  labelText: 'Date of Birth *',
                                  prefixIcon: Icon(Icons.cake_outlined, size: 20),
                                  border: OutlineInputBorder(),
                                  isDense: true,
                                ),
                                child: Text(
                                  _selectedDob != null
                                      ? '${_selectedDob!.day}/${_selectedDob!.month}/${_selectedDob!.year}'
                                      : 'Select DOB',
                                  style: const TextStyle(fontSize: 14),
                                ),
                              ),
                            ),
                          ),
                          const SizedBox(width: 10),
                          Expanded(
                            flex: 2,
                            child: InputDecorator(
                              decoration: const InputDecoration(
                                labelText: 'Age',
                                border: OutlineInputBorder(),
                                isDense: true,
                              ),
                              child: Text(
                                age != null ? '$age yrs' : '--',
                                style: TextStyle(
                                  fontSize: 14,
                                  fontWeight: FontWeight.bold,
                                  color: age != null && age < 18
                                      ? Colors.red
                                      : Colors.black87,
                                ),
                              ),
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 12),

                      // Phone and Email
                      TextFormField(
                        controller: _phoneController,
                        keyboardType: TextInputType.phone,
                        decoration: const InputDecoration(
                          labelText: 'Mobile Number *',
                          hintText: '10-digit mobile number',
                          prefixIcon: Icon(Icons.phone_outlined, size: 20),
                          border: OutlineInputBorder(),
                          isDense: true,
                        ),
                        validator: (val) {
                          if (val == null || val.trim().isEmpty) {
                            return 'Mobile number is required';
                          }
                          final digits =
                              val.replaceAll(RegExp(r'[^0-9]'), '');
                          if (digits.length < 10) {
                            return 'Valid 10-digit phone number is required';
                          }
                          return null;
                        },
                      ),
                      const SizedBox(height: 12),

                      TextFormField(
                        controller: _emailController,
                        keyboardType: TextInputType.emailAddress,
                        decoration: const InputDecoration(
                          labelText: 'Email Address *',
                          hintText: 'name@example.com',
                          prefixIcon: Icon(Icons.email_outlined, size: 20),
                          border: OutlineInputBorder(),
                          isDense: true,
                        ),
                        validator: (val) {
                          if (val == null || val.trim().isEmpty) {
                            return 'Email is required';
                          }
                          if (!val.contains('@') || !val.contains('.')) {
                            return 'Enter a valid email address';
                          }
                          return null;
                        },
                      ),
                      const SizedBox(height: 12),

                      // Blood Group Dropdown
                      DropdownButtonFormField<String>(
                        initialValue: _selectedBloodGroup,
                        decoration: const InputDecoration(
                          labelText: 'Blood Group *',
                          prefixIcon:
                              Icon(Icons.water_drop_outlined, size: 20),
                          border: OutlineInputBorder(),
                          isDense: true,
                        ),
                        items: _bloodGroups.map((bg) {
                          return DropdownMenuItem(
                            value: bg,
                            child: Text(
                              bg,
                              style: const TextStyle(fontWeight: FontWeight.w600),
                            ),
                          );
                        }).toList(),
                        onChanged: (val) {
                          if (val != null) {
                            setState(() => _selectedBloodGroup = val);
                          }
                        },
                      ),
                      const SizedBox(height: 12),

                      // Address & City
                      Row(
                        children: [
                          Expanded(
                            flex: 3,
                            child: TextFormField(
                              controller: _addressController,
                              decoration: const InputDecoration(
                                labelText: 'Address / Area *',
                                hintText: 'Street / Colony',
                                border: OutlineInputBorder(),
                                isDense: true,
                              ),
                              validator: (val) {
                                if (val == null || val.trim().isEmpty) {
                                  return 'Address is required';
                                }
                                return null;
                              },
                            ),
                          ),
                          const SizedBox(width: 10),
                          Expanded(
                            flex: 2,
                            child: TextFormField(
                              controller: _cityController,
                              decoration: const InputDecoration(
                                labelText: 'City *',
                                hintText: 'City',
                                border: OutlineInputBorder(),
                                isDense: true,
                              ),
                              validator: (val) {
                                if (val == null || val.trim().isEmpty) {
                                  return 'City is required';
                                }
                                return null;
                              },
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 18),

                      // Section 2: Emergency Contact
                      const Text(
                        '2. Emergency Contact',
                        style: TextStyle(
                          fontSize: 14,
                          fontWeight: FontWeight.bold,
                          color: NetraColors.textPrimary,
                        ),
                      ),
                      const SizedBox(height: 12),

                      Row(
                        children: [
                          Expanded(
                            child: TextFormField(
                              controller: _emergencyNameController,
                              decoration: const InputDecoration(
                                labelText: 'Contact Name *',
                                hintText: 'Kin / Guardian',
                                border: OutlineInputBorder(),
                                isDense: true,
                              ),
                              validator: (val) {
                                if (val == null || val.trim().isEmpty) {
                                  return 'Contact name required';
                                }
                                return null;
                              },
                            ),
                          ),
                          const SizedBox(width: 10),
                          Expanded(
                            child: TextFormField(
                              controller: _emergencyPhoneController,
                              keyboardType: TextInputType.phone,
                              decoration: const InputDecoration(
                                labelText: 'Contact Phone *',
                                hintText: '10 digits',
                                border: OutlineInputBorder(),
                                isDense: true,
                              ),
                              validator: (val) {
                                if (val == null || val.trim().isEmpty) {
                                  return 'Phone required';
                                }
                                final digits =
                                    val.replaceAll(RegExp(r'[^0-9]'), '');
                                if (digits.length < 10) {
                                  return 'Valid phone required';
                                }
                                return null;
                              },
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 20),

                      // Section 3: Pre-Screening Self-Declaration
                      const Text(
                        '3. Pre-Screening Self-Declaration',
                        style: TextStyle(
                          fontSize: 14,
                          fontWeight: FontWeight.bold,
                          color: NetraColors.textPrimary,
                        ),
                      ),
                      const SizedBox(height: 6),
                      Text(
                        'NBTC safety checklist for prospective blood donors:',
                        style: TextStyle(fontSize: 12, color: Colors.grey.shade600),
                      ),
                      const SizedBox(height: 8),

                      _buildDeclarationItem(
                        value: _declGoodHealth,
                        onChanged: (v) => setState(() => _declGoodHealth = v ?? false),
                        label: 'I confirm that I am in good general health and feel fit to donate blood.',
                      ),
                      _buildDeclarationItem(
                        value: _declAgeWeight,
                        onChanged: (v) => setState(() => _declAgeWeight = v ?? false),
                        label: 'I confirm that I am at least 18 years old and weigh at least 45 kg.',
                      ),
                      _buildDeclarationItem(
                        value: _declNoRecentSurgery,
                        onChanged: (v) =>
                            setState(() => _declNoRecentSurgery = v ?? false),
                        label:
                            'I have not had major surgery, dental extraction, or gotten a tattoo in the last 6 months.',
                      ),
                      _buildDeclarationItem(
                        value: _declClinicalScreening,
                        onChanged: (v) =>
                            setState(() => _declClinicalScreening = v ?? false),
                        label:
                            'I understand that final medical eligibility is determined on-site by clinical staff.',
                      ),
                      const SizedBox(height: 16),

                      // Section 4: Event Consent
                      Container(
                        padding: const EdgeInsets.all(12),
                        decoration: BoxDecoration(
                          color: const Color(0xFFF8FAFC),
                          borderRadius: BorderRadius.circular(10),
                          border: Border.all(color: const Color(0xFFCBD5E1)),
                        ),
                        child: Row(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Checkbox(
                              value: _consentConfirmed,
                              activeColor: NetraColors.primaryRed,
                              onChanged: (v) =>
                                  setState(() => _consentConfirmed = v ?? false),
                            ),
                            Expanded(
                              child: GestureDetector(
                                onTap: () => setState(
                                    () => _consentConfirmed = !_consentConfirmed),
                                child: Text(
                                  'I consent to my registration information being used for organizing this donation event. I understand that pre-registration does not guarantee medical eligibility, which is decided during clinical screening at the camp.',
                                  style: TextStyle(
                                    fontSize: 12,
                                    color: Colors.grey.shade800,
                                    height: 1.4,
                                  ),
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),

            // Modal Footer Actions
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 14),
              decoration: const BoxDecoration(
                color: Color(0xFFF8FAFC),
                border: Border(
                  top: BorderSide(color: Color(0xFFE2E8F0)),
                ),
              ),
              child: Row(
                children: [
                  Expanded(
                    child: OutlinedButton(
                      style: OutlinedButton.styleFrom(
                        padding: const EdgeInsets.symmetric(vertical: 12),
                        side: const BorderSide(color: Color(0xFFCBD5E1)),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(10),
                        ),
                      ),
                      onPressed: () => Navigator.of(context).pop(),
                      child: const Text(
                        'Cancel',
                        style: TextStyle(color: NetraColors.textSecondary),
                      ),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: ElevatedButton(
                      style: ElevatedButton.styleFrom(
                        backgroundColor: NetraColors.primaryRed,
                        foregroundColor: Colors.white,
                        padding: const EdgeInsets.symmetric(vertical: 12),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(10),
                        ),
                        elevation: 1,
                      ),
                      onPressed: _handleSubmit,
                      child: const Text(
                        'Confirm & Register',
                        style: TextStyle(fontWeight: FontWeight.bold),
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

  Widget _buildDeclarationItem({
    required bool value,
    required ValueChanged<bool?> onChanged,
    required String label,
  }) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 6),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 28,
            height: 28,
            child: Checkbox(
              value: value,
              activeColor: NetraColors.primaryRed,
              onChanged: onChanged,
            ),
          ),
          const SizedBox(width: 8),
          Expanded(
            child: GestureDetector(
              onTap: () => onChanged(!value),
              child: Text(
                label,
                style: TextStyle(
                  fontSize: 12.5,
                  color: Colors.grey.shade800,
                  height: 1.35,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
