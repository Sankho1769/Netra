import 'package:flutter/material.dart';
import '../models/donor_match_model.dart';

/// Presentation card for a matched donor candidate.
///
/// Designed strictly for decision support and clinical readiness review.
/// Never exposes raw personal coordinates or contact information.
class DonorMatchCard extends StatelessWidget {
  final DonorMatch match;
  final int rank;
  final bool isMatched;
  final bool isProcessing;
  final VoidCallback? onSelectDonor;

  const DonorMatchCard({
    super.key,
    required this.match,
    required this.rank,
    this.isMatched = false,
    this.isProcessing = false,
    this.onSelectDonor,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isExact = match.compatibilityType == CompatibilityType.exact;

    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      elevation: 2,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: BorderSide(
          color: isExact
              ? const Color(0xFFDC2626).withValues(alpha: 0.3)
              : Colors.grey.shade300,
          width: isExact ? 1.5 : 1.0,
        ),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Top Row: Rank, Donor Masked Name, Compatibility Badge
            Row(
              children: [
                // Rank Circle
                Container(
                  width: 28,
                  height: 28,
                  alignment: Alignment.center,
                  decoration: BoxDecoration(
                    color: Colors.grey.shade200,
                    shape: BoxShape.circle,
                  ),
                  child: Text(
                    '#$rank',
                    style: TextStyle(
                      fontSize: 12,
                      fontWeight: FontWeight.bold,
                      color: Colors.grey.shade700,
                    ),
                  ),
                ),
                const SizedBox(width: 10),
                // Masked Name
                Expanded(
                  child: Text(
                    match.donorDisplayName,
                    style: theme.textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
                // Compatibility Badge
                _buildCompatibilityBadge(isExact),
              ],
            ),
            const SizedBox(height: 12),

            // Middle Row: Blood Group, Quality, Verification Chips
            Wrap(
              spacing: 8,
              runSpacing: 6,
              crossAxisAlignment: WrapCrossAlignment.center,
              children: [
                // Blood Group Badge
                Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                  decoration: BoxDecoration(
                    color: const Color(0xFFFEE2E2),
                    borderRadius: BorderRadius.circular(6),
                    border:
                        Border.all(color: const Color(0xFFDC2626), width: 1),
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
                // Match Quality Chip
                _buildQualityChip(match.matchQuality),
                // Verification Chip
                _buildVerificationChip(match.isVerified),
              ],
            ),
            const SizedBox(height: 12),
            const Divider(height: 1),
            const SizedBox(height: 10),

            // Bottom Row: Haversine Distance & Action
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Row(
                  children: [
                    const Icon(Icons.near_me,
                        size: 16, color: Color(0xFF4B5563)),
                    const SizedBox(width: 4),
                    Text(
                      '${match.distanceKm.toStringAsFixed(1)} km away',
                      style: const TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.w500,
                        color: Color(0xFF374151),
                      ),
                    ),
                  ],
                ),
                if (isMatched)
                  Container(
                    padding:
                        const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: const Color(0xFFDCFCE7),
                      borderRadius: BorderRadius.circular(6),
                      border: Border.all(color: const Color(0xFF16A34A)),
                    ),
                    child: const Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(Icons.check, size: 14, color: Color(0xFF16A34A)),
                        SizedBox(width: 4),
                        Text(
                          'Match Created',
                          style: TextStyle(
                            color: Color(0xFF15803D),
                            fontWeight: FontWeight.bold,
                            fontSize: 12,
                          ),
                        ),
                      ],
                    ),
                  )
                else if (onSelectDonor != null)
                  ElevatedButton(
                    onPressed: isProcessing ? null : onSelectDonor,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: const Color(0xFFDC2626),
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(
                          horizontal: 14, vertical: 6),
                      shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(6)),
                    ),
                    child: isProcessing
                        ? const SizedBox(
                            width: 14,
                            height: 14,
                            child: CircularProgressIndicator(
                                strokeWidth: 2, color: Colors.white),
                          )
                        : const Text(
                            'Select Donor',
                            style: TextStyle(
                                fontSize: 12, fontWeight: FontWeight.bold),
                          ),
                  ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildCompatibilityBadge(bool isExact) {
    if (isExact) {
      return Container(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
        decoration: BoxDecoration(
          color: const Color(0xFFEFF6FF),
          borderRadius: BorderRadius.circular(6),
          border: Border.all(color: const Color(0xFF3B82F6)),
        ),
        child: const Text(
          'Exact Match',
          style: TextStyle(
            color: Color(0xFF1D4ED8),
            fontSize: 11,
            fontWeight: FontWeight.w600,
          ),
        ),
      );
    }
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: const Color(0xFFF3F4F6),
        borderRadius: BorderRadius.circular(6),
      ),
      child: const Text(
        'Compatible',
        style: TextStyle(
          color: Color(0xFF4B5563),
          fontSize: 11,
          fontWeight: FontWeight.w500,
        ),
      ),
    );
  }

  Widget _buildQualityChip(MatchQuality quality) {
    Color bg;
    Color text;
    switch (quality) {
      case MatchQuality.excellent:
        bg = const Color(0xFFD1FAE5);
        text = const Color(0xFF065F46);
        break;
      case MatchQuality.good:
        bg = const Color(0xFFE0E7FF);
        text = const Color(0xFF3730A3);
        break;
      case MatchQuality.fair:
        bg = const Color(0xFFF3F4F6);
        text = const Color(0xFF6B7280);
        break;
    }
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(6),
      ),
      child: Text(
        '${quality.displayName} Quality',
        style:
            TextStyle(color: text, fontSize: 11, fontWeight: FontWeight.w600),
      ),
    );
  }

  Widget _buildVerificationChip(bool isVerified) {
    if (isVerified) {
      return Container(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
        decoration: BoxDecoration(
          color: const Color(0xFFDCFCE7),
          borderRadius: BorderRadius.circular(6),
        ),
        child: const Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.verified, size: 13, color: Color(0xFF16A34A)),
            SizedBox(width: 3),
            Text(
              'Verified',
              style: TextStyle(
                color: Color(0xFF16A34A),
                fontSize: 11,
                fontWeight: FontWeight.w600,
              ),
            ),
          ],
        ),
      );
    }
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: const Color(0xFFFEF3C7),
        borderRadius: BorderRadius.circular(6),
      ),
      child: const Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(Icons.info_outline, size: 13, color: Color(0xFFB45309)),
          SizedBox(width: 3),
          Text(
            'Self-Reported',
            style: TextStyle(
              color: Color(0xFFB45309),
              fontSize: 11,
              fontWeight: FontWeight.w500,
            ),
          ),
        ],
      ),
    );
  }
}
