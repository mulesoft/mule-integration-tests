/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.test.allure.AllureConstants.XmlSdk.XML_SDK;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.test.functional.model.TestElement;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import io.qameta.allure.Feature;

@Feature(XML_SDK)
public class ModuleUsingJavaModuleTestCase extends AbstractCeXmlExtensionMuleArtifactFunctionalTestCase {

  private static final String JAVA_KEY = "java_key";

  @Override
  protected String getModulePath() {
    return "modules/module-using-java.xml";
  }

  @Override
  protected String getConfigFile() {
    return "flows/flows-with-module-using-java.xml";
  }

  @Test
  public void testInvokeStaticMethod() throws Exception {
    final CoreEvent event = flowRunner("invoke-static-method-flow").run();
    final Object value = event.getMessage().getPayload().getValue();
    assertThat(value, is(instanceOf(Set.class)));
    assertThat(((Set<?>) value), is(empty()));
  }

  @Test
  public void testInvokeMethod() throws Exception {
    final Map<String, String> instance = new HashMap<>();
    instance.put(JAVA_KEY, "javaValue");
    final CoreEvent event = flowRunner("invoke-method-flow")
        .withVariable("instance", instance)
        .withVariable("method", "keySet()")
        .run();
    final Object value = event.getMessage().getPayload().getValue();
    assertThat(value, is(instanceOf(Set.class)));
    assertThat(((Set<String>) value), hasItem(JAVA_KEY));
  }

  @Test
  public void testInvokeMethodWithArgs() throws Exception {
    final Map<String, String> instance = new HashMap<>();
    flowRunner("invoke-method-with-args-flow")
        .withVariable("instance", instance)
        .withVariable("method", "put(Object,Object)")
        .withVariable("args", ImmutableMap.of("arg0", JAVA_KEY, "arg1", "daValue"))
        .run();
    assertThat(instance.entrySet().size(), is(1));
    assertThat(instance.keySet(), hasItem(JAVA_KEY));
  }

  @Test
  public void testNewMethod() throws Exception {
    final CoreEvent event = flowRunner("new-method-flow").run();
    final Object value = event.getMessage().getPayload().getValue();
    assertThat(value, is(instanceOf(Map.class)));
    assertThat(((Map<?, ?>) value).entrySet(), is(empty()));
  }

  @Test
  public void testNewMethodCustomClass() throws Exception {
    final CoreEvent event = flowRunner("new-method-custom-class-flow").run();
    final Object value = event.getMessage().getPayload().getValue();
    assertThat(value, is(instanceOf(TestElement.class)));
  }

  @Test
  public void testInvokeMethodCustomClass() throws Exception {
    final TestElement instance = new TestElement();
    final CoreEvent event = flowRunner("invoke-method-custom-class-flow")
        .withVariable("instance", instance)
        .withVariable("method", "sayHi()")
        .run();
    final Object value = event.getMessage().getPayload().getValue();
    assertThat(value, is(instance.sayHi()));
  }

}
