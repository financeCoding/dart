// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// WARNING: Do not edit - generated code.

interface Node extends EventTarget {

  static final int ATTRIBUTE_NODE = 2;

  static final int CDATA_SECTION_NODE = 4;

  static final int COMMENT_NODE = 8;

  static final int DOCUMENT_FRAGMENT_NODE = 11;

  static final int DOCUMENT_NODE = 9;

  static final int DOCUMENT_POSITION_CONTAINED_BY = 0x10;

  static final int DOCUMENT_POSITION_CONTAINS = 0x08;

  static final int DOCUMENT_POSITION_DISCONNECTED = 0x01;

  static final int DOCUMENT_POSITION_FOLLOWING = 0x04;

  static final int DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC = 0x20;

  static final int DOCUMENT_POSITION_PRECEDING = 0x02;

  static final int DOCUMENT_TYPE_NODE = 10;

  static final int ELEMENT_NODE = 1;

  static final int ENTITY_NODE = 6;

  static final int ENTITY_REFERENCE_NODE = 5;

  static final int NOTATION_NODE = 12;

  static final int PROCESSING_INSTRUCTION_NODE = 7;

  static final int TEXT_NODE = 3;

  final NamedNodeMap attributes;

  final String baseURI;

  final NodeList childNodes;

  final Node firstChild;

  final Node lastChild;

  final String localName;

  final String namespaceURI;

  final Node nextSibling;

  final String nodeName;

  final int nodeType;

  String nodeValue;

  final Document ownerDocument;

  final Element parentElement;

  final Node parentNode;

  String prefix;

  final Node previousSibling;

  String textContent;

  void addEventListener(String type, EventListener listener, [bool useCapture]);

  Node appendChild(Node newChild);

  Node cloneNode(bool deep);

  int compareDocumentPosition(Node other);

  bool contains(Node other);

  bool dispatchEvent(Event event);

  bool hasAttributes();

  bool hasChildNodes();

  Node insertBefore(Node newChild, Node refChild);

  bool isDefaultNamespace(String namespaceURI);

  bool isEqualNode(Node other);

  bool isSameNode(Node other);

  bool isSupported(String feature, String version);

  String lookupNamespaceURI(String prefix);

  String lookupPrefix(String namespaceURI);

  void normalize();

  Node removeChild(Node oldChild);

  void removeEventListener(String type, EventListener listener, [bool useCapture]);

  Node replaceChild(Node newChild, Node oldChild);
}
