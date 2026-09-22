import 'package:flutter/material.dart';
import '../models/blood_request.dart';
import 'urgency_badge.dart';
import 'request_status_badge.dart';

class BloodRequestCard extends StatelessWidget {
  final BloodRequestSummary request;
  final VoidCallback? onTap;

  const BloodRequestCard({
    super.key,
    required this.request,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final deadlineString = _formatDateTime(request.requiredBy);

    return Card(
      elevation: 2,
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(
          color: request.urgency == BloodRequestUrgency.critical
              ? const Color(0xFFFCA5A5)
              : Colors.grey.shade200,
          width: request.urgency == BloodRequestUrgency.critical ? 1.5 : 1.0,
        ),
      ),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(16),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Header Row: Blood Group badge + Urgency & Status
              Row(
                crossAxisAlignment: CrossAxisAlignment.center,
                children: [
                  Container(
                    width: 52,
                    height: 52,
                    decoration: BoxDecoration(
                      color: const Color(0xFFDC2626).withOpacity(0.1),
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(
                        color: const Color(0xFFDC2626).withOpacity(0.3),
                        width: 1.5,
                      ),
                    ),
                    alignment: Alignment.center,
                    child: Text(
                      request.bloodGroup,
                      style: const TextStyle(
                        fontSize: 20,
                        fontWeight: FontWeight.w800,
                        color: Color(0xFFDC2626),
                      ),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            Text(
                              '${request.unitsRequired} ${request.unitsRequired == 1 ? "Unit" : "Units"} Needed',
                              style: theme.textTheme.titleMedium?.copyWith(
                                fontWeight: FontWeight.w700,
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 4),
                        Row(
                          children: [
                            UrgencyBadge(urgency: request.urgency),
                            const SizedBox(width: 8),
                            if (request.status != BloodRequestStatus.open)
                              RequestStatusBadge(status: request.status),
                          ],
                        ),
                      ],
                    ),
                  ),
                  if (request.distanceKm != null)
                    Container(
                      padding: const EdgeInsets.symmetric(
                          horizontal: 8, vertical: 4),
                      decoration: BoxDecoration(
                        color: Colors.blueGrey.shade50,
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Icon(Icons.near_me_outlined,
                              size: 14, color: Colors.blueGrey.shade700),
                          const SizedBox(width: 4),
                          Text(
                            '${request.distanceKm} km',
                            style: TextStyle(
                              fontSize: 12,
                              fontWeight: FontWeight.w600,
                              color: Colors.blueGrey.shade700,
                            ),
                          ),
                        ],
                      ),
                    ),
                ],
              ),
              const Divider(height: 24),
              // Hospital & Location
              Row(
                children: [
                  const Icon(Icons.local_hospital_outlined,
                      size: 18, color: Color(0xFF4B5563)),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      request.hospitalName,
                      style: theme.textTheme.bodyMedium?.copyWith(
                        fontWeight: FontWeight.w600,
                        color: const Color(0xFF1F2937),
                      ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 6),
              Row(
                children: [
                  const Icon(Icons.location_on_outlined,
                      size: 18, color: Color(0xFF6B7280)),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      '${request.city}, ${request.state}',
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: const Color(0xFF6B7280),
                      ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 10),
              // Required By Deadline
              Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                decoration: BoxDecoration(
                  color: Colors.grey.shade100,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Row(
                  children: [
                    Icon(
                      Icons.schedule_outlined,
                      size: 16,
                      color: request.urgency == BloodRequestUrgency.critical
                          ? const Color(0xFFDC2626)
                          : const Color(0xFF4B5563),
                    ),
                    const SizedBox(width: 6),
                    Text(
                      'Required by: ',
                      style: TextStyle(
                        fontSize: 12,
                        fontWeight: FontWeight.w500,
                        color: Colors.grey.shade600,
                      ),
                    ),
                    Expanded(
                      child: Text(
                        deadlineString,
                        style: TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.w700,
                          color: request.urgency == BloodRequestUrgency.critical
                              ? const Color(0xFFDC2626)
                              : const Color(0xFF1F2937),
                        ),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                    const Icon(Icons.chevron_right,
                        size: 18, color: Color(0xFF9CA3AF)),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  String _formatDateTime(DateTime dt) {
    final local = dt.toLocal();
    final year = local.year;
    final month = local.month.toString().padLeft(2, '0');
    final day = local.day.toString().padLeft(2, '0');
    final hour = local.hour.toString().padLeft(2, '0');
    final min = local.minute.toString().padLeft(2, '0');
    return '$year-$month-$day $hour:$min';
  }
}
