// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

final m1 = const { 'a': 400 + 99 };
final m2 = const { 'a': 499, 'b': 42 };
final m3 = const { 'm1': m1, 'm2': m2 };
final m4 = const { 'z': 9, 'a': 8, 'm': 7 };
final m5 = const { '': 499 };
final m6 = const { 'a': 499 };
final m7 = const {};

bool isIllegalAccessException(o) => o is IllegalAccessException;

main() {
  Expect.equals(499, m1['a']);
  Expect.equals(null, m1['b']);
  Expect.listEquals(['a'], m1.getKeys());
  Expect.listEquals([499], m1.getValues());
  Expect.isTrue(m1.containsKey('a'));
  Expect.isFalse(m1.containsKey('toString'));
  Expect.isTrue(m1.containsValue(499));
  Expect.isFalse(m1.containsValue(42));
  Expect.isFalse(m1.containsValue(null));
  var seenKeys = [];
  var seenValues = [];
  m1.forEach((key, value) {
    seenKeys.add(key);
    seenValues.add(value);
  });
  Expect.listEquals(['a'], seenKeys);
  Expect.listEquals([499], seenValues);
  Expect.isFalse(m1.isEmpty());
  Expect.equals(1, m1.length);
  Expect.throws(() => m1.remove('a'), isIllegalAccessException);
  Expect.throws(() => m1.remove('b'), isIllegalAccessException);
  Expect.throws(() => m1.clear(), isIllegalAccessException);
  Expect.throws(() => m1['b'] = 42, isIllegalAccessException);
  Expect.throws(() => m1['a'] = 499, isIllegalAccessException);
  Expect.throws(() => m1.putIfAbsent('a', () => 499),
                isIllegalAccessException);
  Expect.throws(() => m1.putIfAbsent('z', () => 499),
                isIllegalAccessException);

  Expect.equals(499, m2['a']);
  Expect.equals(42, m2['b']);
  Expect.equals(null, m2['c']);
  Expect.listEquals(['a', 'b'], m2.getKeys());
  Expect.listEquals([499, 42], m2.getValues());
  Expect.isTrue(m2.containsKey('a'));
  Expect.isTrue(m2.containsKey('b'));
  Expect.isFalse(m2.containsKey('toString'));
  Expect.isTrue(m2.containsValue(499));
  Expect.isTrue(m2.containsValue(42));
  Expect.isFalse(m2.containsValue(99));
  Expect.isFalse(m2.containsValue(null));
  seenKeys = [];
  seenValues = [];
  m2.forEach((key, value) {
    seenKeys.add(key);
    seenValues.add(value);
  });
  Expect.listEquals(['a', 'b'], seenKeys);
  Expect.listEquals([499, 42], seenValues);
  Expect.isFalse(m2.isEmpty());
  Expect.equals(2, m2.length);
  Expect.throws(() => m2.remove('a'), isIllegalAccessException);
  Expect.throws(() => m2.remove('b'), isIllegalAccessException);
  Expect.throws(() => m2.remove('c'), isIllegalAccessException);
  Expect.throws(() => m2.clear(), isIllegalAccessException);
  Expect.throws(() => m2['a'] = 499, isIllegalAccessException);
  Expect.throws(() => m2['b'] = 42, isIllegalAccessException);
  Expect.throws(() => m2['c'] = 499, isIllegalAccessException);
  Expect.throws(() => m2.putIfAbsent('a', () => 499),
                isIllegalAccessException);
  Expect.throws(() => m2.putIfAbsent('z', () => 499),
                isIllegalAccessException);
  Expect.throws(() => m2['a'] = 499, isIllegalAccessException);

  Expect.isTrue(m3['m1'] === m1);
  Expect.isTrue(m3['m2'] === m2);

  Expect.listEquals(['z', 'a', 'm'], m4.getKeys());
  Expect.listEquals([9, 8, 7], m4.getValues());
  seenKeys = [];
  seenValues = [];
  m4.forEach((key, value) {
    seenKeys.add(key);
    seenValues.add(value);
  });
  Expect.listEquals(['z', 'a', 'm'], seenKeys);
  Expect.listEquals([9, 8, 7], seenValues);

  Expect.equals(499, m5['']);
  Expect.isTrue(m5.containsKey(''));
  Expect.equals(1, m5.length);

  Expect.isTrue(m1 === m6);

  Expect.isTrue(m7.isEmpty());
  Expect.equals(0, m7.length);
  Expect.equals(null, m7['b']);
  Expect.listEquals([], m7.getKeys());
  Expect.listEquals([], m7.getValues());
  Expect.isFalse(m7.containsKey('a'));
  Expect.isFalse(m7.containsKey('toString'));
  Expect.isFalse(m7.containsValue(499));
  Expect.isFalse(m7.containsValue(null));
  seenKeys = [];
  seenValues = [];
  m7.forEach((key, value) {
    seenKeys.add(key);
    seenValues.add(value);
  });
  Expect.listEquals([], seenKeys);
  Expect.listEquals([], seenValues);
  Expect.throws(() => m7.remove('a'), isIllegalAccessException);
  Expect.throws(() => m7.remove('b'), isIllegalAccessException);
  Expect.throws(() => m7.clear(), isIllegalAccessException);
  Expect.throws(() => m7['b'] = 42, isIllegalAccessException);
  Expect.throws(() => m7['a'] = 499, isIllegalAccessException);
  Expect.throws(() => m7.putIfAbsent('a', () => 499),
                isIllegalAccessException);
  Expect.throws(() => m7.putIfAbsent('z', () => 499),
                isIllegalAccessException);
}
