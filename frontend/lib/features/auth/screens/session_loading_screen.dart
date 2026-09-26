import 'dart:async';
import 'dart:math' as math;
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

class _SessionLoadingScreenState extends State<SessionLoadingScreen>
    with TickerProviderStateMixin {
  Timer? _timeoutTimer;
  late final AnimationController _ringController;
  late final AnimationController _pulseController;
  late final AnimationController _entranceController;

  late final Animation<double> _iconOpacity;
  late final Animation<Offset> _iconSlide;
  late final Animation<double> _titleOpacity;
  late final Animation<Offset> _titleSlide;
  late final Animation<double> _subtitleOpacity;
  late final Animation<Offset> _subtitleSlide;
  late final Animation<double> _indicatorOpacity;

  @override
  void initState() {
    super.initState();

    // 12s linear rotation for dashed outer ring (matching reference spinRight 12s)
    _ringController = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 12),
    )..repeat();

    // 1.2s ease-in-out pulse for heartbeat (matching reference pulseDrop 1.2s)
    _pulseController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1200),
    )..repeat();

    // Staggered entrance controller (matching reference GSAP timeline)
    _entranceController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1200),
    );

    _iconOpacity = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(
        parent: _entranceController,
        curve: const Interval(0.0, 0.5, curve: Curves.easeOut),
      ),
    );
    _iconSlide = Tween<Offset>(begin: const Offset(0, 0.2), end: Offset.zero).animate(
      CurvedAnimation(
        parent: _entranceController,
        curve: const Interval(0.0, 0.5, curve: Curves.easeOutCubic),
      ),
    );

    _titleOpacity = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(
        parent: _entranceController,
        curve: const Interval(0.3, 0.75, curve: Curves.easeOut),
      ),
    );
    _titleSlide = Tween<Offset>(begin: const Offset(0, 0.2), end: Offset.zero).animate(
      CurvedAnimation(
        parent: _entranceController,
        curve: const Interval(0.3, 0.75, curve: Curves.easeOutCubic),
      ),
    );

    _subtitleOpacity = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(
        parent: _entranceController,
        curve: const Interval(0.5, 0.95, curve: Curves.easeOut),
      ),
    );
    _subtitleSlide =
        Tween<Offset>(begin: const Offset(0, 0.2), end: Offset.zero).animate(
      CurvedAnimation(
        parent: _entranceController,
        curve: const Interval(0.5, 0.95, curve: Curves.easeOutCubic),
      ),
    );

    _indicatorOpacity = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(
        parent: _entranceController,
        curve: const Interval(0.7, 1.0, curve: Curves.easeIn),
      ),
    );

    _entranceController.forward();

    WidgetsBinding.instance.addPostFrameCallback((_) => _initSession());
  }

  @override
  void dispose() {
    _timeoutTimer?.cancel();
    _ringController.dispose();
    _pulseController.dispose();
    _entranceController.dispose();
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
      PageRouteBuilder(
        transitionDuration: const Duration(milliseconds: 600),
        pageBuilder: (_, animation, __) {
          return FadeTransition(
            opacity: animation,
            child: isAuthenticated ? const HomeScreen() : const LoginScreen(),
          );
        },
      ),
    );
  }

  double _calculateHeartbeatScale(double val) {
    // Mimics heartbeat: 0->1.0, 0.15->1.14, 0.3->1.0, 0.45->1.14, 0.6->1.0, 1.0->1.0
    if (val < 0.15) {
      return 1.0 + (val / 0.15) * 0.14;
    } else if (val < 0.30) {
      return 1.14 - ((val - 0.15) / 0.15) * 0.14;
    } else if (val < 0.45) {
      return 1.0 + ((val - 0.30) / 0.15) * 0.14;
    } else if (val < 0.60) {
      return 1.14 - ((val - 0.45) / 0.15) * 0.14;
    }
    return 1.0;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: NetraColors.surfaceWhite,
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            // Rotating Ring + Pulsing Drop
            SlideTransition(
              position: _iconSlide,
              child: FadeTransition(
                opacity: _iconOpacity,
                child: SizedBox(
                  width: 170,
                  height: 170,
                  child: Stack(
                    alignment: Alignment.center,
                    children: [
                      // Rotating Dashed Ring
                      AnimatedBuilder(
                        animation: _ringController,
                        builder: (_, __) {
                          return Transform.rotate(
                            angle: _ringController.value * 2 * math.pi,
                            child: CustomPaint(
                              size: const Size(160, 160),
                              painter: _DashedCirclePainter(
                                color: NetraColors.primaryRed
                                    .withValues(alpha: 0.35),
                                strokeWidth: 2.2,
                                dashCount: 24,
                              ),
                            ),
                          );
                        },
                      ),
                      // Pulsing Center Official NETRA Logo
                      AnimatedBuilder(
                        animation: _pulseController,
                        builder: (_, child) {
                          final scale =
                              _calculateHeartbeatScale(_pulseController.value);
                          return Transform.scale(
                            scale: scale,
                            child: child,
                          );
                        },
                        child: Container(
                          width: 104,
                          height: 104,
                          decoration: BoxDecoration(
                            borderRadius: BorderRadius.circular(24),
                            boxShadow: [
                              BoxShadow(
                                color: NetraColors.primaryRed
                                    .withValues(alpha: 0.18),
                                blurRadius: 20,
                                offset: const Offset(0, 6),
                              ),
                            ],
                          ),
                          child: ClipRRect(
                            borderRadius: BorderRadius.circular(24),
                            child: Image.asset(
                              'assets/branding/netra_logo.png',
                              fit: BoxFit.contain,
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
            NetraSpacing.gapH24,

            // Brand Title
            SlideTransition(
              position: _titleSlide,
              child: FadeTransition(
                opacity: _titleOpacity,
                child: Text(
                  "NETRA",
                  style: NetraTypography.headlineLarge.copyWith(
                    fontWeight: FontWeight.w900,
                    letterSpacing: 2.0,
                    color: NetraColors.primaryRed,
                  ),
                ),
              ),
            ),
            NetraSpacing.gapH8,

            // Subtitle
            SlideTransition(
              position: _subtitleSlide,
              child: FadeTransition(
                opacity: _subtitleOpacity,
                child: Text(
                  "Connecting Donors. Saving Lives.",
                  style: NetraTypography.bodyMedium.copyWith(
                    color: NetraColors.textSecondary,
                    letterSpacing: 0.4,
                  ),
                ),
              ),
            ),
            NetraSpacing.gapH32,

            // Loading status indicator
            FadeTransition(
              opacity: _indicatorOpacity,
              child: const SizedBox(
                width: 24,
                height: 24,
                child: CircularProgressIndicator(
                  strokeWidth: 2.2,
                  valueColor:
                      AlwaysStoppedAnimation<Color>(NetraColors.primaryRed),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _DashedCirclePainter extends CustomPainter {
  final Color color;
  final double strokeWidth;
  final int dashCount;

  _DashedCirclePainter({
    required this.color,
    required this.strokeWidth,
    required this.dashCount,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = color
      ..strokeWidth = strokeWidth
      ..style = PaintingStyle.stroke;

    final center = Offset(size.width / 2, size.height / 2);
    final radius = size.width / 2 - strokeWidth;

    final sweepAngle = (2 * math.pi) / dashCount;
    final dashSweep = sweepAngle * 0.55;

    for (int i = 0; i < dashCount; i++) {
      final startAngle = i * sweepAngle;
      canvas.drawArc(
        Rect.fromCircle(center: center, radius: radius),
        startAngle,
        dashSweep,
        false,
        paint,
      );
    }
  }

  @override
  bool shouldRepaint(covariant _DashedCirclePainter oldDelegate) {
    return oldDelegate.color != color ||
        oldDelegate.strokeWidth != strokeWidth ||
        oldDelegate.dashCount != dashCount;
  }
}
