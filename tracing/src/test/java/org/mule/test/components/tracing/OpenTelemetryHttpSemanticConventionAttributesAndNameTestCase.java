/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.components.tracing;

import static org.mule.runtime.api.util.MuleSystemProperties.ADD_MULE_SPECIFIC_TRACING_INFORMATION_IN_TRACE_STATE_PROPERTY;
import static org.mule.runtime.api.util.MuleSystemProperties.TRACING_LEVEL_CONFIGURATION_PATH;
import static org.mule.runtime.tracer.customization.api.InternalSpanNames.GET_CONNECTION_SPAN_NAME;
import static org.mule.runtime.tracer.customization.api.InternalSpanNames.OPERATION_EXECUTION_SPAN_NAME;
import static org.mule.runtime.tracer.customization.api.InternalSpanNames.PARAMETERS_RESOLUTION_SPAN_NAME;
import static org.mule.runtime.tracer.customization.api.InternalSpanNames.VALUE_RESOLUTION_SPAN_NAME;
import static org.mule.runtime.tracing.level.api.config.TracingLevel.DEBUG;
import static org.mule.runtime.tracing.level.api.config.TracingLevel.MONITORING;
import static org.mule.runtime.tracing.level.api.config.TracingLevel.OVERVIEW;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;
import static org.mule.test.infrastructure.profiling.tracing.SpanTestHierarchy.UNSET_STATUS;

import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
import static java.util.Arrays.asList;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.core.privileged.profiling.PrivilegedProfilingService;
import org.mule.runtime.tracer.api.sniffer.CapturedExportedSpan;
import org.mule.runtime.tracer.api.sniffer.ExportedSpanSniffer;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
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
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
@RunnerDelegateTo(Parameterized.class)
public class OpenTelemetryHttpSemanticConventionAttributesAndNameTestCase extends MuleArtifactFunctionalTestCase
    implements OpenTelemetryTracingTestRunnerConfigAnnotation {

  @Rule
  public SystemProperty addAncestorSpanId =
      new SystemProperty(ADD_MULE_SPECIFIC_TRACING_INFORMATION_IN_TRACE_STATE_PROPERTY, "true");

  private static final int TIMEOUT_MILLIS = 30000;
  private static final int POLL_DELAY_MILLIS = 100;

  private static final String STARTING_FLOW = "startingFlow";
  private static final String HTTP_FLOW_URI_PARAMS = "httpRequestUriParams";
  private static final String HTTP_LISTENER_ERROR_200_FLOW = "httpListenerErrorButReturns200";
  public static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  private static final String EXPECTED_HTTP_REQUEST_SPAN_NAME = "GET";
  private static final String EXPECTED_HTTP_FLOW_SPAN_NAME = "GET /test";
  private static final String EXPECTED_HTTP_URI_PARAMS_FLOW_SPAN_NAME = "GET /{uriParam1}/{uriParam2}";
  private static final String EXPECTED_HTTP_FLOW_SPAN_NAME_200 = "GET /test200";
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
  public static final String HTTP_ROUTE = "http.route";
  public static final String SPAN_KIND_ATTRIBUTE = "span.kind.override";
  public static final String SPAN_STATUS_ATTRIBUTE = "status.override";
  public static final String ANCESTOR_MULE_SPAN_ID = "ancestor-mule-span-id";

  private final String tracingLevel;
  private final int expectedSpansForSuccessCount;
  private final Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> spanHierarchySuccessRetriever;
  private final Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> spanHierarchySuccessUriRetriever;
  private final int expectedSpansForErrorCount;
  private final Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> spanHierarchyErrorRetriever;

  @Inject
  PrivilegedProfilingService profilingService;

  @Parameterized.Parameters(name = "tracingLevel: {0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {OVERVIEW.name(), 3, getOverviewExpectedSpanTestHierarchyForSuccessFlow(EXPECTED_HTTP_FLOW_SPAN_NAME), 3,
            getOverviewExpectedSpanTestHierarchyForErrorFlow(),
            getOverviewExpectedSpanTestHierarchyForSuccessFlow(EXPECTED_HTTP_URI_PARAMS_FLOW_SPAN_NAME)},
        {MONITORING.name(), 4, getMonitoringExpectedSpanTestHierarchyForSuccessFlow(EXPECTED_HTTP_FLOW_SPAN_NAME), 5,
            getMonitoringExpectedSpanTestHierarchyForErrorFlow(),
            getMonitoringExpectedSpanTestHierarchyForSuccessFlow(EXPECTED_HTTP_URI_PARAMS_FLOW_SPAN_NAME)},
        {DEBUG.name(), 19, getDebugExpectedSpanTestHierarchyForSuccessFlow(EXPECTED_HTTP_FLOW_SPAN_NAME), 20,
            getDebugExpectedSpanTestHierarchyForErrorFlow(),
            getDebugExpectedSpanTestHierarchyForSuccessFlow(EXPECTED_HTTP_URI_PARAMS_FLOW_SPAN_NAME)}
    });
  }

  public OpenTelemetryHttpSemanticConventionAttributesAndNameTestCase(String tracingLevel,
                                                                      int expectedSpansForSuccessCount,
                                                                      Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> spanHierarchySuccessRetriever,
                                                                      int expectedSpansForErrorCount,
                                                                      Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> spanHierarchyErrorRetriever,
                                                                      Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> spanHierarchySuccessUriRetriever) {
    this.tracingLevel = tracingLevel;
    this.expectedSpansForSuccessCount = expectedSpansForSuccessCount;
    this.expectedSpansForErrorCount = expectedSpansForErrorCount;
    this.spanHierarchySuccessRetriever = spanHierarchySuccessRetriever;
    this.spanHierarchyErrorRetriever = spanHierarchyErrorRetriever;
    this.spanHierarchySuccessUriRetriever = spanHierarchySuccessUriRetriever;
  }

  private static Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> getOverviewExpectedSpanTestHierarchyForSuccessFlow(String httpFlowSpanName) {
    return exportedSpans -> {
      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_HTTP_REQUEST_SPAN_NAME)
          .beginChildren()
          .child(httpFlowSpanName)
          .addTraceStateKeyPresentAssertion("ancestor-mule-span-id")
          .endChildren();

      return expectedSpanHierarchy;
    };
  }

  private static Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> getMonitoringExpectedSpanTestHierarchyForSuccessFlow(String httpFlowSpanName) {
    return exportedSpans -> {
      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_HTTP_REQUEST_SPAN_NAME)
          .beginChildren()
          .child(httpFlowSpanName)
          .addTraceStateKeyPresentAssertion(ANCESTOR_MULE_SPAN_ID)
          .beginChildren()
          .child(EXPECTED_LOGGER_SPAN_NAME)
          .addTraceStateKeyNotPresentAssertion(ANCESTOR_MULE_SPAN_ID)
          .endChildren()
          .endChildren();

      return expectedSpanHierarchy;
    };
  }

  private static Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> getDebugExpectedSpanTestHierarchyForSuccessFlow(String httpFlowSpanName) {
    return exportedSpans -> {
      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_HTTP_REQUEST_SPAN_NAME)
          .beginChildren()
          .child(PARAMETERS_RESOLUTION_SPAN_NAME)
          .beginChildren()
          .child(VALUE_RESOLUTION_SPAN_NAME)
          .child(VALUE_RESOLUTION_SPAN_NAME)
          .child(VALUE_RESOLUTION_SPAN_NAME)
          .child(VALUE_RESOLUTION_SPAN_NAME)
          .child(VALUE_RESOLUTION_SPAN_NAME)
          .child(VALUE_RESOLUTION_SPAN_NAME)
          .child(VALUE_RESOLUTION_SPAN_NAME)
          .child(VALUE_RESOLUTION_SPAN_NAME)
          .child(VALUE_RESOLUTION_SPAN_NAME)
          .child(VALUE_RESOLUTION_SPAN_NAME)
          .child(VALUE_RESOLUTION_SPAN_NAME)
          .child(VALUE_RESOLUTION_SPAN_NAME)
          .endChildren()
          .child(GET_CONNECTION_SPAN_NAME)
          .child(OPERATION_EXECUTION_SPAN_NAME)
          .beginChildren()
          .child(httpFlowSpanName)
          .addTraceStateKeyPresentAssertion(ANCESTOR_MULE_SPAN_ID)
          .beginChildren()
          .child(EXPECTED_LOGGER_SPAN_NAME)
          .addTraceStateKeyNotPresentAssertion(ANCESTOR_MULE_SPAN_ID)
          .endChildren()
          .endChildren()
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
          .child(PARAMETERS_RESOLUTION_SPAN_NAME)
          .beginChildren()
          .child(VALUE_RESOLUTION_SPAN_NAME)
          .child(VALUE_RESOLUTION_SPAN_NAME)
          .child(VALUE_RESOLUTION_SPAN_NAME)
          .child(VALUE_RESOLUTION_SPAN_NAME)
          .child(VALUE_RESOLUTION_SPAN_NAME)
          .child(VALUE_RESOLUTION_SPAN_NAME)
          .child(VALUE_RESOLUTION_SPAN_NAME)
          .child(VALUE_RESOLUTION_SPAN_NAME)
          .child(VALUE_RESOLUTION_SPAN_NAME)
          .child(VALUE_RESOLUTION_SPAN_NAME)
          .child(VALUE_RESOLUTION_SPAN_NAME)
          .child(VALUE_RESOLUTION_SPAN_NAME)
          .endChildren()
          .child(GET_CONNECTION_SPAN_NAME)
          .child(OPERATION_EXECUTION_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_HTTP_FLOW_SPAN_NAME_200).addExceptionData("ANY:EXPECTED").addStatusData(UNSET_STATUS)
          .addTraceStateKeyPresentAssertion(ANCESTOR_MULE_SPAN_ID)
          .beginChildren()
          .child(EXPECTED_RAISE_ERROR_SPAN).addExceptionData("ANY:EXPECTED")
          .addTraceStateKeyNotPresentAssertion(ANCESTOR_MULE_SPAN_ID)
          .child(EXPECTED_ON_ERROR_PROPAGATE_SPAN_NAME)
          .addTraceStateKeyNotPresentAssertion(ANCESTOR_MULE_SPAN_ID)
          .endChildren()
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
          .child(EXPECTED_HTTP_FLOW_SPAN_NAME_200).addExceptionData("ANY:EXPECTED").addStatusData(UNSET_STATUS)
          .beginChildren()
          .child(EXPECTED_RAISE_ERROR_SPAN).addExceptionData("ANY:EXPECTED")
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
          .child(EXPECTED_HTTP_FLOW_SPAN_NAME_200).addExceptionData("ANY:EXPECTED").addStatusData(UNSET_STATUS)
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


  @After
  public void doAfter() {
    // TODO W-13160648: Add a Rule for selecting LEVEL of tracing in integration test and make it work in parallel
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

      assertThat(listenerExportedSpan.getAttributes(), aMapWithSize(16));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(NET_HOST_NAME, "0.0.0.0"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_TARGET, "/test"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_SCHEME, "http"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_FLAVOR, "1.1"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_USER_AGENT, "AHC/1.0"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(NET_HOST_PORT, httpPort.getValue()));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_METHOD, "GET"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_STATUS_CODE, "200"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_ROUTE, "/test"));
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
  public void testSuccessFlowWithUriParams() throws Exception {
    ExportedSpanSniffer spanCapturer = profilingService.getSpanExportManager().getExportedSpanSniffer();

    try {
      flowRunner(HTTP_FLOW_URI_PARAMS).run();

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

      spanHierarchySuccessUriRetriever.apply(exportedSpans).assertSpanTree();

      CapturedExportedSpan listenerExportedSpan =
          exportedSpans.stream().filter(exportedSpan -> exportedSpan.getName().equals(EXPECTED_HTTP_URI_PARAMS_FLOW_SPAN_NAME))
              .findFirst()
              .orElseThrow(() -> new AssertionFailedError("No span for http listener flow found!"));

      assertThat(listenerExportedSpan.getAttributes(), aMapWithSize(16));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(NET_HOST_NAME, "0.0.0.0"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_TARGET, "/{uriParam1}/{uriParam2}"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_SCHEME, "http"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_FLAVOR, "1.1"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_USER_AGENT, "AHC/1.0"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(NET_HOST_PORT, httpPort.getValue()));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_METHOD, "GET"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_STATUS_CODE, "200"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_ROUTE, "/{uriParam1}/{uriParam2}"));
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
      assertThat(requestExportedSpan.getAttributes(),
                 hasEntry(HTTP_URL, "http://localhost:" + httpPort.getValue() + "/param1/param2"));
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

      assertThat(listenerExportedSpan.getAttributes(), aMapWithSize(16));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(NET_HOST_NAME, "0.0.0.0"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_TARGET, "/test200"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_SCHEME, "http"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_FLAVOR, "1.1"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_USER_AGENT, "AHC/1.0"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(NET_HOST_PORT, httpPort.getValue()));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_METHOD, "GET"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_STATUS_CODE, "200"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_ROUTE, "/test200"));
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
