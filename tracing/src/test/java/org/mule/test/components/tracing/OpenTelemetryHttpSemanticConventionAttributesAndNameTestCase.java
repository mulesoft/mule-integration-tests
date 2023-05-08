/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.components.tracing;

import static org.mule.runtime.api.util.MuleSystemProperties.TRACING_LEVEL_CONFIGURATION_PATH;
import static org.mule.runtime.tracer.customization.api.InternalSpanNames.GET_CONNECTION_SPAN_NAME;
import static org.mule.runtime.tracing.level.api.config.TracingLevel.DEBUG;
import static org.mule.runtime.tracing.level.api.config.TracingLevel.MONITORING;
import static org.mule.runtime.tracing.level.api.config.TracingLevel.OVERVIEW;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;

import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
import static java.util.Arrays.asList;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.tracer.api.sniffer.CapturedExportedSpan;
import org.mule.runtime.tracer.api.sniffer.ExportedSpanSniffer;
import org.mule.runtime.core.privileged.profiling.PrivilegedProfilingService;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.infrastructure.profiling.tracing.SpanTestHierarchy;
import org.mule.test.runner.RunnerDelegateTo;

import java.nio.file.FileSystems;
import java.util.Collection;
import java.util.function.Function;

import javax.inject.Inject;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import junit.framework.AssertionFailedError;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
@RunnerDelegateTo(Parameterized.class)
public class OpenTelemetryHttpSemanticConventionAttributesAndNameTestCase extends MuleArtifactFunctionalTestCase
    implements OpenTelemetryTracingTestRunnerConfigAnnotation {

  private static final int TIMEOUT_MILLIS = 30000;
  private static final int POLL_DELAY_MILLIS = 100;

  private static final String STARTING_FLOW = "startingFlow";
  private static final String HTTP_LISTENER_ERROR_200_FLOW = "httpListenerErrorButReturns200";
  public static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  private static final String EXPECTED_HTTP_REQUEST_SPAN_NAME = "HTTP GET";
  private static final String EXPECTED_HTTP_FLOW_SPAN_NAME = "/test";
  private static final String EXPECTED_HTTP_FLOW_SPAN_NAME_200 = "/test200";
  private static final String EXPECTED_LOGGER_SPAN_NAME = "mule:logger";
  public static final String EXPECTED_ON_ERROR_PROPAGATE_SPAN_NAME = "mule:on-error-propagate";
  public static final String EXPECTED_RAISE_ERROR_SPAN = "mule:raise-error";
  public static final String NET_PEER_NAME = "net.peer.name";
  public static final String NET_PEER_PORT = "net.peer.port";
  public static final String HTTP_URL = "http.url";
  public static final String HTTP_METHOD = "http.method";
  public static final String HTTP_FLAVOR = "http.flavor";
  public static final String NET_HOST_NAME = "net.host.name";
  public static final String HTTP_TARGET = "http.target";
  public static final String HTTP_USER_AGENT = "http.user_agent";
  public static final String NET_HOST_PORT = "net.host.port";
  public static final String HTTP_SCHEME = "http.scheme";
  public static final String HTTP_STATUS_CODE = "http.status_code";
  public static final String SPAN_KIND_ATTRIBUTE = "span.kind.override";
  public static final String SPAN_STATUS_ATTRIBUTE = "status.override";

  private final String tracingLevel;
  private final int expectedSpansForSuccessCount;
  private final Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> spanHierarchySuccessRetriever;
  private final int expectedSpansForErrorCount;
  private final Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> spanHierarchyErrorRetriever;

  @Inject
  PrivilegedProfilingService profilingService;

  @Parameterized.Parameters(name = "tracingLevel: {0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {OVERVIEW.name(), 3, getOverviewExpectedSpanTestHierarchyForSuccessFlow(), 3,
            getOverviewExpectedSpanTestHierarchyForErrorFlow()},
        {MONITORING.name(), 4, getMonitoringExpectedSpanTestHierarchyForSuccessFlow(), 5,
            getMonitoringExpectedSpanTestHierarchyForErrorFlow()},
        {DEBUG.name(), 5, getDebugExpectedSpanTestHierarchyForSuccessFlow(), 6, getDebugExpectedSpanTestHierarchyForErrorFlow()}
    });
  }

  public OpenTelemetryHttpSemanticConventionAttributesAndNameTestCase(String tracingLevel,
                                                                      int expectedSpansForSuccessCount,
                                                                      Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> spanHierarchySuccessRetriever,
                                                                      int expectedSpansForErrorCount,
                                                                      Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> spanHierarchyErrorRetriever) {
    this.tracingLevel = tracingLevel;
    this.expectedSpansForSuccessCount = expectedSpansForSuccessCount;
    this.expectedSpansForErrorCount = expectedSpansForErrorCount;
    this.spanHierarchySuccessRetriever = spanHierarchySuccessRetriever;
    this.spanHierarchyErrorRetriever = spanHierarchyErrorRetriever;
  }

  private static Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> getOverviewExpectedSpanTestHierarchyForSuccessFlow() {
    return exportedSpans -> {
      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_HTTP_REQUEST_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_HTTP_FLOW_SPAN_NAME)
          .endChildren();

      return expectedSpanHierarchy;
    };
  }

  private static Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> getMonitoringExpectedSpanTestHierarchyForSuccessFlow() {
    return exportedSpans -> {
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

      return expectedSpanHierarchy;
    };
  }

  private static Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> getDebugExpectedSpanTestHierarchyForSuccessFlow() {
    return exportedSpans -> {
      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_HTTP_REQUEST_SPAN_NAME)
          .beginChildren()
          .child(GET_CONNECTION_SPAN_NAME)
          .child(EXPECTED_HTTP_FLOW_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_LOGGER_SPAN_NAME)
          .endChildren()
          .endChildren();

      return expectedSpanHierarchy;
    };
  }

  private static Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> getDebugExpectedSpanTestHierarchyForErrorFlow() {
    return exportedSpans -> {
      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_HTTP_REQUEST_SPAN_NAME)
          .beginChildren()
          .child(GET_CONNECTION_SPAN_NAME)
          .child(EXPECTED_HTTP_FLOW_SPAN_NAME_200)
          .beginChildren()
          .child(EXPECTED_RAISE_ERROR_SPAN)
          .child(EXPECTED_ON_ERROR_PROPAGATE_SPAN_NAME)
          .endChildren()
          .endChildren()
          .endChildren();

      return expectedSpanHierarchy;
    };
  }

  private static Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> getMonitoringExpectedSpanTestHierarchyForErrorFlow() {
    return exportedSpans -> {
      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_HTTP_REQUEST_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_HTTP_FLOW_SPAN_NAME_200)
          .beginChildren()
          .child(EXPECTED_RAISE_ERROR_SPAN)
          .child(EXPECTED_ON_ERROR_PROPAGATE_SPAN_NAME)
          .endChildren()
          .endChildren()
          .endChildren();

      return expectedSpanHierarchy;
    };
  }

  private static Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> getOverviewExpectedSpanTestHierarchyForErrorFlow() {
    return exportedSpans -> {
      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_HTTP_REQUEST_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_HTTP_FLOW_SPAN_NAME_200)
          .endChildren();

      return expectedSpanHierarchy;
    };
  }

  @Rule
  public DynamicPort httpPort = new DynamicPort("port");

  @Override
  protected String getConfigFile() {
    return "tracing/http-semantic-conventions-tracing.xml";
  }

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    setProperty(TRACING_LEVEL_CONFIGURATION_PATH, tracingLevel.toLowerCase() + FileSystems.getDefault().getSeparator());
    super.doSetUpBeforeMuleContextCreation();
  }


  public void doAfter() {
    clearProperty(TRACING_LEVEL_CONFIGURATION_PATH);
  }

  @Test
  public void testSuccessFlow() throws Exception {
    ExportedSpanSniffer spanCapturer = profilingService.getSpanExportManager().getExportedSpanSniffer();

    try {
      flowRunner(STARTING_FLOW).run();

      PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);

      prober.check(new JUnitProbe() {

        @Override
        protected boolean test() {
          Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();
          return exportedSpans.size() == expectedSpansForSuccessCount;
        }

        @Override
        public String describeFailure() {
          return "The exact amount of spans was not captured";
        }
      });

      Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();

      spanHierarchySuccessRetriever.apply(exportedSpans).assertSpanTree();

      CapturedExportedSpan listenerExportedSpan =
          exportedSpans.stream().filter(exportedSpan -> exportedSpan.getName().equals(EXPECTED_HTTP_FLOW_SPAN_NAME))
              .findFirst()
              .orElseThrow(() -> new AssertionFailedError("No span for http listener flow found!"));

      assertThat(listenerExportedSpan.getAttributes(), aMapWithSize(15));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(NET_HOST_NAME, "0.0.0.0"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_TARGET, "/test"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_SCHEME, "http"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_FLAVOR, "1.1"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_USER_AGENT, "AHC/1.0"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(NET_HOST_PORT, httpPort.getValue()));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_METHOD, "GET"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_STATUS_CODE, "200"));
      assertThat(listenerExportedSpan.getAttributes().get(SPAN_KIND_ATTRIBUTE), nullValue());
      assertThat(listenerExportedSpan.getAttributes().get(SPAN_STATUS_ATTRIBUTE), nullValue());
      assertThat(listenerExportedSpan.getSpanKindName(), equalTo("SERVER"));
      assertThat(listenerExportedSpan.hasErrorStatus(), equalTo(false));
      assertThat(listenerExportedSpan.getStatusAsString(), equalTo("UNSET"));

      CapturedExportedSpan requestExportedSpan =
          exportedSpans.stream().filter(exportedSpan -> exportedSpan.getName().equals(EXPECTED_HTTP_REQUEST_SPAN_NAME))
              .findFirst()
              .orElseThrow(() -> new AssertionFailedError("No span for http request flow found!"));

      assertThat(requestExportedSpan.getAttributes(), aMapWithSize(13));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(NET_PEER_NAME, "localhost"));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(NET_PEER_PORT, httpPort.getValue()));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(HTTP_URL, "http://localhost:" + httpPort.getValue() + "/test"));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(HTTP_METHOD, "GET"));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(HTTP_FLAVOR, "1.1"));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(HTTP_STATUS_CODE, "200"));
      assertThat(requestExportedSpan.getAttributes().get(SPAN_KIND_ATTRIBUTE), nullValue());
      assertThat(requestExportedSpan.getAttributes().get(SPAN_STATUS_ATTRIBUTE), nullValue());
      assertThat(requestExportedSpan.getSpanKindName(), equalTo("CLIENT"));
      assertThat(requestExportedSpan.hasErrorStatus(), equalTo(false));
      assertThat(requestExportedSpan.getStatusAsString(), equalTo("UNSET"));
    } finally {
      spanCapturer.dispose();
    }
  }

  @Test
  public void testWhenHTTPListenerFlowThrowsErrorButReturns200SpanStatusShouldNotBeSetAsError() throws Exception {
    ExportedSpanSniffer spanCapturer = profilingService.getSpanExportManager().getExportedSpanSniffer();

    try {
      PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);
      flowRunner(HTTP_LISTENER_ERROR_200_FLOW).dispatch();

      prober.check(new JUnitProbe() {

        @Override
        protected boolean test() {
          Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();
          return exportedSpans.size() == expectedSpansForErrorCount;
        }

        @Override
        public String describeFailure() {
          return "The exact amount of spans was not captured";
        }
      });

      Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();

      spanHierarchyErrorRetriever.apply(exportedSpans).assertSpanTree();

      CapturedExportedSpan listenerExportedSpan =
          exportedSpans.stream().filter(exportedSpan -> exportedSpan.getName().equals(EXPECTED_HTTP_FLOW_SPAN_NAME_200))
              .findFirst()
              .orElseThrow(() -> new AssertionFailedError("No span for http listener flow found!"));

      assertThat(listenerExportedSpan.getAttributes(), aMapWithSize(15));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(NET_HOST_NAME, "0.0.0.0"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_TARGET, "/test200"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_SCHEME, "http"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_FLAVOR, "1.1"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_USER_AGENT, "AHC/1.0"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(NET_HOST_PORT, httpPort.getValue()));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_METHOD, "GET"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_STATUS_CODE, "200"));
      assertThat(listenerExportedSpan.getAttributes().get(SPAN_KIND_ATTRIBUTE), nullValue());
      assertThat(listenerExportedSpan.getAttributes().get(SPAN_STATUS_ATTRIBUTE), nullValue());
      assertThat(listenerExportedSpan.getSpanKindName(), equalTo("SERVER"));
      assertThat(listenerExportedSpan.hasErrorStatus(), equalTo(false));
      assertThat(listenerExportedSpan.getStatusAsString(), equalTo("UNSET"));

      CapturedExportedSpan requestExportedSpan =
          exportedSpans.stream().filter(exportedSpan -> exportedSpan.getName().equals(EXPECTED_HTTP_REQUEST_SPAN_NAME))
              .findFirst()
              .orElseThrow(() -> new AssertionFailedError("No span for http request flow found!"));

      assertThat(requestExportedSpan.getAttributes(), aMapWithSize(13));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(NET_PEER_NAME, "localhost"));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(NET_PEER_PORT, httpPort.getValue()));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(HTTP_URL, "http://localhost:" + httpPort.getValue() + "/test200"));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(HTTP_METHOD, "GET"));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(HTTP_FLAVOR, "1.1"));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(HTTP_STATUS_CODE, "200"));
      assertThat(requestExportedSpan.getAttributes().get(SPAN_KIND_ATTRIBUTE), nullValue());
      assertThat(requestExportedSpan.getAttributes().get(SPAN_STATUS_ATTRIBUTE), nullValue());
      assertThat(requestExportedSpan.getSpanKindName(), equalTo("CLIENT"));
      assertThat(requestExportedSpan.hasErrorStatus(), equalTo(false));
      assertThat(requestExportedSpan.getStatusAsString(), equalTo("UNSET"));
    } finally {
      spanCapturer.dispose();
    }
  }
}
