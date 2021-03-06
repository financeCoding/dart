// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.resolver;

import com.google.common.base.Joiner;
import com.google.dart.compiler.DartCompilationError;
import com.google.dart.compiler.ErrorCode;
import com.google.dart.compiler.ast.DartClass;
import com.google.dart.compiler.type.DynamicType;
import com.google.dart.compiler.type.InterfaceType;
import com.google.dart.compiler.type.Type;
import com.google.dart.compiler.type.Types;

import static com.google.dart.compiler.common.ErrorExpectation.errEx;

import junit.framework.Assert;

import java.util.List;

/**
 * Basic tests of the resolver.
 */
public class ResolverTest extends ResolverTestCase {
  private final DartClass object = makeClass("Object", null);
  private final DartClass array = makeClass("Array", makeType("Object"), "E");
  private final DartClass growableArray = makeClass("GrowableArray", makeType("Array", "S"), "S");
  private final Types types = Types.getInstance(null);

  private ClassElement findElementOrFail(Scope libScope, String elementName) {
    Element element = libScope.findElement(libScope.getLibrary(), elementName);
    assertEquals(ElementKind.CLASS, ElementKind.of(element));
    return (ClassElement) element;
  }

  public void testToString() {
    Assert.assertEquals("class Object {\n}", object.toString().trim());
    Assert.assertEquals("class Array<E> extends Object {\n}", array.toString().trim());
  }

  public void testResolve() {
    Scope libScope = resolve(makeUnit(object, array, growableArray), getContext());
    LibraryElement library = libScope.getLibrary();
    ClassElement objectElement = (ClassElement) libScope.findElement(library, "Object");
    Assert.assertNotNull(objectElement);
    ClassElement arrayElement = (ClassElement) libScope.findElement(library, "Array");
    Assert.assertNotNull(arrayElement);
    ClassElement growableArrayElement = (ClassElement) libScope.findElement(library,
                                                                            "GrowableArray");
    Assert.assertNotNull(growableArrayElement);

    Type objectType = objectElement.getType();
    Type arrayType = arrayElement.getType();
    Type growableArrayType = growableArrayElement.getType();
    Assert.assertNotNull(objectType);
    Assert.assertNotNull(arrayType);
    Assert.assertNotNull(growableArrayType);

    Assert.assertTrue(types.isSubtype(arrayType, objectType));
    Assert.assertFalse(types.isSubtype(objectType, arrayType));

    Assert.assertTrue(types.isSubtype(growableArrayType, objectType));

    // GrowableArray<S> is not a subtype of Array<E> because S and E aren't
    // related.
    Assert.assertFalse(types.isSubtype(growableArrayType, arrayType));
    Assert.assertFalse(types.isSubtype(objectType, growableArrayType));
    Assert.assertFalse(types.isSubtype(arrayType, growableArrayType));
  }

  /**
   * class A {}
   * class B extends A {}
   * class C extends A {}
   * class E extends C {}
   * class D extends C {}
   */
  public void testGetSubtypes() {
    DartClass a = makeClass("A", makeType("Object"));
    DartClass b = makeClass("B", makeType("A"));
    DartClass c = makeClass("C", makeType("A"));
    DartClass e = makeClass("E", makeType("C"));
    DartClass d = makeClass("D", makeType("C"));

    Scope libScope = resolve(makeUnit(object, a, b, c, d, e), getContext());

    ClassElement elementA = findElementOrFail(libScope, "A");
    ClassElement elementB = findElementOrFail(libScope, "B");
    ClassElement elementC = findElementOrFail(libScope, "C");
    ClassElement elementD = findElementOrFail(libScope, "D");
    ClassElement elementE = findElementOrFail(libScope, "E");
  }

  /**
   * interface IA extends ID default B {}
   * interface IB extends IA {}
   * interface IC extends IA, IB {}
   * interface ID extends IB {}
   * class A extends IA {}
   * class B {}
   */
  public void testGetSubtypesWithInterfaceCycles() {
    DartClass ia = makeInterface("IA", makeTypes("ID"), makeDefault("B"));
    DartClass ib = makeInterface("IB", makeTypes("IA"), null);
    DartClass ic = makeInterface("IC", makeTypes("IA", "IB"), null);
    DartClass id = makeInterface("ID", makeTypes("IB"), null);

    DartClass a = makeClass("A", null, makeTypes("IA"));
    DartClass b = makeClass("B", null);

    Scope libScope = resolve(makeUnit(object, ia, ib, ic, id, a, b), getContext());
    ErrorCode[] expected = {
        ResolverErrorCode.CYCLIC_CLASS,
        ResolverErrorCode.CYCLIC_CLASS,
        ResolverErrorCode.CYCLIC_CLASS,
        ResolverErrorCode.CYCLIC_CLASS,
        ResolverErrorCode.CYCLIC_CLASS,
    };
    checkExpectedErrors(expected);

    ClassElement elementIA = findElementOrFail(libScope, "IA");
    ClassElement elementIB = findElementOrFail(libScope, "IB");
    ClassElement elementIC = findElementOrFail(libScope, "IC");
    ClassElement elementID = findElementOrFail(libScope, "ID");
    ClassElement elementA = findElementOrFail(libScope, "A");
    ClassElement elementB = findElementOrFail(libScope, "B");

    assert(elementIA.getDefaultClass().getElement().getName().equals("B"));
  }

  /**
   * interface IA extends IB {}
   * interface IB extends IA {}
   */
  public void testGetSubtypesWithSimpleInterfaceCycle() {
    DartClass ia = makeInterface("IA", makeTypes("IB"), null);
    DartClass ib = makeInterface("IB", makeTypes("IA"), null);


    Scope libScope = resolve(makeUnit(object, ia, ib), getContext());
    ErrorCode[] expected = {
        ResolverErrorCode.CYCLIC_CLASS,
        ResolverErrorCode.CYCLIC_CLASS,
    };
    checkExpectedErrors(expected);

    ClassElement elementIA = findElementOrFail(libScope, "IA");
    ClassElement elementIB = findElementOrFail(libScope, "IB");
  }

  /**
   * class A<T> {}
   * class B extends A<C> {}
   * class C {}
   */
  public void testGetSubtypesWithParemeterizedSupertypes() {
    DartClass a = makeClass("A", null, "T");
    DartClass b = makeClass("B", makeType("A", "C"));
    DartClass c = makeClass("C", null);

    Scope libScope = resolve(makeUnit(object, a, b, c), getContext());

    ClassElement elementA = findElementOrFail(libScope, "A");
    ClassElement elementB = findElementOrFail(libScope, "B");
    ClassElement elementC = findElementOrFail(libScope, "C");

  }

  public void testDuplicatedInterfaces() {
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "interface int {}",
        "interface bool {}",
        "interface I<X> {",
        "}",
        "class A extends C implements I<int> {}",
        "class B extends C implements I<bool> {}",
        "class C implements I<int> {}"),
        ResolverErrorCode.DUPLICATED_INTERFACE);
  }

  public void testImplicitDefaultConstructor() {
    // Check that the implicit constructor is resolved correctly
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "class B {}",
        "class C { main() { new B(); } }"));

    /*
     * We should check for signature mismatch but that is a TypeAnalyzer issue.
     */
  }

  public void testImplicitDefaultConstructor_OnInterfaceWithoutFactory() {
    // Check that the implicit constructor is resolved correctly
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "interface B {}",
        "class C { main() { new B(); } }"),
        ResolverErrorCode.NEW_EXPRESSION_NOT_CONSTRUCTOR);

    /*
     * We should check for signature mismatch but that is a TypeAnalyzer issue.
     */
  }

  public void testImplicitDefaultConstructor_ThroughFactories() {
    // Check that we generate implicit constructors through factories also.
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "interface B default C {}",
        "class C {}",
        "class D { main() { new B(); } }"));
  }

  public void testImplicitDefaultConstructor_WithConstCtor() {
    // Check that we generate an error if the implicit constructor would violate const.
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "class B { const B() {} }",
        "class C extends B {}",
        "class D { main() { new C(); } }"),
          ResolverErrorCode.CONST_CONSTRUCTOR_CANNOT_HAVE_BODY);
  }

  public void testImplicitSuperCall_ImplicitCtor() {
    // Check that we can properly resolve the super ctor that exists.
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "class B { B() {} }",
        "class C extends B {}",
        "class D { main() { new C(); } }"));
  }

  public void testImplicitSuperCall_OnExistingCtor() {
    // Check that we can properly resolve the super ctor that exists.
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "class B { B() {} }",
        "class C extends B { C(){} }",
        "class D { main() { new C(); } }"));
  }

  public void testImplicitSuperCall_NonExistentSuper() {
    // Check that we generate an error if the implicit constructor would call a non-existent super.
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "class B { B(Object o) {} }",
        "class C extends B {}",
        "class D { main() { new C(); } }"),
        ResolverErrorCode.CANNOT_RESOLVE_IMPLICIT_CALL_TO_SUPER_CONSTRUCTOR);
  }

  public void testImplicitSuperCall_NonExistentSuper2() {
    // Check that we generate an error if the implicit constructor would call a non-existent super.
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "class B { B.foo() {} }",
        "class C extends B {}",
        "class D { main() { new C(); } }"),
        ResolverErrorCode.CANNOT_RESOLVE_IMPLICIT_CALL_TO_SUPER_CONSTRUCTOR);
  }

  public void testCyclicSupertype() {

    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "interface int {}",
        "interface bool {}",
        "class Cyclic extends Cyclic {",
        "}",
        "class A extends B {",
        "}",
        "class B extends A {",
        "}",
        "interface I extends I {",
        "}",
        "class C implements I1, I {",
        "}",
        "interface I1 {",
        "}",
        "class D implements I1, I2 {",
        "}",
        "interface I2 extends I3 {",
        "}",
        "interface I3 extends I2 {",
        "}"),
        ResolverErrorCode.CYCLIC_CLASS,
        ResolverErrorCode.CYCLIC_CLASS,
        ResolverErrorCode.CYCLIC_CLASS,
        ResolverErrorCode.CYCLIC_CLASS,
        ResolverErrorCode.CYCLIC_CLASS,
        ResolverErrorCode.CYCLIC_CLASS,
        ResolverErrorCode.CYCLIC_CLASS,
        ResolverErrorCode.CYCLIC_CLASS
    );
  }

  public void testBadFactory() {
    // Another interface should be in scope to name 'foo' as a constructor
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "class Zebra {",
        "  factory foo() {}",
        "}"),
        ResolverErrorCode.NO_SUCH_TYPE_CONSTRUCTOR);
  }


  public void testDefaultTypeArgs1() {
    // Type arguments match
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "interface int {}",
        "interface A<T> default B<T> {",
        "  A();",
        "}",
        "class B<T> implements A<T> {",
        "  B() {}",
        "}"));
  }

  public void testDefaultTypeArgs2() {
    // Type arguments match
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "interface A<T> default B<T> {",
        "}",
        "class B<T> {",
        "  factory A.construct () {}",
        "}"));
  }

  public void testDefaultTypeArgs3() {
    // Type arguments match
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "interface A<T> default B<T> {",
        "}",
        "class B<T> {",
        "  B() {}",
        "}"));
  }

  public void testDefaultTypeArgs4() {
    // Type arguments match
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "interface A<T> default B<T> {",
        "}",
        "class B<T> implements A<T> {",
        "  B() {}",
        "}"));
  }

  public void testDefaultTypeArgs5() {
    // Invoke constructor in factory method with (wrong) type args
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "interface int {}",
        "interface A<T> default B {",
        "}",
        "class B<T> {",
        "}"));
  }

  public void testDefaultTypeArgs6() {
    // Invoke constructor in factory method with (wrong) type args
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "interface int {}",
        "interface A<T> default B {",
        "}",
        "class B<T extends int> {",
        "}"));
  }

  public void testDefaultTypeArgs7() {
    // Example from spec v0.6
    resolveAndTest(Joiner.on("\n").join(
        "class Object{}",
        "interface Hashable {}",
        "class HashMapImplementation<K extends Hashable, V> {",
        "}",
        "interface Map<K, V> default HashMapImplementation<K extends Hashable, V> {",
        "}"));
  }

  public void testDefaultTypeArgs8() {
    resolveAndTest(Joiner.on("\n").join(
        "class Object{}",
        "interface A<K,V> default B<K, V extends K> {",
        "}",
        "class B<K, V extends K> {",
        "}"));
  }

  public void testDefaultTypeArgs9() {
    resolveAndTest(Joiner.on("\n").join(
        "class Object{}",
        "interface List<T> {}",
        "interface A<K,V> default B<K, V extends List<K>> {}",
        "class B<K, V extends List<K>> {",
        "}"));
  }

  public void testDefaultTypeArgs10() {
    resolveAndTest(Joiner.on("\n").join(
        "class Object{}",
        "interface List<T> {}",
        "class A<T, U, V> {}",
        "interface I2<T> default B<T extends A> {}",
        "class B<T extends A> {}",
        ""));
  }


  public void testDefaultTypeArgsNew() {
    // Invoke constructor in factory method with type args
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "interface int {}",
        "interface A<T> default B<T> {",
        "  B();",
        "}",
        "class C<T> implements A<T> {}",
        "class B<T> {",
        "  factory B() { return new C<T>();}",
        "}"));
  }

  public void testFactoryBadTypeArgs1() {
    // Invoke constructor in factory method with (wrong) type args
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "interface int {}",
        "interface A<T> default B<T> {",
        "  A();",
        "}",
        "class C<T> implements A<T> {}",
        "class B<T> {",
        "  factory A() { return new C<K>();}",
        "}"),
        ResolverErrorCode.NO_SUCH_TYPE);
  }

  public void testFactoryBadTypeArgs2() {
    // Invoke constructor in factory method with (wrong) type args
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "interface int {}",
        "interface A<T> default B {",
        "  A();",
        "}",
        "class C<T> implements A<T> {}",
        "class B {",
        "  factory A() { return new C<int>();}",
        "}"),
        ResolverErrorCode.DEFAULT_CLASS_MUST_HAVE_SAME_TYPE_PARAMS);
  }

  public void testFactoryBadTypeArgs3() {
    // Invoke constructor in factory method with (wrong) type args
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "interface int {}",
        "interface A<T> default B<K> {",
        "  A();",
        "}",
        "class C<T> implements A<T> {}",
        "class B<T> {",
        "  factory A() { return new C<T>();}",
        "}"),
        ResolverErrorCode.TYPE_PARAMETERS_MUST_MATCH_EXACTLY);
  }

  public void testFactoryBadTypeArgs4() {
    // Invoke constructor in factory method with (wrong) type args
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "interface int {}",
        "interface A<T> default B<T> {",
        "  A();",
        "}",
        "class C<T> implements A<T> {}",
        "class B<K> {",
        "  factory A() { return new C<K>();}",
        "}"),
        ResolverErrorCode.TYPE_PARAMETERS_MUST_MATCH_EXACTLY,
        ResolverErrorCode.TYPE_VARIABLE_DOES_NOT_MATCH);
  }

  public void testFactoryBadTypeArgs5() {
    // Invoke constructor in factory method with (wrong) type args
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "interface int {}",
        "interface A<T> default B<T,L> {",
        "  A();",
        "}",
        "class C<T> implements A<T> {}",
        "class B<T> {",
        "  factory A() { return new C<T>();}",
        "}"),
        ResolverErrorCode.TYPE_PARAMETERS_MUST_MATCH_EXACTLY);
  }

  public void testFactoryBadTypeArgs6() {
    // Invoke constructor in factory method with (wrong) type args
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "interface int {}",
        "interface A<T> default B<K> {",
        "  A();",
        "}",
        "class C<T> implements A<T> {}",
        "class B<K> {",
        "  factory A() { return new C<K>();}",
        "}"),
        ResolverErrorCode.TYPE_VARIABLE_DOES_NOT_MATCH);
  }

  public void testFactoryBadTypeArgs7() {
    // Invoke constructor in factory method with (wrong) type args
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "interface int {}",
        "interface A<T,K> default B<T,K> {",
        "  A();",
        "}",
        "class C<T,K> implements A<T,K> {}",
        "class B<K,T> {",
        "  factory A() { return new C<int, Object>();}",
        "}"),
        ResolverErrorCode.TYPE_PARAMETERS_MUST_MATCH_EXACTLY,
        ResolverErrorCode.TYPE_VARIABLE_DOES_NOT_MATCH,
        ResolverErrorCode.TYPE_VARIABLE_DOES_NOT_MATCH);
  }

  public void testFactoryBadTypeArgs8() {
    // Invoke constructor in factory method with (wrong) type args
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "interface int {}",
        "interface A<T> default B<T> {",
        "  A();",
        "}",
        "class B<T extends int> {",
        "  factory A() {}",
        "}"),
        ResolverErrorCode.TYPE_PARAMETERS_MUST_MATCH_EXACTLY);
  }

  public void testFactoryBadTypeArgs9() {
    // Invoke constructor in factory method with (wrong) type args
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "interface int {}",
        "interface A<T> default B<T extends int> {",
        "  A();",
        "}",
        "class B<T> {",
        "  factory A() {}",
        "}"),
        ResolverErrorCode.TYPE_PARAMETERS_MUST_MATCH_EXACTLY);
  }

  public void testFactoryBadTypeArgs11() {
    // Invoke constructor in factory method with (wrong) type args
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "interface int {}",
        "interface A<T> default Bogus {",
        "}",
        "class B<T> {",
        "}"),
        ResolverErrorCode.NO_SUCH_TYPE);
  }

  public void testFactoryBadTypeArgs10() {
    // Invoke constructor in factory method with (wrong) type args
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "interface int {}",
        "interface A<T> default B {",
        "  A();",
        "}",
        "class B {",
        "  factory A() {}",
        "}"),
        ResolverErrorCode.DEFAULT_CLASS_MUST_HAVE_SAME_TYPE_PARAMS);
  }
  public void testBadDefaultTypeArgs11() {
    // Example from spec v0.6
    resolveAndTest(Joiner.on("\n").join(
        "class Object{}",
        "interface Hashable {}",
        "class HashMapImplementation<K extends Hashable, V> {",
        "}",
        "interface Map<K, V> default HashMapImplementation<K, V> {",
        "}"),
        ResolverErrorCode.TYPE_PARAMETERS_MUST_MATCH_EXACTLY);
  }

  public void testBadGenerativeConstructor1() {
    resolveAndTest(Joiner.on("\n").join(
        "class Object { }",
        "class B { }",
        "class A {",
        "  var val; ",
        "  B.foo() : this.val = 1;",
        "}"),
        ResolverErrorCode.CANNOT_DECLARE_NON_FACTORY_CONSTRUCTOR);
  }

  public void testBadGenerativeConstructor2() {
    resolveAndTest(Joiner.on("\n").join(
        "class Object { }",
        "class A {",
        "  var val; ",
        "  A.foo.bar() : this.val = 1;",
        "}"),
        ResolverErrorCode.TOO_MANY_QUALIFIERS_FOR_METHOD);
  }

  public void testBadGenerativeConstructor3() {
    resolveAndTest(Joiner.on("\n").join(
        "class Object { }",
        "interface B { }",
        "class A extends B {",
        "  var val; ",
        "  B.foo() : this.val = 1;",
        "}"),
        ResolverErrorCode.NOT_A_CLASS,
        ResolverErrorCode.CANNOT_DECLARE_NON_FACTORY_CONSTRUCTOR);
  }

  public void testGenerativeConstructor() {
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "class A {",
        "  var val; ",
        "  A.foo(arg) : this.val = arg;",
        "}"));
  }

  /**
   * Test that a class may implement the implied interface of another class and that interfaces may
   * extend the implied interface of a class.
   */
  public void testImpliedInterfaces() throws Exception {
    DartClass a = makeClass("A", null);
    DartClass b = makeClass("B", null, makeTypes("A"));
    DartClass ia = makeInterface("IA", makeTypes("B"), null);
    Scope libScope = resolve(makeUnit(object, a, b, ia), getContext());
    ErrorCode[] expected = {};
    checkExpectedErrors(expected);

    ClassElement elementA = findElementOrFail(libScope, "A");
    ClassElement elementB = findElementOrFail(libScope, "B");
    ClassElement elementIA = findElementOrFail(libScope, "IA");
    List<InterfaceType> superTypes = elementB.getAllSupertypes();
    assertEquals(2, superTypes.size()); // Object and A
    superTypes = elementIA.getAllSupertypes();
    assertEquals(3, superTypes.size()); // Object, A, and B
  }

  public void testUnresolvedSuper() {
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "class Foo {",
        "  foo() { super.foo(); }",
        "}"));
  }

  /**
   * Tests for the 'new' keyword
   */
  public void testNewExpression1() {
    // A very ordinary new expression is OK
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "class Foo {",
        "  Foo create() {",
        "    return new Foo();",
        "  }",
        "}"));
  }

  public void testNewExpression2() {
    // A  new expression with generic type argument is OK
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "class Foo<T> {",
        "  Foo<T> create() {",
        "    return new Foo<T>();",
        "  }",
        "}"));
  }

  public void testNewExpression3() {
    // Trying new on a variable name shouldn't work.
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "class Foo {",
        "  var Bar;",
        "  create() { return new Bar();}",
        "}"),
        TypeErrorCode.NOT_A_TYPE,
        ResolverErrorCode.NO_SUCH_TYPE,
        ResolverErrorCode.NEW_EXPRESSION_NOT_CONSTRUCTOR);
  }

  public void testNewExpression4() {
    // New expression tied to an unbound type variable is not allowed.
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "class Foo<T> {",
        "  T create() {",
        "    return new T();",
        "  }",
        "}"),
        ResolverErrorCode.NEW_EXPRESSION_CANT_USE_TYPE_VAR);
  }

  public void testNewExpression5() {
    // More cowbell. (Foo<T> isn't a type yet)
    resolveAndTest(Joiner.on("\n").join(
      "class Object {}",
      "class Foo<T> { }",
      "class B {",
      "  foo() { return new Foo<T>(); }",
      "}"),
      ResolverErrorCode.NO_SUCH_TYPE);
  }

  public void testNewExpression6() {
    resolveAndTest(Joiner.on("\n").join(
      "class Object {}",
      "interface int {}",
      "interface A<T> default B<T> {",
      "  A.construct(); ",
      "}",
      "class B<T> implements A<T> {",
      "  B() { }",
      "  factory B.construct() { return new B<T>(); }",
      "}"));
  }

  public void test_noSuchType_field() throws Exception {
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "class MyClass {",
        "  Unknown field;",
        "}"),
        TypeErrorCode.NO_SUCH_TYPE);
  }

  public void test_variableStatement_noSuchType() throws Exception {
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "class MyClass {",
        "  foo() {",
        "    Unknown bar;",
        "  }",
        "}"),
        TypeErrorCode.NO_SUCH_TYPE);
  }

  public void test_variableStatement_noSuchType_typeArgument() throws Exception {
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "class Foo<T> {}",
        "class MyClass {",
        "  bar() {",
        "    Foo<Unknown> foo;",
        "  }",
        "}"),
        TypeErrorCode.NO_SUCH_TYPE);
  }

  public void test_variableStatement_wrongTypeArgumentsNumber() throws Exception {
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "class Foo<T> {}",
        "class MyClass {",
        "  bar() {",
        "    Foo<Object, Object> foo;",
        "  }",
        "}"),
        TypeErrorCode.WRONG_NUMBER_OF_TYPE_ARGUMENTS);
  }

  public void test_variableStatement_typeArgumentsForNonGeneric() throws Exception {
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "class Foo {}",
        "class MyClass {",
        "  bar() {",
        "    Foo<Object> foo;",
        "  }",
        "}"),
        TypeErrorCode.WRONG_NUMBER_OF_TYPE_ARGUMENTS);
  }

  public void test_noSuchType_classExtends() throws Exception {
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "class MyClass extends Unknown {",
        "}"),
        ResolverErrorCode.NO_SUCH_TYPE);
  }

  public void test_noSuchType_classExtendsTypeVariable() throws Exception {
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "class MyClass<E> extends E {",
        "}"),
        ResolverErrorCode.NOT_A_CLASS);
  }

  public void test_noSuchType_superClass_typeArgument() throws Exception {
    String source =
        Joiner.on("\n").join(
            "class Object {}",
            "class Base<T> {}",
            "class MyClass extends Base<Unknown> {",
            "}");
    List<DartCompilationError> errors = resolveAndTest(source, ResolverErrorCode.NO_SUCH_TYPE);
    assertEquals(1, errors.size());
    {
      DartCompilationError error = errors.get(0);
      assertEquals(3, error.getLineNumber());
      assertEquals(28, error.getColumnNumber());
      assertEquals("Unknown".length(), error.getLength());
      assertEquals(source.indexOf("Unknown"), error.getStartPosition());
    }
  }

  public void test_noSuchType_superInterface_typeArgument() throws Exception {
    String source =
        Joiner.on("\n").join(
            "class Object {}",
            "interface Base<T> {}",
            "class MyClass implements Base<Unknown> {",
            "}");
    List<DartCompilationError> errors = resolveAndTest(source, ResolverErrorCode.NO_SUCH_TYPE);
    assertEquals(1, errors.size());
    {
      DartCompilationError error = errors.get(0);
      assertEquals(3, error.getLineNumber());
      assertEquals(31, error.getColumnNumber());
      assertEquals("Unknown".length(), error.getLength());
      assertEquals(source.indexOf("Unknown"), error.getStartPosition());
    }
  }

  public void test_noSuchType_methodParameterType() throws Exception {
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "class MyClass {",
        "  Object foo(Unknown p) {",
        "    return null;",
        "  }",
        "}"),
        TypeErrorCode.NO_SUCH_TYPE);
  }

  public void test_noSuchType_methodParameterType_noQualifier() throws Exception {
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "class MyClass {",
        "  Object foo(lib.Unknown p) {",
        "    return null;",
        "  }",
        "}"),
        TypeErrorCode.NO_SUCH_TYPE);
  }

  public void test_noSuchType_returnType() throws Exception {
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "class MyClass {",
        "  Unknown foo() {",
        "    return null;",
        "  }",
        "}"),
        TypeErrorCode.NO_SUCH_TYPE);
  }

  public void test_noSuchType_inExpression() throws Exception {
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "class MyClass {",
        "  foo() {",
        "    var bar;",
        "    if (bar is Bar) {",
        "    }",
        "  }",
        "}"),
        ResolverErrorCode.NO_SUCH_TYPE);
  }

  public void test_noSuchType_inCatch() throws Exception {
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "class MyClass {",
        "  foo() {",
        "    try {",
        "    } catch (Unknown e) {",
        "    }",
        "  }",
        "}"),
        ResolverErrorCode.NO_SUCH_TYPE);
  }

  public void test_new_noSuchType() throws Exception {
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "class MyClass {",
        "  foo() {",
        "    new Unknown();",
        "  }",
        "}"),
        ResolverErrorCode.NO_SUCH_TYPE,
        ResolverErrorCode.NEW_EXPRESSION_NOT_CONSTRUCTOR);
  }

  public void test_new_noSuchType_typeArgument() throws Exception {
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "class Foo<T> {}",
        "class MyClass {",
        "  foo() {",
        "    new Foo<T>();",
        "  }",
        "}"),
        ResolverErrorCode.NO_SUCH_TYPE);
  }

  public void test_new_wrongTypeArgumentsNumber() throws Exception {
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "class Foo<T> {}",
        "class MyClass {",
        "  foo() {",
        "    new Foo<Object, Object>();",
        "  }",
        "}"),
        ResolverErrorCode.WRONG_NUMBER_OF_TYPE_ARGUMENTS);
  }

  public void test_noSuchType_mapLiteral_typeArgument() throws Exception {
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "class String {}",
        "class MyClass {",
        "  foo() {",
        "    var map = <T>{};",
        "  }",
        "}"),
        ResolverErrorCode.NO_SUCH_TYPE);
  }

  public void test_noSuchType_mapLiteral_num_type_args() throws Exception {
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "interface int {}",
        "interface String {}",
        "class MyClass {",
        "  foo() {",
        "    var map0 = {};",
        "    var map1 = <int>{'foo': 1};",
        "    var map2 = <String, int>{'foo' : 1};",
        "    var map3 = <String, int, int>{'foo' : 1};",
        "  }",
        "}"),
        ResolverErrorCode.DEPRECATED_MAP_LITERAL_SYNTAX,
        ResolverErrorCode.WRONG_NUMBER_OF_TYPE_ARGUMENTS);
  }

  public void test_noSuchType_arrayLiteral_typeArgument() throws Exception {
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "class MyClass {",
        "  foo() {",
        "    var map = <T>[null];",
        "  }",
        "}"),
        ResolverErrorCode.NO_SUCH_TYPE);
  }

  /**
   * When {@link SupertypeResolver} can not find "UnknownA", it uses {@link DynamicType}, which
   * returns {@link DynamicElement}. By itself, this is OK. However when we later try to resolve
   * second unknown type "UnknownB", we expect in {@link Elements#findElement()} specific
   * {@link ClassElement} implementation and {@link DynamicElement} is not valid.
   */
  public void test_classExtendsUnknown_fieldUnknownType() throws Exception {
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "class MyClass extends UnknownA {",
        "  UnknownB field;",
        "}"),
        ResolverErrorCode.NO_SUCH_TYPE,
        TypeErrorCode.NO_SUCH_TYPE);
  }

  /**
   * When {@link SupertypeResolver} can not find "UnknownA", it uses {@link DynamicType}, which
   * returns {@link DynamicElement}. By itself, this is OK. However when we later try to resolve
   * super() constructor invocation, this should not cause exception.
   */
  public void test_classExtendsUnknown_callSuperConstructor() throws Exception {
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "class MyClass extends UnknownA {",
        "  MyClass() : super() {",
        "  }",
        "}"),
        ResolverErrorCode.NO_SUCH_TYPE,
        ResolverErrorCode.CANNOT_RESOLVE_SUPER_CONSTRUCTOR);
  }

  public void test_shadowType_withVariable() throws Exception {
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "class Foo<T> {}",
        "class Param {}",
        "class MyClass {",
        "  foo() {",
        "    var Param;",
        "    new Foo<Param>();",
        "  }",
        "}"),
        ResolverErrorCode.DUPLICATE_LOCAL_VARIABLE_WARNING,
        TypeErrorCode.NOT_A_TYPE,
        ResolverErrorCode.NO_SUCH_TYPE);
  }

  public void test_operatorIs_withFunctionAlias() throws Exception {
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "class int {}",
        "typedef Dynamic F1<T>(Dynamic x, T y);",
        "class MyClass {",
        "  main() {",
        "    F1<int> f1 = (Object o, int i) => null;",
        "    if (f1 is F1<int>) {",
        "    }",
        "  }",
        "}"));
  }

  public void testTypeVariableInStatic() {
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "class A<T> {",
        "  static foo() { new T(); }", // can't ref type variable in method
        "}"),
        ResolverErrorCode.TYPE_VARIABLE_IN_STATIC_CONTEXT,
        ResolverErrorCode.NEW_EXPRESSION_NOT_CONSTRUCTOR);
  }

  public void testTypeVariableShadowsClass() {
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "class T {}",
        "class A<T> {",  // type var T shadows class T
        "  static foo() { new T(); }", // should resolve to class T
        "}"),
        ResolverErrorCode.DUPLICATE_TYPE_VARIABLE_WARNING);
  }
  
  public void testConstClass() {
    resolveAndTest(Joiner.on("\n").join(
        "class Object {}",
        "interface int {}",
        "class GoodBase {",
        "  const GoodBase() : foo = 1;",
        "  final foo;",
        "}",
        "class BadBase {",
        "  BadBase() {}",
        "  var foo;",
        "}",                                       // line 10
        "class Bad {", 
        "  const Bad() : bar = 1;",               
        "  var bar;", // error: non-final field in const class
        "}",
        "class BadSub1 extends BadBase {",
        "  const BadSub1() : super(),  bar = 1;", // error2: inherits non-final field, super !const
        "  final bar;",
        "}", 
        "class BadSub2 extends GoodBase {",
        "  const BadSub2() : super(),  bar = 1;", // line 20
        "  var bar;",                             // error: non-final field in constant class
        "}",
        "class GoodSub1 extends GoodBase {",
        "  const GoodSub1() : super(),  bar = 1;",
        "  final bar;",
        "}",
        "class GoodSub2 extends GoodBase {",
        "  const GoodSub2() : super();",
        "  static int bar;",                      // OK, non-final but it is static
        "}"),
        errEx(ResolverErrorCode.CONST_CLASS_WITH_NONFINAL_FIELDS, 13, 7, 3),
        errEx(ResolverErrorCode.CONST_CONSTRUCTOR_MUST_CALL_CONST_SUPER, 16, 9, 7),
        errEx(ResolverErrorCode.CONST_CLASS_WITH_INHERITED_NONFINAL_FIELDS, 9, 7, 3),
        errEx(ResolverErrorCode.CONST_CLASS_WITH_NONFINAL_FIELDS, 21, 7, 3));
  }
}
