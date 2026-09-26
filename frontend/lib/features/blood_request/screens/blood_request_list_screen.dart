import 'package:flutter/material.dart';
import '../../../core/location/location_service.dart';
import '../models/blood_request.dart';
import '../state/blood_request_controller.dart';
import '../widgets/blood_request_card.dart';
import 'blood_request_details_screen.dart';
import 'create_blood_request_screen.dart';
import 'my_blood_requests_screen.dart';

class BloodRequestListScreen extends StatefulWidget {
  final BloodRequestController? controller;
  final LocationService? locationService;

  const BloodRequestListScreen({
    super.key,
    this.controller,
    this.locationService,
  });

  @override
  State<BloodRequestListScreen> createState() => _BloodRequestListScreenState();
}

class _BloodRequestListScreenState extends State<BloodRequestListScreen>
    with SingleTickerProviderStateMixin {
  late final BloodRequestController _controller;
  late final LocationService _locationService;
  late final TabController _tabController;
  final TextEditingController _citySearchController = TextEditingController();

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
  String _selectedGroup = 'All';
  String _selectedUrgency = 'All';
  bool _locationUnavailable = false;
  double? _userLatitude;
  double? _userLongitude;

  @override
  void initState() {
    super.initState();
    _controller = widget.controller ?? BloodRequestController();
    _locationService = widget.locationService ?? DefaultLocationService();
    _tabController = TabController(length: 2, vsync: this);
    _controller.addListener(_onControllerUpdate);

    WidgetsBinding.instance.addPostFrameCallback((_) {
      _controller.loadDiscoverableRequests();
      _fetchNearbyRequests();
    });
  }

  Future<void> _fetchNearbyRequests() async {
    final loc =
        await _locationService.getCurrentLocation(approximateOnly: true);
    if (loc != null && loc.latitude != null && loc.longitude != null) {
      if (mounted) {
        setState(() {
          _userLatitude = loc.latitude;
          _userLongitude = loc.longitude;
          _locationUnavailable = false;
        });
      }
      await _controller.loadNearbyRequests(
        latitude: loc.latitude!,
        longitude: loc.longitude!,
      );
    } else {
      if (mounted) {
        setState(() {
          _userLatitude = null;
          _userLongitude = null;
          _locationUnavailable = true;
        });
      }
    }
  }

  @override
  void dispose() {
    _controller.removeListener(_onControllerUpdate);
    _tabController.dispose();
    _citySearchController.dispose();
    super.dispose();
  }

  void _onControllerUpdate() {
    if (mounted) setState(() {});
  }

  void _applyFilters() {
    _controller.setFilters(
      bloodGroup: _selectedGroup == 'All' ? null : _selectedGroup,
      city: _citySearchController.text.trim().isEmpty
          ? null
          : _citySearchController.text.trim(),
      urgency: _selectedUrgency == 'All' ? null : _selectedUrgency,
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text(
          'Blood Requests',
          style: TextStyle(fontWeight: FontWeight.w700),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.bookmark_outline),
            tooltip: 'My Requests',
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (_) =>
                      MyBloodRequestsScreen(controller: _controller),
                ),
              );
            },
          ),
        ],
        bottom: TabBar(
          controller: _tabController,
          tabs: const [
            Tab(text: 'All Open Requests'),
            Tab(text: 'Nearby Camps / Hospitals'),
          ],
        ),
      ),
      body: Column(
        children: [
          // Filter section
          _buildFilterBar(),
          Expanded(
            child: _controller.isLoading
                ? const Center(child: CircularProgressIndicator())
                : TabBarView(
                    controller: _tabController,
                    children: [
                      // Tab 1: Discoverable open requests
                      _buildRequestsList(
                        requests: _controller.discoverableRequests,
                        onRefresh: () =>
                            _controller.loadDiscoverableRequests(refresh: true),
                        emptyMessage:
                            'No active blood requests found matching your filters.',
                      ),
                      // Tab 2: Nearby requests
                      (_locationUnavailable ||
                              _userLatitude == null ||
                              _userLongitude == null)
                          ? Center(
                              child: Padding(
                                padding: const EdgeInsets.symmetric(
                                    horizontal: 24.0),
                                child: Column(
                                  mainAxisAlignment: MainAxisAlignment.center,
                                  children: [
                                    Icon(Icons.location_off_outlined,
                                        size: 64, color: Colors.grey.shade400),
                                    const SizedBox(height: 16),
                                    Text(
                                      'Device location unavailable or permission denied.',
                                      textAlign: TextAlign.center,
                                      style: TextStyle(
                                        fontSize: 16,
                                        fontWeight: FontWeight.w600,
                                        color: Colors.grey.shade800,
                                      ),
                                    ),
                                    const SizedBox(height: 8),
                                    Text(
                                      'Enable location access to discover urgent blood requests near your area.',
                                      textAlign: TextAlign.center,
                                      style: TextStyle(
                                        fontSize: 13,
                                        color: Colors.grey.shade600,
                                      ),
                                    ),
                                    const SizedBox(height: 16),
                                    ElevatedButton.icon(
                                      onPressed: _fetchNearbyRequests,
                                      icon: const Icon(Icons.my_location),
                                      label:
                                          const Text('Retry Location Access'),
                                      style: ElevatedButton.styleFrom(
                                        backgroundColor:
                                            const Color(0xFFDC2626),
                                        foregroundColor: Colors.white,
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                            )
                          : _buildRequestsList(
                              requests: _controller.nearbyRequests,
                              onRefresh: _fetchNearbyRequests,
                              emptyMessage:
                                  'No nearby blood requests found within search radius.',
                            ),
                    ],
                  ),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        icon: const Icon(Icons.add),
        label: const Text('Request Blood'),
        backgroundColor: const Color(0xFFDC2626),
        foregroundColor: Colors.white,
        onPressed: () async {
          final result = await Navigator.push(
            context,
            MaterialPageRoute(
              builder: (_) => CreateBloodRequestScreen(controller: _controller),
            ),
          );
          if (result == true) {
            _controller.loadDiscoverableRequests(refresh: true);
          }
        },
      ),
    );
  }

  Widget _buildFilterBar() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      decoration: BoxDecoration(
        color: Colors.white,
        border: Border(bottom: BorderSide(color: Colors.grey.shade200)),
      ),
      child: Column(
        children: [
          // City search input
          TextField(
            controller: _citySearchController,
            decoration: InputDecoration(
              hintText: 'Search city (e.g. Mumbai, Pune)...',
              prefixIcon: const Icon(Icons.search, size: 20),
              suffixIcon: _citySearchController.text.isNotEmpty
                  ? IconButton(
                      icon: const Icon(Icons.clear, size: 18),
                      onPressed: () {
                        _citySearchController.clear();
                        _applyFilters();
                      },
                    )
                  : null,
              contentPadding:
                  const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              isDense: true,
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(10),
                borderSide: BorderSide(color: Colors.grey.shade300),
              ),
            ),
            onSubmitted: (_) => _applyFilters(),
          ),
          const SizedBox(height: 8),
          // Blood group chips
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              children: _bloodGroups.map((group) {
                final isSelected = _selectedGroup == group;
                return Padding(
                  padding: const EdgeInsets.only(right: 6),
                  child: FilterChip(
                    label: Text(group),
                    selected: isSelected,
                    selectedColor: const Color(0xFFFEE2E2),
                    checkmarkColor: const Color(0xFFDC2626),
                    labelStyle: TextStyle(
                      fontSize: 12,
                      fontWeight:
                          isSelected ? FontWeight.w700 : FontWeight.w500,
                      color: isSelected
                          ? const Color(0xFFDC2626)
                          : Colors.grey.shade700,
                    ),
                    onSelected: (selected) {
                      setState(() {
                        _selectedGroup = group;
                      });
                      _applyFilters();
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

  Widget _buildRequestsList({
    required List<BloodRequestSummary> requests,
    required Future<void> Function() onRefresh,
    required String emptyMessage,
  }) {
    if (requests.isEmpty) {
      return RefreshIndicator(
        onRefresh: onRefresh,
        child: ListView(
          physics: const AlwaysScrollableScrollPhysics(),
          children: [
            const SizedBox(height: 80),
            Icon(Icons.bloodtype_outlined,
                size: 64, color: Colors.grey.shade400),
            const SizedBox(height: 16),
            Center(
              child: Text(
                emptyMessage,
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: 14,
                  color: Colors.grey.shade600,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ),
          ],
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: onRefresh,
      child: ListView.builder(
        physics: const AlwaysScrollableScrollPhysics(),
        itemCount: requests.length,
        itemBuilder: (context, index) {
          final req = requests[index];
          return BloodRequestCard(
            request: req,
            onTap: () {
              Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (_) => BloodRequestDetailsScreen(
                    requestId: req.id,
                    controller: _controller,
                  ),
                ),
              );
            },
          );
        },
      ),
    );
  }
}
