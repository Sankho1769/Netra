import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../models/donor_match_response_model.dart';
import '../services/donor_response_api_service.dart';
import '../widgets/match_status_badge.dart';

/// Screen presenting safe, privacy-preserving details of a matched donor to the requester,
/// with actions to accept/decline offers and view contact details upon mutual acceptance.
class RequesterMatchDetailScreen extends StatefulWidget {
  final RequesterDonorMatch match;
  final String? requestId;
  final DonorResponseApiService? apiService;

  const RequesterMatchDetailScreen({
    super.key,
    required this.match,
    this.requestId,
    this.apiService,
  });

  @override
  State<RequesterMatchDetailScreen> createState() =>
      _RequesterMatchDetailScreenState();
}

class _RequesterMatchDetailScreenState
    extends State<RequesterMatchDetailScreen> {
  late final DonorResponseApiService _apiService;
  late DonorMatchStatus _currentStatus;
  bool _isProcessing = false;

  @override
  void initState() {
    super.initState();
    _apiService = widget.apiService ?? DonorResponseApiService();
    _currentStatus = widget.match.responseStatus;
  }

  Future<void> _acceptMatch() async {
    final reqId = widget.requestId;
    if (reqId == null) return;

    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Accept Helper Offer?'),
        content: const Text(
          'Accepting this helper will confirm the match and share direct contact numbers so you can coordinate donation at the hospital.\n\n'
          'Do you wish to proceed?',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('Cancel'),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: const Color(0xFF16A34A),
              foregroundColor: Colors.white,
            ),
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('Confirm Accept'),
          ),
        ],
      ),
    );

    if (confirmed != true) return;

    setState(() => _isProcessing = true);
    try {
      await _apiService.acceptHelper(reqId, widget.match.matchId);
      if (mounted) {
        setState(() {
          _currentStatus = DonorMatchStatus.accepted;
          _isProcessing = false;
        });
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content:
                Text('Helper accepted! Contact details are now available.'),
            backgroundColor: Color(0xFF16A34A),
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isProcessing = false);
        final msg = e
            .toString()
            .replaceFirst('ValidationException: ', '')
            .replaceFirst('Exception: ', '');
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
              content: Text('Failed to accept: $msg'),
              backgroundColor: Colors.red.shade700),
        );
      }
    }
  }

  Future<void> _declineMatch() async {
    final reqId = widget.requestId;
    if (reqId == null) return;

    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Decline Helper Offer?'),
        content: const Text(
          'Are you sure you want to decline this offer? The helper will be notified and this request will remain open for other donors.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('Keep Offer'),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: const Color(0xFFDC2626),
              foregroundColor: Colors.white,
            ),
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('Decline Offer'),
          ),
        ],
      ),
    );

    if (confirmed != true) return;

    setState(() => _isProcessing = true);
    try {
      await _apiService.declineHelper(reqId, widget.match.matchId);
      if (mounted) {
        setState(() {
          _currentStatus = DonorMatchStatus.declined;
          _isProcessing = false;
        });
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Offer declined.')),
        );
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isProcessing = false);
        final msg = e
            .toString()
            .replaceFirst('ValidationException: ', '')
            .replaceFirst('Exception: ', '');
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
              content: Text('Failed to decline: $msg'),
              backgroundColor: Colors.red.shade700),
        );
      }
    }
  }

  Future<void> _viewContact() async {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (_) => const Center(child: CircularProgressIndicator()),
    );

    try {
      final contact =
          await _apiService.getMatchContact(widget.match.matchId);
      if (mounted) {
        Navigator.pop(context);
        _showContactBottomSheet(contact);
      }
    } catch (e) {
      if (mounted) {
        Navigator.pop(context);
        final msg = e
            .toString()
            .replaceFirst('ValidationException: ', '')
            .replaceFirst('Exception: ', '');
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
              content: Text('Failed to load contact: $msg'),
              backgroundColor: Colors.red.shade700),
        );
      }
    }
  }

  void _showContactBottomSheet(MatchContactInfo contact) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (ctx) => Padding(
        padding: EdgeInsets.only(
          left: 20,
          right: 20,
          top: 20,
          bottom: MediaQuery.of(ctx).viewInsets.bottom + 24,
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              children: [
                const Icon(Icons.contact_phone,
                    color: Color(0xFF16A34A), size: 28),
                const SizedBox(width: 12),
                const Expanded(
                  child: Text(
                    'Donor Contact & Coordination',
                    style:
                        TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                  ),
                ),
                IconButton(
                  icon: const Icon(Icons.close),
                  onPressed: () => Navigator.pop(ctx),
                ),
              ],
            ),
            const Divider(height: 24),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: const Color(0xFFF0FDF4),
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: const Color(0xFF86EFAC)),
              ),
              child: Text(
                contact.instructions,
                style: const TextStyle(fontSize: 13, color: Color(0xFF166534)),
              ),
            ),
            const SizedBox(height: 16),
            ListTile(
              contentPadding: EdgeInsets.zero,
              leading: CircleAvatar(
                backgroundColor: const Color(0xFFDCFCE7),
                child: Text(
                  contact.donorName.isNotEmpty
                      ? contact.donorName[0].toUpperCase()
                      : 'D',
                  style: const TextStyle(
                      color: Color(0xFF15803D), fontWeight: FontWeight.bold),
                ),
              ),
              title: Text(
                contact.donorName,
                style:
                    const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
              ),
              subtitle: Text(
                'Accepted Blood Donor (${contact.bloodGroup})',
                style: TextStyle(color: Colors.grey.shade600, fontSize: 13),
              ),
            ),
            const SizedBox(height: 12),
            if (contact.donorPhone.isNotEmpty)
              Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                decoration: BoxDecoration(
                  color: Colors.grey.shade100,
                  borderRadius: BorderRadius.circular(10),
                  border: Border.all(color: Colors.grey.shade300),
                ),
                child: Row(
                  children: [
                    const Icon(Icons.phone, color: Color(0xFF16A34A)),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text('Donor Phone Number',
                              style: TextStyle(
                                  fontSize: 11, color: Colors.grey.shade600)),
                          Text(
                            contact.donorPhone,
                            style: const TextStyle(
                                fontSize: 16,
                                fontWeight: FontWeight.bold,
                                letterSpacing: 0.5),
                          ),
                        ],
                      ),
                    ),
                    IconButton(
                      icon: const Icon(Icons.copy,
                          size: 20, color: Color(0xFF2563EB)),
                      tooltip: 'Copy Number',
                      onPressed: () {
                        Clipboard.setData(
                            ClipboardData(text: contact.donorPhone));
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(
                              content:
                                  Text('Donor phone number copied to clipboard.')),
                        );
                      },
                    ),
                  ],
                ),
              ),
            const SizedBox(height: 12),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: Colors.grey.shade50,
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: Colors.grey.shade200),
              ),
              child: Row(
                children: [
                  const Icon(Icons.local_hospital,
                      color: Color(0xFFDC2626), size: 20),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(contact.hospitalName,
                            style: const TextStyle(
                                fontWeight: FontWeight.bold, fontSize: 13)),
                        if (contact.hospitalAddress.isNotEmpty)
                          Text(contact.hospitalAddress,
                              style: TextStyle(
                                  fontSize: 12, color: Colors.grey.shade600)),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFF16A34A),
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(vertical: 12),
                  shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(8)),
                ),
                onPressed: () => Navigator.pop(ctx),
                child: const Text('Close'),
              ),
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Donor Match Details'),
        backgroundColor: const Color(0xFFDC2626),
        foregroundColor: Colors.white,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            _buildPrivacySafeguardBanner(),
            const SizedBox(height: 12),
            _buildClinicalDisclaimerBanner(),
            const SizedBox(height: 16),
            _buildDonorOverviewCard(),
            const SizedBox(height: 16),
            _buildMatchTimelineCard(),
            const SizedBox(height: 16),
            _buildActionSection(),
          ],
        ),
      ),
    );
  }

  Widget _buildActionSection() {
    if (_currentStatus == DonorMatchStatus.matched &&
        widget.requestId != null) {
      return Card(
        elevation: 2,
        shape:
            RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Text(
                'Requester Action Required',
                style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 8),
              const Text(
                'This helper is ready to donate. Accept to confirm the match and unlock coordination contact details.',
                style: TextStyle(fontSize: 13, color: Colors.black87),
              ),
              const SizedBox(height: 14),
              Row(
                children: [
                  Expanded(
                    child: ElevatedButton.icon(
                      style: ElevatedButton.styleFrom(
                        backgroundColor: const Color(0xFF16A34A),
                        foregroundColor: Colors.white,
                        padding: const EdgeInsets.symmetric(vertical: 12),
                        shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(8)),
                      ),
                      icon: _isProcessing
                          ? const SizedBox(
                              width: 16,
                              height: 16,
                              child: CircularProgressIndicator(
                                  strokeWidth: 2, color: Colors.white),
                            )
                          : const Icon(Icons.check, size: 18),
                      label: const Text('Accept Helper',
                          style: TextStyle(fontWeight: FontWeight.bold)),
                      onPressed: _isProcessing ? null : _acceptMatch,
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: OutlinedButton.icon(
                      style: OutlinedButton.styleFrom(
                        foregroundColor: const Color(0xFFDC2626),
                        side: const BorderSide(color: Color(0xFFDC2626)),
                        padding: const EdgeInsets.symmetric(vertical: 12),
                        shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(8)),
                      ),
                      icon: const Icon(Icons.close, size: 18),
                      label: const Text('Decline',
                          style: TextStyle(fontWeight: FontWeight.bold)),
                      onPressed: _isProcessing ? null : _declineMatch,
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      );
    } else if (_currentStatus == DonorMatchStatus.accepted) {
      return Card(
        elevation: 2,
        shape:
            RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        color: const Color(0xFFF0FDF4),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Row(
                children: [
                  Icon(Icons.check_circle,
                      color: Color(0xFF16A34A), size: 20),
                  SizedBox(width: 8),
                  Text(
                    'Helper Accepted',
                    style: TextStyle(
                      fontSize: 15,
                      fontWeight: FontWeight.bold,
                      color: Color(0xFF15803D),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              const Text(
                'You have accepted this donor. You can view their contact information and coordinate arrival at the hospital blood bank.',
                style: TextStyle(fontSize: 13, color: Color(0xFF166534)),
              ),
              const SizedBox(height: 12),
              ElevatedButton.icon(
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFF16A34A),
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(vertical: 12),
                  shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(8)),
                ),
                icon: const Icon(Icons.contact_phone, size: 18),
                label: const Text('View Contact & Coordinate',
                    style: TextStyle(fontWeight: FontWeight.bold)),
                onPressed: _viewContact,
              ),
            ],
          ),
        ),
      );
    }
    return const SizedBox.shrink();
  }

  Widget _buildClinicalDisclaimerBanner() {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: const Color(0xFFFFFBEB),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: const Color(0xFFFDE68A), width: 1),
      ),
      child: const Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.info_outline, color: Color(0xFFB45309), size: 20),
          SizedBox(width: 10),
          Expanded(
            child: Text(
              'Clinical Advisory: Matching status and preliminary compatibility do not guarantee a donor\'s medical clearance or current donation suitability. Official screening, crossmatching, and eligibility verification must be conducted by certified blood bank or hospital staff.',
              style: TextStyle(
                fontSize: 12.5,
                color: Color(0xFF92400E),
                height: 1.35,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildPrivacySafeguardBanner() {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: const Color(0xFFEFF6FF),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: const Color(0xFF93C5FD), width: 1),
      ),
      child: const Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.shield_outlined, color: Color(0xFF1D4ED8), size: 20),
          SizedBox(width: 10),
          Expanded(
            child: Text(
              'NETRA Privacy Guarantee: Donor phone numbers and contact details are only released upon mutual acceptance and for clinical coordination.',
              style: TextStyle(
                fontSize: 12.5,
                color: Color(0xFF1E3A8A),
                height: 1.35,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildDonorOverviewCard() {
    return Card(
      elevation: 2,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  widget.match.donorDisplayName,
                  style: const TextStyle(
                      fontSize: 18, fontWeight: FontWeight.bold),
                ),
                MatchStatusBadge(status: _currentStatus),
              ],
            ),
            const Divider(height: 24),
            _buildDetailRow(
              Icons.bloodtype_outlined,
              'Blood Group',
              widget.match.bloodGroup,
              trailingWidget: widget.match.isVerified
                  ? Container(
                      padding: const EdgeInsets.symmetric(
                          horizontal: 8, vertical: 2),
                      decoration: BoxDecoration(
                        color: const Color(0xFFDCFCE7),
                        borderRadius: BorderRadius.circular(4),
                      ),
                      child: const Text(
                        'Verified at matching',
                        style: TextStyle(
                          fontSize: 11,
                          color: Color(0xFF15803D),
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    )
                  : null,
            ),
            _buildDetailRow(
              Icons.check_circle_outline,
              'Availability',
              widget.match.availabilityStatus,
            ),
            if (widget.match.distanceKm != null)
              _buildDetailRow(
                Icons.route_outlined,
                'Approx Distance',
                '${widget.match.distanceKm!.toStringAsFixed(1)} km away',
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildMatchTimelineCard() {
    return Card(
      elevation: 2,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('Match Timeline',
                style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold)),
            const SizedBox(height: 12),
            _buildDetailRow(
              Icons.schedule_outlined,
              'Matched At',
              widget.match.createdAt.toLocal().toString().substring(0, 16),
            ),
            _buildDetailRow(
              Icons.timer_off_outlined,
              'Response Expiration',
              widget.match.expiresAt.toLocal().toString().substring(0, 16),
            ),
            if (widget.match.respondedAt != null)
              _buildDetailRow(
                Icons.done_all_outlined,
                'Responded At',
                widget.match.respondedAt!
                    .toLocal()
                    .toString()
                    .substring(0, 16),
              ),
            _buildDetailRow(
              Icons.history_outlined,
              'Last Updated',
              widget.match.updatedAt.toLocal().toString().substring(0, 16),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildDetailRow(IconData icon, String label, String value,
      {Widget? trailingWidget}) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, size: 18, color: Colors.grey.shade600),
          const SizedBox(width: 10),
          SizedBox(
            width: 130,
            child: Text(label,
                style: TextStyle(color: Colors.grey.shade600, fontSize: 13)),
          ),
          Expanded(
            child: Row(
              children: [
                Text(
                  value,
                  style: const TextStyle(
                      fontWeight: FontWeight.w600, fontSize: 13),
                ),
                if (trailingWidget != null) ...[
                  const SizedBox(width: 8),
                  trailingWidget,
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }
}
