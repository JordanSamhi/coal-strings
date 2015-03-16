/*
 * Copyright (C) 2015 The University of Wisconsin and the Pennsylvania State University
 *
 * Author: Damien Octeau
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.psu.cse.siis.coal.arguments.cases;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class StringProblems {
  public String myStringField;
  public static final String myNullString = null;
  public int myIntField = 10;
  public static String staticField;
  public static String myUninitializedField;
  public String myUninitializedInstanceField;
  public static String myConcatenatedStringField;
  public static OtherClass otherClass;

  static {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("FIRST_");
    stringBuilder.append("SECOND");
    myConcatenatedStringField = stringBuilder.toString();
  }

  public void modifyMyStringField() {
    myStringField = "STRING_FIELD";
  }

  private class MyClass {
    private String type = "MyType";

    public void setType(String type) {
      this.type = type;
    }

    public String getType() {
      return type;
    }
  }

  /**
   * Method showing what makes the current analysis fail.
   * 
   * Each one of the cases in the switch would make the current analysis fail.
   * 
   * @param myInt
   *          An integer
   * @return A string.
   */
  public String makeString(int myInt, String testString) {
    switch (myInt) {
    // Cases 1, 2 and 3 seem to be the most common, from the few cases we looked at.
      case 1:
        modifyMyStringField();
        return myStringField;
      case 2:
        return "CONCA" + "TENATION";
      case 3:
        String[] myStringArray = { "ARRAY0", "ARRAY1", "ARRAY2" };
        return myStringArray[1];
      case 4:
        return myNullString;
      case 5:
        return testString + "InterproceduralSuffix";
      case 6:
        MyClass myClass = new MyClass();
        MyClass myClass2 = new MyClass();
        myClass2.setType(myClass.getType());
        return myClass2.getType();
      case 7:
        return "TestInt" + 5;
      case 8:
        // Int field example.
        return "TestIntField" + myIntField;
      case 9:
        // The value of the int is unknowable statically, but we at
        // least know that it is ([0-9]*).
        return "TestUnknowableInt" + this.hashCode();
      case 10:
        List<String> strings = new ArrayList<String>();
        strings.add("ListString1");
        strings.add("ListString2");
        return strings.get(0);
      case 11:
        Set<String> strings2 = new HashSet<String>();
        strings2.add("SetString1");
        strings2.add("SetString2");
        strings2.add("SetString3");
        Iterator<String> iterator = strings2.iterator();
        return iterator.next();
      default:
        // Not found in the wild in the few cases we looked at, but perfectly possible:
        // StringBuilder myComplexString = new StringBuilder("COMPLEX_STRING");
        // myComplexString.setCharAt(7, '-');
        // return myComplexString.toString();
        return null;
        // Not represented here, but also found in the wild:
        // - Cases where a string comes from a native method.
        // - Cases where a number of complex operations are performed on a string, for example
        // decoding from Base 64.
    }
  }

  public static void main(String[] args) {
    String outputString;
    if (args.length != 2) {
      outputString = "Missing command line argument.";
    }
    int intArgument = Integer.valueOf(args[0]);
    if (intArgument > 0) {
      // Here the current analysis would fail.
      StringProblems stringProblems = new StringProblems();
      outputString = stringProblems.makeString(intArgument, "Prefix");
    } else {
      // Here the current analysis would succeed.
      outputString = "CONSTANT_STRING";
    }

    System.out.println(outputString);

    testAppend();
    staticField = new StringProblems().toString() + "APPENDED_STRING";
    testCallChaining();
    testArgument("TEST_ARGUMENT");
    testUninitializedInstanceField();
    testSimpleStringFieldInOtherClass();
    testAppendToInt(0);
    otherClass = new OtherClass("ARGUMENT_FIELD_VALUE");
    testArgumentField();
    testStringArrayField();
    testMergePaths(intArgument);
  }

  public static void testAppend() {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("TEST_APPEND");
    System.out.println(stringBuilder.toString());
  }

  public static void testCallChaining() {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("CALL_").append("CHAIN");
    System.out.println(stringBuilder.toString());
  }

  public static void testArgument(String argString) {
    System.out.println(argString);
  }

  public static void testException() {
    System.out.println("test" + testExceptionHelper());
  }

  public static String testExceptionHelper() {
    throw new RuntimeException();
  }

  public static void testUninitializedField() {
    System.out.println(myUninitializedField);
  }

  public static void testUninitializedInstanceField() {
    System.out.println(new StringProblems().myUninitializedInstanceField);
  }

  public static void testConcatenatedStringField() {
    System.out.println(myConcatenatedStringField);
  }

  public static void testConcatenatedStringFieldInOtherClass() {
    OtherClass.getAppId();
    System.out.println(OtherClass.staticField);
  }

  public static void testSimpleStringFieldInOtherClass() {
    OtherClass.getAppId();
    System.out.println(OtherClass.simpleStaticField);
  }

  public static void testAppendToInt(int myInt) {
    OtherClass.getAppId();
    String string = OtherClass.simpleStaticField + myInt;
    System.out.println(string);
  }

  public static void testArgumentField() {
    System.out.println(otherClass.argumentField);
  }

  public static void testStringArrayField() {
    String string = OtherClass.myStringArrayField[0] + "MY_STRING_ARRAY_SUFFIX";
    System.out.println(string);
  }

  public static void testMergePaths(int intArgument) {
    StringBuilder prefix;
    String suffix;
    if (intArgument == 0) {
      StringBuilder stringBuilder = new StringBuilder();
      String interm = StringProblems.class.toString();
      prefix = stringBuilder.append(interm);
      suffix = "suffix2";
    } else {
      prefix = new StringBuilder("test");
      suffix = "suffix1";
    }
    StringBuilder stringBuilder2 = prefix.append(suffix);

    System.out.println(stringBuilder2.toString());
  }
}
