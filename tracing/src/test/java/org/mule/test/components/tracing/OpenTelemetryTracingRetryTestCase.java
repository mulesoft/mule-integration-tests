/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.components.tracing;

import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_BACKOFF_MAX_ATTEMPTS;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_ENABLED;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_ENDPOINT;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_TYPE;
import static org.mule.test.components.tracing.OpenTelemetryProtobufSpanUtils.getSpans;
import static org.mule.test.infrastructure.profiling.tracing.TracingTestUtils.createAttributeMap;
import static org.mule.test.infrastructure.profiling.tracing.TracingTestUtils.getDefaultAttributesToAssertExistence;

import static java.lang.Boolean.TRUE;
import static java.lang.String.valueOf;
import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
import static java.util.Arrays.asList;

import static com.linecorp.armeria.common.HttpResponse.from;
import static com.linecorp.armeria.common.HttpStatus.OK;
import static com.linecorp.armeria.common.HttpStatus.REQUEST_TIMEOUT;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.tracer.api.sniffer.CapturedExportedSpan;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.infrastructure.profiling.tracing.SpanTestHierarchy;
import org.mule.test.runner.RunnerDelegateTo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.server.AbstractHttpService;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.testing.junit4.server.ServerRule;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
public class OpenTelemetryTracingRetryTestCase extends
    MuleArtifactFunctionalTestCase implements OpenTelemetryTracingTestRunnerConfigAnnotation {

  private static final int TIMEOUT_MILLIS = 30000;
  private static final int POLL_DELAY_MILLIS = 100;
  private static final int MAX_BACKOFF_ATTEMPTS = 2;
  private static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  private static final String EXPECTED_SET_PAYLOAD_SPAN_NAME = "mule:set-payload";
  private static final String FLOW_LOCATION = "retry-flow";
  private static final String SET_PAYLOAD_LOCATION = "retry-flow/processors/0";
  private static final String TEST_ARTIFACT_ID = "OpenTelemetryTracingRetryTestCase#testRetryBackoffTest";
  private final String type;
  private final String path;

  @Override
  protected String getConfigFile() {
    return "tracing/retry-backoff.xml";
  }

  @ClassRule
  public static final TestServerRule httpServer = new TestServerRule();

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        // TODO: Add the GRPC Version
        {"HTTP", ""}
    });
  }

  public OpenTelemetryTracingRetryTestCase(String type, String path) {
    this.type = type;
    this.path = path;
  }

  @Override
  protected void doSetUpBeforeMuleContextCreation() {
    setProperty(MULE_OPEN_TELEMETRY_EXPORTER_ENABLED, TRUE.toString());
    setProperty(MULE_OPEN_TELEMETRY_EXPORTER_TYPE, type);
    setProperty(MULE_OPEN_TELEMETRY_EXPORTER_ENDPOINT,
                "http://localhost:" + httpServer.httpPort() + "/" + path);
    setProperty(MULE_OPEN_TELEMETRY_EXPORTER_BACKOFF_MAX_ATTEMPTS, valueOf(MAX_BACKOFF_ATTEMPTS));
  }

  @After
  public void after() {
    clearProperty(MULE_OPEN_TELEMETRY_EXPORTER_ENABLED);
    clearProperty(MULE_OPEN_TELEMETRY_EXPORTER_TYPE);
    clearProperty(MULE_OPEN_TELEMETRY_EXPORTER_ENDPOINT);
    clearProperty(MULE_OPEN_TELEMETRY_EXPORTER_BACKOFF_MAX_ATTEMPTS);
  }

  @Test
  public void testRetryBackoffTest() throws Exception {
    flowRunner(FLOW_LOCATION).withPayload(TEST_PAYLOAD).run().getMessage();
    PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);

    prober.check(new JUnitProbe() {

      @Override
      protected boolean test() {
        Collection<CapturedExportedSpan> exportedSpans = httpServer.getCapturedExportedSpans();
        return exportedSpans.size() == 2;
      }

      @Override
      public String describeFailure() {
        return "The exact amount of spans was not captured";
      }
    });

    List<String> attributesToAssertExistence = getDefaultAttributesToAssertExistence();

    Collection<CapturedExportedSpan> exportedSpans = httpServer.getCapturedExportedSpans();

    String artifactId = TEST_ARTIFACT_ID + "[" + type + "]";

    Map<String, String> setPayloadAttributeMap = createAttributeMap(SET_PAYLOAD_LOCATION, artifactId);

    SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
    expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
        .addAttributesToAssertValue(createAttributeMap(FLOW_LOCATION, artifactId))
        .addAttributesToAssertExistence(attributesToAssertExistence)
        .beginChildren()
        .child(EXPECTED_SET_PAYLOAD_SPAN_NAME)
        .addAttributesToAssertValue(setPayloadAttributeMap)
        .addAttributesToAssertExistence(attributesToAssertExistence)
        .endChildren();

    expectedSpanHierarchy.assertSpanTree();
  }

  private static final class TestServerRule extends ServerRule {

    public static final String PATH_PATTERN = "/";

    private final List<CapturedExportedSpan> capturedExportedSpans = new ArrayList<>();

    private final AtomicInteger exportAttempts = new AtomicInteger(0);

    @Override
    protected void configure(ServerBuilder sb) {
      sb.service(PATH_PATTERN,
                 new AbstractHttpService() {

                   @Override
                   protected @NotNull HttpResponse doPost(@NotNull ServiceRequestContext ctx, @NotNull HttpRequest req) {
                     return HttpResponse.from(req.aggregate().handle((aReq, cause) -> {
                       CompletableFuture<HttpResponse> responseFuture = new CompletableFuture<>();
                       HttpResponse res = from(responseFuture);
                       if (exportAttempts.incrementAndGet() < MAX_BACKOFF_ATTEMPTS) {
                         responseFuture.complete(HttpResponse.of(REQUEST_TIMEOUT));
                         return res;
                       }
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
  }
}
