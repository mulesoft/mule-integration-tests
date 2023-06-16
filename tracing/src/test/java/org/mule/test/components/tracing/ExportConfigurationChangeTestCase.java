/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components.tracing;

import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_CONFIGURATION_WATCHER_DEFAULT_DELAY_PROPERTY;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_ENABLED;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_ENDPOINT;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_TRACING_CONFIGURATION_FILE_PATH;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;
import static org.mule.test.components.tracing.OpenTelemetryProtobufSpanUtils.getSpans;
import static org.mule.test.infrastructure.profiling.tracing.TracingTestUtils.createAttributeMap;
import static org.mule.test.infrastructure.profiling.tracing.TracingTestUtils.getDefaultAttributesToAssertExistence;

import static java.io.File.createTempFile;
import static java.lang.Boolean.TRUE;
import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
import static java.lang.Thread.sleep;
import static java.nio.file.Files.copy;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import static com.linecorp.armeria.common.HttpResponse.from;
import static com.linecorp.armeria.common.HttpStatus.OK;
import static com.linecorp.armeria.common.HttpStatus.REQUEST_TIMEOUT;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.tracer.api.sniffer.CapturedExportedSpan;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.infrastructure.profiling.tracing.SpanTestHierarchy;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class ExportConfigurationChangeTestCase extends
    MuleArtifactFunctionalTestCase implements OpenTelemetryTracingTestRunnerConfigAnnotation {

  private static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  private static final String EXPECTED_SET_PAYLOAD_SPAN_NAME = "mule:set-payload";
  private static final String FLOW_LOCATION = "flow";

  private static final String TEST_ARTIFACT_ID = "ExportConfigurationChangeTestCase#test";

  public static final int TIMEOUT_MILLIS = 30000;

  private static final int POLL_DELAY_MILLIS = 100;
  public static final int MAX_BACKOFF_ATTEMPTS = 2;

  private static final String SET_PAYLOAD_LOCATION = "flow/processors/0";
  private Path configFile;
  private File file;

  @Override
  protected String getConfigFile() {
    return "tracing/export-configuration-change.xml";
  }

  @ClassRule
  public static final TestServerRule originalServer = new TestServerRule();

  @ClassRule
  public static final TestServerRule afterConfigurationChangeServer = new TestServerRule();

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    file = createTempFile("tracing", "test");
    configFile = Paths.get(getClass().getResource("/tracing/config/exporter.conf").getPath());
    copy(configFile, Paths.get(file.getPath()), REPLACE_EXISTING);
    setProperty(MULE_OPEN_TELEMETRY_TRACING_CONFIGURATION_FILE_PATH, file.getAbsolutePath());
    setProperty(MULE_OPEN_TELEMETRY_EXPORTER_ENABLED, TRUE.toString());
    setProperty(MULE_OPEN_TELEMETRY_EXPORTER_ENDPOINT,
                "http://localhost:" + originalServer.httpPort());
    setProperty(MULE_OPEN_TELEMETRY_EXPORTER_CONFIGURATION_WATCHER_DEFAULT_DELAY_PROPERTY, "1000");
  }

  @After
  public void after() {
    clearProperty(MULE_OPEN_TELEMETRY_TRACING_CONFIGURATION_FILE_PATH);
    clearProperty(MULE_OPEN_TELEMETRY_EXPORTER_ENDPOINT);
    clearProperty(MULE_OPEN_TELEMETRY_EXPORTER_CONFIGURATION_WATCHER_DEFAULT_DELAY_PROPERTY);
  }

  @Test
  public void test() throws Exception {
    flowRunner(FLOW_LOCATION).withPayload(TEST_PAYLOAD).run().getMessage();

    pollTillExportedSpansCaptured(originalServer);

    List<String> attributesToAssertExistence = getDefaultAttributesToAssertExistence();

    Collection<CapturedExportedSpan> exportedSpans = originalServer.getCapturedExportedSpans();

    Map<String, String> setPayloadAttributeMap = createAttributeMap(SET_PAYLOAD_LOCATION, TEST_ARTIFACT_ID);

    assertExpectedSpanTree(attributesToAssertExistence, exportedSpans, setPayloadAttributeMap);

    // We update the file by recopying it.
    setProperty(MULE_OPEN_TELEMETRY_EXPORTER_ENDPOINT,
                "http://localhost:" + afterConfigurationChangeServer.httpPort());
    copy(configFile, Paths.get(file.getPath()), REPLACE_EXISTING);

    // We wait for the configuration to take place.
    sleep(1000);

    flowRunner(FLOW_LOCATION).withPayload(TEST_PAYLOAD).run().getMessage();
    pollTillExportedSpansCaptured(afterConfigurationChangeServer);

    exportedSpans = afterConfigurationChangeServer.getCapturedExportedSpans();

    assertExpectedSpanTree(attributesToAssertExistence, exportedSpans, setPayloadAttributeMap);
  }

  private static void pollTillExportedSpansCaptured(TestServerRule server) {
    new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS).check(new JUnitProbe() {

      @Override
      protected boolean test() {
        Collection<CapturedExportedSpan> exportedSpans = server.getCapturedExportedSpans();
        return exportedSpans.size() == 2;
      }

      @Override
      public String describeFailure() {
        return "The exact amount of spans was not captured";
      }
    });
  }

  private static void assertExpectedSpanTree(List<String> attributesToAssertExistence,
                                             Collection<CapturedExportedSpan> exportedSpans,
                                             Map<String, String> setPayloadAttributeMap) {
    SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
    expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
        .addAttributesToAssertValue(createAttributeMap(FLOW_LOCATION, TEST_ARTIFACT_ID))
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
