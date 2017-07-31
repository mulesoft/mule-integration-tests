/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing.outbound;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;

import org.mule.runtime.api.message.Message;
import org.mule.tck.processor.FlowAssert;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.List;

import org.junit.Test;

public class ExpressionSplitterMixedSyncAsyncTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/routing/outbound/expression-splitter-mixed-sync-async-test-flow.xml";
  }

  @Test
  public void testRecipientList() throws Exception {
    FruitBowl fruitBowl = new FruitBowl(new Apple(), new Banana());
    fruitBowl.addFruit(new Orange());

    Message result = flowRunner("Distributor").withPayload(fruitBowl).run().getMessage();

    assertThat((List<Message>) result.getPayload().getValue(),
               containsInAnyOrder(hasPayload(instanceOf(Apple.class)), hasPayload(instanceOf(Orange.class))));

    FlowAssert.verify();
  }
}
