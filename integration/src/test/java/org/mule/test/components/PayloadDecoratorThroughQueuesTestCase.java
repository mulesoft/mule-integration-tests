/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_ENABLE_STATISTICS;
import static org.mule.test.allure.AllureConstants.SerializationFeature.SERIALIZATION;
import static org.mule.test.allure.AllureConstants.SerializationFeature.SerializationStory.STATISTICS;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Vector;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mule.functional.api.component.TestConnectorQueueHandler;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.management.stats.PayloadStatistics;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(SERIALIZATION)
@Story(STATISTICS)
@RunnerDelegateTo(Parameterized.class)
public class PayloadDecoratorThroughQueuesTestCase extends AbstractIntegrationTestCase {

  @Rule
  public SystemProperty withStatistics = new SystemProperty(MULE_ENABLE_STATISTICS, "true");

  private TestConnectorQueueHandler queueHandler;

  private Object payload;

  @Parameters
  public static Collection<Object[]> data() {
    return asList(new Object[][] {{asList("cat", "cow", "dog")},
        {new HashSet(asList("cat", "cow", "dog"))},
        {new Vector(asList("cat", "cow", "dog"))}
    });
  }

  @Before
  public void before() throws IOException {
    queueHandler = new TestConnectorQueueHandler(registry);
  }

  public PayloadDecoratorThroughQueuesTestCase(Object payload) {
    this.payload = payload;
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/test/components/payload-through-vm-config.xml";
  }

  @Test
  @Issue("MULE-18894")
  @Description("Assert statistics for a component that serializes the payload")
  public void testPayload() throws Exception {
    sendPayload("publishConsumeThroughVM", payload);
    assertThat(queueHandler.read("processed", RECEIVE_TIMEOUT), notNullValue());
    final PayloadStatistics stats =
        muleContext.getStatistics().getPayloadStatistics("publishConsumeThroughVM/processors/0");

    assertThat(stats.getComponentIdentifier(), is("vm:publish-consume"));

    assertThat(stats.getInvocationCount(), is(1L));

    assertThat(stats.getInputObjectCount(), is(3L));
    assertThat(stats.getInputByteCount(), is(0L));
    assertThat(stats.getOutputObjectCount(), is(0L));
  }

  private void sendPayload(String flowName, Object payload) throws Exception {
    flowRunner(flowName)
        .withPayload(payload)
        .withMediaType(MediaType.APPLICATION_JAVA)
        .run();
  }

}
