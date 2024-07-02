/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components.metrics;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static org.apache.commons.lang3.StringUtils.countMatches;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_ERROR_METRICS_FACTORY_KEY;

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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
public class OpenTelemetryErrorCountersTestCase extends AbstractOpenTelemetryMetricsTestCase {

  private static final int TIMEOUT_MILLIS = 5000;
  private static final int POLL_DELAY_MILLIS = 100;
  private static final SystemOutRecorder logRecorder = new SystemOutRecorder();

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

  @Before
  public void initialize() {
    logRecorder.startRecording();
  }

  @After
  public void dispose() {
    logRecorder.stopRecording();
    logRecorder.clearRecord();
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
        return getLoggedDataPoints() == expected;
      }

      @Override
      public String describeFailure() {
        return "Expected data points where not exported. Check the test log for non matching exported metrics.";
      }
    });
  }

  private int getLoggedDataPoints() throws UnsupportedEncodingException {
    return stream(logRecorder.getRecordedLogLines(StandardCharsets.UTF_8))
        .map(s -> countMatches(s, "ImmutableLongPointData"))
        .reduce(Integer::sum).orElse(0);
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

  private static class SystemOutRecorder extends PrintStream {

    private final PrintStream systemOut;
    private boolean recording = false;

    private SystemOutRecorder() {
      super(new ByteArrayOutputStream(), true);
      if (System.out instanceof SystemOutRecorder) {
        throw new IllegalStateException("Multiple recorder instances are not supported");
      }
      this.systemOut = System.out;
      System.setOut(this);
    }

    @Override
    public void write(byte[] b) throws IOException {
      systemOut.write(b);
      if (recording) {
        super.write(b);
      }
    }

    @Override
    public void write(byte[] b, int off, int len) {
      systemOut.write(b, off, len);
      if (recording) {
        super.write(b, off, len);
      }
    }

    @Override
    public void write(int b) {
      systemOut.write(b);
      if (recording) {
        super.write(b);
      }
    }

    @Override
    public void flush() {
      systemOut.flush();
      // ByteArrayOutputStream does not flush
    }

    @Override
    public void close() {
      // ByteArrayOutputStream does not close and System.out should not close
    }

    public void startRecording() {
      if (recording) {
        throw new IllegalStateException("Recording already in progress!");
      } else {
        System.setOut(this);
        recording = true;
      }
    }

    public void stopRecording() {
      if (!recording) {
        throw new IllegalStateException("Recording not in progress!");
      } else {
        System.setOut(systemOut);
        recording = false;
      }
    }

    public boolean isRecording() {
      return recording;
    }

    public void clearRecord() {
      out = new ByteArrayOutputStream();
    }

    public String[] getRecordedLogLines(Charset charset) throws UnsupportedEncodingException {
      return ((ByteArrayOutputStream) out).toString(charset.name()).split(System.lineSeparator());
    }

  }

}
