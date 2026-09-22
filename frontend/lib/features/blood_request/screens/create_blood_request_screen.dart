import 'package:flutter/material.dart';
import '../models/blood_request.dart';
import '../state/blood_request_controller.dart';

class CreateBloodRequestScreen extends StatefulWidget {
  final BloodRequestController? controller;

  const CreateBloodRequestScreen({super.key, this.controller});

  @override
  State<CreateBloodRequestScreen> createState() =>
      _CreateBloodRequestScreenState();
}

class _CreateBloodRequestScreenState extends State<CreateBloodRequestScreen> {
  late final BloodRequestController _controller;
  final _formKey = GlobalKey<FormState>();

  String _selectedBloodGroup = 'O+';
  int _unitsRequired = 1;
  BloodRequestUrgency _selectedUrgency = BloodRequestUrgency.normal;

  final TextEditingController _hospitalNameController = TextEditingController();
  final TextEditingController _hospitalAddressController =
      TextEditingController();
  final TextEditingController _cityController = TextEditingController();
  final TextEditingController _stateController = TextEditingController();
  final TextEditingController _postalCodeController = TextEditingController();
  final TextEditingController _latitudeController =
      TextEditingController(text: '18.9401');
  final TextEditingController _longitudeController =
      TextEditingController(text: '72.8347');
  final TextEditingController _descriptionController = TextEditingController();

  DateTime _requiredBy = DateTime.now().add(const Duration(hours: 24));

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

  @override
  void initState() {
    super.initState();
    _controller = widget.controller ?? BloodRequestController();
  }

  @override
  void dispose() {
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

  Future<void> _pickDeadline() async {
    final now = DateTime.now();
    final pickedDate = await showDatePicker(
      context: context,
      initialDate: _requiredBy.isAfter(now)
          ? _requiredBy
          : now.add(const Duration(hours: 1)),
      firstDate: now,
      lastDate: now.add(const Duration(days: 30)),
    );

    if (pickedDate != null && mounted) {
      final pickedTime = await showTimePicker(
        context: context,
        initialTime: TimeOfDay.fromDateTime(_requiredBy),
      );

      if (pickedTime != null && mounted) {
        final combined = DateTime(
          pickedDate.year,
          pickedDate.month,
          pickedDate.day,
          pickedTime.hour,
          pickedTime.minute,
        );

        if (combined.isAfter(DateTime.now())) {
          setState(() {
            _requiredBy = combined;
          });
        } else {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text('Deadline must be in the future.'),
              backgroundColor: Colors.red,
            ),
          );
        }
      }
    }
  }

  Future<void> _submitRequest() async {
    if (!_formKey.currentState!.validate()) return;

    final lat = double.tryParse(_latitudeController.text.trim());
    final lng = double.tryParse(_longitudeController.text.trim());

    if (lat == null || lat < -90.0 || lat > 90.0) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
            content: Text('Latitude must be between -90 and 90.'),
            backgroundColor: Colors.red),
      );
      return;
    }

    if (lng == null || lng < -180.0 || lng > 180.0) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
            content: Text('Longitude must be between -180 and 180.'),
            backgroundColor: Colors.red),
      );
      return;
    }

    final payload = <String, dynamic>{
      'bloodGroup': _selectedBloodGroup,
      'unitsRequired': _unitsRequired,
      'urgency': _selectedUrgency.name.toUpperCase(),
      'hospitalName': _hospitalNameController.text.trim(),
      'hospitalAddress': _hospitalAddressController.text.trim(),
      'city': _cityController.text.trim(),
      'state': _stateController.text.trim(),
      'postalCode': _postalCodeController.text.trim(),
      'latitude': lat,
      'longitude': lng,
      'requiredBy': _requiredBy.toUtc().toIso8601String(),
      if (_descriptionController.text.trim().isNotEmpty)
        'description': _descriptionController.text.trim(),
    };

    final created = await _controller.createRequest(payload);
    if (created != null && mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Blood request published successfully!'),
          backgroundColor: Color(0xFF16A34A),
        ),
      );
      Navigator.pop(context, true);
    } else if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
              _controller.errorMessage ?? 'Failed to create blood request.'),
          backgroundColor: Colors.red,
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('New Blood Request',
            style: TextStyle(fontWeight: FontWeight.w700)),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Notice banner
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: const Color(0xFFFEF2F2),
                  borderRadius: BorderRadius.circular(10),
                  border: Border.all(color: const Color(0xFFFECACA)),
                ),
                child: Row(
                  children: [
                    const Icon(Icons.info_outline,
                        color: Color(0xFFDC2626), size: 20),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Text(
                        'Operational hospital details only. Never enter patient names, telephone numbers, or medical history.',
                        style: TextStyle(
                          fontSize: 12,
                          color: Colors.red.shade900,
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 20),

              // Blood Group & Units Row
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                    flex: 1,
                    child: DropdownButtonFormField<String>(
                      value: _selectedBloodGroup,
                      decoration: const InputDecoration(
                        labelText: 'Blood Group',
                        border: OutlineInputBorder(),
                        contentPadding:
                            EdgeInsets.symmetric(horizontal: 12, vertical: 14),
                      ),
                      items: _bloodGroups.map((bg) {
                        return DropdownMenuItem(
                          value: bg,
                          child: Text(bg,
                              style:
                                  const TextStyle(fontWeight: FontWeight.bold)),
                        );
                      }).toList(),
                      onChanged: (val) {
                        if (val != null)
                          setState(() => _selectedBloodGroup = val);
                      },
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    flex: 1,
                    child: TextFormField(
                      initialValue: '$_unitsRequired',
                      decoration: const InputDecoration(
                        labelText: 'Units (1-50)',
                        border: OutlineInputBorder(),
                        contentPadding:
                            EdgeInsets.symmetric(horizontal: 12, vertical: 14),
                      ),
                      keyboardType: TextInputType.number,
                      validator: (val) {
                        final parsed = int.tryParse(val?.trim() ?? '');
                        if (parsed == null || parsed < 1 || parsed > 50) {
                          return 'Enter 1 - 50';
                        }
                        return null;
                      },
                      onChanged: (val) {
                        final parsed = int.tryParse(val.trim());
                        if (parsed != null && parsed >= 1 && parsed <= 50) {
                          _unitsRequired = parsed;
                        }
                      },
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),

              // Urgency Selector
              const Text('Urgency Level',
                  style: TextStyle(fontWeight: FontWeight.w600, fontSize: 13)),
              const SizedBox(height: 6),
              Row(
                children: BloodRequestUrgency.values.map((urgency) {
                  final isSelected = _selectedUrgency == urgency;
                  return Expanded(
                    child: Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 4),
                      child: ChoiceChip(
                        label: Text(urgency.displayName),
                        selected: isSelected,
                        selectedColor: urgency == BloodRequestUrgency.critical
                            ? const Color(0xFFFEE2E2)
                            : urgency == BloodRequestUrgency.urgent
                                ? const Color(0xFFFEF3C7)
                                : const Color(0xFFDBEAFE),
                        labelStyle: TextStyle(
                          fontSize: 12,
                          fontWeight:
                              isSelected ? FontWeight.w700 : FontWeight.w500,
                          color:
                              isSelected ? urgency.color : Colors.grey.shade700,
                        ),
                        onSelected: (selected) {
                          if (selected)
                            setState(() => _selectedUrgency = urgency);
                        },
                      ),
                    ),
                  );
                }).toList(),
              ),
              const SizedBox(height: 16),

              // Hospital Name & Address
              TextFormField(
                controller: _hospitalNameController,
                decoration: const InputDecoration(
                  labelText: 'Hospital Name *',
                  hintText: 'e.g. Apollo Memorial Hospital',
                  border: OutlineInputBorder(),
                ),
                validator: (val) => val == null || val.trim().isEmpty
                    ? 'Hospital name is required'
                    : null,
              ),
              const SizedBox(height: 14),
              TextFormField(
                controller: _hospitalAddressController,
                decoration: const InputDecoration(
                  labelText: 'Hospital Address *',
                  hintText: 'e.g. 100 Central Road, Block B',
                  border: OutlineInputBorder(),
                ),
                validator: (val) => val == null || val.trim().isEmpty
                    ? 'Hospital address is required'
                    : null,
              ),
              const SizedBox(height: 14),

              // City, State, Postal Code
              Row(
                children: [
                  Expanded(
                    flex: 2,
                    child: TextFormField(
                      controller: _cityController,
                      decoration: const InputDecoration(
                        labelText: 'City *',
                        border: OutlineInputBorder(),
                      ),
                      validator: (val) => val == null || val.trim().isEmpty
                          ? 'City required'
                          : null,
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    flex: 2,
                    child: TextFormField(
                      controller: _stateController,
                      decoration: const InputDecoration(
                        labelText: 'State *',
                        border: OutlineInputBorder(),
                      ),
                      validator: (val) => val == null || val.trim().isEmpty
                          ? 'State required'
                          : null,
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    flex: 2,
                    child: TextFormField(
                      controller: _postalCodeController,
                      decoration: const InputDecoration(
                        labelText: 'PIN Code *',
                        border: OutlineInputBorder(),
                      ),
                      validator: (val) => val == null || val.trim().isEmpty
                          ? 'PIN required'
                          : null,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 14),

              // Coordinates (GPS)
              Row(
                children: [
                  Expanded(
                    child: TextFormField(
                      controller: _latitudeController,
                      decoration: const InputDecoration(
                        labelText: 'Latitude (-90 to 90)',
                        border: OutlineInputBorder(),
                      ),
                      keyboardType:
                          const TextInputType.numberWithOptions(decimal: true),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: TextFormField(
                      controller: _longitudeController,
                      decoration: const InputDecoration(
                        labelText: 'Longitude (-180 to 180)',
                        border: OutlineInputBorder(),
                      ),
                      keyboardType:
                          const TextInputType.numberWithOptions(decimal: true),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),

              // Required By Deadline Picker
              ListTile(
                contentPadding:
                    const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(8),
                  side: BorderSide(color: Colors.grey.shade400),
                ),
                leading:
                    const Icon(Icons.calendar_today, color: Color(0xFFDC2626)),
                title: const Text('Required By Deadline *',
                    style: TextStyle(fontSize: 12, color: Colors.grey)),
                subtitle: Text(
                  _formatDateTime(_requiredBy),
                  style: const TextStyle(
                      fontSize: 15, fontWeight: FontWeight.w700),
                ),
                trailing: TextButton(
                  onPressed: _pickDeadline,
                  child: const Text('Change'),
                ),
              ),
              const SizedBox(height: 16),

              // Description / Medical context
              TextFormField(
                controller: _descriptionController,
                decoration: const InputDecoration(
                  labelText: 'General Context / Requirement Notes (Optional)',
                  hintText:
                      'e.g. Needed for scheduled bypass procedure tomorrow morning',
                  border: OutlineInputBorder(),
                ),
                maxLines: 3,
              ),
              const SizedBox(height: 24),

              // Submit Button
              SizedBox(
                width: double.infinity,
                height: 50,
                child: ElevatedButton(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFFDC2626),
                    foregroundColor: Colors.white,
                    shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12)),
                  ),
                  onPressed: _controller.isSubmitting ? null : _submitRequest,
                  child: _controller.isSubmitting
                      ? const SizedBox(
                          width: 24,
                          height: 24,
                          child: CircularProgressIndicator(
                              color: Colors.white, strokeWidth: 2),
                        )
                      : const Text(
                          'Publish Blood Request',
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
