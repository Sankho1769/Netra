import 'package:flutter/material.dart';
import '../../../common/widgets/common_widgets.dart';
import '../../../core/location/location_service.dart';
import '../../../core/responsive/responsive.dart';
import '../../../core/theme/netra_colors.dart';
import '../../../core/theme/netra_spacing.dart';
import '../../../core/theme/netra_typography.dart';
import '../../blood_request/models/blood_request.dart';
import '../../blood_request/models/verified_hospital_model.dart';
import '../../blood_request/screens/blood_request_details_screen.dart';
import '../../blood_request/widgets/blood_request_card.dart';
import '../../blood_request/widgets/hospital_search_field.dart';
import '../../bloodbank/models/blood_bank.dart';
import '../../bloodbank/screens/bloodbank_details_screen.dart';
import '../../bloodbank/widgets/bloodbank_card.dart';
import '../services/emergency_api_service.dart';

class EmergencyNearbyScreen extends StatefulWidget {
  final int initialTabIndex;
  final EmergencyApiService? apiService;
  final LocationService? locationService;

  const EmergencyNearbyScreen({
    super.key,
    this.initialTabIndex = 0,
    this.apiService,
    this.locationService,
  });

  @override
  State<EmergencyNearbyScreen> createState() => _EmergencyNearbyScreenState();
}

class _EmergencyNearbyScreenState extends State<EmergencyNearbyScreen>
    with SingleTickerProviderStateMixin {
  late final TabController _tabController;
  late final EmergencyApiService _apiService;
  late final LocationService _locationService;

  // Real device coordinates or verified healthcare center coordinates
  double? _latitude;
  double? _longitude;
  String? _locationLabel;
  double _radiusKm = 15.0;
  String? _selectedBloodGroup;

  bool _isDetectingLocation = false;
  bool _locationUnavailable = false;
  String? _locationErrorMessage;
  bool _showFacilitySearch = false;

  bool _isLoadingBanks = false;
  bool _isLoadingRequests = false;
  String? _bankError;
  String? _requestError;

  List<BloodBankSummary> _bloodBanks = [];
  List<BloodRequestSummary> _bloodRequests = [];

  final List<String> _bloodGroups = [
    'All',
    'A+',
    'A-',
    'B+',
    'B-',
    'O+',
    'O-',
    'AB+',
    'AB-'
  ];

  final List<double> _radii = [5.0, 10.0, 15.0, 25.0, 50.0];

  @override
  void initState() {
    super.initState();
    _tabController = TabController(
      length: 2,
      vsync: this,
      initialIndex: widget.initialTabIndex,
    );
    _apiService = widget.apiService ?? EmergencyApiService();
    _locationService = widget.locationService ?? DefaultLocationService();

    _detectLocationAndFetch();
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  Future<void> _detectLocationAndFetch() async {
    setState(() {
      _isDetectingLocation = true;
      _locationUnavailable = false;
      _locationErrorMessage = null;
    });

    try {
      final loc =
          await _locationService.getCurrentLocation(approximateOnly: true);
      if (loc != null && mounted) {
        setState(() {
          _latitude = loc.latitude;
          _longitude = loc.longitude;
          _locationLabel = loc.city != null ? "${loc.city} (GPS)" : "Device GPS";
          _isDetectingLocation = false;
          _locationUnavailable = false;
          _showFacilitySearch = false;
        });
        _loadNearbyBloodBanks();
        _loadNearbyBloodRequests();
        return;
      }
    } catch (_) {}

    if (mounted) {
      setState(() {
        _latitude = null;
        _longitude = null;
        _locationLabel = null;
        _isDetectingLocation = false;
        _locationUnavailable = true;
        _locationErrorMessage =
            "Location permission was denied or device GPS is unavailable. NETRA never assumes arbitrary fallback coordinates. Please enable device location or select a verified healthcare center.";
      });
    }
  }

  void _onHospitalSelected(VerifiedHospitalModel? hospital) {
    if (hospital != null &&
        hospital.latitude != null &&
        hospital.longitude != null) {
      setState(() {
        _latitude = hospital.latitude;
        _longitude = hospital.longitude;
        _locationLabel = hospital.name;
        _locationUnavailable = false;
        _showFacilitySearch = false;
      });
      _loadNearbyBloodBanks();
      _loadNearbyBloodRequests();
    }
  }

  Future<void> _loadNearbyBloodBanks() async {
    if (_latitude == null || _longitude == null) return;

    setState(() {
      _isLoadingBanks = true;
      _bankError = null;
    });

    try {
      final banks = await _apiService.getNearbyBloodBanks(
        latitude: _latitude!,
        longitude: _longitude!,
        radiusKm: _radiusKm,
        bloodGroup: _selectedBloodGroup == 'All' ? null : _selectedBloodGroup,
      );
      if (mounted) {
        setState(() {
          _bloodBanks = banks;
          _isLoadingBanks = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _isLoadingBanks = false;
          _bankError = "Could not load nearby blood banks. Please retry.";
        });
      }
    }
  }

  Future<void> _loadNearbyBloodRequests() async {
    if (_latitude == null || _longitude == null) return;

    setState(() {
      _isLoadingRequests = true;
      _requestError = null;
    });

    try {
      final reqs = await _apiService.getNearbyBloodRequests(
        latitude: _latitude!,
        longitude: _longitude!,
        radiusKm: _radiusKm,
        bloodGroup: _selectedBloodGroup == 'All' ? null : _selectedBloodGroup,
      );
      if (mounted) {
        setState(() {
          _bloodRequests = reqs;
          _isLoadingRequests = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _isLoadingRequests = false;
          _requestError = "Could not load nearby requests. Please retry.";
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return ResponsiveScaffold(
      appBar: NetraAppBar(
        title: "Emergency Nearby Discovery",
        showBackButton: true,
        bottom: (_latitude != null && _longitude != null)
            ? TabBar(
                controller: _tabController,
                indicatorColor: const Color(0xFFDC2626),
                labelColor: const Color(0xFFDC2626),
                unselectedLabelColor: NetraColors.textSecondary,
                tabs: const [
                  Tab(
                      icon: Icon(Icons.local_hospital_rounded),
                      text: "Blood Centres"),
                  Tab(
                      icon: Icon(Icons.bloodtype_outlined),
                      text: "Active Requests"),
                ],
              )
            : null,
      ),
      body: ResponsiveContainer.standard(
        scrollable: false,
        child: _buildBodyContent(),
      ),
    );
  }

  Widget _buildBodyContent() {
    if (_isDetectingLocation) {
      return const Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            CircularProgressIndicator(color: Color(0xFFDC2626)),
            NetraSpacing.gapH16,
            Text("Detecting your location..."),
          ],
        ),
      );
    }

    if (_latitude == null || _longitude == null || _locationUnavailable) {
      return _buildLocationUnavailableView();
    }

    return Column(
      children: [
        _buildFilterHeader(),
        Expanded(
          child: TabBarView(
            controller: _tabController,
            children: [
              _buildBloodBanksTab(),
              _buildBloodRequestsTab(),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildLocationUnavailableView() {
    return Center(
      child: SingleChildScrollView(
        padding: NetraSpacing.cardPaddingSpacious,
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              padding: const EdgeInsets.all(16),
              decoration: const BoxDecoration(
                color: Color(0xFFFEE2E2),
                shape: BoxShape.circle,
              ),
              child: const Icon(
                Icons.location_off_rounded,
                color: Color(0xFFDC2626),
                size: 48,
              ),
            ),
            NetraSpacing.gapH16,
            Text(
              "Location Access Required",
              style: NetraTypography.headlineSmall.copyWith(
                fontWeight: FontWeight.bold,
                color: NetraColors.textPrimary,
              ),
              textAlign: TextAlign.center,
            ),
            NetraSpacing.gapH8,
            Text(
              "Emergency Mode requires device location to accurately locate nearby verified blood banks and active requests. NETRA never assumes or falls back to arbitrary coordinates.",
              style: NetraTypography.bodyMedium
                  .copyWith(color: NetraColors.textSecondary),
              textAlign: TextAlign.center,
            ),
            if (_locationErrorMessage != null) ...[
              NetraSpacing.gapH12,
              Container(
                padding: NetraSpacing.cardPaddingStandard,
                decoration: BoxDecoration(
                  color: const Color(0xFFFEF2F2),
                  borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
                  border: Border.all(color: const Color(0xFFFCA5A5)),
                ),
                child: Text(
                  _locationErrorMessage!,
                  style: NetraTypography.bodySmall
                      .copyWith(color: const Color(0xFF991B1B)),
                  textAlign: TextAlign.center,
                ),
              ),
            ],
            NetraSpacing.gapH24,
            SizedBox(
              width: double.infinity,
              height: 48,
              child: ElevatedButton.icon(
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFFDC2626),
                  foregroundColor: NetraColors.surfaceWhite,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
                  ),
                ),
                onPressed: _detectLocationAndFetch,
                icon: const Icon(Icons.my_location_rounded, size: 20),
                label: const Text("Enable / Retry Location Detection"),
              ),
            ),
            NetraSpacing.gapH12,
            SizedBox(
              width: double.infinity,
              height: 48,
              child: OutlinedButton.icon(
                style: OutlinedButton.styleFrom(
                  foregroundColor: NetraColors.textPrimary,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
                  ),
                ),
                onPressed: () {
                  setState(() {
                    _showFacilitySearch = !_showFacilitySearch;
                  });
                },
                icon: const Icon(Icons.local_hospital_outlined, size: 20),
                label: Text(_showFacilitySearch
                    ? "Hide Healthcare Search"
                    : "Select Healthcare Center / Hospital"),
              ),
            ),
            if (_showFacilitySearch) ...[
              NetraSpacing.gapH20,
              Container(
                padding: NetraSpacing.cardPaddingStandard,
                decoration: BoxDecoration(
                  color: NetraColors.surfaceWhite,
                  borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
                  border: Border.all(color: NetraColors.borderSubtle),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      "Search Verified Healthcare Center",
                      style: NetraTypography.titleSmall
                          .copyWith(fontWeight: FontWeight.bold),
                    ),
                    NetraSpacing.gapH4,
                    Text(
                      "Select a registered hospital or blood center to center discovery around its verified location.",
                      style: NetraTypography.bodySmall
                          .copyWith(color: NetraColors.textSecondary),
                    ),
                    NetraSpacing.gapH12,
                    HospitalSearchField(
                      onHospitalSelected: _onHospitalSelected,
                    ),
                  ],
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildFilterHeader() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: const BoxDecoration(
        color: NetraColors.surfaceWhite,
        border: Border(bottom: BorderSide(color: NetraColors.borderSubtle)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Icons.place, size: 16, color: Color(0xFFDC2626)),
              NetraSpacing.gapW4,
              Expanded(
                child: Text(
                  _locationLabel != null
                      ? "Location: $_locationLabel"
                      : "Location: ${_latitude!.toStringAsFixed(3)}, ${_longitude!.toStringAsFixed(3)}",
                  style: NetraTypography.bodySmall.copyWith(
                    fontWeight: FontWeight.w600,
                    color: NetraColors.textPrimary,
                  ),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              InkWell(
                onTap: _detectLocationAndFetch,
                child: Padding(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Icon(Icons.refresh_rounded,
                          size: 14, color: NetraColors.primaryRed),
                      NetraSpacing.gapW4,
                      Text(
                        "Update",
                        style: NetraTypography.labelSmall
                            .copyWith(color: NetraColors.primaryRed),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
          const Divider(height: 12),
          Row(
            children: [
              const Icon(Icons.tune_rounded,
                  size: 18, color: NetraColors.textSecondary),
              NetraSpacing.gapW8,
              Text(
                "Filter by Blood Group:",
                style: NetraTypography.bodySmall
                    .copyWith(fontWeight: FontWeight.bold),
              ),
              const Spacer(),
              DropdownButton<double>(
                value: _radiusKm,
                underline: const SizedBox(),
                isDense: true,
                items: _radii.map((r) {
                  return DropdownMenuItem<double>(
                    value: r,
                    child: Text("${r.toInt()} km radius",
                        style: NetraTypography.bodySmall),
                  );
                }).toList(),
                onChanged: (val) {
                  if (val != null) {
                    setState(() {
                      _radiusKm = val;
                    });
                    _loadNearbyBloodBanks();
                    _loadNearbyBloodRequests();
                  }
                },
              ),
            ],
          ),
          NetraSpacing.gapH4,
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              children: _bloodGroups.map((group) {
                final isSelected =
                    (_selectedBloodGroup == null && group == 'All') ||
                        _selectedBloodGroup == group;
                return Padding(
                  padding: const EdgeInsets.only(right: 6.0),
                  child: ChoiceChip(
                    label: Text(group, style: const TextStyle(fontSize: 12)),
                    selected: isSelected,
                    selectedColor: const Color(0xFFDC2626),
                    labelStyle: TextStyle(
                      color: isSelected
                          ? NetraColors.surfaceWhite
                          : NetraColors.textPrimary,
                      fontWeight:
                          isSelected ? FontWeight.bold : FontWeight.normal,
                    ),
                    backgroundColor: NetraColors.backgroundGray,
                    onSelected: (selected) {
                      if (selected) {
                        setState(() {
                          _selectedBloodGroup = group == 'All' ? null : group;
                        });
                        _loadNearbyBloodBanks();
                        _loadNearbyBloodRequests();
                      }
                    },
                  ),
                );
              }).toList(),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildBloodBanksTab() {
    if (_isLoadingBanks) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_bankError != null) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.error_outline_rounded,
                color: Colors.red, size: 40),
            NetraSpacing.gapH8,
            Text(_bankError!, style: NetraTypography.bodyMedium),
            NetraSpacing.gapH12,
            ElevatedButton(
              onPressed: _loadNearbyBloodBanks,
              child: const Text("Retry"),
            ),
          ],
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: _loadNearbyBloodBanks,
      child: ListView(
        padding: const EdgeInsets.symmetric(vertical: 8),
        children: [
          // Emergency Stock Disclaimer
          Container(
            margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            padding: NetraSpacing.cardPaddingStandard,
            decoration: BoxDecoration(
              color: const Color(0xFFFEF3C7),
              borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
              border: Border.all(color: const Color(0xFFFDE68A)),
            ),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Icon(Icons.announcement_outlined,
                    color: Color(0xFFD97706), size: 20),
                NetraSpacing.gapW8,
                Expanded(
                  child: Text(
                    "Emergency Inventory Advisory: Blood units fluctuate dynamically. Always call the blood center directly to verify unit reservation before dispatching transport.",
                    style: NetraTypography.bodySmall.copyWith(
                      color: const Color(0xFF92400E),
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                ),
              ],
            ),
          ),
          if (_bloodBanks.isEmpty)
            Padding(
              padding: const EdgeInsets.all(32.0),
              child: Center(
                child: Text(
                  "No authorized blood banks found within ${_radiusKm.toInt()} km.",
                  style: NetraTypography.bodyMedium
                      .copyWith(color: NetraColors.textSecondary),
                  textAlign: TextAlign.center,
                ),
              ),
            )
          else
            ..._bloodBanks.map(
              (bank) => Padding(
                padding:
                    const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
                child: BloodBankCard(
                  bank: bank,
                  onTap: () {
                    Navigator.of(context).push(
                      MaterialPageRoute(
                        builder: (context) =>
                            BloodBankDetailsScreen(bloodBankId: bank.id),
                      ),
                    );
                  },
                ),
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildBloodRequestsTab() {
    if (_isLoadingRequests) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_requestError != null) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.error_outline_rounded,
                color: Colors.red, size: 40),
            NetraSpacing.gapH8,
            Text(_requestError!, style: NetraTypography.bodyMedium),
            NetraSpacing.gapH12,
            ElevatedButton(
              onPressed: _loadNearbyBloodRequests,
              child: const Text("Retry"),
            ),
          ],
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: _loadNearbyBloodRequests,
      child: _bloodRequests.isEmpty
          ? Center(
              child: Padding(
                padding: const EdgeInsets.all(32.0),
                child: Text(
                  "No active blood requests found within ${_radiusKm.toInt()} km.",
                  style: NetraTypography.bodyMedium
                      .copyWith(color: NetraColors.textSecondary),
                  textAlign: TextAlign.center,
                ),
              ),
            )
          : ListView.builder(
              padding: const EdgeInsets.symmetric(vertical: 8),
              itemCount: _bloodRequests.length,
              itemBuilder: (context, index) {
                final req = _bloodRequests[index];
                return BloodRequestCard(
                  request: req,
                  onTap: () {
                    Navigator.of(context).push(
                      MaterialPageRoute(
                        builder: (context) =>
                            BloodRequestDetailsScreen(requestId: req.id),
                      ),
                    );
                  },
                );
              },
            ),
    );
  }
}
