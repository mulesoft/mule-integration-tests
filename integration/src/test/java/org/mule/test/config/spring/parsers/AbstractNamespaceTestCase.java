/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring.parsers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.config.spring.parsers.beans.AbstractBean;

import org.junit.Test;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

public abstract class AbstractNamespaceTestCase extends AbstractIntegrationTestCase {

  private static final Logger LOGGER = getLogger(AbstractNamespaceTestCase.class);

  @Test
  public void testParse() {
    // just parse the config
  }

  protected Object assertBeanExists(String name, Class clazz) {
    Object bean = registry.lookupByName(name).get();
    assertNotNull(name + " bean missing", bean);
    assertTrue(bean.getClass().equals(clazz));
    LOGGER.debug("found bean " + name + "/" + ClassUtils.getSimpleName(bean.getClass()));
    return bean;
  }

  protected Object assertContentExists(Object object, Class clazz) {
    assertNotNull(ClassUtils.getSimpleName(clazz) + " content missing", object);
    assertTrue(clazz.isAssignableFrom(object.getClass()));
    LOGGER.debug("found content " + ClassUtils.getSimpleName(object.getClass()));
    return object;
  }

  protected void assertBeanPopulated(AbstractBean bean, String name) {
    assertMapExists(bean.getMap(), name);
    assertListExists(bean.getList(), name);
    String string = bean.getString();
    assertNotNull("string for " + name, string);
    assertEquals(name + "String", string);
  }

  protected void assertMapExists(Map map, String name) {
    assertNotNull("map for " + name, map);
    assertMapEntryExists(map, name, 1);
    assertMapEntryExists(map, name, 2);
  }

  protected void assertMapEntryExists(Map map, String name, int index) {
    String key = "key" + index;
    Object value = map.get(key);
    assertNotNull(key + " returns null", value);
    assertTrue(value instanceof String);
    assertEquals(name + "Map" + index, value);
  }

  protected void assertListExists(List list, String name) {
    assertNotNull("list for " + name, list);
    assertListEntryExists(list, name, 1);
    assertListEntryExists(list, name, 2);
  }

  protected void assertListEntryExists(List list, String name, int index) {
    String value = name + "List" + index;
    assertTrue(value, list.contains(value));
  }

}
