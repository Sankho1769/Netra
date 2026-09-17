import 'package:flutter/material.dart';
import '../models/donation_event.dart';
import 'event_capacity_indicator.dart';
import 'event_status_badge.dart';

class DonationEventCard extends StatelessWidget {
  final DonationEventSummary event;
  final VoidCallback onTap;

  const DonationEventCard({
    super.key,
    required this.event,
    required this.onTap,
  });

  String _formatDate(DateTime dt) {
    const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    const days = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
    final weekday = days[dt.weekday - 1];
    final month = months[dt.month - 1];
    return '$weekday, $month ${dt.day}';
  }

  String _formatTime(DateTime dt) {
    final hour = dt.hour % 12 == 0 ? 12 : dt.hour % 12;
    final minute = dt.minute.toString().padLeft(2, '0');
    final period = dt.hour >= 12 ? 'PM' : 'AM';
    return '$hour:$minute $period';
  }

  @override
  Widget build(BuildContext context) {
    final dateStr = _formatDate(event.startAt.toLocal());
    final startTimeStr = _formatTime(event.startAt.toLocal());
    final endTimeStr = _formatTime(event.endAt.toLocal());

    return Card(
      elevation: 1,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: BorderSide(color: Colors.grey.shade200),
      ),
      margin: const EdgeInsets.symmetric(vertical: 8, horizontal: 4),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Header: Status Badge + Distance
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  EventStatusBadge(status: event.status),
                  if (event.distanceKm != null)
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                      decoration: BoxDecoration(
                        color: Colors.blue.shade50,
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Row(
                        children: [
                          Icon(Icons.near_me, size: 12, color: Colors.blue.shade700),
                          const SizedBox(width: 4),
                          Text(
                            '${event.distanceKm} km away',
                            style: TextStyle(
                              fontSize: 12,
                              fontWeight: FontWeight.w600,
                              color: Colors.blue.shade700,
                            ),
                          ),
                        ],
                      ),
                    ),
                ],
              ),
              const SizedBox(height: 12),

              // Title
              Text(
                event.title,
                style: const TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                  letterSpacing: -0.2,
                ),
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
              ),
              const SizedBox(height: 4),

              // Blood Bank name
              Row(
                children: [
                  Icon(Icons.local_hospital_outlined, size: 14, color: Colors.grey.shade700),
                  const SizedBox(width: 4),
                  Expanded(
                    child: Text(
                      event.bloodBankName,
                      style: TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.w500,
                        color: Colors.grey.shade700,
                      ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 8),

              // Venue & City
              Row(
                children: [
                  Icon(Icons.location_on_outlined, size: 14, color: Colors.grey.shade700),
                  const SizedBox(width: 4),
                  Expanded(
                    child: Text(
                      '${event.venueName}, ${event.city}',
                      style: TextStyle(fontSize: 13, color: Colors.grey.shade600),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 6),

              // Date & Time
              Row(
                children: [
                  Icon(Icons.calendar_today_outlined, size: 14, color: Colors.grey.shade700),
                  const SizedBox(width: 4),
                  Text(
                    '$dateStr ($startTimeStr - $endTimeStr)',
                    style: TextStyle(
                      fontSize: 13,
                      fontWeight: FontWeight.w500,
                      color: Colors.grey.shade800,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 14),

              // Capacity Indicator
              EventCapacityIndicator(
                currentCount: event.currentRegistrationCount,
                capacity: event.donorCapacity,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
