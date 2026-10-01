import 'package:flutter/material.dart';
import '../../../core/theme/clay_glass_theme.dart';
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
    final isCritical = request.urgency == BloodRequestUrgency.critical;

    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      decoration: BoxDecoration(
        color: ClayGlassTheme.claySurfaceLight,
        borderRadius: BorderRadius.circular(22),
        border: Border.all(
          color: isCritical ? const Color(0xFFFCA5A5) : const Color(0xFFF1F5F9),
          width: isCritical ? 1.5 : 1.0,
        ),
        boxShadow: ClayGlassTheme.clayShadow(
          depth: isCritical ? 7.0 : 5.0,
          shadowColor: isCritical ? const Color(0xFFDC2626) : const Color(0xFF0F172A),
          opacity: isCritical ? 0.08 : 0.06,
        ),
      ),
      child: Material(
        color: Colors.transparent,
        borderRadius: BorderRadius.circular(22),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(22),
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
                      width: 54,
                      height: 54,
                      decoration: BoxDecoration(
                        gradient: const LinearGradient(
                          colors: [Color(0xFFFFF5F5), Color(0xFFFEE2E2)],
                          begin: Alignment.topLeft,
                          end: Alignment.bottomRight,
                        ),
                        borderRadius: BorderRadius.circular(16),
                        border: Border.all(
                          color: const Color(0xFFFCA5A5),
                          width: 1.5,
                        ),
                        boxShadow: [
                          BoxShadow(
                            color: const Color(0xFFDC2626).withValues(alpha: 0.15),
                            blurRadius: 8,
                            offset: const Offset(0, 3),
                          ),
                        ],
                      ),
                      alignment: Alignment.center,
                      child: Text(
                        request.bloodGroup,
                        style: const TextStyle(
                          fontSize: 20,
                          fontWeight: FontWeight.w900,
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
                            if (request.unitsFulfilled > 0) ...[
                              const SizedBox(width: 8),
                              Container(
                                padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                                decoration: BoxDecoration(
                                  color: const Color(0xFFDCFCE7),
                                  borderRadius: BorderRadius.circular(6),
                                ),
                                child: Text(
                                  '${request.unitsFulfilled}/${request.unitsRequired} Fulfilled',
                                  style: const TextStyle(
                                    fontSize: 11,
                                    fontWeight: FontWeight.bold,
                                    color: Color(0xFF16A34A),
                                  ),
                                ),
                              ),
                            ],
                          ],
                        ),
                        const SizedBox(height: 4),
                        Row(
                          children: [
                            UrgencyBadge(urgency: request.urgency),
                            const SizedBox(width: 8),
                            if (request.status != BloodRequestStatus.open)
                              RequestStatusBadge(status: request.status),
                            if (request.helperCount > 0) ...[
                              const SizedBox(width: 6),
                              Container(
                                padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                                decoration: BoxDecoration(
                                  color: const Color(0xFFEFF6FF),
                                  borderRadius: BorderRadius.circular(6),
                                ),
                                child: Text(
                                  '${request.helperCount} ${request.helperCount == 1 ? "Offer" : "Offers"}',
                                  style: const TextStyle(
                                    fontSize: 11,
                                    fontWeight: FontWeight.w600,
                                    color: Color(0xFF1D4ED8),
                                  ),
                                ),
                              ),
                            ],
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
              const SizedBox(height: 12),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.symmetric(vertical: 10, horizontal: 14),
                decoration: BoxDecoration(
                  gradient: request.status == BloodRequestStatus.open
                      ? const LinearGradient(
                          colors: [Color(0xFFFFF1F2), Color(0xFFFEE2E2)],
                          begin: Alignment.topLeft,
                          end: Alignment.bottomRight,
                        )
                      : null,
                  color: request.status == BloodRequestStatus.open
                      ? null
                      : const Color(0xFFF8FAFC),
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(
                    color: request.status == BloodRequestStatus.open
                        ? const Color(0xFFFCA5A5)
                        : const Color(0xFFE2E8F0),
                  ),
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Icon(
                      request.status == BloodRequestStatus.open
                          ? Icons.volunteer_activism_rounded
                          : Icons.visibility_outlined,
                      size: 16,
                      color: request.status == BloodRequestStatus.open
                          ? const Color(0xFFDC2626)
                          : Colors.grey.shade700,
                    ),
                    const SizedBox(width: 6),
                    Text(
                      request.status == BloodRequestStatus.open
                          ? 'RAISE HAND / OFFER HELP'
                          : 'VIEW REQUEST DETAILS',
                      style: TextStyle(
                        fontSize: 12,
                        fontWeight: FontWeight.w800,
                        letterSpacing: 0.3,
                        color: request.status == BloodRequestStatus.open
                            ? const Color(0xFFDC2626)
                            : Colors.grey.shade700,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
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
