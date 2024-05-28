/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components.metrics;

import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;

import org.junit.Test;

public class OpenTelemetryErrorCountersTestCase extends AbstractOpenTelemetryMetricsTestCase {

  private static final int TIMEOUT_MILLIS = 30000;
  private static final int POLL_DELAY_MILLIS = 100;
  public static final String EXPECTED_METRIC_NAME = "error-count-97abc3d9";
  public static final String EXPECTED_METRIC_DESCRIPTION = "Mule runtime error count";
  public static final String EXPECTED_RESOURCE_NAME = "OpenTelemetryErrorCountersTestCase#test";
  public static final String EXPECTED_INSTRUMENTATION_NAME = "flow.construct.statistics";

  @Test
  public void test() throws Exception {
    flowRunner("simple-flow").withPayload(TEST_PAYLOAD).run().getMessage();
    PollingProber probe = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);
    probe.check(new JUnitProbe() {

      @Override
      protected boolean test() throws Exception {
        try {
          verifyMetricsExists(EXPECTED_METRIC_NAME, EXPECTED_METRIC_DESCRIPTION, EXPECTED_RESOURCE_NAME,
                              EXPECTED_INSTRUMENTATION_NAME, 1, server.getMetrics());
        } catch (Throwable e) {
          return false;
        }
        return true;
      }

      @Override
      public String describeFailure() {
        return "Error on verifying error counters metric: " + getShowStatsInfo();
      }

      private String getShowStatsInfo() {
        StringBuffer statsInfo = new StringBuffer();
        server.getMetrics().forEach(metric -> statsInfo.append(metric.getName()).append(": ").append(metric.getValue())
            .append(System.lineSeparator()));
        return statsInfo.toString();
      }
    });
  }

  @Override
  protected String getConfigFile() {
    return "metrics/error-counters-metric.xml";
  }
}
