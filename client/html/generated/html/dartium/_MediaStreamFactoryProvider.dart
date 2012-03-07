// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class _MediaStreamFactoryProvider {
  factory MediaStream(MediaStreamTrackList audioTracks, MediaStreamTrackList videoTracks) =>
      _wrap(new dom.MediaStream(_unwrap(audioTracks), _unwrap(videoTracks)));
}