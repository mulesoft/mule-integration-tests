/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.components.tracing;

import static org.mule.runtime.core.api.config.MuleProperties.MULE_CORE_EXPORTER_FACTORY_KEY;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_BATCH_QUEUE_SIZE;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_BATCH_SCHEDULED_DELAY;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_ENABLED;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_MAX_BATCH_SIZE;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.OPEN_TELEMETRY_EXPORTER;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.config.custom.ServiceConfigurator;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.tracer.exporter.config.api.SpanExporterConfiguration;
import org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporterFactory;
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
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;

@Feature(PROFILING)
@Story(OPEN_TELEMETRY_EXPORTER)
public class OpenTelemetryGracefulShutdownTestCase extends MuleArtifactFunctionalTestCase
    implements OpenTelemetryTracingTestRunnerConfigAnnotation {

  @Inject
  MuleContext muleContext;

  private static final CountingSpanExporter COUNTING_SPAN_EXPORTER = new CountingSpanExporter();

  // A long export delay prevents export triggering
  public static final String EXPORTER_SCHEDULED_DELAY = "60000";

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
