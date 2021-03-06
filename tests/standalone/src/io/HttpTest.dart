// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
//
// VMOptions=
// VMOptions=--short_socket_read
// VMOptions=--short_socket_write
// VMOptions=--short_socket_read --short_socket_write

#import("dart:isolate");
#import("dart:io");

class TestServerMain {
  TestServerMain()
      : _statusPort = new ReceivePort(),
        _serverPort = null {
    new TestServer().spawn().then((SendPort port) {
      _serverPort = port;
    });
  }

  void setServerStartedHandler(void startedCallback(int port)) {
    _startedCallback = startedCallback;
  }

  void start() {
    // Handle status messages from the server.
    _statusPort.receive((var status, SendPort replyTo) {
      if (status.isStarted) {
        _startedCallback(status.port);
      }
    });

    // Send server start message to the server.
    var command = new TestServerCommand.start();
    _serverPort.send(command, _statusPort.toSendPort());
  }

  void shutdown() {
    // Send server stop message to the server.
    _serverPort.send(new TestServerCommand.stop(), _statusPort.toSendPort());
    _statusPort.close();
  }

  void chunkedEncoding() {
    // Send chunked encoding message to the server.
    _serverPort.send(
        new TestServerCommand.chunkedEncoding(), _statusPort.toSendPort());
  }

  ReceivePort _statusPort;  // Port for receiving messages from the server.
  SendPort _serverPort;  // Port for sending messages to the server.
  var _startedCallback;
}


class TestServerCommand {
  static final START = 0;
  static final STOP = 1;
  static final CHUNKED_ENCODING = 2;

  TestServerCommand.start() : _command = START;
  TestServerCommand.stop() : _command = STOP;
  TestServerCommand.chunkedEncoding() : _command = CHUNKED_ENCODING;

  bool get isStart() => _command == START;
  bool get isStop() => _command == STOP;
  bool get isChunkedEncoding() => _command == CHUNKED_ENCODING;

  int _command;
}


class TestServerStatus {
  static final STARTED = 0;
  static final STOPPED = 1;
  static final ERROR = 2;

  TestServerStatus.started(this._port) : _state = STARTED;
  TestServerStatus.stopped() : _state = STOPPED;
  TestServerStatus.error() : _state = ERROR;

  bool get isStarted() => _state == STARTED;
  bool get isStopped() => _state == STOPPED;
  bool get isError() => _state == ERROR;

  int get port() => _port;

  int _state;
  int _port;
}


class TestServer extends Isolate {
  // Echo the request content back to the response.
  void _echoHandler(HttpRequest request, HttpResponse response) {
    Expect.equals("POST", request.method);
    response.contentLength = request.contentLength;
    request.inputStream.pipe(response.outputStream);
  }

  // Echo the request content back to the response.
  void _zeroToTenHandler(HttpRequest request, HttpResponse response) {
    Expect.equals("GET", request.method);
    request.inputStream.onData = () {};
    request.inputStream.onClosed = () {
      response.outputStream.writeString("01234567890");
      response.outputStream.close();
    };
  }

  // Return a 404.
  void _notFoundHandler(HttpRequest request, HttpResponse response) {
    response.statusCode = HttpStatus.NOT_FOUND;
    response.setHeader("Content-Type", "text/html; charset=UTF-8");
    response.outputStream.writeString("Page not found");
    response.outputStream.close();
  }

  // Return a 301 with a custom reason phrase.
  void _reasonForMovingHandler(HttpRequest request, HttpResponse response) {
    response.statusCode = HttpStatus.MOVED_PERMANENTLY;
    response.reasonPhrase = "Don't come looking here any more";
    response.outputStream.close();
  }

  // Check the "Host" header.
  void _hostHandler(HttpRequest request, HttpResponse response) {
    Expect.equals("www.dartlang.org:1234", request.headers["host"]);
    response.statusCode = HttpStatus.OK;
    response.outputStream.close();
  }

  // Set the "Expires" header using the expires property.
  void _expires1Handler(HttpRequest request, HttpResponse response) {
    Date date =
        new Date.withTimeZone(
            1999, Date.JUN, 11, 18, 46, 53, 0, new TimeZone.utc());
    response.expires = date;
    Expect.equals(date, response.expires);
    response.outputStream.close();
  }

  // Set the "Expires" header.
  void _expires2Handler(HttpRequest request, HttpResponse response) {
    response.setHeader("Expires", "Fri, 11 Jun 1999 18:46:53 GMT");
    Date date =
        new Date.withTimeZone(
            1999, Date.JUN, 11, 18, 46, 53, 0, new TimeZone.utc());
    Expect.equals(date, response.expires);
    response.outputStream.close();
  }

  void main() {
    // Setup request handlers.
    _requestHandlers = new Map();
    _requestHandlers["/echo"] = (HttpRequest request, HttpResponse response) {
      _echoHandler(request, response);
    };
    _requestHandlers["/0123456789"] =
        (HttpRequest request, HttpResponse response) {
          _zeroToTenHandler(request, response);
        };
    _requestHandlers["/reasonformoving"] =
        (HttpRequest request, HttpResponse response) {
          _reasonForMovingHandler(request, response);
        };
    _requestHandlers["/host"] =
        (HttpRequest request, HttpResponse response) {
          _hostHandler(request, response);
        };
    _requestHandlers["/expires1"] =
        (HttpRequest request, HttpResponse response) {
          _expires1Handler(request, response);
        };
    _requestHandlers["/expires2"] =
        (HttpRequest request, HttpResponse response) {
          _expires2Handler(request, response);
        };

    this.port.receive((var message, SendPort replyTo) {
      if (message.isStart) {
        _server = new HttpServer();
        try {
          _server.listen("127.0.0.1", 0);
          _server.onRequest = (HttpRequest req, HttpResponse rsp) {
            _requestReceivedHandler(req, rsp);
          };
          replyTo.send(new TestServerStatus.started(_server.port), null);
        } catch (var e) {
          replyTo.send(new TestServerStatus.error(), null);
        }
      } else if (message.isStop) {
        _server.close();
        this.port.close();
        replyTo.send(new TestServerStatus.stopped(), null);
      } else if (message.isChunkedEncoding) {
        _chunkedEncoding = true;
      }
    });
  }

  void _requestReceivedHandler(HttpRequest request, HttpResponse response) {
    var requestHandler =_requestHandlers[request.path];
    if (requestHandler != null) {
      requestHandler(request, response);
    } else {
      _notFoundHandler(request, response);
    }
  }

  HttpServer _server;  // HTTP server instance.
  Map _requestHandlers;
  bool _chunkedEncoding = false;
}


void testStartStop() {
  TestServerMain testServerMain = new TestServerMain();
  testServerMain.setServerStartedHandler((int port) {
    testServerMain.shutdown();
  });
  testServerMain.start();
}


void testGET() {
  TestServerMain testServerMain = new TestServerMain();
  testServerMain.setServerStartedHandler((int port) {
    HttpClient httpClient = new HttpClient();
    HttpClientConnection conn =
        httpClient.get("127.0.0.1", port, "/0123456789");
    conn.onResponse = (HttpClientResponse response) {
      Expect.equals(HttpStatus.OK, response.statusCode);
      StringInputStream stream = new StringInputStream(response.inputStream);
      StringBuffer body = new StringBuffer();
      stream.onData = () => body.add(stream.read());
      stream.onClosed = () {
        Expect.equals("01234567890", body.toString());
        httpClient.shutdown();
        testServerMain.shutdown();
      };
    };
  });
  testServerMain.start();
}


void testPOST(bool chunkedEncoding) {
  String data = "ABCDEFGHIJKLMONPQRSTUVWXYZ";
  final int kMessageCount = 10;

  TestServerMain testServerMain = new TestServerMain();

  void runTest(int port) {
    int count = 0;
    HttpClient httpClient = new HttpClient();
    void sendRequest() {
      HttpClientConnection conn =
          httpClient.post("127.0.0.1", port, "/echo");
      conn.onRequest = (HttpClientRequest request) {
        if (chunkedEncoding) {
          request.outputStream.writeString(data.substring(0, 10));
          request.outputStream.writeString(data.substring(10, data.length));
        } else {
          request.contentLength = data.length;
          request.outputStream.write(data.charCodes());
        }
        request.outputStream.close();
      };
      conn.onResponse = (HttpClientResponse response) {
        Expect.equals(HttpStatus.OK, response.statusCode);
        StringInputStream stream = new StringInputStream(response.inputStream);
        StringBuffer body = new StringBuffer();
        stream.onData = () => body.add(stream.read());
        stream.onClosed = () {
          Expect.equals(data, body.toString());
          count++;
          if (count < kMessageCount) {
            sendRequest();
          } else {
            httpClient.shutdown();
            testServerMain.shutdown();
          }
        };
      };
    }

    sendRequest();
  }

  testServerMain.setServerStartedHandler(runTest);
  if (chunkedEncoding) {
    testServerMain.chunkedEncoding();
  }
  testServerMain.start();
}


void testReadInto(bool chunkedEncoding) {
  String data = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  final int kMessageCount = 10;

  TestServerMain testServerMain = new TestServerMain();

  void runTest(int port) {
    int count = 0;
    HttpClient httpClient = new HttpClient();
    void sendRequest() {
      HttpClientConnection conn =
          httpClient.post("127.0.0.1", port, "/echo");
      conn.onRequest = (HttpClientRequest request) {
        if (chunkedEncoding) {
          request.outputStream.writeString(data.substring(0, 10));
          request.outputStream.writeString(data.substring(10, data.length));
        } else {
          request.contentLength = data.length;
          request.outputStream.write(data.charCodes());
        }
        request.outputStream.close();
      };
      conn.onResponse = (HttpClientResponse response) {
        Expect.equals(HttpStatus.OK, response.statusCode);
        InputStream stream = response.inputStream;
        List<int> body = new List<int>();
        stream.onData = () {
          List tmp = new List(3);
          int bytes = stream.readInto(tmp);
          body.addAll(tmp.getRange(0, bytes));
        };
        stream.onClosed = () {
          Expect.equals(data, new String.fromCharCodes(body));
          count++;
          if (count < kMessageCount) {
            sendRequest();
          } else {
            httpClient.shutdown();
            testServerMain.shutdown();
          }
        };
      };
    }

    sendRequest();
  }

  testServerMain.setServerStartedHandler(runTest);
  if (chunkedEncoding) {
    testServerMain.chunkedEncoding();
  }
  testServerMain.start();
}


void testReadShort(bool chunkedEncoding) {
  String data = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  final int kMessageCount = 10;

  TestServerMain testServerMain = new TestServerMain();

  void runTest(int port) {
    int count = 0;
    HttpClient httpClient = new HttpClient();
    void sendRequest() {
      HttpClientConnection conn =
          httpClient.post("127.0.0.1", port, "/echo");
      conn.onRequest = (HttpClientRequest request) {
        if (chunkedEncoding) {
          request.outputStream.writeString(data.substring(0, 10));
          request.outputStream.writeString(data.substring(10, data.length));
        } else {
          request.contentLength = data.length;
          request.outputStream.write(data.charCodes());
        }
        request.outputStream.close();
      };
      conn.onResponse = (HttpClientResponse response) {
        Expect.equals(HttpStatus.OK, response.statusCode);
        InputStream stream = response.inputStream;
        List<int> body = new List<int>();
        stream.onData = () {
          List tmp = stream.read(2);
          body.addAll(tmp);
        };
        stream.onClosed = () {
          Expect.equals(data, new String.fromCharCodes(body));
          count++;
          if (count < kMessageCount) {
            sendRequest();
          } else {
            httpClient.shutdown();
            testServerMain.shutdown();
          }
        };
      };
    }

    sendRequest();
  }

  testServerMain.setServerStartedHandler(runTest);
  if (chunkedEncoding) {
    testServerMain.chunkedEncoding();
  }
  testServerMain.start();
}


void test404() {
  TestServerMain testServerMain = new TestServerMain();
  testServerMain.setServerStartedHandler((int port) {
    HttpClient httpClient = new HttpClient();
    HttpClientConnection conn =
        httpClient.get("127.0.0.1", port, "/thisisnotfound");
    conn.onResponse = (HttpClientResponse response) {
      Expect.equals(HttpStatus.NOT_FOUND, response.statusCode);
      httpClient.shutdown();
      testServerMain.shutdown();
    };
  });
  testServerMain.start();
}


void testReasonPhrase() {
  TestServerMain testServerMain = new TestServerMain();
  testServerMain.setServerStartedHandler((int port) {
    HttpClient httpClient = new HttpClient();
    HttpClientConnection conn =
        httpClient.get("127.0.0.1", port, "/reasonformoving");
    conn.onResponse = (HttpClientResponse response) {
      Expect.equals(HttpStatus.MOVED_PERMANENTLY, response.statusCode);
      Expect.equals("Don't come looking here any more", response.reasonPhrase);
      httpClient.shutdown();
      testServerMain.shutdown();
    };
  });
  testServerMain.start();
}


void testHost() {
  TestServerMain testServerMain = new TestServerMain();
  testServerMain.setServerStartedHandler((int port) {
    HttpClient httpClient = new HttpClient();
    HttpClientConnection conn =
        httpClient.get("127.0.0.1", port, "/host");
    conn.onRequest = (HttpClientRequest request) {
      Expect.equals("127.0.0.1:$port", request.headers["host"]);
      request.host = "www.dartlang.com";
      Expect.equals("www.dartlang.com:$port", request.headers["host"]);
      request.port = 1234;
      Expect.equals("www.dartlang.com:1234", request.headers["host"]);
      request.port = HttpClient.DEFAULT_HTTP_PORT;
      Expect.equals("www.dartlang.com", request.headers["host"]);
      request.setHeader("Host", "www.dartlang.org");
      Expect.equals("www.dartlang.org", request.host);
      Expect.equals(HttpClient.DEFAULT_HTTP_PORT, request.port);
      request.setHeader("Host", "www.dartlang.org:");
      Expect.equals("www.dartlang.org", request.host);
      Expect.equals(HttpClient.DEFAULT_HTTP_PORT, request.port);
      request.setHeader("Host", "www.dartlang.org:1234");
      Expect.equals("www.dartlang.org", request.host);
      Expect.equals(1234, request.port);
      request.outputStream.close();
    };
    conn.onResponse = (HttpClientResponse response) {
      Expect.equals(HttpStatus.OK, response.statusCode);
      httpClient.shutdown();
      testServerMain.shutdown();
    };
  });
  testServerMain.start();
}

void testExpires() {
  TestServerMain testServerMain = new TestServerMain();
  testServerMain.setServerStartedHandler((int port) {
    int responses = 0;
    HttpClient httpClient = new HttpClient();

    void processResponse(HttpClientResponse response) {
      Expect.equals(HttpStatus.OK, response.statusCode);
      Expect.equals("Fri, 11 Jun 1999 18:46:53 GMT",
                    response.headers["expires"]);
      Expect.equals(
          new Date.withTimeZone(
              1999, Date.JUN, 11, 18, 46, 53, 0, new TimeZone.utc()),
          response.expires);
      responses++;
      if (responses == 2) {
        httpClient.shutdown();
        testServerMain.shutdown();
      }
    }

    HttpClientConnection conn1 = httpClient.get("127.0.0.1", port, "/expires1");
    conn1.onResponse = (HttpClientResponse response) {
      processResponse(response);
    };
    HttpClientConnection conn2 = httpClient.get("127.0.0.1", port, "/expires2");
    conn2.onResponse = (HttpClientResponse response) {
      processResponse(response);
    };
  });
  testServerMain.start();
}


void main() {
  testStartStop();
  testGET();
  testPOST(true);
  testPOST(false);
  testReadInto(true);
  testReadInto(false);
  testReadShort(true);
  testReadShort(false);
  test404();
  testReasonPhrase();
  testHost();
  testExpires();
}
