/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.management.stats.AllStatistics;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class ActiveFlowStatisticsTestCase extends AbstractIntegrationTestCase {

  private AllStatistics statistics;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/construct/active-flow-statistics-config.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    statistics = muleContext.getStatistics();
  }

  @Test
  public void initialState() throws Exception {
    assertThat(statistics.getDeclaredPrivateFlows(), is(2));
    assertThat(statistics.getDeclaredTriggerFlows(), is(2));
    assertThat(statistics.getActiveTriggerFlows(), is(1));
    assertThat(statistics.getActivePrivateFlows(), is(1));
  }

  @Test
  public void startPrivateFlow() throws Exception {
    ((Startable) getFlowConstruct("stoppedPrivateFlow")).start();
    assertThat(statistics.getActivePrivateFlows(), is(2));
    assertThat(statistics.getActiveTriggerFlows(), is(1));
  }

  @Test
  public void startTriggerFlow() throws Exception {
    ((Startable) getFlowConstruct("stoppedTriggerFlow")).start();
    assertThat(statistics.getActiveTriggerFlows(), is(2));
    assertThat(statistics.getActivePrivateFlows(), is(1));
  }

  @Test
  public void stopPrivateFlow() throws Exception {
    ((Stoppable) getFlowConstruct("activePrivateFlow")).stop();
    assertThat(statistics.getActivePrivateFlows(), is(0));
    assertThat(statistics.getActiveTriggerFlows(), is(1));
  }

  @Test
  public void stopTriggerFlow() throws Exception {
    ((Stoppable) getFlowConstruct("activeTriggerFlow")).stop();
    assertThat(statistics.getActiveTriggerFlows(), is(0));
    assertThat(statistics.getActivePrivateFlows(), is(1));
  }
}
