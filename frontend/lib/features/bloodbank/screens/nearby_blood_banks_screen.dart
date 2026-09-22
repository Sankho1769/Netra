import 'package:flutter/material.dart';
import '../../../common/widgets/common_widgets.dart';
import '../../../core/location/location_service.dart';
import '../../../core/responsive/responsive.dart';
import '../../../core/theme/netra_colors.dart';
import '../../../core/theme/netra_spacing.dart';
import '../../../core/theme/netra_typography.dart';
import '../models/blood_bank.dart';
import '../services/bloodbank_api_service.dart';
import '../widgets/bloodbank_card.dart';
import 'bloodbank_details_screen.dart';

class NearbyBloodBanksScreen extends StatefulWidget {
  const NearbyBloodBanksScreen({super.key});

  @override
  State<NearbyBloodBanksScreen> createState() => _NearbyBloodBanksScreenState();
}

class _NearbyBloodBanksScreenState extends State<NearbyBloodBanksScreen> {
  final TextEditingController _searchController = TextEditingController();
  final BloodBankApiService _apiService = BloodBankApiService();
  final LocationService _locationService = DefaultLocationService();

  List<BloodBankSummary> _bloodBanks = [];
  bool _isLoading = false;
  String? _errorMessage;
  double? _currentLat;
  double? _currentLon;
  String? _selectedBloodGroup;

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
    _fetchDefaultOrNearby();
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _fetchDefaultOrNearby() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final loc =
          await _locationService.getCurrentLocation(approximateOnly: true);
      if (loc != null && loc.latitude != null && loc.longitude != null) {
        _currentLat = loc.latitude;
        _currentLon = loc.longitude;
        _searchController.text = loc.displayName;
        final list = await _apiService.getNearbyBloodBanks(
          latitude: loc.latitude!,
          longitude: loc.longitude!,
          radiusKm: 25.0,
        );
        if (mounted) {
          setState(() {
            _bloodBanks = list;
            _isLoading = false;
          });
        }
      } else {
        // Fallback to general discovery
        final list = await _apiService.discoverBloodBanks(
          bloodGroup: _selectedBloodGroup,
        );
        if (mounted) {
          setState(() {
            _bloodBanks = list;
            _isLoading = false;
          });
        }
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _errorMessage = e.toString().replaceFirst('Exception: ', '');
          _isLoading = false;
        });
      }
    }
  }

  Future<void> _search() async {
    final query = _searchController.text.trim();
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final list = await _apiService.discoverBloodBanks(
        city: query.isNotEmpty ? query : null,
        bloodGroup: _selectedBloodGroup,
      );
      if (mounted) {
        setState(() {
          _bloodBanks = list;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _errorMessage = e.toString().replaceFirst('Exception: ', '');
          _isLoading = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final isDesktopOrTablet = !context.isMobile;

    return Scaffold(
      appBar: const NetraAppBar(
        title: "Authorized Blood Centres",
      ),
      backgroundColor: NetraColors.backgroundGray,
      body: SafeArea(
        child: Column(
          children: [
            // Search & Privacy Banner
            Container(
              color: NetraColors.surfaceWhite,
              child: ResponsiveContainer.wide(
                padding:
                    const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
                child: Column(
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: TextField(
                            controller: _searchController,
                            style: NetraTypography.bodyLarge,
                            onSubmitted: (_) => _search(),
                            decoration: InputDecoration(
                              hintText: "Enter city or region to search",
                              prefixIcon: const Icon(Icons.search_rounded,
                                  color: NetraColors.textSecondary),
                              suffixIcon: _searchController.text.isNotEmpty
                                  ? IconButton(
                                      icon: const Icon(Icons.clear_rounded,
                                          size: 20),
                                      onPressed: () {
                                        _searchController.clear();
                                        _search();
                                      },
                                    )
                                  : null,
                            ),
                          ),
                        ),
                        NetraSpacing.gapW8,
                        IconButton(
                          icon: const Icon(Icons.my_location_rounded,
                              color: NetraColors.primaryRed),
                          tooltip: "Use approximate device location",
                          onPressed: () async {
                            final loc = await _locationService
                                .getCurrentLocation(approximateOnly: true);
                            if (mounted && loc != null) {
                              _currentLat = loc.latitude;
                              _currentLon = loc.longitude;
                              _searchController.text = loc.displayName;
                              _fetchDefaultOrNearby();
                              ScaffoldMessenger.of(context).showSnackBar(
                                SnackBar(
                                  content: Text(
                                      "Approximate location used: ${loc.displayName}. Exact coordinates are not recorded."),
                                ),
                              );
                            }
                          },
                        ),
                      ],
                    ),

                    NetraSpacing.gapH8,

                    // Blood Group Filter Chips
                    SingleChildScrollView(
                      scrollDirection: Axis.horizontal,
                      child: Row(
                        children: [
                          ChoiceChip(
                            label: const Text('All Blood Groups'),
                            selected: _selectedBloodGroup == null,
                            onSelected: (selected) {
                              if (selected) {
                                setState(() => _selectedBloodGroup = null);
                                _search();
                              }
                            },
                          ),
                          NetraSpacing.gapW8,
                          ..._bloodGroups.map((bg) {
                            return Padding(
                              padding: const EdgeInsets.only(right: 8.0),
                              child: ChoiceChip(
                                label: Text(bg),
                                selected: _selectedBloodGroup == bg,
                                onSelected: (selected) {
                                  setState(() {
                                    _selectedBloodGroup = selected ? bg : null;
                                  });
                                  _search();
                                },
                              ),
                            );
                          }),
                        ],
                      ),
                    ),

                    NetraSpacing.gapH8,

                    // Location Privacy Notice
                    Row(
                      children: [
                        const Icon(Icons.shield_outlined,
                            size: 14, color: NetraColors.textMuted),
                        NetraSpacing.gapW8,
                        Expanded(
                          child: Text(
                            "NETRA uses approximate location for discovery. Exact location is never permanently recorded.",
                            style: NetraTypography.bodySmall
                                .copyWith(color: NetraColors.textMuted),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
            const Divider(height: 1, color: NetraColors.borderGray),

            // Content Area
            Expanded(
              child: _isLoading
                  ? const Center(
                      child: CircularProgressIndicator(
                          color: NetraColors.primaryRed))
                  : _errorMessage != null
                      ? Center(
                          child: Padding(
                            padding: const EdgeInsets.all(24.0),
                            child: Column(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                const Icon(Icons.error_outline_rounded,
                                    size: 48, color: NetraColors.ineligibleRed),
                                NetraSpacing.gapH12,
                                Text(_errorMessage!,
                                    style: NetraTypography.bodyLarge,
                                    textAlign: TextAlign.center),
                                NetraSpacing.gapH16,
                                NetraButton.primary(
                                  text: "Retry",
                                  onPressed: _search,
                                ),
                              ],
                            ),
                          ),
                        )
                      : _bloodBanks.isEmpty
                          ? Center(
                              child: Padding(
                                padding: const EdgeInsets.all(24.0),
                                child: Column(
                                  mainAxisAlignment: MainAxisAlignment.center,
                                  children: [
                                    const Icon(Icons.search_off_rounded,
                                        size: 48, color: NetraColors.textMuted),
                                    NetraSpacing.gapH12,
                                    Text(
                                      "No authorized blood centres found matching your query.",
                                      style: NetraTypography.titleMedium
                                          .copyWith(
                                              color: NetraColors.textSecondary),
                                      textAlign: TextAlign.center,
                                    ),
                                    NetraSpacing.gapH8,
                                    Text(
                                      "Try expanding your search radius or selecting 'All Blood Groups'.",
                                      style: NetraTypography.bodySmall.copyWith(
                                          color: NetraColors.textMuted),
                                      textAlign: TextAlign.center,
                                    ),
                                  ],
                                ),
                              ),
                            )
                          : ResponsiveContainer.wide(
                              padding: const EdgeInsets.symmetric(
                                  horizontal: 16, vertical: 12),
                              child: isDesktopOrTablet
                                  ? GridView.builder(
                                      gridDelegate:
                                          const SliverGridDelegateWithFixedCrossAxisCount(
                                        crossAxisCount: 2,
                                        crossAxisSpacing: 16,
                                        mainAxisSpacing: 16,
                                        mainAxisExtent: 148,
                                      ),
                                      itemCount: _bloodBanks.length,
                                      itemBuilder: (context, index) {
                                        final bank = _bloodBanks[index];
                                        return BloodBankCard(
                                          bank: bank,
                                          onTap: () => _navigateToDetails(bank),
                                        );
                                      },
                                    )
                                  : ListView.separated(
                                      itemCount: _bloodBanks.length,
                                      separatorBuilder: (context, index) =>
                                          NetraSpacing.gapH12,
                                      itemBuilder: (context, index) {
                                        final bank = _bloodBanks[index];
                                        return BloodBankCard(
                                          bank: bank,
                                          onTap: () => _navigateToDetails(bank),
                                        );
                                      },
                                    ),
                            ),
            ),
          ],
        ),
      ),
    );
  }

  void _navigateToDetails(BloodBankSummary bank) {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (context) => BloodBankDetailsScreen(
          bloodBankId: bank.id,
          userLat: _currentLat,
          userLon: _currentLon,
        ),
      ),
    );
  }
}
