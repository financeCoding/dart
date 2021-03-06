// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library("standalone_test_config");

#import("../../tools/testing/dart/test_suite.dart");

class StandaloneTestSuite extends StandardTestSuite {
  StandaloneTestSuite(Map configuration)
      : super(configuration,
              "standalone",
              "tests/standalone/src",
              ["tests/standalone/standalone.status",
               "tests/standalone/standalone-leg.status"]);

  bool listRecursively() => true;
}
