import 'package:flutter/material.dart';
import '../../common/widgets/netra_app_bar.dart';
import '../theme/netra_colors.dart';
import '../theme/netra_spacing.dart';
import '../theme/netra_typography.dart';
import 'responsive_breakpoints.dart';

class ResponsiveNavigationDestination {
  final IconData icon;
  final IconData? selectedIcon;
  final String label;
  final String? tooltip;

  const ResponsiveNavigationDestination({
    required this.icon,
    this.selectedIcon,
    required this.label,
    this.tooltip,
  });
}

class ResponsiveScaffold extends StatelessWidget {
  final PreferredSizeWidget? appBar;
  final String? title;
  final List<Widget>? actions;
  final Widget body;
  final List<ResponsiveNavigationDestination>? destinations;
  final int selectedIndex;
  final ValueChanged<int>? onDestinationSelected;
  final Widget? floatingActionButton;
  final FloatingActionButtonLocation? floatingActionButtonLocation;
  final Color? backgroundColor;
  final Widget? customBottomBar;

  const ResponsiveScaffold({
    super.key,
    this.appBar,
    this.title,
    this.actions,
    required this.body,
    this.destinations,
    this.selectedIndex = 0,
    this.onDestinationSelected,
    this.floatingActionButton,
    this.floatingActionButtonLocation,
    this.backgroundColor,
    this.customBottomBar,
  });

  @override
  Widget build(BuildContext context) {
    final effectiveAppBar = appBar ??
        (title != null ? NetraAppBar(title: title!, actions: actions) : null);
    final hasNavigation = destinations != null && destinations!.isNotEmpty;
    final isMobile = context.isMobile;
    final isTablet = context.isTablet;

    if (!hasNavigation || isMobile) {
      return Scaffold(
        appBar: effectiveAppBar,
        backgroundColor: backgroundColor ?? NetraColors.backgroundGray,
        body: body,
        floatingActionButton: floatingActionButton,
        floatingActionButtonLocation: floatingActionButtonLocation,
        bottomNavigationBar: customBottomBar ??
            (hasNavigation
                ? NavigationBar(
                    selectedIndex: selectedIndex,
                    onDestinationSelected: onDestinationSelected,
                    backgroundColor: NetraColors.surfaceWhite,
                    indicatorColor: NetraColors.backgroundRed,
                    elevation: 3,
                    destinations: destinations!.map((d) {
                      return NavigationDestination(
                        icon: Icon(d.icon),
                        selectedIcon: Icon(d.selectedIcon ?? d.icon,
                            color: NetraColors.primaryRed),
                        label: d.label,
                        tooltip: d.tooltip,
                      );
                    }).toList(),
                  )
                : null),
      );
    }

    // Tablet: NavigationRail
    if (isTablet) {
      return Scaffold(
        appBar: effectiveAppBar,
        backgroundColor: backgroundColor ?? NetraColors.backgroundGray,
        floatingActionButton: floatingActionButton,
        floatingActionButtonLocation: floatingActionButtonLocation,
        body: Row(
          children: [
            NavigationRail(
              selectedIndex: selectedIndex,
              onDestinationSelected: onDestinationSelected,
              backgroundColor: NetraColors.surfaceWhite,
              indicatorColor: NetraColors.backgroundRed,
              labelType: NavigationRailLabelType.all,
              destinations: destinations!.map((d) {
                return NavigationRailDestination(
                  icon: Icon(d.icon),
                  selectedIcon: Icon(d.selectedIcon ?? d.icon,
                      color: NetraColors.primaryRed),
                  label: Text(
                    d.label,
                    style: NetraTypography.labelSmall,
                  ),
                );
              }).toList(),
            ),
            const VerticalDivider(
                thickness: 1, width: 1, color: NetraColors.borderGray),
            Expanded(child: body),
          ],
        ),
      );
    }

    // Desktop: Extended NavigationRail / Persistent Drawer
    return Scaffold(
      appBar: effectiveAppBar,
      backgroundColor: backgroundColor ?? NetraColors.backgroundGray,
      floatingActionButton: floatingActionButton,
      floatingActionButtonLocation: floatingActionButtonLocation,
      body: Row(
        children: [
          NavigationRail(
            extended: true,
            minExtendedWidth: 220,
            selectedIndex: selectedIndex,
            onDestinationSelected: onDestinationSelected,
            backgroundColor: NetraColors.surfaceWhite,
            indicatorColor: NetraColors.backgroundRed,
            leading: Padding(
              padding: const EdgeInsets.symmetric(
                  vertical: NetraSpacing.lg, horizontal: NetraSpacing.md),
              child: Row(
                children: [
                  Container(
                    padding: NetraSpacing.paddingSm,
                    decoration: BoxDecoration(
                      color: NetraColors.backgroundRed,
                      borderRadius:
                          BorderRadius.circular(NetraSpacing.radiusMd),
                    ),
                    child: const Icon(Icons.water_drop_rounded,
                        color: NetraColors.primaryRed, size: 28),
                  ),
                  NetraSpacing.gapW12,
                  const Text(
                    'NETRA',
                    style: TextStyle(
                      fontSize: 20,
                      fontWeight: FontWeight.w800,
                      color: NetraColors.primaryRed,
                      letterSpacing: 1.2,
                    ),
                  ),
                ],
              ),
            ),
            destinations: destinations!.map((d) {
              return NavigationRailDestination(
                icon: Icon(d.icon),
                selectedIcon: Icon(d.selectedIcon ?? d.icon,
                    color: NetraColors.primaryRed),
                label: Text(
                  d.label,
                  style: NetraTypography.labelLarge,
                ),
              );
            }).toList(),
          ),
          const VerticalDivider(
              thickness: 1, width: 1, color: NetraColors.borderGray),
          Expanded(child: body),
        ],
      ),
    );
  }
}
