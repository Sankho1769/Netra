import 'package:flutter/material.dart';
import '../models/donor_match_response_model.dart';
import 'match_status_badge.dart';

/// Card widget presenting an assigned match to a donor with action triggers.
class DonorResponseCard extends StatelessWidget {
  final DonorMatchDetail match;
  final VoidCallback onTap;

  const DonorResponseCard({
    super.key,
    required this.match,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isCritical = match.urgency.toUpperCase() == 'CRITICAL';

    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      elevation: 2,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: BorderSide(
          color: isCritical ? Colors.red.shade300 : Colors.grey.shade300,
          width: isCritical ? 1.5 : 1.0,
        ),
      ),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Header: Urgency tag, Blood Group badge, Status
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Row(
                    children: [
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                        decoration: BoxDecoration(
                          color: const Color(0xFFDC2626),
                          borderRadius: BorderRadius.circular(6),
                        ),
                        child: Text(
                          match.bloodGroupRequired.replaceAll('_', ' '),
                          style: const TextStyle(
                            color: Colors.white,
                            fontWeight: FontWeight.bold,
                            fontSize: 13,
                          ),
                        ),
                      ),
                      const SizedBox(width: 8),
                      if (isCritical)
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                          decoration: BoxDecoration(
                            color: Colors.red.shade50,
                            borderRadius: BorderRadius.circular(6),
                            border: Border.all(color: Colors.red.shade300),
                          ),
                          child: Row(
                            children: [
                              Icon(Icons.warning_amber_rounded, size: 14, color: Colors.red.shade700),
                              const SizedBox(width: 4),
                              Text(
                                'CRITICAL',
                                style: TextStyle(
                                  color: Colors.red.shade700,
                                  fontSize: 11,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                            ],
                          ),
                        ),
                    ],
                  ),
                  MatchStatusBadge(status: match.responseStatus),
                ],
              ),
              const SizedBox(height: 12),

              // Hospital & Location
              Row(
                children: [
                  const Icon(Icons.local_hospital_outlined, size: 18, color: Colors.grey),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      match.hospitalName,
                      style: theme.textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.bold,
                      ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 4),

              // City and Distance
              Row(
                children: [
                  const Icon(Icons.place_outlined, size: 16, color: Colors.grey),
                  const SizedBox(width: 6),
                  Text(
                    match.city.isNotEmpty ? '${match.city}, ${match.state}' : 'Location provided upon clearance',
                    style: theme.textTheme.bodySmall?.copyWith(color: Colors.grey.shade700),
                  ),
                  if (match.distanceKm != null) ...[
                    const SizedBox(width: 12),
                    Text(
                      '•  ~${match.distanceKm!.toStringAsFixed(1)} km away',
                      style: TextStyle(
                        fontSize: 12,
                        fontWeight: FontWeight.w600,
                        color: Colors.blueGrey.shade700,
                      ),
                    ),
                  ],
                ],
              ),
              const SizedBox(height: 12),

              // Units & Expiry notice
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    '${match.unitsRequired} unit(s) requested',
                    style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w500),
                  ),
                  Text(
                    match.isExpired ? 'Expired' : 'Expires in ${_formatRemainingTime(match.expiresAt)}',
                    style: TextStyle(
                      fontSize: 11,
                      color: match.isExpired ? Colors.red.shade700 : Colors.grey.shade600,
                      fontStyle: FontStyle.italic,
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  String _formatRemainingTime(DateTime expiry) {
    final diff = expiry.difference(DateTime.now());
    if (diff.isNegative) return 'now';
    if (diff.inHours > 0) return '${diff.inHours}h ${diff.inMinutes % 60}m';
    return '${diff.inMinutes}m';
  }
}
