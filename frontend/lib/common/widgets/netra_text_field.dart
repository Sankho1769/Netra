import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../../core/theme/netra_colors.dart';
import '../../core/theme/netra_spacing.dart';
import '../../core/theme/netra_typography.dart';

class NetraTextField extends StatelessWidget {
  final String label;
  final String? hint;
  final String? helperText;
  final String? errorText;
  final TextEditingController? controller;
  final TextInputType keyboardType;
  final List<TextInputFormatter>? inputFormatters;
  final ValueChanged<String>? onChanged;
  final VoidCallback? onTap;
  final bool readOnly;
  final Widget? prefixIcon;
  final Widget? suffixIcon;
  final bool autofocus;
  final int maxLines;
  final FormFieldValidator<String>? validator;

  const NetraTextField({
    super.key,
    required this.label,
    this.hint,
    this.helperText,
    this.errorText,
    this.controller,
    this.keyboardType = TextInputType.text,
    this.inputFormatters,
    this.onChanged,
    this.onTap,
    this.readOnly = false,
    this.prefixIcon,
    this.suffixIcon,
    this.autofocus = false,
    this.maxLines = 1,
    this.validator,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(
          label,
          style: NetraTypography.titleSmall.copyWith(
            color: errorText != null
                ? NetraColors.errorRed
                : NetraColors.textPrimary,
          ),
        ),
        NetraSpacing.gapH8,
        TextFormField(
          controller: controller,
          keyboardType: keyboardType,
          inputFormatters: inputFormatters,
          onChanged: onChanged,
          onTap: onTap,
          readOnly: readOnly,
          autofocus: autofocus,
          maxLines: maxLines,
          validator: validator,
          style: NetraTypography.bodyLarge,
          decoration: InputDecoration(
            hintText: hint,
            helperText: helperText,
            errorText: errorText,
            prefixIcon: prefixIcon,
            suffixIcon: suffixIcon,
            filled: true,
            fillColor: readOnly
                ? NetraColors.backgroundGray
                : NetraColors.surfaceWhite,
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
                color: errorText != null
                    ? NetraColors.errorRed
                    : NetraColors.borderGray,
              ),
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
              borderSide: BorderSide(
                color: errorText != null
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
