import 'package:flutter/material.dart';
import '../../../core/responsive/responsive_container.dart';
import '../../../core/theme/netra_colors.dart';
import '../../../core/theme/netra_spacing.dart';
import '../../../core/theme/netra_typography.dart';
import '../../eligibility/screens/eligibility_intro_screen.dart';
import '../models/community_impact_model.dart';
import '../models/community_timeseries_model.dart';
import '../services/community_metrics_api_service.dart';

class AboutScreen extends StatefulWidget {
  final bool isEmbedded;
  final CommunityMetricsApiService? metricsApiService;

  const AboutScreen({
    super.key,
    this.isEmbedded = false,
    this.metricsApiService,
  });

  @override
  State<AboutScreen> createState() => _AboutScreenState();
}

class _AboutScreenState extends State<AboutScreen> {
  late final CommunityMetricsApiService _metricsApiService;
  String _selectedBloodGroup = 'O+';
  CommunityImpactModel? _communityImpact;
  bool _isLoadingImpact = true;
  CommunityTimeSeriesModel? _timeSeries;
  bool _isLoadingTimeSeries = true;
  int _selectedDays = 30;

  @override
  void initState() {
    super.initState();
    _metricsApiService =
        widget.metricsApiService ?? CommunityMetricsApiService();
    _loadMetrics();
  }

  Future<void> _loadMetrics({bool isRefresh = false}) async {
    if (!isRefresh) {
      setState(() {
        _isLoadingImpact = true;
        _isLoadingTimeSeries = true;
      });
    }
    try {
      final futures = await Future.wait([
        _metricsApiService.getCommunityImpact(),
        _metricsApiService.getCommunityTimeSeries(days: _selectedDays),
      ]);
      if (mounted) {
        setState(() {
          _communityImpact = futures[0] as CommunityImpactModel;
          _timeSeries = futures[1] as CommunityTimeSeriesModel;
          _isLoadingImpact = false;
          _isLoadingTimeSeries = false;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _isLoadingImpact = false;
          _isLoadingTimeSeries = false;
        });
      }
    }
  }

  Future<void> _changeDays(int days) async {
    if (_selectedDays == days) return;
    setState(() {
      _selectedDays = days;
      _isLoadingTimeSeries = true;
    });
    try {
      final ts = await _metricsApiService.getCommunityTimeSeries(days: days);
      if (mounted) {
        setState(() {
          _timeSeries = ts;
          _isLoadingTimeSeries = false;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _isLoadingTimeSeries = false;
        });
      }
    }
  }

  static const Map<String, Map<String, dynamic>> _compatibilityData = {
    'O-': {
      'giveTo': ['O-', 'O+', 'A-', 'A+', 'B-', 'B+', 'AB-', 'AB+'],
      'receiveFrom': ['O-'],
      'badge': 'Universal Red Cell Donor',
      'note': 'Can donate red blood cells to any patient in emergency situations.',
    },
    'O+': {
      'giveTo': ['O+', 'A+', 'B+', 'AB+'],
      'receiveFrom': ['O+', 'O-'],
      'badge': 'Most Common Blood Type',
      'note': 'Can donate to any positive blood group; high clinical demand.',
    },
    'A-': {
      'giveTo': ['A-', 'A+', 'AB-', 'AB+'],
      'receiveFrom': ['A-', 'O-'],
      'badge': 'Rare & Critical',
      'note': 'Can safely donate to both A and AB patients of any Rh factor.',
    },
    'A+': {
      'giveTo': ['A+', 'AB+'],
      'receiveFrom': ['A+', 'A-', 'O+', 'O-'],
      'badge': 'Widely Transfused',
      'note': 'Second most requested blood group in hospitals.',
    },
    'B-': {
      'giveTo': ['B-', 'B+', 'AB-', 'AB+'],
      'receiveFrom': ['B-', 'O-'],
      'badge': 'Rare Rh-Negative',
      'note': 'Crucial for rare patient matches requiring B negative blood.',
    },
    'B+': {
      'giveTo': ['B+', 'AB+'],
      'receiveFrom': ['B+', 'B-', 'O+', 'O-'],
      'badge': 'High Community Need',
      'note': 'Highly prevalent in the Indian subcontinent with steady hospital demand.',
    },
    'AB-': {
      'giveTo': ['AB-', 'AB+'],
      'receiveFrom': ['AB-', 'A-', 'B-', 'O-'],
      'badge': 'Rarest Blood Group',
      'note': 'Less than 1% of population; universal plasma donor.',
    },
    'AB+': {
      'giveTo': ['AB+'],
      'receiveFrom': ['O-', 'O+', 'A-', 'A+', 'B-', 'B+', 'AB-', 'AB+'],
      'badge': 'Universal Recipient',
      'note': 'Can safely receive red blood cells from any blood type.',
    },
  };

  @override
  Widget build(BuildContext context) {
    final content = RefreshIndicator(
      onRefresh: () => _loadMetrics(isRefresh: true),
      color: NetraColors.primaryRed,
      child: ResponsiveContainer.standard(
        scrollable: true,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Screen Title
            Text(
              "Guidelines & Blood Facts",
              style: NetraTypography.headlineMedium,
            ),
            NetraSpacing.gapH4,
            Text(
              "Essential clinical advice, donation readiness, and compatibility data.",
              style: NetraTypography.bodyMedium.copyWith(
                color: NetraColors.textSecondary,
              ),
            ),
            NetraSpacing.gapH20,

            // Monthly Goal Banner (reproducing blood.html)
            _buildGoalBanner(),
            NetraSpacing.gapH24,

            // Community Activity Trends & Time-Series
            _buildTimeSeriesSection(),
            NetraSpacing.gapH24,

            // Guidelines: What you MUST do
            _buildDoCard(),
            NetraSpacing.gapH16,

            // Guidelines: What you MUST AVOID
            _buildAvoidCard(),
            NetraSpacing.gapH24,

            // Blood Compatibility Matrix
            _buildCompatibilitySection(),
            NetraSpacing.gapH24,

            // NBTC Eligibility Standards
            _buildStandardsCard(),
            NetraSpacing.gapH24,
          ],
        ),
      ),
    );

    if (widget.isEmbedded) {
      return content;
    }

    return Scaffold(
      appBar: AppBar(
        title: const Text("Donor Guidelines"),
        backgroundColor: NetraColors.surfaceWhite,
        foregroundColor: NetraColors.textPrimary,
        elevation: 0,
      ),
      backgroundColor: NetraColors.backgroundGray,
      body: content,
    );
  }

  Widget _buildGoalBanner() {
    if (_isLoadingImpact) {
      return Container(
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: NetraColors.surfaceWhite,
          borderRadius: BorderRadius.circular(20),
          border: Border.all(color: const Color(0xFFF1F5F9)),
        ),
        child: const Center(
          child: Padding(
            padding: EdgeInsets.symmetric(vertical: 12),
            child: SizedBox(
              width: 24,
              height: 24,
              child: CircularProgressIndicator(
                strokeWidth: 2,
                color: NetraColors.primaryRed,
              ),
            ),
          ),
        ),
      );
    }

    final hasData = _communityImpact != null &&
        _communityImpact!.hasData &&
        _communityImpact!.totalUnitsCollected > 0;

    if (!hasData) {
      return Container(
        width: double.infinity,
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: NetraColors.surfaceWhite,
          borderRadius: BorderRadius.circular(20),
          border: Border.all(color: const Color(0xFFF1F5F9)),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withValues(alpha: 0.04),
              blurRadius: 10,
              offset: const Offset(0, 4),
            ),
          ],
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(
                  Icons.volunteer_activism_outlined,
                  size: 20,
                  color: NetraColors.primaryRed,
                ),
                NetraSpacing.gapW8,
                Text(
                  "COMMUNITY IMPACT",
                  style: TextStyle(
                    fontSize: 11,
                    fontWeight: FontWeight.w800,
                    color: Colors.grey.shade600,
                    letterSpacing: 1.1,
                  ),
                ),
              ],
            ),
            NetraSpacing.gapH12,
            Text(
              _communityImpact?.notice ??
                  "Community impact data will appear here once verified donations are recorded.",
              style: NetraTypography.bodyMedium.copyWith(
                color: NetraColors.textSecondary,
                height: 1.4,
              ),
            ),
          ],
        ),
      );
    }

    final collected = _communityImpact!.totalUnitsCollected;
    final donations = _communityImpact!.totalVerifiedDonations;
    final donors = _communityImpact!.activeDonorsCount;
    final fulfilled = _communityImpact!.fulfilledRequestsCount;

    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: NetraColors.surfaceWhite,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: const Color(0xFFF1F5F9)),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.04),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    "COMMUNITY VERIFIED IMPACT",
                    style: TextStyle(
                      fontSize: 10,
                      fontWeight: FontWeight.w800,
                      color: Colors.grey.shade500,
                      letterSpacing: 1.2,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    "$donations Donations",
                    style: const TextStyle(
                      fontSize: 20,
                      fontWeight: FontWeight.w900,
                      color: NetraColors.textPrimary,
                    ),
                  ),
                ],
              ),
              Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Text(
                    "$collected",
                    style: const TextStyle(
                      fontSize: 26,
                      fontWeight: FontWeight.w900,
                      color: NetraColors.primaryRed,
                    ),
                  ),
                  Text(
                    "UNITS COLLECTED",
                    style: TextStyle(
                      fontSize: 10,
                      fontWeight: FontWeight.w800,
                      color: Colors.green.shade600,
                      letterSpacing: 0.8,
                    ),
                  ),
                ],
              ),
            ],
          ),
          const SizedBox(height: 14),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                "$donors Active Donors",
                style: TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.w600,
                  color: Colors.grey.shade600,
                ),
              ),
              Text(
                "$fulfilled Requests Fulfilled",
                style: const TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.w700,
                  color: NetraColors.primaryRed,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildTimeSeriesSection() {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: NetraColors.surfaceWhite,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: const Color(0xFFF1F5F9)),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.04),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Row(
                children: [
                  const Icon(
                    Icons.trending_up_rounded,
                    size: 20,
                    color: NetraColors.primaryRed,
                  ),
                  NetraSpacing.gapW8,
                  Text(
                    "ACTIVITY TRENDS",
                    style: TextStyle(
                      fontSize: 11,
                      fontWeight: FontWeight.w800,
                      color: Colors.grey.shade600,
                      letterSpacing: 1.1,
                    ),
                  ),
                ],
              ),
              Row(
                children: [7, 30, 90].map((d) {
                  final isSel = _selectedDays == d;
                  return Padding(
                    padding: const EdgeInsets.only(left: 4),
                    child: InkWell(
                      onTap: () => _changeDays(d),
                      borderRadius: BorderRadius.circular(12),
                      child: Container(
                        padding: const EdgeInsets.symmetric(
                            horizontal: 10, vertical: 4),
                        decoration: BoxDecoration(
                          color: isSel
                              ? NetraColors.primaryRed
                              : const Color(0xFFF1F5F9),
                          borderRadius: BorderRadius.circular(12),
                        ),
                        child: Text(
                          "${d}D",
                          style: TextStyle(
                            fontSize: 11,
                            fontWeight:
                                isSel ? FontWeight.w700 : FontWeight.w500,
                            color: isSel
                                ? Colors.white
                                : NetraColors.textSecondary,
                          ),
                        ),
                      ),
                    ),
                  );
                }).toList(),
              ),
            ],
          ),
          NetraSpacing.gapH16,
          if (_isLoadingTimeSeries)
            const Center(
              child: Padding(
                padding: EdgeInsets.symmetric(vertical: 20),
                child: SizedBox(
                  width: 24,
                  height: 24,
                  child: CircularProgressIndicator(
                    strokeWidth: 2,
                    color: NetraColors.primaryRed,
                  ),
                ),
              ),
            )
          else if (_timeSeries == null || !_timeSeries!.hasData)
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  "No verified community activity in the last $_selectedDays days.",
                  style: NetraTypography.bodyMedium.copyWith(
                    color: NetraColors.textSecondary,
                    height: 1.4,
                  ),
                ),
                NetraSpacing.gapH8,
                Text(
                  _timeSeries?.emptyStateMessage ??
                      "Verified blood requests, donations, and fulfillments will appear here once recorded.",
                  style: NetraTypography.bodySmall.copyWith(
                    color: NetraColors.textMuted,
                  ),
                ),
              ],
            )
          else ...[
            // Stats Row for the period
            Row(
              children: [
                Expanded(
                  child: _buildMetricTile(
                    title: "Requests",
                    value: "${_timeSeries!.totalRequestsReceived}",
                    subtitle: "${_timeSeries!.totalRequestsFulfilled} fulfilled",
                    color: const Color(0xFF2563EB),
                  ),
                ),
                NetraSpacing.gapW12,
                Expanded(
                  child: _buildMetricTile(
                    title: "Donations",
                    value: "${_timeSeries!.totalDonations}",
                    subtitle: "${_timeSeries!.totalUnitsCollected} units",
                    color: NetraColors.primaryRed,
                  ),
                ),
                NetraSpacing.gapW12,
                Expanded(
                  child: _buildMetricTile(
                    title: "Emergency",
                    value: "${_timeSeries!.totalEmergencyRequests}",
                    subtitle:
                        "${_timeSeries!.totalEmergencyFulfilled} fulfilled",
                    color: const Color(0xFFD97706),
                  ),
                ),
              ],
            ),
            if (_timeSeries!.dataPoints.any((p) =>
                p.bloodRequestsReceived > 0 ||
                p.donationsRecorded > 0 ||
                p.emergencyRequests > 0)) ...[
              NetraSpacing.gapH16,
              Text(
                "Recent Daily Distribution",
                style: NetraTypography.labelMedium.copyWith(
                  color: NetraColors.textSecondary,
                  fontWeight: FontWeight.w600,
                ),
              ),
              NetraSpacing.gapH8,
              ..._timeSeries!.dataPoints
                  .where((p) =>
                      p.bloodRequestsReceived > 0 ||
                      p.donationsRecorded > 0 ||
                      p.emergencyRequests > 0)
                  .take(5)
                  .map((p) => Padding(
                        padding: const EdgeInsets.only(bottom: 6),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Text(p.date,
                                style: NetraTypography.bodySmall.copyWith(
                                    fontWeight: FontWeight.w600)),
                            Text(
                              "Req: ${p.bloodRequestsReceived} | Don: ${p.donationsRecorded} (${p.unitsCollected}u)",
                              style: NetraTypography.bodySmall.copyWith(
                                color: NetraColors.textSecondary,
                              ),
                            ),
                          ],
                        ),
                      )),
            ],
          ],
        ],
      ),
    );
  }

  Widget _buildMetricTile({
    required String title,
    required String value,
    required String subtitle,
    required Color color,
  }) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 10),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.06),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: color.withValues(alpha: 0.15)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            title,
            style: TextStyle(
              fontSize: 10,
              fontWeight: FontWeight.w700,
              color: color,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            value,
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.w800,
              color: color,
            ),
          ),
          Text(
            subtitle,
            style: TextStyle(
              fontSize: 9,
              color: Colors.grey.shade600,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildDoCard() {
    final items = [
      "Hydrate well (drink at least 500ml water prior to donation).",
      "Eat a healthy, iron-rich meal 2-3 hours before donating.",
      "Get at least 7-8 hours of sound sleep the night before.",
      "Wear comfortable clothing with loose, rollable sleeves.",
      "Carry a valid Government Photo ID (Aadhaar / Voter ID).",
      "Rest for 15 minutes at the donation camp post-donation.",
    ];

    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: const Color(0xFFF0FDF4),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: const Color(0xFFBBF7D0)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(6),
                decoration: const BoxDecoration(
                  color: Color(0xFF16A34A),
                  shape: BoxShape.circle,
                ),
                child: const Icon(Icons.check, color: Colors.white, size: 16),
              ),
              NetraSpacing.gapW12,
              const Text(
                "What You MUST Do",
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                  color: Color(0xFF15803D),
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          ...items.map(
            (item) => Padding(
              padding: const EdgeInsets.only(bottom: 8),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Padding(
                    padding: EdgeInsets.only(top: 2, right: 8),
                    child: Icon(Icons.check_circle_outline,
                        color: Color(0xFF16A34A), size: 16),
                  ),
                  Expanded(
                    child: Text(
                      item,
                      style: const TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.w500,
                        color: Color(0xFF14532D),
                        height: 1.35,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildAvoidCard() {
    final items = [
      "Avoid smoking and alcohol consumption for 24 hours prior.",
      "Do not donate on an empty stomach or skip breakfast.",
      "Avoid heavy lifting or intense workouts for 24 hours post-donation.",
      "Do not consume caffeinated drinks right before donation.",
      "Do not donate if you had a tattoo or body piercing in the last 6 months.",
      "Do not donate if you are currently taking antibiotics.",
    ];

    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: const Color(0xFFFFF1F2),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: const Color(0xFFFECDD3)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(6),
                decoration: const BoxDecoration(
                  color: Color(0xFFE11D48),
                  shape: BoxShape.circle,
                ),
                child: const Icon(Icons.close, color: Colors.white, size: 16),
              ),
              NetraSpacing.gapW12,
              const Text(
                "What You MUST AVOID",
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                  color: Color(0xFFBE123C),
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          ...items.map(
            (item) => Padding(
              padding: const EdgeInsets.only(bottom: 8),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Padding(
                    padding: EdgeInsets.only(top: 2, right: 8),
                    child: Icon(Icons.cancel_outlined,
                        color: Color(0xFFE11D48), size: 16),
                  ),
                  Expanded(
                    child: Text(
                      item,
                      style: const TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.w500,
                        color: Color(0xFF881337),
                        height: 1.35,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildCompatibilitySection() {
    final data = _compatibilityData[_selectedBloodGroup]!;
    final giveToList = data['giveTo'] as List<String>;
    final receiveFromList = data['receiveFrom'] as List<String>;
    final badge = data['badge'] as String;
    final note = data['note'] as String;

    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: NetraColors.surfaceWhite,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: const Color(0xFFE2E8F0)),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.04),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                "Blood Compatibility Matrix",
                style: NetraTypography.titleLarge,
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                decoration: BoxDecoration(
                  color: NetraColors.backgroundRed,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Text(
                  badge,
                  style: const TextStyle(
                    fontSize: 10,
                    fontWeight: FontWeight.bold,
                    color: NetraColors.primaryRed,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 6),
          Text(
            "Select a blood group to view compatible donors and recipients:",
            style: NetraTypography.bodySmall,
          ),
          const SizedBox(height: 16),

          // Blood Group Selection Chips
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: _compatibilityData.keys.map((bg) {
              final isSelected = bg == _selectedBloodGroup;
              return ChoiceChip(
                label: Text(bg),
                selected: isSelected,
                onSelected: (_) => setState(() => _selectedBloodGroup = bg),
                selectedColor: NetraColors.primaryRed,
                backgroundColor: NetraColors.backgroundGray,
                labelStyle: TextStyle(
                  color: isSelected ? Colors.white : NetraColors.textPrimary,
                  fontWeight: FontWeight.bold,
                ),
              );
            }).toList(),
          ),
          const SizedBox(height: 16),

          // Give To Row
          _buildCompatibilityRow(
            label: "Can Give Blood To:",
            chips: giveToList,
            badgeColor: NetraColors.primaryRed,
          ),
          const SizedBox(height: 12),

          // Receive From Row
          _buildCompatibilityRow(
            label: "Can Receive Blood From:",
            chips: receiveFromList,
            badgeColor: const Color(0xFF0284C7),
          ),
          const SizedBox(height: 12),

          // Clinical Note
          Container(
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(
              color: const Color(0xFFF8FAFC),
              borderRadius: BorderRadius.circular(10),
              border: Border.all(color: const Color(0xFFE2E8F0)),
            ),
            child: Row(
              children: [
                const Icon(Icons.info_outline, size: 16, color: Color(0xFF64748B)),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    note,
                    style: const TextStyle(
                      fontSize: 11,
                      color: Color(0xFF475569),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildCompatibilityRow({
    required String label,
    required List<String> chips,
    required Color badgeColor,
  }) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: const TextStyle(
            fontSize: 12,
            fontWeight: FontWeight.bold,
            color: NetraColors.textSecondary,
          ),
        ),
        const SizedBox(height: 6),
        Wrap(
          spacing: 6,
          runSpacing: 6,
          children: chips.map((bg) {
            return Container(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
              decoration: BoxDecoration(
                color: badgeColor.withValues(alpha: 0.1),
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: badgeColor.withValues(alpha: 0.3)),
              ),
              child: Text(
                bg,
                style: TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.bold,
                  color: badgeColor,
                ),
              ),
            );
          }).toList(),
        ),
      ],
    );
  }

  Widget _buildStandardsCard() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: NetraColors.surfaceWhite,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: const Color(0xFFE2E8F0)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Icons.verified_outlined,
                  color: NetraColors.primaryRed, size: 22),
              NetraSpacing.gapW12,
              Text(
                "NBTC National Donor Criteria",
                style: NetraTypography.titleMedium.copyWith(
                  fontWeight: FontWeight.bold,
                ),
              ),
            ],
          ),
          NetraSpacing.gapH12,
          _buildCriteriaRow("Age", "18 - 65 years"),
          _buildCriteriaRow("Body Weight", ">= 45 kg (whole blood)"),
          _buildCriteriaRow("Hemoglobin", ">= 12.5 g/dL"),
          _buildCriteriaRow("Blood Pressure", "100-140 systolic / 60-90 diastolic"),
          _buildCriteriaRow("Donation Interval", "90 days (males) / 120 days (females)"),
          NetraSpacing.gapH16,
          ElevatedButton.icon(
            style: ElevatedButton.styleFrom(
              backgroundColor: NetraColors.primaryRed,
              foregroundColor: Colors.white,
              minimumSize: const Size(double.infinity, 46),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(12),
              ),
            ),
            onPressed: () {
              Navigator.of(context).push(
                MaterialPageRoute(
                  builder: (_) => const EligibilityIntroScreen(),
                ),
              );
            },
            icon: const Icon(Icons.fact_check_outlined, size: 18),
            label: const Text(
              "Check My Eligibility Now",
              style: TextStyle(fontWeight: FontWeight.bold),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildCriteriaRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: NetraTypography.bodySmall),
          Text(
            value,
            style: NetraTypography.bodySmall.copyWith(
              fontWeight: FontWeight.bold,
              color: NetraColors.textPrimary,
            ),
          ),
        ],
      ),
    );
  }
}
