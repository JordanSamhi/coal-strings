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

public class OtherClass {
  public static String staticField;
  public static String simpleStaticField;
  public String argumentField;
  public static String[] myStringArrayField = new String[5];

  static {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(getAppId()).append("OTHER_APPENDED_STRING");
    staticField = stringBuilder.toString();
  }

  public OtherClass() {
  }

  public OtherClass(String argumentField) {
    setArgumentField(argumentField);
    myStringArrayField[3] = "MY_STRING_ARRAY_PREFIX_";
  }

  public static String getAppId() {
    simpleStaticField = "MY_SIMPLE_STATIC_STRING";
    return new OtherClass().toString();
  }

  public void setArgumentField(String value) {
    argumentField = value;
  }
}
