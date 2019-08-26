/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.transformers;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertTrue;

import org.mule.functional.api.component.EventCallback;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.FruitBasket;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class AutoTransformerTestCase extends AbstractIntegrationTestCase {

  private static Latch latch;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/transformer/auto-transformer-test-flow.xml";
  }

  @Test
  public void testInboundAutoTransform() throws Exception {
    latch = new Latch();
    flowRunner("test").withPayload(new FruitBowl(new Apple(), new Banana())).run();

    assertTrue(latch.await(3000, MILLISECONDS));
  }

  public static class FruitBasketCallback extends AbstractComponent implements EventCallback {

    @Override
    public void eventReceived(CoreEvent event, Object component, MuleContext muleContext) throws Exception {
      assertTrue(((FruitBasket) event.getMessage().getPayload().getValue()).hasApple());
      assertTrue(((FruitBasket) event.getMessage().getPayload().getValue()).hasBanana());
      latch.countDown();
    }
  }
}
