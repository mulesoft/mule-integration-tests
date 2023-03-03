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
import org.mule.runtime.core.privileged.profiling.PrivilegedProfilingService;
import org.mule.runtime.tracer.api.sniffer.ExportedSpanSniffer;
import org.mule.runtime.tracer.impl.exporter.OpenTelemetrySpanExporterFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_CORE_EXPORTER_FACTORY_KEY;
import static org.mule.runtime.tracer.exporter.api.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_ENABLED;

public class OpenTelemetrySpanDropTestCase extends MuleArtifactFunctionalTestCase
    implements OpenTelemetryTracingTestRunnerConfigAnnotation {

  @Inject
  PrivilegedProfilingService profilingService;
  private ExportedSpanSniffer spanCapturer;

  @Rule
  public SystemProperty enableTracingExport = new SystemProperty(MULE_OPEN_TELEMETRY_EXPORTER_ENABLED, "true");

  @Before
  public void initialize() {
    spanCapturer = profilingService.getSpanExportManager().getExportedSpanSniffer();
  }

  @After
  public void dispose() {
    spanCapturer.dispose();
  }

  @Override
  protected String getConfigFile() {
    return "tracing/span-drop.xml";
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

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
  }

  @Test
  public void testSpanGetsDroppedWhenExportQueueIsFull() throws Exception {
    flowRunner("drops-one-span").withPayload(AbstractMuleTestCase.TEST_PAYLOAD)
        .run();
  }

  private static class BlockingSpanExporterFactory extends OpenTelemetrySpanExporterFactory {

    @Override
    protected SpanExporter resolveOpenTelemetrySpanExporter() {
      return mock(SpanExporter.class);
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

}
