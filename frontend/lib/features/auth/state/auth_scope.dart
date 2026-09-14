import 'package:flutter/material.dart';
import 'auth_controller.dart';

/// Provides [AuthController] down the widget tree using Flutter's native InheritedNotifier.
/// Automatically rebuilds widgets when AuthController state changes.
class AuthScope extends InheritedNotifier<AuthController> {
  const AuthScope({
    super.key,
    required AuthController controller,
    required super.child,
  }) : super(notifier: controller);

  static AuthController of(BuildContext context) {
    final scope = context.dependOnInheritedWidgetOfExactType<AuthScope>();
    assert(scope != null, 'No AuthScope found in context. Ensure AuthScope wraps your widget tree.');
    return scope!.notifier!;
  }

  static AuthController? maybeOf(BuildContext context) {
    return context.dependOnInheritedWidgetOfExactType<AuthScope>()?.notifier;
  }
}
