/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components.tracing;

import static org.mule.runtime.core.api.config.MuleProperties.MULE_CORE_EXPORTER_FACTORY_KEY;
import static org.mule.runtime.http.api.HttpConstants.Method.GET;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_DEFAULT_TRACING_LEVEL;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_ENABLED;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_OTEL_TRACES_SAMPLER;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_OTEL_TRACES_SAMPLER_ARG;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.USE_MULE_OPEN_TELEMETRY_EXPORTER_SNIFFER;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.OPEN_TELEMETRY_EXPORTER;

import static java.lang.Boolean.TRUE;
import static java.lang.String.format;

import static io.opentelemetry.sdk.trace.samplers.SamplingDecision.RECORD_AND_SAMPLE;
import static org.apache.commons.lang3.JavaVersion.JAVA_11;
import static org.apache.commons.lang3.SystemUtils.isJavaVersionAtMost;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assume.assumeThat;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.config.custom.ServiceConfigurator;
import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.privileged.profiling.PrivilegedProfilingService;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.client.HttpRequestOptions;
import org.mule.runtime.tracer.api.sniffer.ExportedSpanSniffer;
import org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporterFactory;
import org.mule.service.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;

import java.util.ArrayList;
import java.util.List;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;

import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

import jakarta.inject.Inject;

@Feature(PROFILING)
@Story(OPEN_TELEMETRY_EXPORTER)
public abstract class OpenTelemetrySamplerTestCase extends MuleArtifactFunctionalTestCase
    implements OpenTelemetryTracingTestRunnerConfigAnnotation {

  public static final int REQUESTS = 10;
  public static final int EXPECTED_EVALUATED_SPANS = 2;
  @Inject
  PrivilegedProfilingService profilingService;
  private final TestSamplerSpanExporter spanExporter = new TestSamplerSpanExporter();

  private static final int TIMEOUT_MILLIS = 30000;
  private static final int POLL_DELAY_MILLIS = 100;

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder(getService(HttpService.class)).build();

  @Rule
  public SystemProperty systemPropertySampler = new SystemProperty(MULE_OPEN_TELEMETRY_OTEL_TRACES_SAMPLER, getSamplerName());

  @Rule
  public SystemProperty systemPropertySamplerArg =
      new SystemProperty(MULE_OPEN_TELEMETRY_OTEL_TRACES_SAMPLER_ARG, getSamplerArg());
  @Rule
  public DynamicPort entryListenerPort = new DynamicPort("httpPort");

  @Rule
  public SystemProperty defaultTracingLevel =
      new SystemProperty(MULE_OPEN_TELEMETRY_EXPORTER_DEFAULT_TRACING_LEVEL, "monitoring");

  @Rule
  public SystemProperty enableSniffing = new SystemProperty(USE_MULE_OPEN_TELEMETRY_EXPORTER_SNIFFER, TRUE.toString());

  @Rule
  public SystemProperty enableTracingExport = new SystemProperty(MULE_OPEN_TELEMETRY_EXPORTER_ENABLED, "true");


  @Override
  protected String getConfigFile() {
    return "tracing/simple-flow-sampling.xml";
  }

  @Test
  public void test() throws Exception {
    // TODO W-14229036 Remove this and reenable the test
    assumeThat(isJavaVersionAtMost(JAVA_11), is(true));

    for (int i = 0; i < REQUESTS; i++) {
      ExportedSpanSniffer spanCapturer = profilingService.getSpanExportManager().getExportedSpanSniffer();
      MultiMap<String, String> headers = new MultiMap<>();
      if (!isRemoteRequestSpanSampled()) {
        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder().build();
        Tracer tracer = openTelemetry.getTracer("testing-instrumentation-library", "1.0.0");
        Span span = tracer.spanBuilder("root").startSpan();
        TextMapSetter<MultiMap> setter =
            (carrier, key, value) -> {
              carrier.put(key, value);
            };

        span = Span.wrap(SpanContext.create(span.getSpanContext().getTraceId(),
                                            span.getSpanContext().getSpanId(),
                                            TraceFlags.getDefault(),
                                            TraceState.getDefault()));
        W3CTraceContextPropagator.getInstance().inject(
                                                       Context.current()
                                                           .with(span),
                                                       headers,
                                                       setter);
      }
      org.mule.runtime.http.api.domain.message.request.HttpRequest request =
          org.mule.runtime.http.api.domain.message.request.HttpRequest.builder()
              .uri(format("http://localhost:%s/", entryListenerPort.getNumber()))
              .headers(headers)
              .method(GET).build();
      httpClient.send(request, HttpRequestOptions.builder().responseTimeout(TIMEOUT_MILLIS).build());

      PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);

      prober.check(new JUnitProbe() {

        @Override
        protected boolean test() {
          return spanExporter.getEvaluatedSpans() == EXPECTED_EVALUATED_SPANS;
        }

        @Override
        public String describeFailure() {
          return "The exact amount of evaluated spans did not occurred";
        }
      });

      prober.check(new JUnitProbe() {

        @Override
        protected boolean test() {
          return spanCapturer.getExportedSpans().size() == expectedSpan();
        }

        @Override
        public String describeFailure() {
          return "The amount of exported spans is not correct. Exported: " + spanCapturer.getExportedSpans().size()
              + ", Expected: " + expectedSpan();
        }
      });

      spanExporter.clear();

    }
  }

  private int expectedSpan() {
    return !isRemoteRequestSpanSampled() ? 0 : spanExporter.getSampledCount();
  }

  @Override
  protected void addBuilders(List<ConfigurationBuilder> builders) {
    super.addBuilders(builders);
    builders.add(getCustomSpanExporterFactoryBuilder());
  }

  abstract String getSamplerName();

  abstract String getSamplerArg();

  private ConfigurationBuilder getCustomSpanExporterFactoryBuilder() {
    return new ConfigurationBuilder() {

      @Override
      public void addServiceConfigurator(ServiceConfigurator serviceConfigurator) {
        // Nothing to do
      }

      @Override
      public void configure(MuleContext muleContext) {
        muleContext.getCustomizationService().overrideDefaultServiceImpl(MULE_CORE_EXPORTER_FACTORY_KEY,
                                                                         spanExporter);
      }
    };
  }

  protected boolean isRemoteRequestSpanSampled() {
    return true;
  }

  private static class TestSamplerSpanExporter extends OpenTelemetrySpanExporterFactory {

    private TestSamplerWrapper sampler;

    @Override
    protected Sampler resolveSampler() {
      sampler = new TestSamplerWrapper(super.resolveSampler());
      return sampler;
    }

    @Override
    protected SpanExporter resolveOpenTelemetrySpanExporter() {
      return super.resolveOpenTelemetrySpanExporter();
    }

    public int getSampledCount() {
      return sampler.getSampledCount();
    }

    public int getEvaluatedSpans() {
      return sampler.getEvaluatedSpans();
    }

    public void clear() {
      sampler.clear();
    }

    private final static class TestSamplerWrapper implements Sampler {

      private final Sampler sampler;

      private final List<String> evaluatedSpans = new ArrayList<>();

      private final List<String> sampledSpans = new ArrayList<>();


      public TestSamplerWrapper(Sampler sampler) {
        this.sampler = sampler;
      }

      @Override
      public SamplingResult shouldSample(Context context, String traceId, String name, SpanKind spanKind, Attributes attributes,
                                         List<LinkData> list) {
        SamplingResult samplingResult = sampler.shouldSample(context, traceId, name, spanKind, attributes, list);

        String spanTestKey = transformSpanNameIfMuleFlowSemanticVersion(name);
        if (samplingResult.getDecision().equals(RECORD_AND_SAMPLE)) {
          if (!sampledSpans.contains(spanTestKey)) {
            sampledSpans.add(spanTestKey);
          }
        } else {
          if (sampledSpans.contains(spanTestKey)) {
            sampledSpans.remove(spanTestKey);
          }
        }

        if (!evaluatedSpans.contains(spanTestKey)) {
          evaluatedSpans.add(spanTestKey);
        }

        return samplingResult;
      }

      /**
       * This is used to identify that the span is the same in case of a mule-flow
       *
       * @param name the name of the span
       * @return
       */
      private String transformSpanNameIfMuleFlowSemanticVersion(String name) {
        if (name.contains("GET")) {
          return "mule:flow";
        } else {
          return name;
        }
      }

      @Override
      public String getDescription() {
        return "Test sampler wrapper";
      }

      public int getSampledCount() {
        return sampledSpans.size();
      }

      public int getEvaluatedSpans() {
        return evaluatedSpans.size();
      }

      public void clear() {
        evaluatedSpans.clear();
        sampledSpans.clear();
      }
    }
  }

}
