import 'package:flutter/material.dart';
import '../../../core/responsive/responsive_breakpoints.dart';
import '../../../core/responsive/responsive_scaffold.dart';
import '../../../common/widgets/netra_loading_indicator.dart';
import '../../../common/widgets/netra_error_view.dart';
import '../../../common/widgets/netra_empty_view.dart';
import '../models/donation_event.dart';
import '../state/donation_event_controller.dart';
import '../widgets/donation_event_card.dart';
import 'donation_event_details_screen.dart';
import 'my_event_registrations_screen.dart';
import 'create_donation_event_screen.dart';

class DonationEventListScreen extends StatefulWidget {
  final DonationEventController? controller;

  const DonationEventListScreen({super.key, this.controller});

  @override
  State<DonationEventListScreen> createState() => _DonationEventListScreenState();
}

class _DonationEventListScreenState extends State<DonationEventListScreen> {
  late final DonationEventController _controller;
  final TextEditingController _cityController = TextEditingController();

  @override
  void initState() {
    super.initState();
    _controller = widget.controller ?? DonationEventController();
    _controller.loadEvents();
  }

  @override
  void dispose() {
    _cityController.dispose();
    if (widget.controller == null) {
      _controller.dispose();
    }
    super.dispose();
  }

  void _onCitySubmitted(String value) {
    _controller.setCity(value.trim().isEmpty ? null : value.trim());
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _controller,
      builder: (context, _) {
        final screenWidth = MediaQuery.of(context).size.width;
        int crossAxisCount = 1;
        if (screenWidth >= ResponsiveBreakpoints.desktop) {
          crossAxisCount = 3;
        } else if (screenWidth >= ResponsiveBreakpoints.tablet) {
          crossAxisCount = 2;
        }

        return ResponsiveScaffold(
          title: 'Donation Camps',
          actions: [
            IconButton(
              icon: const Icon(Icons.bookmark_outline),
              tooltip: 'My Registrations',
              onPressed: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(builder: (_) => const MyEventRegistrationsScreen()),
                );
              },
            ),
            IconButton(
              icon: const Icon(Icons.add_circle_outline),
              tooltip: 'Host a Camp',
              onPressed: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(builder: (_) => const CreateDonationEventScreen()),
                );
              },
            ),
          ],
          body: Column(
            children: [
              // Search & Filter Header
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 8.0),
                child: Column(
                  children: [
                    TextField(
                      controller: _cityController,
                      decoration: InputDecoration(
                        hintText: 'Search camps by city (e.g. Mumbai, Pune)',
                        prefixIcon: const Icon(Icons.search),
                        suffixIcon: _cityController.text.isNotEmpty
                            ? IconButton(
                                icon: const Icon(Icons.clear),
                                onPressed: () {
                                  _cityController.clear();
                                  _controller.setCity(null);
                                },
                              )
                            : null,
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                      ),
                      onSubmitted: _onCitySubmitted,
                    ),
                    const SizedBox(height: 8),
                    Row(
                      children: [
                        FilterChip(
                          label: const Text('Upcoming Only'),
                          selected: _controller.upcomingOnly,
                          onSelected: (val) => _controller.setUpcomingOnly(val),
                        ),
                        const SizedBox(width: 8),
                        ActionChip(
                          avatar: const Icon(Icons.refresh, size: 16),
                          label: const Text('Refresh'),
                          onPressed: () => _controller.loadEvents(),
                        ),
                      ],
                    ),
                  ],
                ),
              ),

              // Content Area
              Expanded(
                child: _buildContent(crossAxisCount),
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildContent(int crossAxisCount) {
    if (_controller.isLoading) {
      return const Center(child: NetraLoadingIndicator(message: 'Finding donation camps...'));
    }

    if (_controller.errorMessage != null) {
      return NetraErrorView(
        message: _controller.errorMessage!,
        onRetry: () => _controller.loadEvents(),
      );
    }

    if (_controller.events.isEmpty) {
      return NetraEmptyView(
        title: 'No Donation Camps Found',
        message: _controller.selectedCity != null
            ? 'No camps found in "${_controller.selectedCity}". Try searching for another city or viewing all upcoming camps.'
            : 'There are no active donation camps at this time. Please check back soon.',
        icon: Icons.event_busy,
        actionLabel: _controller.selectedCity != null ? 'View All Camps' : null,
        onAction: _controller.selectedCity != null
            ? () {
                _cityController.clear();
                _controller.setCity(null);
              }
            : null,
      );
    }

    if (crossAxisCount > 1) {
      return GridView.builder(
        padding: const EdgeInsets.all(12),
        gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
          crossAxisCount: crossAxisCount,
          childAspectRatio: 1.4,
          crossAxisSpacing: 12,
          mainAxisSpacing: 12,
        ),
        itemCount: _controller.events.length,
        itemBuilder: (context, index) {
          final event = _controller.events[index];
          return DonationEventCard(
            event: event,
            onTap: () => _navigateToDetails(event.id),
          );
        },
      );
    }

    return ListView.builder(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      itemCount: _controller.events.length,
      itemBuilder: (context, index) {
        final event = _controller.events[index];
        return DonationEventCard(
          event: event,
          onTap: () => _navigateToDetails(event.id),
        );
      },
    );
  }

  void _navigateToDetails(String eventId) {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) => DonationEventDetailsScreen(eventId: eventId),
      ),
    );
  }
}
