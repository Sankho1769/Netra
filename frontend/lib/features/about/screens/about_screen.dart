import 'package:flutter/material.dart';
import '../../../core/responsive/responsive_container.dart';
import '../../../core/theme/netra_colors.dart';
import '../../../core/theme/netra_spacing.dart';
import '../../../core/theme/netra_typography.dart';
import '../../eligibility/screens/eligibility_intro_screen.dart';

class AboutScreen extends StatefulWidget {
  final bool isEmbedded;

  const AboutScreen({super.key, this.isEmbedded = false});

  @override
  State<AboutScreen> createState() => _AboutScreenState();
}

class _AboutScreenState extends State<AboutScreen> {
  String _selectedBloodGroup = 'O+';

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
    final content = ResponsiveContainer.standard(
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
                    "COMMUNITY MONTHLY GOAL",
                    style: TextStyle(
                      fontSize: 10,
                      fontWeight: FontWeight.w800,
                      color: Colors.grey.shade500,
                      letterSpacing: 1.2,
                    ),
                  ),
                  const SizedBox(height: 4),
                  const Text(
                    "5,000 Units",
                    style: TextStyle(
                      fontSize: 22,
                      fontWeight: FontWeight.w900,
                      color: NetraColors.textPrimary,
                    ),
                  ),
                ],
              ),
              Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  const Text(
                    "4,250",
                    style: TextStyle(
                      fontSize: 26,
                      fontWeight: FontWeight.w900,
                      color: NetraColors.primaryRed,
                    ),
                  ),
                  Text(
                    "COLLECTED",
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
          ClipRRect(
            borderRadius: BorderRadius.circular(10),
            child: LinearProgressIndicator(
              value: 0.85,
              minHeight: 10,
              backgroundColor: const Color(0xFFF1F5F9),
              valueColor: const AlwaysStoppedAnimation<Color>(NetraColors.primaryRed),
            ),
          ),
          const SizedBox(height: 10),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                "85% Achieved",
                style: TextStyle(
                  fontSize: 11,
                  fontWeight: FontWeight.w700,
                  color: Colors.grey.shade600,
                  fontStyle: FontStyle.italic,
                ),
              ),
              const Text(
                "750 Units Left",
                style: TextStyle(
                  fontSize: 11,
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
