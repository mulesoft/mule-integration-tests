/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components.metrics;

import static org.mule.runtime.core.api.config.MuleProperties.MULE_ERROR_METRICS_FACTORY_KEY;

import static java.util.Arrays.asList;

import org.mule.functional.api.flow.FlowRunner;
import org.mule.runtime.api.config.custom.ServiceConfigurator;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.util.UUID;
import org.mule.runtime.metrics.api.error.ErrorIdProvider;
import org.mule.runtime.metrics.api.error.ErrorMetrics;
import org.mule.runtime.metrics.api.meter.Meter;
import org.mule.runtime.metrics.impl.meter.error.DefaultErrorMetricsFactory;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
public class OpenTelemetryErrorCountersTestCase extends AbstractOpenTelemetryMetricsTestCase {

  private static final int TIMEOUT_MILLIS = 5000;
  private static final int POLL_DELAY_MILLIS = 100;

  private final String flowName;
  private final boolean shouldFail;
  private final int expectedErrorDataPoints;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {"simple-flow", true, 1},
        {"simple-propagate-flow", true, 1},
        {"simple-continue-flow", false, 1},
        {"failed-propagate-flow", true, 2},
        {"failed-continue-flow", true, 2},
        {"failed-try-propagate-flow", true, 2},
        {"failed-try-continue-flow", false, 2},
        {"failed-try-and-propagate-flow", true, 3},
        {"failed-try-and-continue-flow", true, 3}
    });
  }

  public OpenTelemetryErrorCountersTestCase(String flowName, boolean shouldFail, int expectedErrorDataPoints) {
    this.flowName = flowName;
    this.shouldFail = shouldFail;
    this.expectedErrorDataPoints = expectedErrorDataPoints;
  }

  @Override
  protected void addBuilders(List<ConfigurationBuilder> builders) {
    super.addBuilders(builders);
    builders.add(getRandomErrorIdErrorMetricsFactoryConfigurationBuilder());
  }

  private ConfigurationBuilder getRandomErrorIdErrorMetricsFactoryConfigurationBuilder() {
    return new ConfigurationBuilder() {

      @Override
      public void addServiceConfigurator(ServiceConfigurator serviceConfigurator) {
        // Nothing to do.
      }

      @Override
      public void configure(MuleContext muleContext) throws ConfigurationException {
        muleContext.getCustomizationService().overrideDefaultServiceClass(MULE_ERROR_METRICS_FACTORY_KEY,
                                                                          OpenTelemetryErrorCountersTestCase.RandomErrorIdErrorMetricsFactory.class);
      }
    };
  }

  @Test
  public void errorMetricsCount() throws Exception {
    FlowRunner flowRunner = flowRunner(flowName).withPayload(TEST_PAYLOAD);
    if (shouldFail) {
      flowRunner.runExpectingException();
    } else {
      flowRunner.run();
    }
    assertExportedDataPointsCount(expectedErrorDataPoints);
  }

  private void assertExportedDataPointsCount(int expected) {
    new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS).check(new JUnitProbe() {

      @Override
      protected boolean test() throws Exception {
        try {
          verifyMetricsExists("error-count", "Mule runtime error count",
                              "OpenTelemetryErrorCountersTestCase#errorMetricsCount[" + flowName + "]",
                              "Mule runtime error metrics", expectedErrorDataPoints, server.getMetrics());
        } catch (Throwable e) {
          return false;
        }
        return true;
      }

      @Override
      public String describeFailure() {
        return "Expected data points where not exported. Check the test log for non matching exported metrics.";
      }
    });
  }

  @Override
  protected String getConfigFile() {
    return "metrics/error-counters-metric.xml";
  }

  private static class RandomErrorIdErrorMetricsFactory extends DefaultErrorMetricsFactory {

    @Override
    public ErrorMetrics create(Meter errorMetricsMeter) {
      return create(errorMetricsMeter, new ErrorIdProvider() {

        @Override
        public String getErrorId(Error error) {
          return UUID.getUUID();
        }

        @Override
        public String getErrorId(Throwable error) {
          return UUID.getUUID();
        }
      });
    }
  }

}
