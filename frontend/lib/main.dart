import 'package:flutter/material.dart';
import 'core/theme/netra_theme.dart';
import 'features/auth/state/auth_controller.dart';
import 'features/auth/state/auth_scope.dart';
import 'features/auth/screens/session_loading_screen.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const NetraApp());
}

class NetraApp extends StatefulWidget {
  const NetraApp({super.key});

  @override
  State<NetraApp> createState() => _NetraAppState();
}

class _NetraAppState extends State<NetraApp> {
  final AuthController _authController = AuthController();

  @override
  void dispose() {
    _authController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AuthScope(
      controller: _authController,
      child: MaterialApp(
        title: 'NETRA - Blood Donation Platform',
        debugShowCheckedModeBanner: false,
        theme: NetraTheme.lightTheme,
        home: const SessionLoadingScreen(),
      ),
    );
  }
}
