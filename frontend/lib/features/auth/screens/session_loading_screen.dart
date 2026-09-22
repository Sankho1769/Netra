import 'dart:async';
import 'package:flutter/material.dart';
import '../../../core/theme/netra_colors.dart';
import '../../../core/theme/netra_spacing.dart';
import '../../../core/theme/netra_typography.dart';
import '../state/auth_scope.dart';
import '../../home/home_screen.dart';
import 'login_screen.dart';

class SessionLoadingScreen extends StatefulWidget {
  const SessionLoadingScreen({super.key});

  @override
  State<SessionLoadingScreen> createState() => _SessionLoadingScreenState();
}

class _SessionLoadingScreenState extends State<SessionLoadingScreen> {
  Timer? _timeoutTimer;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _initSession());
  }

  @override
  void dispose() {
    _timeoutTimer?.cancel();
    super.dispose();
  }

  Future<void> _initSession() async {
    final authController = AuthScope.of(context);

    // Hard ceiling safety timeout (5 seconds) so app NEVER hangs infinitely
    _timeoutTimer = Timer(const Duration(seconds: 5), () {
      if (mounted) {
        _navigate(authController.isAuthenticated);
      }
    });

    try {
      await authController.initialize();
    } catch (_) {
      // Handled internally in controller
    } finally {
      _timeoutTimer?.cancel();
      if (mounted) {
        _navigate(authController.isAuthenticated);
      }
    }
  }

  void _navigate(bool isAuthenticated) {
    Navigator.of(context).pushReplacement(
      MaterialPageRoute(
        builder: (_) =>
            isAuthenticated ? const HomeScreen() : const LoginScreen(),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: NetraColors.surfaceWhite,
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                color: NetraColors.backgroundRed,
                shape: BoxShape.circle,
                boxShadow: [
                  BoxShadow(
                    color: NetraColors.primaryRed.withOpacity(0.15),
                    blurRadius: 24,
                    offset: const Offset(0, 8),
                  ),
                ],
              ),
              child: const Icon(
                Icons.water_drop_rounded,
                color: NetraColors.primaryRed,
                size: 48,
              ),
            ),
            NetraSpacing.gapH24,
            Text(
              "NETRA",
              style: NetraTypography.displaySmall.copyWith(
                color: NetraColors.primaryRed,
                fontWeight: FontWeight.bold,
                letterSpacing: 2.0,
              ),
            ),
            NetraSpacing.gapH8,
            Text(
              "Connecting Donors. Saving Lives.",
              style: NetraTypography.bodyMedium.copyWith(
                color: NetraColors.textSecondary,
              ),
            ),
            NetraSpacing.gapH32,
            const SizedBox(
              width: 24,
              height: 24,
              child: CircularProgressIndicator(
                strokeWidth: 2.5,
                valueColor:
                    AlwaysStoppedAnimation<Color>(NetraColors.primaryRed),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
