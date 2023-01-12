/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.components.tracing;

import static org.mule.runtime.tracer.exporter.api.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_BACKOFF_MAX_ATTEMPTS;
import static org.mule.runtime.tracer.exporter.api.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_ENABLED;
import static org.mule.runtime.tracer.exporter.api.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_ENDPOINT;
import static org.mule.runtime.tracer.exporter.api.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_TYPE;
import static org.mule.test.components.tracing.OpenTelemetryProtobufSpanUtils.getSpans;

import static java.lang.Boolean.TRUE;
import static java.lang.String.valueOf;
import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
import static java.util.Arrays.asList;

import static com.linecorp.armeria.common.HttpResponse.from;
import static com.linecorp.armeria.common.HttpStatus.OK;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.tracer.api.sniffer.CapturedExportedSpan;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.infrastructure.profiling.tracing.SpanTestHierarchy;
import org.mule.test.runner.RunnerDelegateTo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.server.AbstractHttpService;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.testing.junit4.server.ServerRule;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
public class DistributedTracingTestCase extends
    MuleArtifactFunctionalTestCase implements TracingTestRunnerConfigAnnotation {

  @Rule
  public DynamicPort httpPort = new DynamicPort("port");

  private static final String STARTING_FLOW = "startingFlow";

  private static final String EXPECTED_HTTP_REQUEST_SPAN_NAME = "HTTP GET";
  private static final String EXPECTED_HTTP_FLOW_SPAN_NAME = "/test";
  private static final String EXPECTED_LOGGER_SPAN_NAME = "mule:logger";
  public static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";

  public static final int TIMEOUT_MILLIS = 30000;

  private static final int POLL_DELAY_MILLIS = 100;
  public static final int MAX_BACKOFF_ATTEMPTS = 2;

  private final String type;
  private final String path;

  @Override
  protected String getConfigFile() {
    return "tracing/distributed-tracing.xml";
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

  public DistributedTracingTestCase(String type, String path) {
    this.type = type;
    this.path = path;
  }

  @Before
  public void before() {
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
  public void testDistributedTracing() throws Exception {
    flowRunner(STARTING_FLOW).run();
    PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);

    prober.check(new JUnitProbe() {

      @Override
      protected boolean test() {
        Collection<CapturedExportedSpan> exportedSpans = httpServer.getCapturedExportedSpans();
        return exportedSpans.size() == 4;
      }

      @Override
      public String describeFailure() {
        return "The exact amount of spans was not captured";
      }
    });

    Collection<CapturedExportedSpan> exportedSpans = httpServer.getCapturedExportedSpans();


    SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
    expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
        .beginChildren()
        .child(EXPECTED_HTTP_REQUEST_SPAN_NAME)
        .beginChildren()
        .child(EXPECTED_HTTP_FLOW_SPAN_NAME)
        .beginChildren()
        .child(EXPECTED_LOGGER_SPAN_NAME)
        .endChildren()
        .endChildren();

    expectedSpanHierarchy.assertSpanTree();
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
  }
}