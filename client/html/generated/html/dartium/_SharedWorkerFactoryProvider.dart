// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class _SharedWorkerFactoryProvider {
  factory SharedWorker(String scriptURL, [String name = null]) =>
      _wrap(new dom.SharedWorker(_unwrap(scriptURL), _unwrap(name)));
}
