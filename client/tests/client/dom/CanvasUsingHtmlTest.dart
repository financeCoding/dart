#library('CanvasUsingHtmlTest');
#import('../../../../lib/unittest/unittest_dom.dart');
#import('dart:html', prefix: 'html');
#import('dart:dom');

// Version of Canvas test that implicitly uses dart:html library via unittests.

main() {
  HTMLCanvasElement canvas;
  CanvasRenderingContext2D context;

  // FIXME: once main is run on content loaded, this hack won't be necessary.
  window.setTimeout(() {
    canvas = document.createElement('canvas');
    canvas.setAttribute('width', '100');
    canvas.setAttribute('height', '100');
    document.body.appendChild(canvas);
    context = canvas.getContext('2d');
  }, 0);

  forLayoutTests();
  test('FillStyle', () {
    context.fillStyle = "red";
    context.fillRect(10, 10, 20, 20);

    // TODO(vsm): Verify the result once we have the ability to read pixels.
  });
  test('SetFillColor', () {
    // With floats.
    context.setFillColor(10, 10, 10, 10);
    context.fillRect(10, 10, 20, 20);

    // With rationals.
    context.setFillColor(10.0, 10.0, 10.0, 10.0);
    context.fillRect(20, 20, 30, 30);

    // With ints.
    context.setFillColor(10, 10, 10, 10);
    context.fillRect(30, 30, 40, 40);

    // TODO(vsm): Verify the result once we have the ability to read pixels.
  });
  test('StrokeStyle', () {
    context.strokeStyle = "blue";
    context.strokeRect(30, 30, 10, 20);

    // TODO(vsm): Verify the result once we have the ability to read pixels.
  });
  test('CreateImageData', () {
    ImageData image = context.createImageData(canvas.width,
                                              canvas.height);
    CanvasPixelArray bytes = image.data;

    // FIXME: uncomment when numeric index getters are supported.
    //var byte = bytes[0];

    Expect.equals(40000, bytes.length);
  });
}
