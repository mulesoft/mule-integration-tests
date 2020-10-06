/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Ignore;
import org.junit.Test;

public class JndiFunctionalTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/spring/jndi-functional-test.xml";
  }

  @Test
  @Ignore("MULE-17628")
  public void testJndi() {
    Object obj;

    obj = registry.lookupByName("apple").get();
    assertNotNull(obj);
    assertEquals(Apple.class, obj.getClass());

    obj = registry.lookupByName("orange").get();
    assertNotNull(obj);
    assertEquals(Orange.class, obj.getClass());
    assertEquals(new Integer(8), ((Orange) obj).getSegments());
    assertEquals("Florida Sunny", ((Orange) obj).getBrand());
  }
}


