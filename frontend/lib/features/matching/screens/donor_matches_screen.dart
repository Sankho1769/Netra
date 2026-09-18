import 'package:flutter/material.dart';
import '../models/donor_match_model.dart';
import '../services/matching_api_service.dart';
import '../widgets/donor_match_card.dart';

/// Screen displaying ranked compatible donor candidates for an open blood request.
///
/// Features:
/// - Authoritative medical disclaimer banner
/// - Dynamic radius filtering (10km, 25km, 50km, 100km)
/// - Verified donor candidates only (unverified/self-reported donors strictly excluded)
/// - Ranked candidate cards with verification & distance chips
class DonorMatchesScreen extends StatefulWidget {
  final String requestId;
  final String? targetBloodGroup;
  final String? hospitalName;
  final MatchingApiService? matchingApiService;

  const DonorMatchesScreen({
    super.key,
    required this.requestId,
    this.targetBloodGroup,
    this.hospitalName,
    this.matchingApiService,
  });

  @override
  State<DonorMatchesScreen> createState() => _DonorMatchesScreenState();
}

class _DonorMatchesScreenState extends State<DonorMatchesScreen> {
  late final MatchingApiService _apiService;

  bool _isLoading = true;
  String? _errorMessage;
  DonorMatchResponse? _response;

  double _selectedRadiusKm = 25.0;

  final List<double> _radiusOptions = [10.0, 25.0, 50.0, 100.0];

  @override
  void initState() {
    super.initState();
    _apiService = widget.matchingApiService ?? MatchingApiService();
    _fetchMatches();
  }

  Future<void> _fetchMatches() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final res = await _apiService.fetchMatches(
        widget.requestId,
        radiusKm: _selectedRadiusKm,
      );

      if (mounted) {
        setState(() {
          _response = res;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _errorMessage = e.toString().replaceFirst('ValidationException: ', '');
          _isLoading = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Matching Donors'),
        backgroundColor: const Color(0xFFDC2626),
        foregroundColor: Colors.white,
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            tooltip: 'Refresh Matches',
            onPressed: _isLoading ? null : _fetchMatches,
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: _fetchMatches,
        child: SingleChildScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // Medical Disclaimer Alert Box
              _buildDisclaimerBanner(),

              // Interactive Filters Header
              _buildFilterSection(),

              // Main Content Area: Loading / Error / Empty / List
              if (_isLoading)
                const Padding(
                  padding: EdgeInsets.symmetric(vertical: 64),
                  child: Center(
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        CircularProgressIndicator(color: Color(0xFFDC2626)),
                        SizedBox(height: 16),
                        Text('Finding compatible donors near request...'),
                      ],
                    ),
                  ),
                )
              else if (_errorMessage != null)
                _buildErrorState()
              else if (_response == null || _response!.matches.isEmpty)
                _buildEmptyState()
              else
                _buildMatchesList(),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildDisclaimerBanner() {
    final disclaimerText = _response?.disclaimer.isNotEmpty == true
        ? _response!.disclaimer
        : 'Donor matching results represent potential, preliminary candidates based on registered availability and standard ABO/Rh blood compatibility rules. A match does NOT constitute medical clearance or guaranteed donation eligibility. Final medical evaluation must be performed by authorized clinical staff prior to collection.';

    return Container(
      margin: const EdgeInsets.all(16),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: const Color(0xFFFEF3C7), // Warm amber/yellow
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: const Color(0xFFF59E0B), width: 1),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Icon(
            Icons.warning_amber_rounded,
            color: Color(0xFFB45309),
            size: 24,
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Medical Suitability Disclaimer',
                  style: TextStyle(
                    fontWeight: FontWeight.bold,
                    color: Color(0xFF92400E),
                    fontSize: 13,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  disclaimerText,
                  style: const TextStyle(
                    color: Color(0xFF78350F),
                    fontSize: 12,
                    height: 1.35,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildFilterSection() {
    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  'Search Radius: ${_selectedRadiusKm.toInt()} km',
                  style: const TextStyle(
                    fontWeight: FontWeight.w600,
                    fontSize: 14,
                  ),
                ),
                if (_response != null)
                  Text(
                    '${_response!.candidateCount} found',
                    style: const TextStyle(
                      color: Color(0xFFDC2626),
                      fontWeight: FontWeight.bold,
                      fontSize: 13,
                    ),
                  ),
              ],
            ),
            const SizedBox(height: 10),
            // Radius Buttons
            Row(
              children: _radiusOptions.map((radius) {
                final isSelected = _selectedRadiusKm == radius;
                return Expanded(
                  child: Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 3),
                    child: OutlinedButton(
                      style: OutlinedButton.styleFrom(
                        padding: const EdgeInsets.symmetric(vertical: 8),
                        backgroundColor: isSelected ? const Color(0xFFDC2626) : Colors.white,
                        side: BorderSide(
                          color: isSelected ? const Color(0xFFDC2626) : Colors.grey.shade300,
                        ),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                      ),
                      onPressed: () {
                        if (_selectedRadiusKm != radius) {
                          setState(() {
                            _selectedRadiusKm = radius;
                          });
                          _fetchMatches();
                        }
                      },
                      child: Text(
                        '${radius.toInt()} km',
                        style: TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.w600,
                          color: isSelected ? Colors.white : Colors.black87,
                        ),
                      ),
                    ),
                  ),
                );
              }).toList(),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildMatchesList() {
    final matches = _response!.matches;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(20, 16, 20, 8),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                'Compatible Candidates (${matches.length})',
                style: const TextStyle(
                  fontWeight: FontWeight.bold,
                  fontSize: 15,
                  color: Color(0xFF1F2937),
                ),
              ),
              Text(
                'Req: ${_response!.bloodGroupRequired}',
                style: const TextStyle(
                  color: Color(0xFFDC2626),
                  fontWeight: FontWeight.bold,
                  fontSize: 13,
                ),
              ),
            ],
          ),
        ),
        ListView.builder(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          itemCount: matches.length,
          itemBuilder: (ctx, index) {
            return DonorMatchCard(
              match: matches[index],
              rank: index + 1,
            );
          },
        ),
        const SizedBox(height: 32),
      ],
    );
  }

  Widget _buildEmptyState() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 48),
      child: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                color: Colors.grey.shade100,
                shape: BoxShape.circle,
              ),
              child: const Icon(
                Icons.person_search_outlined,
                size: 48,
                color: Colors.grey,
              ),
            ),
            const SizedBox(height: 16),
            const Text(
              'No Compatible Donors Found',
              style: TextStyle(
                fontSize: 17,
                fontWeight: FontWeight.bold,
                color: Color(0xFF1F2937),
              ),
            ),
            const SizedBox(height: 8),
            Text(
              'No registered and available donors matched within ${_selectedRadiusKm.toInt()} km.\nTry increasing the search radius to 50 km or 100 km.',
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontSize: 13,
                color: Color(0xFF6B7280),
                height: 1.4,
              ),
            ),
            const SizedBox(height: 20),
            if (_selectedRadiusKm < 100.0)
              ElevatedButton.icon(
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFFDC2626),
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                ),
                icon: const Icon(Icons.zoom_out_map, size: 18),
                label: const Text('Expand to 100 km'),
                onPressed: () {
                  setState(() {
                    _selectedRadiusKm = 100.0;
                  });
                  _fetchMatches();
                },
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildErrorState() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 48),
      child: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(
              Icons.error_outline,
              size: 48,
              color: Colors.redAccent,
            ),
            const SizedBox(height: 16),
            const Text(
              'Unable to Load Matches',
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              _errorMessage ?? 'An unexpected error occurred while querying the donor matching engine.',
              textAlign: TextAlign.center,
              style: const TextStyle(fontSize: 13, color: Colors.grey),
            ),
            const SizedBox(height: 20),
            ElevatedButton(
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFFDC2626),
                foregroundColor: Colors.white,
              ),
              onPressed: _fetchMatches,
              child: const Text('Try Again'),
            ),
          ],
        ),
      ),
    );
  }
}
