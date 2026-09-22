import 'package:flutter/material.dart';
import '../../core/theme/netra_colors.dart';
import '../../core/theme/netra_typography.dart';

class NetraAppBar extends StatelessWidget implements PreferredSizeWidget {
  final String title;
  final Widget? leading;
  final List<Widget>? actions;
  final bool centerTitle;
  final PreferredSizeWidget? bottom;
  final VoidCallback? onBackPressed;
  final bool showBackButton;

  const NetraAppBar({
    super.key,
    required this.title,
    this.leading,
    this.actions,
    this.centerTitle = true,
    this.bottom,
    this.onBackPressed,
    this.showBackButton = true,
  });

  @override
  Widget build(BuildContext context) {
    Widget? effectiveLeading = leading;
    if (effectiveLeading == null &&
        showBackButton &&
        Navigator.of(context).canPop()) {
      effectiveLeading = IconButton(
        icon: const Icon(Icons.arrow_back_ios_new_rounded, size: 20),
        color: NetraColors.textPrimary,
        tooltip: 'Back',
        onPressed: onBackPressed ?? () => Navigator.of(context).maybePop(),
      );
    }

    return AppBar(
      title: Text(title, style: NetraTypography.headlineSmall),
      centerTitle: centerTitle,
      backgroundColor: NetraColors.surfaceWhite,
      surfaceTintColor: Colors.transparent,
      elevation: 0,
      scrolledUnderElevation: 1.0,
      shadowColor: NetraColors.borderGray,
      leading: effectiveLeading,
      actions: actions,
      bottom: bottom,
    );
  }

  @override
  Size get preferredSize => Size.fromHeight(
        kToolbarHeight + (bottom?.preferredSize.height ?? 0.0),
      );
}
