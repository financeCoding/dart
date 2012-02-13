
class _SVGCircleElementJs extends _SVGElementJs implements SVGCircleElement native "*SVGCircleElement" {

  final _SVGAnimatedLengthJs cx;

  final _SVGAnimatedLengthJs cy;

  final _SVGAnimatedLengthJs r;

  // From SVGTests

  final _SVGStringListJs requiredExtensions;

  final _SVGStringListJs requiredFeatures;

  final _SVGStringListJs systemLanguage;

  bool hasExtension(String extension) native;

  // From SVGLangSpace

  String xmllang;

  String xmlspace;

  // From SVGExternalResourcesRequired

  final _SVGAnimatedBooleanJs externalResourcesRequired;

  // From SVGStylable

  final _SVGAnimatedStringJs className;

  // Use implementation from Element.
  // final _CSSStyleDeclarationJs style;

  _CSSValueJs getPresentationAttribute(String name) native;

  // From SVGTransformable

  final _SVGAnimatedTransformListJs transform;

  // From SVGLocatable

  final _SVGElementJs farthestViewportElement;

  final _SVGElementJs nearestViewportElement;

  _SVGRectJs getBBox() native;

  _SVGMatrixJs getCTM() native;

  _SVGMatrixJs getScreenCTM() native;

  _SVGMatrixJs getTransformToElement(_SVGElementJs element) native;
}
