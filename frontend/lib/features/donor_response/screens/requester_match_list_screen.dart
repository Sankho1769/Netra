import 'package:flutter/material.dart';
import '../models/donor_match_response_model.dart';
import '../services/donor_response_api_service.dart';
import '../widgets/match_status_badge.dart';
import 'requester_match_detail_screen.dart';

/// Screen for requesters and administrators to view persistent donor match records for a blood request.
class RequesterMatchListScreen extends StatefulWidget {
  final String requestId;
  final String? bloodGroup;
  final DonorResponseApiService? apiService;

  const RequesterMatchListScreen({
    super.key,
    required this.requestId,
    this.bloodGroup,
    this.apiService,
  });

  @override
  State<RequesterMatchListScreen> createState() => _RequesterMatchListScreenState();
}

enum MatchFilterType {
  all,
  pending,
  accepted,
  declined,
  expiredOrCancelled,
}

class _RequesterMatchListScreenState extends State<RequesterMatchListScreen> {
  late final DonorResponseApiService _apiService;

  bool _isLoading = true;
  String? _errorMessage;
  List<RequesterDonorMatch> _matches = [];
  MatchFilterType _selectedFilter = MatchFilterType.all;

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
      final matches = await _apiService.getMatchResponses(widget.requestId);
      if (mounted) {
        setState(() {
          _matches = matches;
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

  List<RequesterDonorMatch> get _filteredMatches {
    switch (_selectedFilter) {
      case MatchFilterType.pending:
        return _matches.where((m) => m.responseStatus == DonorMatchStatus.matched).toList();
      case MatchFilterType.accepted:
        return _matches.where((m) => m.responseStatus == DonorMatchStatus.accepted).toList();
      case MatchFilterType.declined:
        return _matches.where((m) => m.responseStatus == DonorMatchStatus.declined).toList();
      case MatchFilterType.expiredOrCancelled:
        return _matches.where((m) => m.responseStatus == DonorMatchStatus.expired || m.responseStatus == DonorMatchStatus.cancelled).toList();
      case MatchFilterType.all:
      default:
        return _matches;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Matched Donor Responses'),
        backgroundColor: const Color(0xFFDC2626),
        foregroundColor: Colors.white,
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            tooltip: 'Refresh',
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
              _buildPrivacyBanner(),
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
                        Text('Loading match responses...'),
                      ],
                    ),
                  ),
                )
              else if (_errorMessage != null)
                _buildErrorState()
              else if (_filteredMatches.isEmpty)
                _buildEmptyState()
              else
                _buildMatchList(),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildPrivacyBanner() {
    return Container(
      margin: const EdgeInsets.all(16),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: const Color(0xFFEFF6FF),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: const Color(0xFF93C5FD), width: 1),
      ),
      child: const Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.privacy_tip_outlined, color: Color(0xFF1D4ED8), size: 20),
          SizedBox(width: 10),
          Expanded(
            child: Text(
              'Donor Safeguards Active: Personal contact information (phone, email, exact location) and medical questionnaire answers are strictly withheld.',
              style: TextStyle(
                fontSize: 12.5,
                color: Color(0xFF1E3A8A),
                height: 1.35,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildFilterChips() {
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      child: Row(
        children: [
          FilterChip(
            label: Text('All (${_matches.length})'),
            selected: _selectedFilter == MatchFilterType.all,
            onSelected: (selected) {
              if (selected) setState(() => _selectedFilter = MatchFilterType.all);
            },
          ),
          const SizedBox(width: 8),
          FilterChip(
            label: Text(
              'Pending (${_matches.where((m) => m.responseStatus == DonorMatchStatus.matched).length})',
            ),
            selected: _selectedFilter == MatchFilterType.pending,
            onSelected: (selected) {
              setState(() => _selectedFilter = selected ? MatchFilterType.pending : MatchFilterType.all);
            },
          ),
          const SizedBox(width: 8),
          FilterChip(
            label: Text(
              'Accepted (${_matches.where((m) => m.responseStatus == DonorMatchStatus.accepted).length})',
            ),
            selected: _selectedFilter == MatchFilterType.accepted,
            onSelected: (selected) {
              setState(() => _selectedFilter = selected ? MatchFilterType.accepted : MatchFilterType.all);
            },
          ),
          const SizedBox(width: 8),
          FilterChip(
            label: Text(
              'Declined (${_matches.where((m) => m.responseStatus == DonorMatchStatus.declined).length})',
            ),
            selected: _selectedFilter == MatchFilterType.declined,
            onSelected: (selected) {
              setState(() => _selectedFilter = selected ? MatchFilterType.declined : MatchFilterType.all);
            },
          ),
          const SizedBox(width: 8),
          FilterChip(
            label: Text(
              'Expired / Cancelled (${_matches.where((m) => m.responseStatus == DonorMatchStatus.expired || m.responseStatus == DonorMatchStatus.cancelled).length})',
            ),
            selected: _selectedFilter == MatchFilterType.expiredOrCancelled,
            onSelected: (selected) {
              setState(() => _selectedFilter = selected ? MatchFilterType.expiredOrCancelled : MatchFilterType.all);
            },
          ),
        ],
      ),
    );
  }

  Widget _buildMatchList() {
    return ListView.builder(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      itemCount: _filteredMatches.length,
      itemBuilder: (context, index) {
        final match = _filteredMatches[index];
        return Card(
          elevation: 2,
          margin: const EdgeInsets.symmetric(vertical: 6),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
          child: InkWell(
            borderRadius: BorderRadius.circular(10),
            onTap: () {
              Navigator.of(context).push(
                MaterialPageRoute(
                  builder: (context) => RequesterMatchDetailScreen(match: match),
                ),
              );
            },
            child: Padding(
              padding: const EdgeInsets.all(14),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(
                        match.donorDisplayName,
                        style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15),
                      ),
                      MatchStatusBadge(status: match.responseStatus),
                    ],
                  ),
                  const SizedBox(height: 10),
                  Row(
                    children: [
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                        decoration: BoxDecoration(
                          color: const Color(0xFFFEE2E2),
                          borderRadius: BorderRadius.circular(6),
                        ),
                        child: Text(
                          match.bloodGroup,
                          style: const TextStyle(
                            color: Color(0xFFDC2626),
                            fontWeight: FontWeight.bold,
                            fontSize: 13,
                          ),
                        ),
                      ),
                      if (match.isVerified) ...[
                        const SizedBox(width: 8),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                          decoration: BoxDecoration(
                            color: const Color(0xFFDCFCE7),
                            borderRadius: BorderRadius.circular(6),
                          ),
                          child: const Text(
                            'Verified at matching',
                            style: TextStyle(
                              color: Color(0xFF15803D),
                              fontWeight: FontWeight.w600,
                              fontSize: 11.5,
                            ),
                          ),
                        ),
                      ],
                      const Spacer(),
                      if (match.distanceKm != null)
                        Text(
                          '${match.distanceKm!.toStringAsFixed(1)} km away',
                          style: TextStyle(fontSize: 12.5, color: Colors.grey.shade600),
                        ),
                    ],
                  ),
                  if (match.respondedAt != null) ...[
                    const SizedBox(height: 8),
                    Text(
                      'Responded: ${match.respondedAt!.toLocal().toString().substring(0, 16)}',
                      style: TextStyle(fontSize: 11.5, color: Colors.grey.shade500),
                    ),
                  ],
                ],
              ),
            ),
          ),
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
            Icon(Icons.person_search_outlined, size: 64, color: Colors.grey.shade400),
            const SizedBox(height: 16),
            Text(
              _selectedFilter == MatchFilterType.all
                  ? 'No Match Records Yet'
                  : 'No matches found with this status filter',
              style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 8),
            Text(
              _selectedFilter == MatchFilterType.all
                  ? 'Candidate donors have not yet been assigned to this blood request.'
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
              'Failed to load match responses',
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
