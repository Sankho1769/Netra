import 'package:flutter/material.dart';
import '../models/blood_request.dart';
import '../state/blood_request_controller.dart';
import '../widgets/blood_request_card.dart';
import 'blood_request_details_screen.dart';

class MyBloodRequestsScreen extends StatefulWidget {
  final BloodRequestController? controller;

  const MyBloodRequestsScreen({super.key, this.controller});

  @override
  State<MyBloodRequestsScreen> createState() => _MyBloodRequestsScreenState();
}

class _MyBloodRequestsScreenState extends State<MyBloodRequestsScreen> {
  late final BloodRequestController _controller;

  @override
  void initState() {
    super.initState();
    _controller = widget.controller ?? BloodRequestController();
    _controller.addListener(_onControllerUpdate);
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _controller.loadMyRequests();
    });
  }

  @override
  void dispose() {
    _controller.removeListener(_onControllerUpdate);
    super.dispose();
  }

  void _onControllerUpdate() {
    if (mounted) setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('My Blood Requests', style: TextStyle(fontWeight: FontWeight.w700)),
      ),
      body: _controller.isLoading && _controller.myRequests.isEmpty
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: () => _controller.loadMyRequests(refresh: true),
              child: _controller.myRequests.isEmpty
                  ? ListView(
                      physics: const AlwaysScrollableScrollPhysics(),
                      children: [
                        const SizedBox(height: 80),
                        Icon(Icons.inbox_outlined, size: 64, color: Colors.grey.shade400),
                        const SizedBox(height: 16),
                        const Center(
                          child: Text(
                            'You have not submitted any blood requests yet.',
                            style: TextStyle(fontSize: 14, color: Colors.grey),
                          ),
                        ),
                      ],
                    )
                  : ListView.builder(
                      physics: const AlwaysScrollableScrollPhysics(),
                      itemCount: _controller.myRequests.length,
                      itemBuilder: (context, index) {
                        final req = _controller.myRequests[index];
                        return BloodRequestCard(
                          request: req,
                          onTap: () async {
                            await Navigator.push(
                              context,
                              MaterialPageRoute(
                                builder: (_) => BloodRequestDetailsScreen(
                                  requestId: req.id,
                                  controller: _controller,
                                ),
                              ),
                            );
                            _controller.loadMyRequests(refresh: true);
                          },
                        );
                      },
                    ),
            ),
    );
  }
}
