import 'package:flutter/material.dart';
import '../models/donor_match_response_model.dart';
import '../services/donor_response_api_service.dart';
import '../widgets/donor_response_card.dart';
import 'donor_match_detail_screen.dart';

/// Filter criteria for incoming donor matches.
enum DonorMatchFilterType {
  all,
  pending,
  accepted,
  declined,
  expiredOrCancelled;

  String get displayName {
    switch (this) {
      case DonorMatchFilterType.all:
        return 'All';
      case DonorMatchFilterType.pending:
        return 'Pending';
      case DonorMatchFilterType.accepted:
        return 'Accepted';
      case DonorMatchFilterType.declined:
        return 'Declined';
      case DonorMatchFilterType.expiredOrCancelled:
        return 'Expired / Cancelled';
    }
  }
}

/// Screen displaying persistent blood request matches assigned to the authenticated donor.
class DonorIncomingMatchesScreen extends StatefulWidget {
  final DonorResponseApiService? apiService;

  const DonorIncomingMatchesScreen({super.key, this.apiService});

  @override
  State<DonorIncomingMatchesScreen> createState() =>
      _DonorIncomingMatchesScreenState();
}

/// Backwards compatibility alias
typedef DonorMatchesScreen = DonorIncomingMatchesScreen;

class _DonorIncomingMatchesScreenState
    extends State<DonorIncomingMatchesScreen> {
  late final DonorResponseApiService _apiService;

  bool _isLoading = true;
  String? _errorMessage;
  List<DonorMatchDetail> _matches = [];
  DonorMatchFilterType _selectedFilter = DonorMatchFilterType.all;

  @override
  void initState() {
    super.initState();
    _apiService = widget.apiService ?? DonorResponseApiService();
    _fetchMatches();
  }

  Future<void> _fetchMatches() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final matches = await _apiService.getMyMatches();
      if (mounted) {
        setState(() {
          _matches = matches;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _errorMessage =
              e.toString().replaceFirst('ValidationException: ', '');
          _isLoading = false;
        });
      }
    }
  }

  List<DonorMatchDetail> get _filteredMatches {
    switch (_selectedFilter) {
      case DonorMatchFilterType.pending:
        return _matches
            .where((m) => m.responseStatus == DonorMatchStatus.matched)
            .toList();
      case DonorMatchFilterType.accepted:
        return _matches
            .where((m) => m.responseStatus == DonorMatchStatus.accepted)
            .toList();
      case DonorMatchFilterType.declined:
        return _matches
            .where((m) => m.responseStatus == DonorMatchStatus.declined)
            .toList();
      case DonorMatchFilterType.expiredOrCancelled:
        return _matches
            .where((m) =>
                m.responseStatus == DonorMatchStatus.expired ||
                m.responseStatus == DonorMatchStatus.cancelled)
            .toList();
      case DonorMatchFilterType.all:
        return _matches;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('My Matched Requests'),
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
              _buildDisclaimerBanner(),
              _buildFilterChips(),
              if (_isLoading)
                const Padding(
                  padding: EdgeInsets.symmetric(vertical: 64),
                  child: Center(
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        CircularProgressIndicator(color: Color(0xFFDC2626)),
                        SizedBox(height: 16),
                        Text('Loading match assignments...'),
                      ],
                    ),
                  ),
                )
              else if (_errorMessage != null)
                _buildErrorState()
              else if (_filteredMatches.isEmpty)
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
    return Container(
      margin: const EdgeInsets.all(16),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: const Color(0xFFFEF3C7),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: const Color(0xFFF59E0B), width: 1),
      ),
      child: const Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.info_outline, color: Color(0xFFB45309), size: 20),
          SizedBox(width: 10),
          Expanded(
            child: Text(
              'Accepting a match does not confirm medical eligibility or donation. Final screening is performed by qualified blood-bank staff.',
              style: TextStyle(
                fontSize: 12.5,
                color: Color(0xFF78350F),
                height: 1.35,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildFilterChips() {
    final pendingCount = _matches
        .where((m) => m.responseStatus == DonorMatchStatus.matched)
        .length;
    final acceptedCount = _matches
        .where((m) => m.responseStatus == DonorMatchStatus.accepted)
        .length;
    final declinedCount = _matches
        .where((m) => m.responseStatus == DonorMatchStatus.declined)
        .length;
    final expiredOrCancelledCount = _matches
        .where((m) =>
            m.responseStatus == DonorMatchStatus.expired ||
            m.responseStatus == DonorMatchStatus.cancelled)
        .length;

    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      child: Row(
        children: [
          FilterChip(
            label: Text('All (${_matches.length})'),
            selected: _selectedFilter == DonorMatchFilterType.all,
            onSelected: (selected) {
              if (selected)
                setState(() => _selectedFilter = DonorMatchFilterType.all);
            },
          ),
          const SizedBox(width: 8),
          FilterChip(
            label: Text('Pending ($pendingCount)'),
            selected: _selectedFilter == DonorMatchFilterType.pending,
            onSelected: (selected) {
              setState(() => _selectedFilter = selected
                  ? DonorMatchFilterType.pending
                  : DonorMatchFilterType.all);
            },
          ),
          const SizedBox(width: 8),
          FilterChip(
            label: Text('Accepted ($acceptedCount)'),
            selected: _selectedFilter == DonorMatchFilterType.accepted,
            onSelected: (selected) {
              setState(() => _selectedFilter = selected
                  ? DonorMatchFilterType.accepted
                  : DonorMatchFilterType.all);
            },
          ),
          const SizedBox(width: 8),
          FilterChip(
            label: Text('Declined ($declinedCount)'),
            selected: _selectedFilter == DonorMatchFilterType.declined,
            onSelected: (selected) {
              setState(() => _selectedFilter = selected
                  ? DonorMatchFilterType.declined
                  : DonorMatchFilterType.all);
            },
          ),
          const SizedBox(width: 8),
          FilterChip(
            label: Text('Expired / Cancelled ($expiredOrCancelledCount)'),
            selected:
                _selectedFilter == DonorMatchFilterType.expiredOrCancelled,
            onSelected: (selected) {
              setState(() => _selectedFilter = selected
                  ? DonorMatchFilterType.expiredOrCancelled
                  : DonorMatchFilterType.all);
            },
          ),
        ],
      ),
    );
  }

  Widget _buildMatchesList() {
    return ListView.builder(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      itemCount: _filteredMatches.length,
      itemBuilder: (context, index) {
        final match = _filteredMatches[index];
        return DonorResponseCard(
          match: match,
          onTap: () async {
            final updated = await Navigator.of(context).push<bool>(
              MaterialPageRoute(
                builder: (context) => DonorMatchDetailScreen(
                  matchId: match.matchId,
                  apiService: _apiService,
                ),
              ),
            );
            if (updated == true) {
              _fetchMatches();
            }
          },
        );
      },
    );
  }

  Widget _buildEmptyState() {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 64, horizontal: 24),
      child: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.inbox_outlined, size: 64, color: Colors.grey.shade400),
            const SizedBox(height: 16),
            Text(
              _selectedFilter == DonorMatchFilterType.all
                  ? 'No Match Assignments Found'
                  : 'No matches found with this status filter',
              style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 8),
            Text(
              _selectedFilter == DonorMatchFilterType.all
                  ? 'You currently have no matched blood requests requiring your response.'
                  : 'Try selecting a different filter above.',
              style: TextStyle(color: Colors.grey.shade600),
              textAlign: TextAlign.center,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildErrorState() {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 48, horizontal: 24),
      child: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.error_outline, size: 56, color: Color(0xFFDC2626)),
            const SizedBox(height: 16),
            const Text(
              'Failed to load match assignments',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            Text(
              _errorMessage ?? 'Unknown error occurred.',
              style: TextStyle(color: Colors.grey.shade600),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 20),
            ElevatedButton.icon(
              onPressed: _fetchMatches,
              icon: const Icon(Icons.refresh),
              label: const Text('Try Again'),
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFFDC2626),
                foregroundColor: Colors.white,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
