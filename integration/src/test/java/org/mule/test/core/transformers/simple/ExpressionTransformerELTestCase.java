/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.transformers.simple;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.functional.junit4.TestLegacyMessageBuilder;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.FruitBasket;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpressionTransformerELTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/transformers/expression-transformers-el-test.xml";
  }

  @Test
  public void testExecutionWithCorrectMessage() throws Exception {
    testExecutionWithCorrectMessage("testTransformer");
  }

  private void testExecutionWithCorrectMessage(String name) throws Exception {
    Transformer transformer = registry.<Transformer>lookupByName(name).get();
    Map<String, Serializable> props = new HashMap<>();
    props.put("foo", "moo");
    props.put("bar", "mar");

    Message message =
        new TestLegacyMessageBuilder().value(new FruitBowl(new Apple(), new Banana())).outboundProperties(props).build();

    Object result = transformer.transform(message);
    assertNotNull(result);
    assertTrue(result.getClass().isArray());
    Object o1 = ((Object[]) result)[0];
    assertTrue(o1 instanceof FruitBasket);

    Object o2 = ((Object[]) result)[1];
    assertTrue(o2 instanceof Map<?, ?>);
    Map<?, ?> map = (Map<?, ?>) o2;
    assertEquals(2, map.size());
    assertEquals("moo", map.get("foo"));
    assertEquals("mar", map.get("bar"));
  }

  @Test
  public void testExecutionWithPartialMissingOptionalParams() throws Exception {
    Transformer transformer = registry.<Transformer>lookupByName("testTransformer").get();
    Map<String, Serializable> props = new HashMap<>();
    props.put("foo", "moo");

    Message message =
        new TestLegacyMessageBuilder().value(new FruitBowl(new Apple(), new Banana())).outboundProperties(props).build();

    Object result = transformer.transform(message);
    assertNotNull(result);
    assertTrue(result.getClass().isArray());
    Object o1 = ((Object[]) result)[0];
    assertTrue(o1 instanceof FruitBasket);

    Object o2 = ((Object[]) result)[1];
    assertTrue(o2 instanceof Map<?, ?>);
    Map<?, ?> map = (Map<?, ?>) o2;
    assertEquals(2, map.size());
    assertEquals("moo", map.get("foo"));
  }

  @Test
  public void testTransformerConfigWithSingleArgument() throws Exception {
    Transformer transformer = registry.<Transformer>lookupByName("testTransformer2").get();
    Map<String, Serializable> props = new HashMap<>();
    props.put("foo", "moo");
    props.put("bar", "mar");

    Message message =
        new TestLegacyMessageBuilder().value(new FruitBowl(new Apple(), new Banana())).outboundProperties(props).build();

    Object result = transformer.transform(message);
    assertNotNull(result);
    assertFalse(result.getClass().isArray());
    assertTrue(result instanceof List<?>);
    List<?> list = (List<?>) result;
    assertTrue(list.contains("moo"));
    assertTrue(list.contains("mar"));
  }

  @Test
  public void testTransformerConfigWithSingleArgumentShortcutConfig() throws Exception {
    testTransformerConfigWithSingleArgumentShortcutConfig("testTransformer4");
  }

  private void testTransformerConfigWithSingleArgumentShortcutConfig(String name) throws Exception {
    Transformer transformer = registry.<Transformer>lookupByName(name).get();
    Map<String, Serializable> props = new HashMap<>();
    props.put("foo", "moo");
    props.put("bar", "mar");

    Message message =
        new TestLegacyMessageBuilder().value(new FruitBowl(new Apple(), new Banana())).outboundProperties(props).build();

    Object result = transformer.transform(message);
    assertNotNull(result);
    assertFalse(result.getClass().isArray());
    assertTrue(result instanceof List<?>);
    List<?> list = (List<?>) result;
    assertTrue(list.contains("moo"));
    assertTrue(list.contains("mar"));
  }

}
