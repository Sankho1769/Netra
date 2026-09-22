import 'package:flutter/material.dart';
import 'responsive_breakpoints.dart';

enum ResponsiveContainerWidth {
  narrow, // 560px
  reading, // 680px (ideal for eligibility questionnaire, single column forms)
  standard, // 880px (ideal for review screens, two column forms)
  wide, // 1140px (ideal for dashboards, lists, grids)
  canvas, // 1400px (ultrawide layout bounds)
}

class ResponsiveContainer extends StatelessWidget {
  final Widget child;
  final ResponsiveContainerWidth widthPreset;
  final double? customMaxWidth;
  final EdgeInsetsGeometry? padding;
  final bool scrollable;
  final ScrollPhysics? physics;
  final ScrollController? controller;

  const ResponsiveContainer({
    super.key,
    required this.child,
    this.widthPreset = ResponsiveContainerWidth.reading,
    this.customMaxWidth,
    this.padding,
    this.scrollable = false,
    this.physics,
    this.controller,
  });

  const ResponsiveContainer.reading({
    super.key,
    required this.child,
    this.padding,
    this.scrollable = false,
    this.physics,
    this.controller,
  })  : widthPreset = ResponsiveContainerWidth.reading,
        customMaxWidth = null;

  const ResponsiveContainer.standard({
    super.key,
    required this.child,
    this.padding,
    this.scrollable = false,
    this.physics,
    this.controller,
  })  : widthPreset = ResponsiveContainerWidth.standard,
        customMaxWidth = null;

  const ResponsiveContainer.wide({
    super.key,
    required this.child,
    this.padding,
    this.scrollable = false,
    this.physics,
    this.controller,
  })  : widthPreset = ResponsiveContainerWidth.wide,
        customMaxWidth = null;

  double _resolveMaxWidth() {
    if (customMaxWidth != null) return customMaxWidth!;
    switch (widthPreset) {
      case ResponsiveContainerWidth.narrow:
        return ResponsiveBreakpoints.maxContentWidthNarrow;
      case ResponsiveContainerWidth.reading:
        return ResponsiveBreakpoints.maxContentWidthReading;
      case ResponsiveContainerWidth.standard:
        return ResponsiveBreakpoints.maxContentWidthStandard;
      case ResponsiveContainerWidth.wide:
        return ResponsiveBreakpoints.maxContentWidthWide;
      case ResponsiveContainerWidth.canvas:
        return ResponsiveBreakpoints.maxContentWidthCanvas;
    }
  }

  @override
  Widget build(BuildContext context) {
    final effectivePadding = padding ?? context.screenGutter;
    final maxWidth = _resolveMaxWidth();

    Widget boundedContent = Center(
      child: ConstrainedBox(
        constraints: BoxConstraints(maxWidth: maxWidth),
        child: Padding(
          padding: effectivePadding,
          child: child,
        ),
      ),
    );

    if (scrollable) {
      return SingleChildScrollView(
        controller: controller,
        physics: physics ?? const AlwaysScrollableScrollPhysics(),
        child: boundedContent,
      );
    }

    return boundedContent;
  }
}
