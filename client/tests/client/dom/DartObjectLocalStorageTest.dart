#library('DartObjectLocalStorageTest');
#import('../../../../lib/unittest/unittest_dom.dart');
#import('dart:dom');

verify(var object) {
  final value = window.document;
  object.dartObjectLocalStorage = value;
  final stored = object.dartObjectLocalStorage;
  Expect.equals(value, stored);
}

main() {
  forLayoutTests();
  test('body', () {
      HTMLBodyElement body = document.body;
      verify(body);
  });
  test('localStorage', () {
      Storage storage = window.localStorage;
      verify(storage);
  });
  test('sessionStorage', () {
      Storage storage = window.sessionStorage;
      verify(storage);
  });
  test('unknown', () {
      var element = document.createElement('canvas');
      element.id = 'test';
      document.body.appendChild(element);
      element = document.getElementById('test');
      verify(element);
  });
}
