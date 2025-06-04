/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components.metrics;

import static org.mule.runtime.core.internal.management.stats.DefaultFlowsSummaryStatistics.ACTIVE_APIKIT_FLOWS_APP_DESCRIPTION;
import static org.mule.runtime.core.internal.management.stats.DefaultFlowsSummaryStatistics.ACTIVE_APIKIT_FLOWS_APP_NAME;
import static org.mule.runtime.core.internal.management.stats.DefaultFlowsSummaryStatistics.ACTIVE_PRIVATE_FLOWS_APP_DESCRIPTION;
import static org.mule.runtime.core.internal.management.stats.DefaultFlowsSummaryStatistics.ACTIVE_PRIVATE_FLOWS_APP_NAME;
import static org.mule.runtime.core.internal.management.stats.DefaultFlowsSummaryStatistics.ACTIVE_TRIGGER_FLOWS_DESCRIPTION;
import static org.mule.runtime.core.internal.management.stats.DefaultFlowsSummaryStatistics.ACTIVE_TRIGGER_FLOWS_NAME;
import static org.mule.runtime.core.internal.management.stats.DefaultFlowsSummaryStatistics.DECLARED_APIKIT_FLOWS_APP_DESCRIPTION;
import static org.mule.runtime.core.internal.management.stats.DefaultFlowsSummaryStatistics.DECLARED_APIKIT_FLOWS_APP_NAME;
import static org.mule.runtime.core.internal.management.stats.DefaultFlowsSummaryStatistics.DECLARED_PRIVATE_FLOWS_APP_DESCRIPTION;
import static org.mule.runtime.core.internal.management.stats.DefaultFlowsSummaryStatistics.DECLARED_PRIVATE_FLOWS_APP_NAME;
import static org.mule.runtime.core.internal.management.stats.DefaultFlowsSummaryStatistics.DECLARED_TRIGGER_FLOWS_APP_DESCRIPTION;
import static org.mule.runtime.core.internal.management.stats.DefaultFlowsSummaryStatistics.DECLARED_TRIGGER_FLOWS_APP_NAME;
import static org.mule.runtime.core.internal.management.stats.DefaultFlowsSummaryStatistics.FLOWS_SUMMARY_APP_STATISTICS_NAME;

import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;

import org.junit.Test;


public abstract class AbstractOpenTelemetryFlowStatisticsSummaryTestCase extends AbstractOpenTelemetryMetricsTestCase {

  private static final int TIMEOUT_MILLIS = 30000;
  private static final int POLL_DELAY_MILLIS = 100;

  // TODO W-18668900: swap and remove once the pilot is concluded
  private static final String V2_SUFFIX = "-v2";

  @Test
  public void test() {
    PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);

    prober.check(new JUnitProbe() {

      @Override
      protected boolean test() {
        try {
          verifyMetricsExists(DECLARED_PRIVATE_FLOWS_APP_NAME, DECLARED_PRIVATE_FLOWS_APP_DESCRIPTION, getResourceName(),
                              FLOWS_SUMMARY_APP_STATISTICS_NAME, getExpectedDeclaredPrivateFlows(), server.getMetrics());
          verifyMetricsExists(ACTIVE_PRIVATE_FLOWS_APP_NAME, ACTIVE_PRIVATE_FLOWS_APP_DESCRIPTION, getResourceName(),
                              FLOWS_SUMMARY_APP_STATISTICS_NAME, getExpectedActivePrivateFlows(), server.getMetrics());

          verifyMetricsExists(DECLARED_APIKIT_FLOWS_APP_NAME, DECLARED_APIKIT_FLOWS_APP_DESCRIPTION, getResourceName(),
                              FLOWS_SUMMARY_APP_STATISTICS_NAME, getExpectedDeclaredApikitFlows(), server.getMetrics());
          verifyMetricsExists(ACTIVE_APIKIT_FLOWS_APP_NAME, ACTIVE_APIKIT_FLOWS_APP_DESCRIPTION, getResourceName(),
                              FLOWS_SUMMARY_APP_STATISTICS_NAME, getExpectedActiveApikitFlows(), server.getMetrics());

          verifyMetricsExists(DECLARED_TRIGGER_FLOWS_APP_NAME, DECLARED_TRIGGER_FLOWS_APP_DESCRIPTION, getResourceName(),
                              FLOWS_SUMMARY_APP_STATISTICS_NAME, getExpectedDeclaredTriggerFlows(), server.getMetrics());
          verifyMetricsExists(ACTIVE_TRIGGER_FLOWS_NAME, ACTIVE_TRIGGER_FLOWS_DESCRIPTION, getResourceName(),
                              FLOWS_SUMMARY_APP_STATISTICS_NAME, getExpectedActiveTriggerFlows(), server.getMetrics());

          // TODO W-18668900: swap and remove once the pilot is concluded
          verifyMetricsExists(DECLARED_PRIVATE_FLOWS_APP_NAME + V2_SUFFIX, DECLARED_PRIVATE_FLOWS_APP_DESCRIPTION,
                              getResourceName(),
                              FLOWS_SUMMARY_APP_STATISTICS_NAME, getExpectedDeclaredPrivateFlowsV2(),
                              server.getMetrics());
          verifyMetricsExists(ACTIVE_PRIVATE_FLOWS_APP_NAME + V2_SUFFIX, ACTIVE_PRIVATE_FLOWS_APP_DESCRIPTION,
                              getResourceName(),
                              FLOWS_SUMMARY_APP_STATISTICS_NAME, getExpectedActivePrivateFlowsV2(),
                              server.getMetrics());

          verifyMetricsExists(DECLARED_APIKIT_FLOWS_APP_NAME + V2_SUFFIX, DECLARED_APIKIT_FLOWS_APP_DESCRIPTION,
                              getResourceName(),
                              FLOWS_SUMMARY_APP_STATISTICS_NAME, getExpectedDeclaredApikitFlowsV2(),
                              server.getMetrics());
          verifyMetricsExists(ACTIVE_APIKIT_FLOWS_APP_NAME + V2_SUFFIX, ACTIVE_APIKIT_FLOWS_APP_DESCRIPTION, getResourceName(),
                              FLOWS_SUMMARY_APP_STATISTICS_NAME, getExpectedActiveApikitFlowsV2(),
                              server.getMetrics());

          verifyMetricsExists(DECLARED_TRIGGER_FLOWS_APP_NAME + V2_SUFFIX, DECLARED_TRIGGER_FLOWS_APP_DESCRIPTION,
                              getResourceName(),
                              FLOWS_SUMMARY_APP_STATISTICS_NAME, getExpectedDeclaredTriggerFlowsV2(),
                              server.getMetrics());
          verifyMetricsExists(ACTIVE_TRIGGER_FLOWS_NAME + V2_SUFFIX, ACTIVE_TRIGGER_FLOWS_DESCRIPTION, getResourceName(),
                              FLOWS_SUMMARY_APP_STATISTICS_NAME, getExpectedActiveTriggerFlowsV2(),
                              server.getMetrics());
        } catch (Throwable e) {
          return false;
        }
        return true;
      }

      @Override
      public String describeFailure() {
        return "Error on verifying metrics: " + getShowStatsInfo();
      }

      private String getShowStatsInfo() {
        StringBuffer statsInfo = new StringBuffer();
        server.getMetrics()
            .forEach(metric -> statsInfo.append(metric.getInstrumentName()).append(" - ").append(metric.getName()).append(": ")
                .append(metric.getValue())
                .append(System.lineSeparator()));
        return statsInfo.toString();
      }
    });
  }

  abstract String getResourceName();

  abstract long getExpectedDeclaredPrivateFlows();

  abstract long getExpectedDeclaredApikitFlows();

  abstract long getExpectedDeclaredTriggerFlows();

  abstract long getExpectedActivePrivateFlows();

  abstract long getExpectedActiveApikitFlows();

  abstract long getExpectedActiveTriggerFlows();

  protected long getExpectedDeclaredPrivateFlowsV2() {
    return getExpectedDeclaredPrivateFlows();
  }

  protected long getExpectedDeclaredApikitFlowsV2() {
    return getExpectedDeclaredApikitFlows();
  }

  protected long getExpectedDeclaredTriggerFlowsV2() {
    return getExpectedDeclaredTriggerFlows();
  }

  protected long getExpectedActivePrivateFlowsV2() {
    return getExpectedActivePrivateFlows();
  }

  protected long getExpectedActiveApikitFlowsV2() {
    return getExpectedActiveApikitFlows();
  }

  protected long getExpectedActiveTriggerFlowsV2() {
    return getExpectedActiveTriggerFlows();
  }
}
