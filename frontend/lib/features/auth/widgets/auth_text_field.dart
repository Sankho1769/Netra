import 'package:flutter/material.dart';
import '../../../core/theme/netra_colors.dart';
import '../../../core/theme/netra_spacing.dart';
import '../../../core/theme/netra_typography.dart';

class AuthTextField extends StatefulWidget {
  final String label;
  final String? hint;
  final String? helperText;
  final String? errorText;
  final TextEditingController? controller;
  final FormFieldValidator<String>? validator;
  final TextInputType keyboardType;
  final TextInputAction? textInputAction;
  final ValueChanged<String>? onFieldSubmitted;
  final ValueChanged<String>? onChanged;
  final bool isPassword;
  final Widget? prefixIcon;
  final Widget? suffixIcon;
  final bool autofocus;
  final bool enabled;
  final bool? autocorrect;
  final bool? enableSuggestions;
  final TextCapitalization? textCapitalization;

  const AuthTextField({
    super.key,
    required this.label,
    this.hint,
    this.helperText,
    this.errorText,
    this.controller,
    this.validator,
    this.keyboardType = TextInputType.text,
    this.textInputAction,
    this.onFieldSubmitted,
    this.onChanged,
    this.isPassword = false,
    this.prefixIcon,
    this.suffixIcon,
    this.autofocus = false,
    this.enabled = true,
    this.autocorrect,
    this.enableSuggestions,
    this.textCapitalization,
  });

  @override
  State<AuthTextField> createState() => _AuthTextFieldState();
}

class _AuthTextFieldState extends State<AuthTextField> {
  late bool _obscureText;

  @override
  void initState() {
    super.initState();
    _obscureText = widget.isPassword;
  }

  @override
  Widget build(BuildContext context) {
    Widget? effectiveSuffixIcon = widget.suffixIcon;

    if (widget.isPassword) {
      effectiveSuffixIcon = IconButton(
        icon: Icon(
          _obscureText
              ? Icons.visibility_outlined
              : Icons.visibility_off_outlined,
          color: NetraColors.textSecondary,
          size: 20,
        ),
        tooltip: _obscureText ? 'Show password' : 'Hide password',
        onPressed: () {
          setState(() {
            _obscureText = !_obscureText;
          });
        },
      );
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(
          widget.label,
          style: NetraTypography.titleSmall.copyWith(
            color: widget.errorText != null
                ? NetraColors.errorRed
                : NetraColors.textPrimary,
          ),
        ),
        NetraSpacing.gapH8,
        TextFormField(
          controller: widget.controller,
          validator: widget.validator,
          keyboardType: widget.keyboardType,
          textInputAction: widget.textInputAction,
          onFieldSubmitted: widget.onFieldSubmitted,
          onChanged: widget.onChanged,
          obscureText: _obscureText,
          autocorrect: widget.autocorrect ??
              !(widget.isPassword ||
                  widget.keyboardType == TextInputType.emailAddress ||
                  widget.keyboardType == TextInputType.phone),
          enableSuggestions: widget.enableSuggestions ??
              !(widget.isPassword ||
                  widget.keyboardType == TextInputType.emailAddress ||
                  widget.keyboardType == TextInputType.phone),
          textCapitalization: widget.textCapitalization ??
              (widget.isPassword ||
                      widget.keyboardType == TextInputType.emailAddress ||
                      widget.keyboardType == TextInputType.phone
                  ? TextCapitalization.none
                  : TextCapitalization.words),
          autofocus: widget.autofocus,
          enabled: widget.enabled,
          style: NetraTypography.bodyLarge,
          decoration: InputDecoration(
            hintText: widget.hint,
            helperText: widget.helperText,
            errorText: widget.errorText,
            prefixIcon: widget.prefixIcon,
            suffixIcon: effectiveSuffixIcon,
            filled: true,
            fillColor: widget.enabled
                ? NetraColors.surfaceWhite
                : NetraColors.backgroundGray,
            contentPadding: const EdgeInsets.symmetric(
              horizontal: NetraSpacing.lg,
              vertical: NetraSpacing.md,
            ),
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
              borderSide: const BorderSide(color: NetraColors.borderGray),
            ),
            enabledBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
              borderSide: BorderSide(
                color: widget.errorText != null
                    ? NetraColors.errorRed
                    : NetraColors.borderGray,
              ),
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
              borderSide: BorderSide(
                color: widget.errorText != null
                    ? NetraColors.errorRed
                    : NetraColors.primaryRed,
                width: 2.0,
              ),
            ),
            errorBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
              borderSide:
                  const BorderSide(color: NetraColors.errorRed, width: 1.5),
            ),
            focusedErrorBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
              borderSide:
                  const BorderSide(color: NetraColors.errorRed, width: 2.0),
            ),
          ),
        ),
      ],
    );
  }
}
