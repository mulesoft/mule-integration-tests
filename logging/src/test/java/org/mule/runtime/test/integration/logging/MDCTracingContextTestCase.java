/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.test.integration.logging;


import static org.mule.runtime.api.util.MuleSystemProperties.ENABLE_PROFILING_SERVICE_PROPERTY;
import static org.mule.runtime.api.util.MuleSystemProperties.PUT_TRACE_ID_AND_SPAN_ID_IN_MDC_PROPERTY;
import static org.mule.runtime.test.integration.logging.plugin.TestPluginsCatalog.loggingExtensionPlugin;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_ENABLED;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_ENDPOINT;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_TYPE;
import static org.mule.test.allure.AllureConstants.Logging.LOGGING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_PROFILING_SERVICE;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.TRACING_CONFIGURATION;

import static java.lang.Boolean.TRUE;
import static java.util.regex.Pattern.compile;

import static org.apache.commons.io.FileUtils.readLines;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.slf4j.bridge.SLF4JBridgeHandler.install;
import static org.slf4j.bridge.SLF4JBridgeHandler.removeHandlersForRootLogger;
import static org.slf4j.bridge.SLF4JBridgeHandler.uninstall;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.tracer.api.sniffer.CapturedExportedSpan;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.infrastructure.deployment.AbstractFakeMuleServerTestCase;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.server.AbstractHttpService;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.testing.junit4.server.ServerRule;

import org.jetbrains.annotations.NotNull;

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Story;

@Features({@Feature(DEFAULT_PROFILING_SERVICE), @Feature(LOGGING)})
@Story(TRACING_CONFIGURATION)
public class MDCTracingContextTestCase extends AbstractFakeMuleServerTestCase {

  @Rule
  public SystemProperty propagateDisposeError = new SystemProperty(PUT_TRACE_ID_AND_SPAN_ID_IN_MDC_PROPERTY, TRUE.toString());

  private static final int TIMEOUT_MILLIS = 30000;
  private static final int POLL_DELAY_MILLIS = 100;

  private static final Pattern pattern = compile("span-id: ([^;]+); trace-id: ([^;]+)");;

  @Rule
  public SystemProperty openTelemetryExporterEnabled = new SystemProperty(MULE_OPEN_TELEMETRY_EXPORTER_ENABLED, TRUE.toString());
  @Rule
  public SystemProperty openTelemetryExporterType = new SystemProperty(MULE_OPEN_TELEMETRY_EXPORTER_TYPE, "HTTP");
  @Rule
  public SystemProperty openTelemetryExporterEndpoint = new SystemProperty(MULE_OPEN_TELEMETRY_EXPORTER_ENDPOINT,
                                                                           "http://localhost:" + httpServer.httpPort() + "/");

  @Rule
  public UseMuleLog4jContextFactory muleLogging = new UseMuleLog4jContextFactory();

  @ClassRule
  public static final TestServerRule httpServer = new TestServerRule();

  @Rule
  public DynamicPort httpPort = new DynamicPort("port");

  @Rule
  public SystemProperty enableProfilingService = new SystemProperty(ENABLE_PROFILING_SERVICE_PROPERTY, "true");

  @Override
  public void setUp() throws Exception {
    super.setUp();
    removeHandlersForRootLogger();
    install();
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
    uninstall();
  }

  @Test
  public void test() throws Exception {
    startRuntimeWithApp();
    new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS).check(new JUnitProbe() {

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

    List<CapturedExportedSpan> capturedExportedSpans = httpServer.getCapturedExportedSpans();
    verifyMDCTracingValues("First Message", "Second Message", "Non Blocking Message", capturedExportedSpans);
  }

  private void startRuntimeWithApp() throws URISyntaxException, IOException, MuleException, MalformedURLException {
    final ApplicationFileBuilder appFileBuilder =
        new ApplicationFileBuilder("logging-app").definedBy("log/tracing/logging-app.xml")
            .containingResource("log/tracing/log4j-config.xml", "log4j2.xml")
            .containingResource("log/tracing/tracer-exporter.conf", "tracer-exporter.conf")
            .dependingOn(loggingExtensionPlugin);


    appFileBuilder.dependingOn(loggingExtensionPlugin);
    muleServer.addAppArchive(appFileBuilder.getArtifactFile().toURI().toURL());
    muleServer.start();
  }

  private void verifyMDCTracingValues(String expectedFirstMessage, String expectedSecondMessage, String nonBlockingMessage,
                                      List<CapturedExportedSpan> capturedExportedSpans)
      throws IOException {
    File logFile = new File(muleServer.getLogsDir().toString() + "/test.log");

    CapturedExportedSpan loggerSpan = getUniqueSpanBuName(capturedExportedSpans, "logging:log");

    CapturedExportedSpan loggerWithMessageSpan = getUniqueSpanBuName(capturedExportedSpans, "logging:log-with-message");

    CapturedExportedSpan nonBlockingOperationMessageSpan =
        getUniqueSpanBuName(capturedExportedSpans, "logging:non-blocking-operation-log");

    List<String> logLinesForFirstMessage =
        readLines(logFile).stream().filter(line -> line.contains(expectedFirstMessage)).collect(Collectors.toList());

    List<String> logLinesForSecondMessage =
        readLines(logFile).stream().filter(line -> line.contains(expectedSecondMessage)).collect(Collectors.toList());

    List<String> logLinesWithNonBlockingOperationMessage =
        readLines(logFile).stream().filter(line -> line.contains(nonBlockingMessage)).collect(Collectors.toList());


    assertThat(logLinesForFirstMessage, hasSize(1));

    verifyLoggingTraceIdAndSpan(loggerSpan, logLinesForFirstMessage);
    verifyLoggingTraceIdAndSpan(loggerWithMessageSpan, logLinesForSecondMessage);
    verifyLoggingTraceIdAndSpan(nonBlockingOperationMessageSpan, logLinesWithNonBlockingOperationMessage);

  }

  private static void verifyLoggingTraceIdAndSpan(CapturedExportedSpan loggerSpan, List<String> logLinesForFirstMessage) {
    Matcher matcher = pattern.matcher(logLinesForFirstMessage.get(0));
    if (matcher.find()) {
      assertThat(matcher.group(1), equalTo(loggerSpan.getSpanId()));
      assertThat(matcher.group(2), equalTo(loggerSpan.getTraceId()));
    } else {
      fail("No lines found in the logs with the corresponding span id and trace id");
    }
  }

  private CapturedExportedSpan getUniqueSpanBuName(List<CapturedExportedSpan> capturedExportedSpans, String componentName) {
    List<CapturedExportedSpan> filteredCapturedSpans = capturedExportedSpans.stream()
        .filter(capturedExportedSpan -> capturedExportedSpan.getName().equals(componentName)).collect(Collectors.toList());
    assertThat(filteredCapturedSpans, hasSize(1));
    return filteredCapturedSpans.get(0);
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
                       HttpResponse res = HttpResponse.from(responseFuture);
                       try {
                         capturedExportedSpans.addAll(OpenTelemetryProtobufSpanUtils.getSpans(ExportTraceServiceRequest
                             .parseFrom(new ByteArrayInputStream(aReq.content().array()))));
                       } catch (IOException e) {
                         // Nothing to do.
                       }
                       responseFuture.complete(HttpResponse.of(HttpStatus.OK));
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
