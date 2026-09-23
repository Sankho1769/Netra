import 'package:flutter/material.dart';
import '../models/fulfillment_model.dart';
import '../services/fulfillment_api_service.dart';
import '../widgets/fulfillment_card.dart';
import 'fulfillment_detail_screen.dart';

class FulfillmentListScreen extends StatefulWidget {
  final FulfillmentApiService? apiService;

  const FulfillmentListScreen({
    super.key,
    this.apiService,
  });

  @override
  State<FulfillmentListScreen> createState() => _FulfillmentListScreenState();
}

class _FulfillmentListScreenState extends State<FulfillmentListScreen>
    with SingleTickerProviderStateMixin {
  late final FulfillmentApiService _apiService;
  late final TabController _tabController;

  List<FulfillmentModel> _myFulfillments = [];
  List<FulfillmentModel> _pendingFulfillments = [];
  bool _isLoading = false;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _apiService = widget.apiService ?? FulfillmentApiService();
    _tabController = TabController(length: 2, vsync: this);
    _loadData();
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  Future<void> _loadData() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final myResults = await _apiService.getMyFulfillments();
      List<FulfillmentModel> pendingResults = [];
      try {
        pendingResults = await _apiService.getPendingFulfillments();
      } catch (_) {
        // Staff queue might return 403 for regular donors/receivers
      }

      if (mounted) {
        setState(() {
          _myFulfillments = myResults;
          _pendingFulfillments = pendingResults;
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

  void _navigateToDetail(FulfillmentModel fulfillment) {
    Navigator.of(context)
        .push(
          MaterialPageRoute(
            builder: (_) => FulfillmentDetailScreen(
              fulfillmentId: fulfillment.id,
              apiService: _apiService,
            ),
          ),
        )
        .then((_) => _loadData());
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Fulfillments'),
        bottom: TabBar(
          controller: _tabController,
          tabs: const [
            Tab(text: 'My Fulfillments'),
            Tab(text: 'Pending Queue'),
          ],
        ),
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _errorMessage != null
              ? Center(
                  child: Padding(
                    padding: const EdgeInsets.all(24.0),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        const Icon(Icons.error_outline,
                            size: 48, color: Colors.red),
                        const SizedBox(height: 12),
                        Text(
                          _errorMessage!,
                          textAlign: TextAlign.center,
                          style: const TextStyle(fontSize: 16),
                        ),
                        const SizedBox(height: 16),
                        ElevatedButton(
                          onPressed: _loadData,
                          child: const Text('Retry'),
                        ),
                      ],
                    ),
                  ),
                )
              : TabBarView(
                  controller: _tabController,
                  children: [
                    _buildList(_myFulfillments, 'No fulfillments found.'),
                    _buildList(_pendingFulfillments,
                        'No pending fulfillments in queue.'),
                  ],
                ),
    );
  }

  Widget _buildList(List<FulfillmentModel> items, String emptyMessage) {
    if (items.isEmpty) {
      return RefreshIndicator(
        onRefresh: _loadData,
        child: ListView(
          children: [
            SizedBox(
              height: 300,
              child: Center(
                child: Text(
                  emptyMessage,
                  style: TextStyle(
                    color: Theme.of(context).colorScheme.outline,
                    fontSize: 16,
                  ),
                ),
              ),
            ),
          ],
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: _loadData,
      child: ListView.builder(
        padding: const EdgeInsets.symmetric(vertical: 8),
        itemCount: items.length,
        itemBuilder: (context, index) {
          final item = items[index];
          return FulfillmentCard(
            fulfillment: item,
            onTap: () => _navigateToDetail(item),
          );
        },
      ),
    );
  }
}
