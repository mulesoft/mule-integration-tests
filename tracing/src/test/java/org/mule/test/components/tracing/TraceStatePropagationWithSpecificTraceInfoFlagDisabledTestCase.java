/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.components.tracing;

import static org.mule.runtime.api.util.MuleSystemProperties.ADD_MULE_SPECIFIC_TRACING_INFORMATION_IN_TRACE_STATE_PROPERTY;
import static org.mule.runtime.http.api.HttpConstants.Method.GET;
import static org.mule.runtime.tracer.customization.api.InternalSpanNames.GET_CONNECTION_SPAN_NAME;
import static org.mule.runtime.tracer.customization.api.InternalSpanNames.OPERATION_EXECUTION_SPAN_NAME;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_ENABLED;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_ENDPOINT;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_TYPE;
import static org.mule.runtime.api.util.MuleSystemProperties.TRACING_LEVEL_CONFIGURATION_PATH;
import static org.mule.runtime.tracing.level.api.config.TracingLevelId.DEBUG;
import static org.mule.runtime.tracing.level.api.config.TracingLevelId.MONITORING;
import static org.mule.runtime.tracing.level.api.config.TracingLevelId.OVERVIEW;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;
import static org.mule.test.components.tracing.OpenTelemetryProtobufSpanUtils.getSpans;

import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

import static com.linecorp.armeria.common.HttpResponse.from;
import static com.linecorp.armeria.common.HttpStatus.OK;
import static io.opentelemetry.api.trace.Span.getInvalid;
import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.StatusCode.UNSET;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.client.HttpRequestOptions;
import org.mule.runtime.tracer.api.sniffer.CapturedEventData;
import org.mule.runtime.tracer.api.sniffer.CapturedExportedSpan;
import org.mule.service.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.infrastructure.profiling.tracing.SpanTestHierarchy;
import org.mule.test.runner.RunnerDelegateTo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.server.AbstractHttpService;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.testing.junit4.server.ServerRule;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class TraceStatePropagationWithSpecificTraceInfoFlagDisabledTestCase extends
    MuleArtifactFunctionalTestCase implements OpenTelemetryTracingTestRunnerConfigAnnotation {

  public static final String ROOT_SPAN_NAME = "rootSpan";
  private final String traceLevel;
  private final Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> spanHierarchyRetriever;

  @Rule
  public SystemProperty addAncestorSpanId =
      new SystemProperty(ADD_MULE_SPECIFIC_TRACING_INFORMATION_IN_TRACE_STATE_PROPERTY, "false");

  @Rule
  public DynamicPort httpPort = new DynamicPort("port");

  @Rule
  public DynamicPort entryListenerPort = new DynamicPort("entryListenerPort");

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder(getService(HttpService.class)).build();

  private static final String EXPECTED_HTTP_REQUEST_SPAN_NAME = "GET";
  private static final String EXPECTED_HTTP_FLOW_SPAN_NAME = "GET /test";
  private static final String EXPECTED_LOGGER_SPAN_NAME = "mule:logger";

  private static final String EXPECTED_FLOW_SPAN_NAME = "GET /entry";

  private static final String EXPECTED_VALUE_RESOLUTION_SPAN_NAME = "mule:value-resolution";

  private static final String EXPECTED_PARAMETERS_RESOLUTION_SPAN_NAME = "mule:parameters-resolution";

  public static final int TIMEOUT_MILLIS = 30000;

  private static final int POLL_DELAY_MILLIS = 100;

  private final String type;
  private final String path;
  private int expectedSpansCount;

  @Rule
  public SystemProperty openTelemetryExporterEnabled = new SystemProperty(MULE_OPEN_TELEMETRY_EXPORTER_ENABLED, TRUE.toString());

  @Rule
  public SystemProperty openTelemetryExporterType = new SystemProperty(MULE_OPEN_TELEMETRY_EXPORTER_TYPE, "HTTP");

  @Rule
  public SystemProperty openTelemetryExporterEndpoint = new SystemProperty(MULE_OPEN_TELEMETRY_EXPORTER_ENDPOINT,
                                                                           "http://localhost:" + httpServer.httpPort() + "/");

  @Override
  protected String getConfigFile() {
    return "tracing/distributed-tracing.xml";
  }

  @ClassRule
  public static final TestServerRule httpServer = new TestServerRule();

  @Parameterized.Parameters(name = "Transport: {0} - Tracing Level: {2}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        // TODO: Add the GRPC Version
        {"HTTP", "", OVERVIEW.name(), 4, getOverviewExpectedSpanTestHierarchy()},
        {"HTTP", "", MONITORING.name(), 5, getMonitoringExpectedSpanTestHierarchy()},
        {"HTTP", "", DEBUG.name(), 20, getDebugExpectedSpanTestHierarchy()}
    });
  }

  private static Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> getOverviewExpectedSpanTestHierarchy() {
    return exportedSpans -> {
      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(ROOT_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_FLOW_SPAN_NAME)
          .addTraceStateKeyValueAssertion("key1", "value1")
          .addTraceStateKeyValueAssertion("key2", "value2")
          .addTraceStateKeyValueAssertion("ancestor-mule-span-id", "1")
          .beginChildren()
          .child(EXPECTED_HTTP_REQUEST_SPAN_NAME)
          .addTraceStateKeyValueAssertion("key1", "value1")
          .addTraceStateKeyValueAssertion("key2", "value2")
          .addTraceStateKeyValueAssertion("ancestor-mule-span-id", "1")
          .beginChildren()
          .child(EXPECTED_HTTP_FLOW_SPAN_NAME)
          .addTraceStateKeyValueAssertion("key1", "value1")
          .addTraceStateKeyValueAssertion("key2", "value2")
          .addTraceStateKeyValueAssertion("ancestor-mule-span-id", "1")
          .endChildren()
          .endChildren()
          .endChildren();
      return expectedSpanHierarchy;
    };
  }

  private static Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> getMonitoringExpectedSpanTestHierarchy() {
    return exportedSpans -> {
      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(ROOT_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_FLOW_SPAN_NAME)
          .addTraceStateKeyValueAssertion("key1", "value1")
          .addTraceStateKeyValueAssertion("key2", "value2")
          .addTraceStateKeyValueAssertion("ancestor-mule-span-id", "1")
          .beginChildren()
          .child(EXPECTED_HTTP_REQUEST_SPAN_NAME)
          .addTraceStateKeyValueAssertion("key1", "value1")
          .addTraceStateKeyValueAssertion("key2", "value2")
          .addTraceStateKeyValueAssertion("ancestor-mule-span-id", "1")
          .beginChildren()
          .child(EXPECTED_HTTP_FLOW_SPAN_NAME)
          .addTraceStateKeyValueAssertion("key1", "value1")
          .addTraceStateKeyValueAssertion("key2", "value2")
          .addTraceStateKeyValueAssertion("ancestor-mule-span-id", "1")
          .beginChildren()
          .child(EXPECTED_LOGGER_SPAN_NAME)
          .addTraceStateKeyValueAssertion("key1", "value1")
          .addTraceStateKeyValueAssertion("key2", "value2")
          .addTraceStateKeyValueAssertion("ancestor-mule-span-id", "1")
          .endChildren()
          .endChildren()
          .endChildren();

      return expectedSpanHierarchy;
    };
  }

  private static Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> getDebugExpectedSpanTestHierarchy() {
    return exportedSpans -> {
      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(ROOT_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_FLOW_SPAN_NAME)
          .addTraceStateKeyValueAssertion("key1", "value1")
          .addTraceStateKeyValueAssertion("key2", "value2")
          .addTraceStateKeyValueAssertion("ancestor-mule-span-id", "1")
          .beginChildren()
          .child(EXPECTED_HTTP_REQUEST_SPAN_NAME)
          .addTraceStateKeyValueAssertion("key1", "value1")
          .addTraceStateKeyValueAssertion("key2", "value2")
          .addTraceStateKeyValueAssertion("ancestor-mule-span-id", "1")
          .beginChildren()
          .child(GET_CONNECTION_SPAN_NAME)
          .addTraceStateKeyValueAssertion("key1", "value1")
          .addTraceStateKeyValueAssertion("key2", "value2")
          .addTraceStateKeyValueAssertion("ancestor-mule-span-id", "1")
          .child(EXPECTED_PARAMETERS_RESOLUTION_SPAN_NAME)
          .addTraceStateKeyValueAssertion("key1", "value1")
          .addTraceStateKeyValueAssertion("key2", "value2")
          .addTraceStateKeyValueAssertion("ancestor-mule-span-id", "1")
          .beginChildren()
          .child(EXPECTED_VALUE_RESOLUTION_SPAN_NAME)
          .addTraceStateKeyValueAssertion("key1", "value1")
          .addTraceStateKeyValueAssertion("key2", "value2")
          .addTraceStateKeyValueAssertion("ancestor-mule-span-id", "1")
          .child(EXPECTED_VALUE_RESOLUTION_SPAN_NAME)
          .addTraceStateKeyValueAssertion("key1", "value1")
          .addTraceStateKeyValueAssertion("key2", "value2")
          .addTraceStateKeyValueAssertion("ancestor-mule-span-id", "1")
          .child(EXPECTED_VALUE_RESOLUTION_SPAN_NAME)
          .addTraceStateKeyValueAssertion("key1", "value1")
          .addTraceStateKeyValueAssertion("key2", "value2")
          .addTraceStateKeyValueAssertion("ancestor-mule-span-id", "1")
          .child(EXPECTED_VALUE_RESOLUTION_SPAN_NAME)
          .addTraceStateKeyValueAssertion("key1", "value1")
          .addTraceStateKeyValueAssertion("key2", "value2")
          .addTraceStateKeyValueAssertion("ancestor-mule-span-id", "1")
          .child(EXPECTED_VALUE_RESOLUTION_SPAN_NAME)
          .addTraceStateKeyValueAssertion("key1", "value1")
          .addTraceStateKeyValueAssertion("key2", "value2")
          .addTraceStateKeyValueAssertion("ancestor-mule-span-id", "1")
          .child(EXPECTED_VALUE_RESOLUTION_SPAN_NAME)
          .addTraceStateKeyValueAssertion("key1", "value1")
          .addTraceStateKeyValueAssertion("key2", "value2")
          .addTraceStateKeyValueAssertion("ancestor-mule-span-id", "1")
          .child(EXPECTED_VALUE_RESOLUTION_SPAN_NAME)
          .addTraceStateKeyValueAssertion("key1", "value1")
          .addTraceStateKeyValueAssertion("key2", "value2")
          .addTraceStateKeyValueAssertion("ancestor-mule-span-id", "1")
          .child(EXPECTED_VALUE_RESOLUTION_SPAN_NAME)
          .addTraceStateKeyValueAssertion("key1", "value1")
          .addTraceStateKeyValueAssertion("key2", "value2")
          .addTraceStateKeyValueAssertion("ancestor-mule-span-id", "1")
          .child(EXPECTED_VALUE_RESOLUTION_SPAN_NAME)
          .addTraceStateKeyValueAssertion("key1", "value1")
          .addTraceStateKeyValueAssertion("key2", "value2")
          .addTraceStateKeyValueAssertion("ancestor-mule-span-id", "1")
          .child(EXPECTED_VALUE_RESOLUTION_SPAN_NAME)
          .addTraceStateKeyValueAssertion("key1", "value1")
          .addTraceStateKeyValueAssertion("key2", "value2")
          .addTraceStateKeyValueAssertion("ancestor-mule-span-id", "1")
          .child(EXPECTED_VALUE_RESOLUTION_SPAN_NAME)
          .addTraceStateKeyValueAssertion("key1", "value1")
          .addTraceStateKeyValueAssertion("key2", "value2")
          .addTraceStateKeyValueAssertion("ancestor-mule-span-id", "1")
          .child(EXPECTED_VALUE_RESOLUTION_SPAN_NAME)
          .addTraceStateKeyValueAssertion("key1", "value1")
          .addTraceStateKeyValueAssertion("key2", "value2")
          .addTraceStateKeyValueAssertion("ancestor-mule-span-id", "1")
          .endChildren()
          .child(OPERATION_EXECUTION_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_HTTP_FLOW_SPAN_NAME)
          .addTraceStateKeyValueAssertion("key1", "value1")
          .addTraceStateKeyValueAssertion("key2", "value2")
          .addTraceStateKeyValueAssertion("ancestor-mule-span-id", "1")
          .endChildren()
          .endChildren()
          .endChildren()
          .endChildren();
      return expectedSpanHierarchy;
    };
  }

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    setProperty(TRACING_LEVEL_CONFIGURATION_PATH, traceLevel.toLowerCase() + FileSystems.getDefault().getSeparator());
    super.doSetUpBeforeMuleContextCreation();
  }

  public TraceStatePropagationWithSpecificTraceInfoFlagDisabledTestCase(String type, String path, String traceLevel,
                                                                        int expectedSpansCount,
                                                                        Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> spanHierarchyRetriever) {
    this.type = type;
    this.path = path;
    this.expectedSpansCount = expectedSpansCount;
    this.traceLevel = traceLevel;
    this.spanHierarchyRetriever = spanHierarchyRetriever;
  }

  @After
  public void after() {
    // TODO W-13160648: Add a Rule for selecting LEVEL of tracing in integration test and make it work in parallel
    clearProperty(TRACING_LEVEL_CONFIGURATION_PATH);
    httpServer.reset();
  }

  @Test
  public void traceStatePropagation() throws Exception {
    OpenTelemetry openTelemetry = OpenTelemetrySdk.builder().build();
    Tracer tracer = openTelemetry.getTracer("testing-instrumentation-library", "1.0.0");
    Span span = tracer.spanBuilder(ROOT_SPAN_NAME).startSpan();

    TextMapSetter<MultiMap> setter =
        (carrier, key, value) -> {
          // Insert the context as Header
          carrier.put(key, value);
        };

    MultiMap<String, String> headers = new MultiMap<>();
    W3CTraceContextPropagator.getInstance().inject(Context.current().with(span), headers, setter);
    httpServer.getCapturedExportedSpans().add(new RootTestCapturedSpan(ROOT_SPAN_NAME, span));
    headers.put("tracestate", "key1=value1,key2=value2,ancestor-mule-span-id=1");
    org.mule.runtime.http.api.domain.message.request.HttpRequest request =
        org.mule.runtime.http.api.domain.message.request.HttpRequest.builder()
            .uri(format("http://localhost:%s/entry", entryListenerPort.getNumber()))
            .headers(headers)
            .method(GET).build();
    org.mule.runtime.http.api.domain.message.response.HttpResponse response =
        httpClient.send(request, HttpRequestOptions.builder().responseTimeout(TIMEOUT_MILLIS).build());

    assertThat(response.getStatusCode(), equalTo(OK.code()));
    span.end();

    PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);

    prober.check(new JUnitProbe() {

      @Override
      protected boolean test() {
        Collection<CapturedExportedSpan> exportedSpans = httpServer.getCapturedExportedSpans();
        return exportedSpans.size() == expectedSpansCount;
      }

      @Override
      public String describeFailure() {
        return "The exact amount of spans was not captured";
      }
    });

    spanHierarchyRetriever.apply(httpServer.getCapturedExportedSpans()).assertSpanTree();

  }

  private static final class TestServerRule extends ServerRule {

    public static final String PATH_PATTERN = "/";

    private final List<CapturedExportedSpan> capturedExportedSpans = new ArrayList<>();

    @Override
    protected void configure(ServerBuilder sb) {
      sb.service(PATH_PATTERN,
                 new AbstractHttpService() {

                   @Override
                   protected @NotNull HttpResponse doPost(@NotNull ServiceRequestContext ctx, @NotNull HttpRequest req) {
                     return HttpResponse.from(req.aggregate().handle((aReq, cause) -> {
                       CompletableFuture<HttpResponse> responseFuture = new CompletableFuture<>();
                       HttpResponse res = from(responseFuture);
                       try {
                         capturedExportedSpans.addAll(getSpans(ExportTraceServiceRequest
                             .parseFrom(new ByteArrayInputStream(aReq.content().array()))));
                       } catch (IOException e) {
                         // Nothing to do.
                       }
                       responseFuture.complete(HttpResponse.of(OK));
                       return res;
                     }));
                   }
                 });
      sb.http(0);
    }

    public List<CapturedExportedSpan> getCapturedExportedSpans() {
      return capturedExportedSpans;
    }

    public void reset() {
      capturedExportedSpans.clear();
    }
  }

  private class RootTestCapturedSpan implements CapturedExportedSpan {

    private final Span span;
    private final String name;

    public RootTestCapturedSpan(String name, Span span) {
      this.name = name;
      this.span = span;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public String getParentSpanId() {
      return getInvalid().getSpanContext().getSpanId();
    }

    @Override
    public String getSpanId() {
      return span.getSpanContext().getSpanId();
    }

    @Override
    public String getTraceId() {
      return span.getSpanContext().getTraceId();
    }

    @Override
    public Map<String, String> getAttributes() {
      return emptyMap();
    }

    @Override
    public String getServiceName() {
      return null;
    }

    @Override
    public String getSpanKindName() {
      return INTERNAL.toString();
    }

    @Override
    public List<CapturedEventData> getEvents() {
      return emptyList();
    }

    @Override
    public boolean hasErrorStatus() {
      return false;
    }

    @Override
    public String getStatusAsString() {
      return UNSET.toString();
    }

    @Override
    public long getStartEpochSpanNanos() {
      return 0l;
    }

    @Override
    public long getEndSpanEpochNanos() {
      return 1l;
    }

    @Override
    public Map<String, String> getTraceState() {
      return emptyMap();
    }
  }
}
