import 'package:flutter/material.dart';

class EventCapacityIndicator extends StatelessWidget {
  final int currentCount;
  final int capacity;

  const EventCapacityIndicator({
    super.key,
    required this.currentCount,
    required this.capacity,
  });

  @override
  Widget build(BuildContext context) {
    final double fraction =
        capacity > 0 ? (currentCount / capacity).clamp(0.0, 1.0) : 0.0;
    final int remaining = capacity > currentCount ? capacity - currentCount : 0;
    final bool isFull = remaining == 0;

    Color progressColor;
    if (fraction < 0.7) {
      progressColor = Colors.green;
    } else if (fraction < 0.9) {
      progressColor = Colors.amber.shade800;
    } else {
      progressColor = Colors.red.shade700;
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              'Registrations: $currentCount / $capacity',
              style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600),
            ),
            Text(
              isFull ? 'Camp Full' : '$remaining slots left',
              style: TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.bold,
                color: isFull ? Colors.red.shade700 : progressColor,
              ),
            ),
          ],
        ),
        const SizedBox(height: 6),
        ClipRRect(
          borderRadius: BorderRadius.circular(4),
          child: LinearProgressIndicator(
            value: fraction,
            minHeight: 6,
            backgroundColor: Colors.grey.shade200,
            valueColor: AlwaysStoppedAnimation<Color>(progressColor),
          ),
        ),
      ],
    );
  }
}
