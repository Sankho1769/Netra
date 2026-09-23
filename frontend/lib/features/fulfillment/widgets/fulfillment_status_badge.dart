import 'package:flutter/material.dart';
import '../models/fulfillment_model.dart';

class FulfillmentStatusBadge extends StatelessWidget {
  final FulfillmentStatus status;

  const FulfillmentStatusBadge({
    super.key,
    required this.status,
  });

  @override
  Widget build(BuildContext context) {
    final (label, bgColor, textColor, borderColor) = switch (status) {
      FulfillmentStatus.ready => (
          'Ready',
          const Color(0xFFEEF2FF), // Indigo 50
          const Color(0xFF4338CA), // Indigo 700
          const Color(0xFFC7D2FE), // Indigo 200
        ),
      FulfillmentStatus.inProgress => (
          'In Progress',
          const Color(0xFFFEF3C7), // Amber 100
          const Color(0xFFB45309), // Amber 700
          const Color(0xFFFDE68A), // Amber 200
        ),
      FulfillmentStatus.fulfilled => (
          'Fulfilled',
          const Color(0xFFDCFCE7), // Green 100
          const Color(0xFF15803D), // Green 700
          const Color(0xFFBBF7D0), // Green 200
        ),
      FulfillmentStatus.cancelled => (
          'Cancelled',
          const Color(0xFFF3F4F6), // Gray 100
          const Color(0xFF4B5563), // Gray 600
          const Color(0xFFE5E7EB), // Gray 200
        ),
      FulfillmentStatus.failed => (
          'Failed',
          const Color(0xFFFEE2E2), // Red 100
          const Color(0xFFB91C1C), // Red 700
          const Color(0xFFFECACA), // Red 200
        ),
      FulfillmentStatus.unknown => (
          'Unknown',
          const Color(0xFFF3F4F6),
          const Color(0xFF4B5563),
          const Color(0xFFE5E7EB),
        ),
    };

    return Semantics(
      label: 'Fulfillment Status: $label',
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
        decoration: BoxDecoration(
          color: bgColor,
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: borderColor, width: 1),
        ),
        child: Text(
          label,
          style: TextStyle(
            color: textColor,
            fontWeight: FontWeight.w600,
            fontSize: 12,
            letterSpacing: 0.2,
          ),
        ),
      ),
    );
  }
}
