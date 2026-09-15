import 'package:flutter/material.dart';
import '../../../common/widgets/common_widgets.dart';
import '../../../core/responsive/responsive.dart';
import '../../../core/theme/netra_colors.dart';
import '../../../core/theme/netra_spacing.dart';
import '../../../core/theme/netra_typography.dart';
import '../models/blood_bank.dart';
import '../models/blood_inventory.dart';
import '../services/bloodbank_api_service.dart';
import '../widgets/blood_availability_card.dart';
import '../widgets/verification_badge.dart';

class BloodBankDetailsScreen extends StatefulWidget {
  final String bloodBankId;
  final double? userLat;
  final double? userLon;

  const BloodBankDetailsScreen({
    super.key,
    required this.bloodBankId,
    this.userLat,
    this.userLon,
  });

  @override
  State<BloodBankDetailsScreen> createState() => _BloodBankDetailsScreenState();
}

class _BloodBankDetailsScreenState extends State<BloodBankDetailsScreen> {
  final BloodBankApiService _apiService = BloodBankApiService();

  BloodBankDetail? _bloodBank;
  bool _isLoading = true;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _loadDetails();
  }

  Future<void> _loadDetails() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final details = await _apiService.getBloodBankDetails(
        widget.bloodBankId,
        userLat: widget.userLat,
        userLon: widget.userLon,
      );
      if (mounted) {
        setState(() {
          _bloodBank = details;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _errorMessage = e.toString().replaceFirst('Exception: ', '');
          _isLoading = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final isDesktopOrTablet = !context.isMobile;

    return Scaffold(
      appBar: NetraAppBar(
        title: _bloodBank?.name ?? "Blood Centre Details",
      ),
      backgroundColor: NetraColors.backgroundGray,
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: NetraColors.primaryRed))
          : _errorMessage != null
              ? Center(
                  child: Padding(
                    padding: const EdgeInsets.all(24.0),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        const Icon(Icons.error_outline_rounded, size: 48, color: NetraColors.ineligibleRed),
                        NetraSpacing.gapH12,
                        Text(_errorMessage!, style: NetraTypography.bodyLarge, textAlign: TextAlign.center),
                        NetraSpacing.gapH16,
                        NetraButton.primary(
                          text: "Retry",
                          onPressed: _loadDetails,
                        ),
                      ],
                    ),
                  ),
                )
              : _buildContent(context, isDesktopOrTablet),
    );
  }

  Widget _buildContent(BuildContext context, bool isDesktopOrTablet) {
    final bank = _bloodBank!;
    final isOpen = bank.operatingStatus == BloodBankOperatingStatus.open;

    return SafeArea(
      child: SingleChildScrollView(
        child: ResponsiveContainer.wide(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Header Card
              NetraCard.elevated(
                padding: NetraSpacing.cardPadding,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(bank.name, style: NetraTypography.headlineSmall),
                              if (bank.registrationNumber != null && bank.registrationNumber!.isNotEmpty) ...[
                                NetraSpacing.gapH4,
                                Text(
                                  'Reg: ${bank.registrationNumber}',
                                  style: NetraTypography.labelSmall.copyWith(color: NetraColors.textMuted),
                                ),
                              ],
                            ],
                          ),
                        ),
                        BloodBankVerificationBadge(status: bank.verificationStatus),
                      ],
                    ),
                    NetraSpacing.gapH16,
                    const Divider(height: 1, color: NetraColors.borderGray),
                    NetraSpacing.gapH16,

                    // Details Info Rows
                    _buildInfoRow(Icons.location_on_outlined, '${bank.address}, ${bank.city}, ${bank.state} - ${bank.postalCode}'),
                    NetraSpacing.gapH12,
                    _buildInfoRow(Icons.phone_outlined, bank.phone),
                    if (bank.email != null && bank.email!.isNotEmpty) ...[
                      NetraSpacing.gapH12,
                      _buildInfoRow(Icons.email_outlined, bank.email!),
                    ],
                    NetraSpacing.gapH12,
                    Row(
                      children: [
                        Icon(
                          isOpen ? Icons.access_time_rounded : Icons.access_time_filled_rounded,
                          size: 18,
                          color: isOpen ? NetraColors.eligibleGreen : NetraColors.ineligibleRed,
                        ),
                        NetraSpacing.gapW8,
                        Text(
                          'Operating Status: ${bank.operatingStatus.displayName}',
                          style: NetraTypography.bodyMedium.copyWith(
                            color: isOpen ? NetraColors.eligibleGreen : NetraColors.ineligibleRed,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                        if (bank.distanceKm != null) ...[
                          const Spacer(),
                          Text(
                            bank.formattedDistance,
                            style: NetraTypography.bodySmall.copyWith(color: NetraColors.textSecondary),
                          ),
                        ],
                      ],
                    ),
                  ],
                ),
              ),

              NetraSpacing.gapH24,

              // Inventory Section Header
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text('Blood Stock Availability', style: NetraTypography.titleLarge),
                  IconButton(
                    icon: const Icon(Icons.refresh_rounded, color: NetraColors.primaryRed),
                    tooltip: 'Refresh Inventory',
                    onPressed: _loadDetails,
                  ),
                ],
              ),
              NetraSpacing.gapH4,
              Text(
                'Reported stock levels with freshness verification. NETRA does not reserve blood units.',
                style: NetraTypography.bodySmall.copyWith(color: NetraColors.textMuted),
              ),

              NetraSpacing.gapH16,

              // Inventory Matrix
              if (bank.inventory.isEmpty)
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.all(24),
                  decoration: BoxDecoration(
                    color: NetraColors.surfaceWhite,
                    borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
                    border: Border.all(color: NetraColors.borderGray),
                  ),
                  child: Center(
                    child: Text(
                      'No inventory reported yet by this blood centre.',
                      style: NetraTypography.bodyMedium.copyWith(color: NetraColors.textSecondary),
                    ),
                  ),
                )
              else
                isDesktopOrTablet
                    ? GridView.builder(
                        shrinkWrap: true,
                        physics: const NeverScrollableScrollPhysics(),
                        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                          crossAxisCount: 4,
                          crossAxisSpacing: 12,
                          mainAxisSpacing: 12,
                          mainAxisExtent: 110,
                        ),
                        itemCount: bank.inventory.length,
                        itemBuilder: (context, index) => BloodAvailabilityCard(item: bank.inventory[index]),
                      )
                    : GridView.builder(
                        shrinkWrap: true,
                        physics: const NeverScrollableScrollPhysics(),
                        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                          crossAxisCount: 2,
                          crossAxisSpacing: 12,
                          mainAxisSpacing: 12,
                          mainAxisExtent: 110,
                        ),
                        itemCount: bank.inventory.length,
                        itemBuilder: (context, index) => BloodAvailabilityCard(item: bank.inventory[index]),
                      ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildInfoRow(IconData icon, String text) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(icon, size: 18, color: NetraColors.textSecondary),
        NetraSpacing.gapW8,
        Expanded(
          child: Text(
            text,
            style: NetraTypography.bodyMedium.copyWith(color: NetraColors.textPrimary),
          ),
        ),
      ],
    );
  }
}
