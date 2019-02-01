/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.streaming;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mule.tck.probe.PollingProber.check;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.api.streaming.StreamingStatistics;
import org.mule.runtime.core.internal.streaming.NullStreamingStatistics;
import org.mule.test.AbstractIntegrationTestCase;

import javax.inject.Inject;

import org.junit.Test;

public class AutoCloseCursorProviderTestCase extends AbstractIntegrationTestCase {

  public static class AssertStatisticsProcessor implements Processor {

    public static StreamingStatistics statistics;

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      check(10000, 100, () -> {
        System.gc();

        assertThat(statistics, not(instanceOf(NullStreamingStatistics.class)));
        assertThat(statistics.getOpenCursorProvidersCount(), is(0));
        assertThat(statistics.getOpenCursorsCount(), is(0));

        return true;
      });

      return event;
    }
  }

  public static class SpyProcessor implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      System.out.println("payload: " + event.getVariables().get("spy").getValue());
      System.out.println("Open providers: " + AssertStatisticsProcessor.statistics.getOpenCursorProvidersCount());

      return event;
    }
  }

  @Inject
  private StreamingManager streamingManager;

  @Override
  protected String getConfigFile() {
    return "org/mule/streaming/auto-close-cursor-provider-config.xml";
  }

  @Test
  public void openManyStreamsInForeachAndDiscard() throws Exception {
    AssertStatisticsProcessor.statistics = streamingManager.getStreamingStatistics();
    try {
      flowRunner("openManyStreamsInForeachAndDiscard").run();
    } finally {
      AssertStatisticsProcessor.statistics = null;
    }
  }
}
