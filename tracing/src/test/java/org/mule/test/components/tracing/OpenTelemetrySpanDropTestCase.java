/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components.tracing;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.config.custom.ServiceConfigurator;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.tracer.impl.exporter.OpenTelemetrySpanExporterFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

import static org.mule.runtime.core.api.config.MuleProperties.MULE_CORE_EXPORTER_FACTORY_KEY;
import static org.mule.runtime.tracer.exporter.api.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_BATCH_QUEUE_SIZE;
import static org.mule.runtime.tracer.exporter.api.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_ENABLED;
import static org.mule.runtime.tracer.exporter.api.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_METRICS_LOG_FREQUENCY;
import static org.mule.runtime.tracer.exporter.api.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_TIMEOUT;

public class OpenTelemetrySpanDropTestCase extends MuleArtifactFunctionalTestCase
    implements OpenTelemetryTracingTestRunnerConfigAnnotation {

  private static final BlockingSpanExporter BLOCKING_SPAN_EXPORTER = new BlockingSpanExporter();

  @Rule
  public SystemProperty enableTracingExport = new SystemProperty(MULE_OPEN_TELEMETRY_EXPORTER_ENABLED, "true");

  // Smaller size possible (the queue uses powers of two for the size)
  @Rule
  public SystemProperty batchQueueSize = new SystemProperty(MULE_OPEN_TELEMETRY_EXPORTER_BATCH_QUEUE_SIZE, "2");

  @Rule
  public SystemProperty batchProcessorTimeout = new SystemProperty(MULE_OPEN_TELEMETRY_EXPORTER_TIMEOUT, "500");

  @Rule
  public SystemProperty exportMetricsFrequency = new SystemProperty(MULE_OPEN_TELEMETRY_EXPORTER_METRICS_LOG_FREQUENCY, "500");


  private static final SystemOutRecorder logRecorder = new SystemOutRecorder();

  @Before
  public void initialize() {
    logRecorder.startRecording();
  }

  @After
  public void dispose() {
    logRecorder.stopRecording();
  }

  @Override
  protected String getConfigFile() {
    return "tracing/span-drop.xml";
  }

  @Test
  public void testWhenSpanGetsDroppedThenWarningLogInformsIt() throws Exception {
    // This two spans should block the exporter after leaving the queue
    flowRunner("drops-one-span").withPayload(AbstractMuleTestCase.TEST_PAYLOAD)
        .run();
    BLOCKING_SPAN_EXPORTER.waitUntilIsBlocked();
    // This two spans should fill the exporter queue
    flowRunner("drops-one-span").withPayload(AbstractMuleTestCase.TEST_PAYLOAD)
        .run();
    // This two spans should drop
    flowRunner("drops-one-span").withPayload(AbstractMuleTestCase.TEST_PAYLOAD)
        .run();
    new PollingProber(5000, 100).check(new Probe() {

      @Override
      public boolean isSatisfied() {
        try {
          String recordedLogs = logRecorder.getLogs(StandardCharsets.UTF_8);
          // TODO: Improve test assertion after implementing the tracing levels feature
          // We cannot make the test fully deterministic because we don't have a way to generate one span per execution (yet)
          return recordedLogs.contains("Total spans dropped since exporter start: 2")
              || recordedLogs.contains("Total spans dropped since exporter start: 3");
        } catch (UnsupportedEncodingException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public String describeFailure() {
        return "Expected span drop did not happen";
      }
    });
  }

  @Override
  protected void addBuilders(List<ConfigurationBuilder> builders) {
    super.addBuilders(builders);
    builders.add(getCustomSpanExporterFactoryBuilder());
  }

  private ConfigurationBuilder getCustomSpanExporterFactoryBuilder() {
    return new ConfigurationBuilder() {

      @Override
      public void addServiceConfigurator(ServiceConfigurator serviceConfigurator) {
        // Nothing to do
      }

      @Override
      public void configure(MuleContext muleContext) {
        muleContext.getCustomizationService().overrideDefaultServiceClass(MULE_CORE_EXPORTER_FACTORY_KEY,
                                                                          BlockingSpanExporterFactory.class);
      }
    };
  }

  private static class BlockingSpanExporterFactory extends OpenTelemetrySpanExporterFactory {

    @Override
    protected SpanExporter resolveOpenTelemetrySpanExporter() {
      return BLOCKING_SPAN_EXPORTER;
    }
  }

  private static final class BlockingSpanExporter implements SpanExporter {

    final Object monitor = new Object();

    private enum State {
      WAIT_TO_BLOCK, BLOCKED, UNBLOCKED
    }

    State state = State.WAIT_TO_BLOCK;

    @Override
    public CompletableResultCode export(Collection<SpanData> spanDataList) {
      synchronized (monitor) {
        while (state != State.UNBLOCKED) {
          try {
            state = State.BLOCKED;
            // Some threads may wait for Blocked State.
            monitor.notifyAll();
            monitor.wait();
          } catch (InterruptedException e) {
            // Do nothing
          }
        }
      }
      return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode flush() {
      return CompletableResultCode.ofSuccess();
    }

    private void waitUntilIsBlocked() {
      synchronized (monitor) {
        while (state != State.BLOCKED) {
          try {
            monitor.wait();
          } catch (InterruptedException e) {
            // Do nothing
          }
        }
      }
    }

    @Override
    public CompletableResultCode shutdown() {
      // Do nothing;
      return CompletableResultCode.ofSuccess();
    }

    private void unblock() {
      synchronized (monitor) {
        state = State.UNBLOCKED;
        monitor.notifyAll();
      }
    }
  }

  private static class SystemOutRecorder extends PrintStream {

    private final PrintStream systemOut;
    private boolean recording = false;

    private SystemOutRecorder() {
      super(new ByteArrayOutputStream());
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

    public void clearRecord() throws IOException {
      out = new ByteArrayOutputStream();
    }

    public String getLogs(Charset charset) throws UnsupportedEncodingException {
      return ((ByteArrayOutputStream) out).toString(charset.name());
    }

  }


}
