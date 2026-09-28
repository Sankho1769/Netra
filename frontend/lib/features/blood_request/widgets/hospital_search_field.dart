import 'dart:async';
import 'package:flutter/material.dart';
import '../../../core/theme/netra_colors.dart';
import '../../../core/theme/netra_typography.dart';
import '../models/verified_hospital_model.dart';
import '../services/hospital_api_service.dart';

class HospitalSearchField extends StatefulWidget {
  final TextEditingController? controller;
  final ValueChanged<VerifiedHospitalModel?> onHospitalSelected;
  final String? initialStatus;
  final String? initialValue;

  const HospitalSearchField({
    super.key,
    this.controller,
    required this.onHospitalSelected,
    this.initialStatus,
    this.initialValue,
  });

  @override
  State<HospitalSearchField> createState() => _HospitalSearchFieldState();
}

class _HospitalSearchFieldState extends State<HospitalSearchField> {
  final HospitalApiService _apiService = HospitalApiService();
  List<VerifiedHospitalModel> _suggestions = [];
  bool _isLoading = false;
  Timer? _debounce;
  VerifiedHospitalModel? _selectedHospital;
  TextEditingController? _internalController;

  TextEditingController get _effectiveController =>
      widget.controller ?? _internalController!;

  @override
  void initState() {
    super.initState();
    if (widget.controller == null) {
      _internalController =
          TextEditingController(text: widget.initialValue ?? '');
    }
  }

  @override
  void dispose() {
    _debounce?.cancel();
    _internalController?.dispose();
    super.dispose();
  }

  void _onSearchChanged(String query) {
    if (_selectedHospital != null && _selectedHospital!.name != query) {
      setState(() {
        _selectedHospital = null;
      });
      widget.onHospitalSelected(null);
    }

    _debounce?.cancel();
    if (query.trim().length < 2) {
      setState(() {
        _suggestions = [];
        _isLoading = false;
      });
      return;
    }

    _debounce = Timer(const Duration(milliseconds: 350), () async {
      setState(() => _isLoading = true);
      try {
        final results = await _apiService.searchHospitals(query: query);
        if (mounted) {
          setState(() {
            _suggestions = results;
            _isLoading = false;
          });
        }
      } catch (_) {
        if (mounted) {
          setState(() => _isLoading = false);
        }
      }
    });
  }

  void _selectHospital(VerifiedHospitalModel hospital) {
    _effectiveController.text = hospital.name;
    setState(() {
      _selectedHospital = hospital;
      _suggestions = [];
    });
    widget.onHospitalSelected(hospital);
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        TextFormField(
          controller: _effectiveController,
          decoration: InputDecoration(
            labelText: 'Hospital or Medical Institution *',
            hintText: 'Search verified hospital (e.g. AIIMS, Fortis, KEM)',
            prefixIcon: const Icon(Icons.local_hospital_outlined),
            suffixIcon: _isLoading
                ? const Padding(
                    padding: EdgeInsets.all(12),
                    child: SizedBox(
                      width: 18,
                      height: 18,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    ),
                  )
                : (_selectedHospital != null
                    ? const Icon(Icons.verified, color: Color(0xFF16A34A))
                    : null),
            border: const OutlineInputBorder(),
          ),
          onChanged: _onSearchChanged,
          validator: (val) => val == null || val.trim().isEmpty
              ? 'Hospital name is required'
              : null,
        ),
        if (_suggestions.isNotEmpty)
          Container(
            margin: const EdgeInsets.only(top: 4),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(8),
              border: Border.all(color: NetraColors.borderGray),
              boxShadow: const [
                BoxShadow(
                  color: Colors.black12,
                  blurRadius: 8,
                  offset: Offset(0, 4),
                ),
              ],
            ),
            constraints: const BoxConstraints(maxHeight: 220),
            child: ListView.separated(
              shrinkWrap: true,
              padding: EdgeInsets.zero,
              itemCount: _suggestions.length,
              separatorBuilder: (_, __) => const Divider(height: 1),
              itemBuilder: (context, index) {
                final h = _suggestions[index];
                return ListTile(
                  dense: true,
                  leading: Icon(
                    h.hasBloodBank
                        ? Icons.local_hospital
                        : Icons.medical_services_outlined,
                    color: h.hasBloodBank
                        ? NetraColors.primaryRed
                        : NetraColors.textPrimary,
                    size: 20,
                  ),
                  title: Row(
                    children: [
                      Expanded(
                        child: Text(
                          h.name,
                          style: NetraTypography.bodyMedium.copyWith(
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                      if (h.isVerified)
                        Container(
                          padding: const EdgeInsets.symmetric(
                              horizontal: 6, vertical: 2),
                          decoration: BoxDecoration(
                            color: const Color(0xFFDCFCE7),
                            borderRadius: BorderRadius.circular(4),
                          ),
                          child: const Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Icon(Icons.check_circle,
                                  size: 11, color: Color(0xFF16A34A)),
                              SizedBox(width: 3),
                              Text(
                                "VERIFIED",
                                style: TextStyle(
                                  fontSize: 9,
                                  fontWeight: FontWeight.w800,
                                  color: Color(0xFF16A34A),
                                ),
                              ),
                            ],
                          ),
                        ),
                    ],
                  ),
                  subtitle: Text(
                    "${h.address}, ${h.city}",
                    style: NetraTypography.labelSmall.copyWith(
                      color: NetraColors.textSecondary,
                    ),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                  onTap: () => _selectHospital(h),
                );
              },
            ),
          ),
        if (_selectedHospital != null) ...[
          const SizedBox(height: 6),
          Row(
            children: [
              const Icon(Icons.shield_outlined,
                  size: 14, color: Color(0xFF16A34A)),
              const SizedBox(width: 4),
              Text(
                _selectedHospital!.hasBloodBank
                    ? "Verified Hospital & Authorized Blood Bank"
                    : "Verified Clinical Institution",
                style: const TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.w600,
                  color: Color(0xFF16A34A),
                ),
              ),
            ],
          ),
        ],
      ],
    );
  }
}
