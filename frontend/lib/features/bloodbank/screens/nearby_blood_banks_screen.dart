import 'package:flutter/material.dart';
import '../../../core/responsive/responsive.dart';
import '../../../core/theme/netra_colors.dart';
import '../../../core/theme/netra_spacing.dart';
import '../../../core/theme/netra_typography.dart';
import '../../../common/widgets/common_widgets.dart';
import '../../../core/location/location_service.dart';
import '../../../core/location/location_models.dart';

class NearbyBloodBanksScreen extends StatefulWidget {
  const NearbyBloodBanksScreen({super.key});

  @override
  State<NearbyBloodBanksScreen> createState() => _NearbyBloodBanksScreenState();
}

class _NearbyBloodBanksScreenState extends State<NearbyBloodBanksScreen> {
  final TextEditingController _searchController = TextEditingController();

  final List<Map<String, String>> _bloodBanks = [
    {
      "name": "Tata Memorial Hospital Blood Centre",
      "address": "Dr. E Borges Road, Parel, Mumbai",
      "distance": "2.4 km away",
      "hours": "Open 24/7",
      "phone": "+91 22 2417 7000",
      "verified": "Govt Authorized / NBTC Certified"
    },
    {
      "name": "KEM Hospital Regional Blood Transfusion Centre",
      "address": "Acharya Donde Marg, Parel, Mumbai",
      "distance": "3.1 km away",
      "hours": "Open 24/7",
      "phone": "+91 22 2410 7000",
      "verified": "Govt Authorized / NBTC Certified"
    },
    {
      "name": "Red Cross Society Blood Centre",
      "address": "141 Shahid Bhagat Singh Road, Fort, Mumbai",
      "distance": "6.8 km away",
      "hours": "9:00 AM - 8:00 PM",
      "phone": "+91 22 2266 1524",
      "verified": "Govt Authorized / NBTC Certified"
    },
    {
      "name": "Lilavati Hospital & Research Centre Blood Bank",
      "address": "A-791 Bandra Reclamation, Bandra West, Mumbai",
      "distance": "8.2 km away",
      "hours": "Open 24/7",
      "phone": "+91 22 2675 1000",
      "verified": "Govt Authorized / NBTC Certified"
    }
  ];

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final isDesktopOrTablet = !context.isMobile;

    return Scaffold(
      appBar: const NetraAppBar(
        title: "Authorized Blood Centres",
      ),
      backgroundColor: NetraColors.backgroundGray,
      body: SafeArea(
        child: Column(
          children: [
            // Search & privacy banner
            Container(
              color: NetraColors.surfaceWhite,
              child: ResponsiveContainer.wide(
                padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
                child: Column(
                  children: [
                    TextField(
                      controller: _searchController,
                      style: NetraTypography.bodyLarge,
                      decoration: InputDecoration(
                        hintText: "Enter city, district, or PIN code",
                        prefixIcon: const Icon(Icons.search_rounded, color: NetraColors.textSecondary),
                        suffixIcon: IconButton(
                          icon: const Icon(Icons.my_location_rounded, color: NetraColors.primaryRed),
                          tooltip: "Use approximate device location",
                          onPressed: () async {
                            final locationService = DefaultLocationService();
                            final loc = await locationService.getCurrentLocation(approximateOnly: true);
                            if (mounted && loc != null) {
                              _searchController.text = loc.displayName;
                              ScaffoldMessenger.of(context).showSnackBar(
                                SnackBar(
                                  content: Text("Approximate location used: ${loc.displayName}. Exact coordinates are not stored."),
                                ),
                              );
                            }
                          },
                        ),
                      ),
                    ),
                    NetraSpacing.gapH8,
                    Row(
                      children: [
                        const Icon(Icons.shield_outlined, size: 14, color: NetraColors.textMuted),
                        NetraSpacing.gapW8,
                        Expanded(
                          child: Text(
                            "NETRA uses approximate location for discovery. Exact location is never permanently recorded.",
                            style: NetraTypography.bodySmall.copyWith(color: NetraColors.textMuted),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
            const Divider(height: 1, color: NetraColors.borderGray),

            // Responsive Blood Banks Content
            Expanded(
              child: ResponsiveContainer.wide(
                child: isDesktopOrTablet
                    ? GridView.builder(
                        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                          crossAxisCount: 2,
                          crossAxisSpacing: 16,
                          mainAxisSpacing: 16,
                          mainAxisExtent: 148,
                        ),
                        itemCount: _bloodBanks.length,
                        itemBuilder: (context, index) => _buildBloodBankCard(_bloodBanks[index]),
                      )
                    : ListView.separated(
                        itemCount: _bloodBanks.length,
                        separatorBuilder: (context, index) => NetraSpacing.gapH12,
                        itemBuilder: (context, index) => _buildBloodBankCard(_bloodBanks[index]),
                      ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildBloodBankCard(Map<String, String> bank) {
    return NetraCard.outlined(
      padding: NetraSpacing.cardPadding,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                padding: NetraSpacing.paddingSm,
                decoration: BoxDecoration(
                  color: NetraColors.backgroundRed,
                  borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
                ),
                child: const Icon(Icons.local_hospital_rounded, color: NetraColors.primaryRed, size: 22),
              ),
              NetraSpacing.gapW12,
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      bank['name']!,
                      style: NetraTypography.titleMedium,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                    NetraSpacing.gapH4,
                    Text(
                      bank['address']!,
                      style: NetraTypography.bodySmall.copyWith(color: NetraColors.textSecondary),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ],
                ),
              ),
            ],
          ),
          Row(
            children: [
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                decoration: BoxDecoration(
                  color: NetraColors.backgroundGray,
                  borderRadius: BorderRadius.circular(NetraSpacing.radiusXs),
                  border: Border.all(color: NetraColors.borderSubtle),
                ),
                child: Text(
                  bank['distance']!,
                  style: NetraTypography.labelSmall.copyWith(color: NetraColors.textPrimary),
                ),
              ),
              NetraSpacing.gapW8,
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                decoration: BoxDecoration(
                  color: NetraColors.eligibleGreenBg,
                  borderRadius: BorderRadius.circular(NetraSpacing.radiusXs),
                ),
                child: Text(
                  bank['hours']!,
                  style: NetraTypography.labelSmall.copyWith(color: NetraColors.eligibleGreen),
                ),
              ),
              const Spacer(),
              Text(
                bank['verified']!,
                style: NetraTypography.bodySmall.copyWith(
                  color: NetraColors.primaryRed,
                  fontWeight: FontWeight.w500,
                  fontSize: 11,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
