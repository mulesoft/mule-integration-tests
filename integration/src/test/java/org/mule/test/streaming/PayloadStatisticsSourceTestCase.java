/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.streaming;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_DISABLE_PAYLOAD_STATISTICS;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_ENABLE_STATISTICS;
import static org.mule.test.allure.AllureConstants.StreamingFeature.STREAMING;
import static org.mule.test.allure.AllureConstants.StreamingFeature.StreamingStory.STATISTICS;

import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.management.stats.PayloadStatistics;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.tests.api.TestQueueManager;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(STREAMING)
@Story(STATISTICS)
public class PayloadStatisticsSourceTestCase extends AbstractIntegrationTestCase {

  public static final int BYTES_SIZE = 1343;

  public static final int MUTANT_SUMMON_BYTE_SIZE = 84;

  public SystemProperty bytesSize = new SystemProperty("bytesSize", "" + BYTES_SIZE);

  @Rule
  public SystemProperty withStatistics = new SystemProperty(MULE_ENABLE_STATISTICS, "true");

  @Rule
  public SystemProperty withPayloadStatistics = new SystemProperty(MULE_DISABLE_PAYLOAD_STATISTICS, "false");

  @Inject
  private TestQueueManager queueManager;

  @Inject
  @Named("streamSource")
  public Flow streamSource;

  @Inject
  @Named("iteratorSource")
  public Flow iteratorSource;

  @Inject
  @Named("iteratorSourceConsumeOnResponse")
  public Flow iteratorSourceConsumeOnResponse;

  @Override
  protected String getConfigFile() {
    return "org/mule/streaming/payload-statistics-source-config.xml";
  }

  @Test
  @Description("Assert statistics for an source that generates an InputStream")
  public void streamSource() throws Exception {
    streamSource.start();

    queueManager.read("streamSourceComplete", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();

    final PayloadStatistics fileListStatistics =
        muleContext.getStatistics().getPayloadStatistics("streamSource/source");

    assertThat(fileListStatistics.getComponentIdentifier(), is("marvel:magneto-mutant-summon"));

    assertThat(fileListStatistics.getInvocationCount(), is(1L));
    new PollingProber().check(new JUnitLambdaProbe(() -> {
      // do not count the container message
      assertThat(fileListStatistics.getOutputObjectCount(), is(0L));
      assertThat(fileListStatistics.getOutputByteCount(), is(MUTANT_SUMMON_BYTE_SIZE * 1L));

      assertThat(fileListStatistics.getInputObjectCount(), is(0L));
      assertThat(fileListStatistics.getInputByteCount(), is(MUTANT_SUMMON_BYTE_SIZE * 1L));
      return true;
    }));
  }

  @Test
  @Description("Assert statistics for a source that returns an Iterator")
  public void iteratorSource() throws Exception {
    iteratorSource.start();

    queueManager.read("iteratorSourceComplete", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();

    final PayloadStatistics fileListStatistics =
        muleContext.getStatistics().getPayloadStatistics("iteratorSource/source");

    assertThat(fileListStatistics.getComponentIdentifier(), is("marvel:magneto-brotherhood"));

    assertThat(fileListStatistics.getInvocationCount(), is(1L));
    new PollingProber().check(new JUnitLambdaProbe(() -> {
      assertThat(fileListStatistics.getOutputObjectCount(), is(6L));
      assertThat(fileListStatistics.getOutputByteCount(), is(0L));

      assertThat(fileListStatistics.getInputObjectCount(), is(0L));
      assertThat(fileListStatistics.getInputByteCount(), is(0L));
      return true;
    }));
  }

  @Test
  @Description("Assert statistics for a source that consumes an iterator on response")
  public void iteratorSourceConsumeElementsOnResponse() throws Exception {
    iteratorSourceConsumeOnResponse.start();

    queueManager.read("iteratorSourceConsumeOnResponseComplete", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();

    final PayloadStatistics fileListStatistics =
        muleContext.getStatistics().getPayloadStatistics("iteratorSourceConsumeOnResponse/source");

    assertThat(fileListStatistics.getComponentIdentifier(), is("marvel:magneto-brotherhood"));

    assertThat(fileListStatistics.getInvocationCount(), is(1L));
    new PollingProber().check(new JUnitLambdaProbe(() -> {
      assertThat(fileListStatistics.getOutputObjectCount(), is(6L));
      assertThat(fileListStatistics.getOutputByteCount(), is(0L));

      assertThat(fileListStatistics.getInputByteCount(), is(0L));
      assertThat(fileListStatistics.getInputObjectCount(), is(6L));
      return true;
    }));
  }

}
