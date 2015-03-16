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
package edu.psu.cse.siis.coal.arguments;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.Transform;
import soot.options.Options;

/**
 * Tests for string analysis. To add a test case, add a method in cases.StringProblems and add a
 * test method below.
 */
public class StringAnalysisTest {
  private static final boolean USE_SHIMPLE = false;
  private static Map<String, Set<Object>> result;

  @BeforeClass
  public static void setUp() {
    Options.v().setPhaseOption("cg.spark", "on");
    Options.v().setPhaseOption("jb.ulp", "off");
    Options.v().set_output_format(Options.output_format_none);
    Options.v().set_prepend_classpath(true);
    // Options.v().set_no_bodies_for_excluded(true);
    Options.v().set_allow_phantom_refs(true);
    Options.v().set_whole_program(true);
    Options.v().set_soot_classpath(StringAnalysisTest.class.getResource("/").getPath());
    Options.v().set_process_dir(
        Arrays.asList("edu.psu.cse.siis.coal.arguments.cases.StringProblems",
            "edu.psu.cse.siis.coal.arguments.cases.OtherClass"));

    SootClass class1 =
        Scene.v().forceResolve("edu.psu.cse.siis.coal.arguments.cases.StringProblems",
            SootClass.BODIES);
    class1.setApplicationClass();
    Scene.v().loadNecessaryClasses();
    Scene.v().setEntryPoints(Collections.singletonList(class1.getMethodByName("main")));

    if (USE_SHIMPLE) {
      Options.v().set_via_shimple(true);
      Options.v().set_whole_shimple(true);
    }
    String pack = USE_SHIMPLE ? "wstp" : "wjtp";
    PackManager.v().getPack(pack)
        .add(new Transform(pack + ".ExampleStringAnalyzer", new CustomSceneTransformer()));

    PackManager.v().runPacks();

    result = CustomSceneTransformer.result;
  }

  @Test
  public void testArgumentField() {
    assertEquals(Collections.singleton("ARGUMENT_FIELD_VALUE"), result.get("testArgumentField"));
  }

  @Test
  public void testAppend() {
    assertEquals(Collections.singleton("TEST_APPEND"), result.get("testAppend"));
  }

  @Test
  public void testCallChaining() {
    assertEquals(Collections.singleton("CALL_CHAIN"), result.get("testCallChaining"));
  }

  @Test
  public void testArgument() {
    assertEquals(Collections.singleton("TEST_ARGUMENT"), result.get("testArgument"));
  }

  @Test
  public void testException() {
    assertEquals(Collections.singleton("test(.*)"), result.get("testException"));
  }

  @Test
  public void testUninitializedField() {
    assertEquals(Collections.singleton("NULL-CONSTANT"), result.get("testUninitializedField"));
  }

  @Test
  public void testUninitializedInstanceField() {
    assertEquals(Collections.singleton("NULL-CONSTANT"),
        result.get("testUninitializedInstanceField"));
  }

  @Test
  public void testConcatenatedStringField() {
    assertEquals(Collections.singleton("FIRST_SECOND"), result.get("testConcatenatedStringField"));
  }

  @Test
  public void testConcatenatedStringFieldInOtherClass() {
    assertEquals(Collections.singleton("(.*)OTHER_APPENDED_STRING"),
        result.get("testConcatenatedStringFieldInOtherClass"));
  }

  @Test
  public void testSimpleStringFieldInOtherClass() {
    assertEquals(Collections.singleton("MY_SIMPLE_STATIC_STRING"),
        result.get("testSimpleStringFieldInOtherClass"));
  }

  @Test
  public void testAppendToInt() {
    // The actual int value is not inferred when it is a method argument.
    assertEquals(Collections.singleton("MY_SIMPLE_STATIC_STRING(.*)"),
        result.get("testAppendToInt"));
  }

  @Test
  public void testStringArrayField() {
    assertEquals(Collections.emptySet(), result.get("testStringArrayField"));
  }

  @Test
  public void testMain() {
    assertEquals(
        new HashSet<Object>(Arrays.asList("STRING_FIELD", "CONCATENATION", "ARRAY0", "ARRAY1",
            "ARRAY2", "PrefixInterproceduralSuffix", "MyType", "TestInt5", "TestIntField(.*)",
            "TestUnknowableInt(.*)", "(.*)", "NULL-CONSTANT", "CONSTANT_STRING")),
        result.get("main"));
  }
}
