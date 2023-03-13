/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.components.tracing;

import static org.mule.runtime.core.api.config.MuleProperties.MULE_CORE_EXPORTER_FACTORY_KEY;
import static org.mule.runtime.tracer.exporter.api.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_BATCH_QUEUE_SIZE;
import static org.mule.runtime.tracer.exporter.api.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_BATCH_SCHEDULED_DELAY;
import static org.mule.runtime.tracer.exporter.api.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_ENABLED;
import static org.mule.runtime.tracer.exporter.api.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_MAX_BATCH_SIZE;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.config.custom.ServiceConfigurator;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.tracer.exporter.api.config.SpanExporterConfiguration;
import org.mule.runtime.tracer.impl.exporter.OpenTelemetrySpanExporterFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.junit.Rule;
import org.junit.Test;

public class OpenTelemetryGracefulShutdownTestCase extends MuleArtifactFunctionalTestCase
    implements OpenTelemetryTracingTestRunnerConfigAnnotation {

  public static final String EXPORTER_SCHEDULED_DELAY = "60000";
  @Inject
  MuleContext muleContext;

  private static final CountingSpanExporter COUNTING_SPAN_EXPORTER = new CountingSpanExporter();

  @Rule
  public SystemProperty enableTracingExport = new SystemProperty(MULE_OPEN_TELEMETRY_EXPORTER_ENABLED, "true");

  // A size higher than the generated spans prevents export triggering
  @Rule
  public SystemProperty maxBatchSize = new SystemProperty(MULE_OPEN_TELEMETRY_EXPORTER_MAX_BATCH_SIZE, "512");

  // A size higher than the generated spans prevents export triggering
  @Rule
  public SystemProperty batchQueueSize = new SystemProperty(MULE_OPEN_TELEMETRY_EXPORTER_BATCH_QUEUE_SIZE, "3");

  private static final SpanExporterConfiguration privilegedConfiguration = key -> {
    // A higher time than the test time prevents export triggering
    if (key.equals(MULE_OPEN_TELEMETRY_EXPORTER_BATCH_SCHEDULED_DELAY)) {
      return EXPORTER_SCHEDULED_DELAY;
    } else {
      return null;
    }
  };

  @Override
  protected boolean doTestClassInjection() {
    return true;
  }

  @Override
  protected boolean isGracefulShutdown() {
    return true;
  }

  @Override
  protected String getConfigFile() {
    return "tracing/span-drop.xml";
  }

  @Test
  public void testShutdownFlushesExportQueue() throws Exception {
    flowRunner("drops-one-span").withPayload(AbstractMuleTestCase.TEST_PAYLOAD)
        .run();
    muleContext.dispose();
    new PollingProber(10000, 100).check(new Probe() {

      @Override
      public boolean isSatisfied() {
        return COUNTING_SPAN_EXPORTER.getExportedSpansCount() == 2;
      }

      @Override
      public String describeFailure() {
        return "Expected span export did not happen";
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
                                                                          CountingSpanExporterFactory.class);
      }
    };
  }

  private static class CountingSpanExporterFactory extends OpenTelemetrySpanExporterFactory {

    public CountingSpanExporterFactory() {
      super(privilegedConfiguration);
    }

    @Override
    protected SpanExporter resolveOpenTelemetrySpanExporter() {
      return COUNTING_SPAN_EXPORTER;
    }
  }

  private static final class CountingSpanExporter implements SpanExporter {

    final AtomicLong exportedSpansCount = new AtomicLong(0);

    @Override
    public CompletableResultCode export(Collection<SpanData> spanDataList) {
      exportedSpansCount.addAndGet(spanDataList.size());
      return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode flush() {
      return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
      // Do nothing;
      return CompletableResultCode.ofSuccess();
    }

    public long getExportedSpansCount() {
      return exportedSpansCount.get();
    }
  }

}
