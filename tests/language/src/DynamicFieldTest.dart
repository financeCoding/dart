// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
//
// Test that ensures that fields can be accessed dynamically.

class A extends C {
  var a;
  var b;
}

class C {
  foo() {
    print(a); /// static type error
    return a; /// static type error
  }
  bar() {
    print(b.a); /// static type error
    return b.a; /// static type error
  }
}

main() {
  var a = new A();
  a.a = 1;
  a.b = a;
  Expect.equals(1, a.foo());
  Expect.equals(1, a.bar());
}
