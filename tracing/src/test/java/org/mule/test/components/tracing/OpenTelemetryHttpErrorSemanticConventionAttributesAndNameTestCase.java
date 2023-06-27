/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.components.tracing;

import static org.mule.runtime.api.util.MuleSystemProperties.ADD_MULE_SPECIFIC_TRACING_INFORMATION_IN_TRACE_STATE_PROPERTY;
import static org.mule.tck.junit4.matcher.ErrorTypeMatcher.errorType;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;
import static org.mule.test.infrastructure.profiling.tracing.SpanTestHierarchy.ERROR_STATUS;
import static org.mule.test.infrastructure.profiling.tracing.SpanTestHierarchy.UNSET_STATUS;

import static java.lang.String.format;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.privileged.profiling.PrivilegedProfilingService;
import org.mule.runtime.tracer.api.sniffer.CapturedExportedSpan;
import org.mule.runtime.tracer.api.sniffer.ExportedSpanSniffer;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.infrastructure.profiling.tracing.SpanTestHierarchy;

import java.util.Collection;

import javax.inject.Inject;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import junit.framework.AssertionFailedError;
import org.junit.Rule;
import org.junit.Test;
import org.junit.Before;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class OpenTelemetryHttpErrorSemanticConventionAttributesAndNameTestCase extends MuleArtifactFunctionalTestCase
    implements OpenTelemetryTracingTestRunnerConfigAnnotation {

  @Rule
  public SystemProperty addAncestorSpanId =
      new SystemProperty(ADD_MULE_SPECIFIC_TRACING_INFORMATION_IN_TRACE_STATE_PROPERTY, "true");
  private static final int TIMEOUT_MILLIS = 30000;
  private static final int POLL_DELAY_MILLIS = 100;

  public static final String ANCESTOR_MULE_SPAN_ID = "ancestor-mule-span-id";

  private static final String HTTP_LISTENER_ERROR_FLOW = "httpListenerError";
  private static final String HTTP_LISTENER_400_FLOW = "httpListenerError400";
  private static final String HTTP_LISTENER_500_FLOW = "httpListenerError500";
  private static final String REQUEST_400_FLOW = "requestTo400";
  private static final String REQUEST_500_FLOW = "requestTo500";
  public static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  private static final String EXPECTED_HTTP_REQUEST_SPAN_NAME = "GET";
  private static final String EXPECTED_HTTPS_REQUEST_SPAN_NAME = "GET";
  private static final String EXPECTED_HTTP_FLOW_SPAN_NAME = "GET /test";
  private static final String EXPECTED_HTTP_FLOW_SPAN_NAME_400 = "GET /test400";
  private static final String EXPECTED_HTTP_FLOW_SPAN_NAME_500 = "GET /test500";
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
  public static final String HTTP_ROUTE = "http.route";
  public static final String HTTP_USER_AGENT = "http.user_agent";
  public static final String NET_HOST_PORT = "net.host.port";
  public static final String HTTP_SCHEME = "http.scheme";
  public static final String HTTP_STATUS_CODE = "http.status_code";
  public static final String SPAN_STATUS_ATTRIBUTE = "status.override";
  public static final String SPAN_KIND_ATTRIBUTE = "span.kind.override";


  @Inject
  PrivilegedProfilingService profilingService;

  @Rule
  public DynamicPort mockedServerPort = new DynamicPort("mockedServerPort");

  public final LazyValue<String> HTTP_URL_400 =
      new LazyValue<>(() -> String.format("https://localhost:%s/status/400", mockedServerPort.getValue()));
  public final LazyValue<String> HTTP_URL_500 =
      new LazyValue<>(() -> String.format("https://localhost:%s/status/500", mockedServerPort.getValue()));

  @Rule
  public DynamicPort listenerServerPort = new DynamicPort("listenerServerPort");

  @Rule
  public WireMockRule wireMock = new WireMockRule(wireMockConfig()
      .bindAddress("127.0.0.1")
      .needClientAuth(false)
      .httpsPort(mockedServerPort.getNumber()));

  @Before
  public void setUp() {
    wireMock.stubFor(get(urlMatching("/status/400")).willReturn(aResponse().withStatus(400)));
    wireMock.stubFor(get(urlMatching("/status/500")).willReturn(aResponse().withStatus(500)));
  }

  @Override
  protected String getConfigFile() {
    return "tracing/http-error-semantic-conventions-tracing.xml";
  }

  @Test
  public void testFlowRequester400() throws Exception {
    ExportedSpanSniffer spanCapturer = profilingService.getSpanExportManager().getExportedSpanSniffer();

    try {
      PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);
      flowRunner(REQUEST_400_FLOW).runExpectingException(errorType("HTTP", "BAD_REQUEST"));

      prober.check(new JUnitProbe() {

        @Override
        protected boolean test() {
          Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();
          return exportedSpans.size() == 3;
        }

        @Override
        public String describeFailure() {
          return format("The exact amount of spans was not captured. Captured spans: %s",
                        spanCapturer.getExportedSpans().toString());
        }
      });

      Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();

      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME).addExceptionData("HTTP:BAD_REQUEST")
          .beginChildren()
          .child(EXPECTED_HTTPS_REQUEST_SPAN_NAME).addExceptionData("HTTP:BAD_REQUEST")
          .child(EXPECTED_ON_ERROR_PROPAGATE_SPAN_NAME)
          .endChildren();

      expectedSpanHierarchy.assertSpanTree();

      CapturedExportedSpan requestExportedSpan =
          exportedSpans.stream().filter(exportedSpan -> exportedSpan.getName().equals(EXPECTED_HTTPS_REQUEST_SPAN_NAME))
              .findFirst()
              .orElseThrow(() -> new AssertionFailedError("No span for http request flow found!"));

      assertThat(requestExportedSpan.getAttributes(), aMapWithSize(13));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(NET_PEER_NAME, "localhost"));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(NET_PEER_PORT, mockedServerPort.getValue()));
      assertThat(requestExportedSpan.getAttributes(),
                 hasEntry(HTTP_URL, HTTP_URL_400.get()));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(HTTP_METHOD, "GET"));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(HTTP_FLAVOR, "1.1"));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(HTTP_STATUS_CODE, "400"));
      assertThat(requestExportedSpan.getAttributes().get(SPAN_KIND_ATTRIBUTE), nullValue());
      assertThat(requestExportedSpan.getAttributes().get(SPAN_STATUS_ATTRIBUTE), nullValue());
      assertThat(requestExportedSpan.getSpanKindName(), equalTo("CLIENT"));
      assertThat(requestExportedSpan.hasErrorStatus(), equalTo(true));
      assertThat(requestExportedSpan.getStatusAsString(), equalTo("ERROR"));
    } finally {
      spanCapturer.dispose();
    }
  }

  @Test
  public void testFlowRequester500() throws Exception {
    ExportedSpanSniffer spanCapturer = profilingService.getSpanExportManager().getExportedSpanSniffer();

    try {
      PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);
      flowRunner(REQUEST_500_FLOW).runExpectingException(errorType("HTTP", "INTERNAL_SERVER_ERROR"));

      prober.check(new JUnitProbe() {

        @Override
        protected boolean test() {
          Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();
          return exportedSpans.size() == 3;
        }

        @Override
        public String describeFailure() {
          return format("The exact amount of spans was not captured. Captured spans: %s",
                        spanCapturer.getExportedSpans().toString());
        }
      });

      Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();

      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME).addExceptionData("HTTP:INTERNAL_SERVER_ERROR")
          .beginChildren()
          .child(EXPECTED_HTTPS_REQUEST_SPAN_NAME).addExceptionData("HTTP:INTERNAL_SERVER_ERROR")
          .child(EXPECTED_ON_ERROR_PROPAGATE_SPAN_NAME)
          .endChildren();

      expectedSpanHierarchy.assertSpanTree();

      CapturedExportedSpan requestExportedSpan =
          exportedSpans.stream().filter(exportedSpan -> exportedSpan.getName().equals(EXPECTED_HTTPS_REQUEST_SPAN_NAME))
              .findFirst()
              .orElseThrow(() -> new AssertionFailedError("No span for http request flow found!"));
      assertThat(requestExportedSpan.getAttributes(), aMapWithSize(13));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(NET_PEER_NAME, "localhost"));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(NET_PEER_PORT, mockedServerPort.getValue()));
      assertThat(requestExportedSpan.getAttributes(),
                 hasEntry(HTTP_URL, HTTP_URL_500.get()));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(HTTP_METHOD, "GET"));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(HTTP_FLAVOR, "1.1"));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(HTTP_STATUS_CODE, "500"));
      assertThat(requestExportedSpan.getAttributes().get(SPAN_KIND_ATTRIBUTE), nullValue());
      assertThat(requestExportedSpan.getAttributes().get(SPAN_STATUS_ATTRIBUTE), nullValue());
      assertThat(requestExportedSpan.getSpanKindName(), equalTo("CLIENT"));
      assertThat(requestExportedSpan.hasErrorStatus(), equalTo(true));
      assertThat(requestExportedSpan.getStatusAsString(), equalTo("ERROR"));
    } finally {
      spanCapturer.dispose();
    }
  }

  @Test
  public void testWhenHTTPListenerFlowThrowsErrorSpanStatusShouldBeSetAsError() throws Exception {
    ExportedSpanSniffer spanCapturer = profilingService.getSpanExportManager().getExportedSpanSniffer();

    try {
      PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);
      flowRunner(HTTP_LISTENER_ERROR_FLOW).dispatch();

      prober.check(new JUnitProbe() {

        @Override
        protected boolean test() {
          Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();
          return exportedSpans.size() == 6;
        }

        @Override
        public String describeFailure() {
          return format("The exact amount of spans was not captured. Captured spans: %s",
                        spanCapturer.getExportedSpans().toString());
        }
      });

      Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();

      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME).addExceptionData("HTTP:INTERNAL_SERVER_ERROR")
          .beginChildren()
          .child(EXPECTED_HTTP_REQUEST_SPAN_NAME).addExceptionData("HTTP:INTERNAL_SERVER_ERROR")
          .beginChildren()
          .child(EXPECTED_HTTP_FLOW_SPAN_NAME).addExceptionData("CUSTOM:ERROR")
          .beginChildren()
          .child(EXPECTED_RAISE_ERROR_SPAN).addExceptionData("CUSTOM:ERROR")
          .child(EXPECTED_ON_ERROR_PROPAGATE_SPAN_NAME)
          .endChildren()
          .endChildren()
          .child(EXPECTED_ON_ERROR_PROPAGATE_SPAN_NAME)
          .endChildren();

      expectedSpanHierarchy.assertSpanTree();

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
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(NET_HOST_PORT, listenerServerPort.getValue()));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_METHOD, "GET"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_STATUS_CODE, "500"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_ROUTE, "/test"));
      assertThat(listenerExportedSpan.getAttributes().get(SPAN_KIND_ATTRIBUTE), nullValue());
      assertThat(listenerExportedSpan.getAttributes().get(SPAN_STATUS_ATTRIBUTE), nullValue());
      assertThat(listenerExportedSpan.getSpanKindName(), equalTo("SERVER"));
      assertThat(listenerExportedSpan.hasErrorStatus(), equalTo(true));
      assertThat(listenerExportedSpan.getStatusAsString(), equalTo(ERROR_STATUS));

      CapturedExportedSpan requestExportedSpan =
          exportedSpans.stream().filter(exportedSpan -> exportedSpan.getName().equals(EXPECTED_HTTP_REQUEST_SPAN_NAME))
              .findFirst()
              .orElseThrow(() -> new AssertionFailedError("No span for http request flow found!"));

      assertThat(requestExportedSpan.getAttributes(), aMapWithSize(13));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(NET_PEER_NAME, "localhost"));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(NET_PEER_PORT, listenerServerPort.getValue()));
      assertThat(requestExportedSpan.getAttributes(),
                 hasEntry(HTTP_URL, "http://localhost:" + listenerServerPort.getValue() + "/test"));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(HTTP_METHOD, "GET"));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(HTTP_FLAVOR, "1.1"));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(HTTP_STATUS_CODE, "500"));
      assertThat(requestExportedSpan.getAttributes().get(SPAN_KIND_ATTRIBUTE), nullValue());
      assertThat(requestExportedSpan.getAttributes().get(SPAN_STATUS_ATTRIBUTE), nullValue());
      assertThat(requestExportedSpan.getSpanKindName(), equalTo("CLIENT"));
      assertThat(requestExportedSpan.hasErrorStatus(), equalTo(true));
      assertThat(requestExportedSpan.getStatusAsString(), equalTo(ERROR_STATUS));
    } finally {
      spanCapturer.dispose();
    }
  }

  @Test
  public void testWhenHTTPListenerFlowThrowsErrorButReturns400SpanStatusShouldNotBeSetAsError() throws Exception {
    ExportedSpanSniffer spanCapturer = profilingService.getSpanExportManager().getExportedSpanSniffer();

    try {
      PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);
      flowRunner(HTTP_LISTENER_400_FLOW).runExpectingException(errorType("HTTP", "BAD_REQUEST"));

      prober.check(new JUnitProbe() {

        @Override
        protected boolean test() {
          Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();
          return exportedSpans.size() == 6;
        }

        @Override
        public String describeFailure() {
          return format("The exact amount of spans was not captured. Captured spans: %s",
                        spanCapturer.getExportedSpans().toString());
        }
      });

      Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();

      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME).addExceptionData("HTTP:BAD_REQUEST")
          .beginChildren()
          .child(EXPECTED_HTTP_REQUEST_SPAN_NAME).addExceptionData("HTTP:BAD_REQUEST")
          .beginChildren()
          .child(EXPECTED_HTTP_FLOW_SPAN_NAME_400).addExceptionData("CUSTOM:ERROR")
          .addStatusData(UNSET_STATUS)
          .addTraceStateKeyPresentAssertion(ANCESTOR_MULE_SPAN_ID)
          .beginChildren()
          .child(EXPECTED_RAISE_ERROR_SPAN).addExceptionData("CUSTOM:ERROR")
          .addTraceStateKeyNotPresentAssertion(ANCESTOR_MULE_SPAN_ID)
          .child(EXPECTED_ON_ERROR_PROPAGATE_SPAN_NAME)
          .addTraceStateKeyNotPresentAssertion(ANCESTOR_MULE_SPAN_ID)
          .endChildren()
          .endChildren()
          .child(EXPECTED_ON_ERROR_PROPAGATE_SPAN_NAME)
          .addTraceStateKeyNotPresentAssertion(ANCESTOR_MULE_SPAN_ID)
          .endChildren();

      expectedSpanHierarchy.assertSpanTree();


      CapturedExportedSpan listenerExportedSpan =
          exportedSpans.stream().filter(exportedSpan -> exportedSpan.getName().equals(EXPECTED_HTTP_FLOW_SPAN_NAME_400))
              .findFirst()
              .orElseThrow(() -> new AssertionFailedError("No span for http listener flow found!"));

      assertThat(listenerExportedSpan.getAttributes(), aMapWithSize(16));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(NET_HOST_NAME, "0.0.0.0"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_TARGET, "/test400"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_SCHEME, "http"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_FLAVOR, "1.1"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_USER_AGENT, "AHC/1.0"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(NET_HOST_PORT, listenerServerPort.getValue()));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_METHOD, "GET"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_STATUS_CODE, "400"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_ROUTE, "/test400"));
      assertThat(listenerExportedSpan.getAttributes().get(SPAN_KIND_ATTRIBUTE), nullValue());
      assertThat(listenerExportedSpan.getAttributes().get(SPAN_STATUS_ATTRIBUTE), nullValue());
      assertThat(listenerExportedSpan.getSpanKindName(), equalTo("SERVER"));
      assertThat(listenerExportedSpan.hasErrorStatus(), equalTo(false));
      assertThat(listenerExportedSpan.getStatusAsString(), equalTo(UNSET_STATUS));

      CapturedExportedSpan requestExportedSpan =
          exportedSpans.stream().filter(exportedSpan -> exportedSpan.getName().equals(EXPECTED_HTTP_REQUEST_SPAN_NAME))
              .findFirst()
              .orElseThrow(() -> new AssertionFailedError("No span for http request flow found!"));

      assertThat(requestExportedSpan.getAttributes(), aMapWithSize(13));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(NET_PEER_NAME, "localhost"));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(NET_PEER_PORT, listenerServerPort.getValue()));
      assertThat(requestExportedSpan.getAttributes(),
                 hasEntry(HTTP_URL, "http://localhost:" + listenerServerPort.getValue() + "/test400"));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(HTTP_METHOD, "GET"));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(HTTP_FLAVOR, "1.1"));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(HTTP_STATUS_CODE, "400"));
      assertThat(requestExportedSpan.getAttributes().get(SPAN_KIND_ATTRIBUTE), nullValue());
      assertThat(requestExportedSpan.getAttributes().get(SPAN_STATUS_ATTRIBUTE), nullValue());
      assertThat(requestExportedSpan.getSpanKindName(), equalTo("CLIENT"));
      assertThat(requestExportedSpan.hasErrorStatus(), equalTo(true));
      assertThat(requestExportedSpan.getStatusAsString(), equalTo(ERROR_STATUS));
    } finally {
      spanCapturer.dispose();
    }
  }

  @Test
  public void testWhenHTTPListenerFlowDoesNotThrowErrorButReturns500SpanStatusShouldBeSetAsError() throws Exception {
    ExportedSpanSniffer spanCapturer = profilingService.getSpanExportManager().getExportedSpanSniffer();

    try {
      PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);
      flowRunner(HTTP_LISTENER_500_FLOW).runExpectingException(errorType("HTTP", "INTERNAL_SERVER_ERROR"));

      prober.check(new JUnitProbe() {

        @Override
        protected boolean test() {
          Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();
          return exportedSpans.size() == 5;
        }

        @Override
        public String describeFailure() {
          return format("The exact amount of spans was not captured. Captured spans: %s",
                        spanCapturer.getExportedSpans().toString());
        }
      });

      Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();

      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME).addExceptionData("HTTP:INTERNAL_SERVER_ERROR")
          .beginChildren()
          .child(EXPECTED_HTTP_REQUEST_SPAN_NAME).addExceptionData("HTTP:INTERNAL_SERVER_ERROR")
          .beginChildren()
          .child(EXPECTED_HTTP_FLOW_SPAN_NAME_500)
          .addStatusData(ERROR_STATUS)
          .addTraceStateKeyPresentAssertion(ANCESTOR_MULE_SPAN_ID)
          .beginChildren()
          .child(EXPECTED_LOGGER_SPAN_NAME)
          .addTraceStateKeyNotPresentAssertion(ANCESTOR_MULE_SPAN_ID)
          .endChildren()
          .endChildren()
          .child(EXPECTED_ON_ERROR_PROPAGATE_SPAN_NAME)
          .addTraceStateKeyNotPresentAssertion(ANCESTOR_MULE_SPAN_ID)
          .endChildren();

      expectedSpanHierarchy.assertSpanTree();

      CapturedExportedSpan listenerExportedSpan =
          exportedSpans.stream().filter(exportedSpan -> exportedSpan.getName().equals(EXPECTED_HTTP_FLOW_SPAN_NAME_500))
              .findFirst()
              .orElseThrow(() -> new AssertionFailedError("No span for http listener flow found!"));

      assertThat(listenerExportedSpan.getAttributes(), aMapWithSize(16));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(NET_HOST_NAME, "0.0.0.0"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_TARGET, "/test500"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_SCHEME, "http"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_FLAVOR, "1.1"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_USER_AGENT, "AHC/1.0"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(NET_HOST_PORT, listenerServerPort.getValue()));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_METHOD, "GET"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_STATUS_CODE, "500"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_ROUTE, "/test500"));
      assertThat(listenerExportedSpan.getAttributes().get(SPAN_KIND_ATTRIBUTE), nullValue());
      assertThat(listenerExportedSpan.getAttributes().get(SPAN_STATUS_ATTRIBUTE), nullValue());
      assertThat(listenerExportedSpan.getSpanKindName(), equalTo("SERVER"));
      assertThat(listenerExportedSpan.hasErrorStatus(), equalTo(true));
      assertThat(listenerExportedSpan.getStatusAsString(), equalTo(ERROR_STATUS));

      CapturedExportedSpan requestExportedSpan =
          exportedSpans.stream().filter(exportedSpan -> exportedSpan.getName().equals(EXPECTED_HTTP_REQUEST_SPAN_NAME))
              .findFirst()
              .orElseThrow(() -> new AssertionFailedError("No span for http request flow found!"));

      assertThat(requestExportedSpan.getAttributes(), aMapWithSize(13));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(NET_PEER_NAME, "localhost"));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(NET_PEER_PORT, listenerServerPort.getValue()));
      assertThat(requestExportedSpan.getAttributes(),
                 hasEntry(HTTP_URL, "http://localhost:" + listenerServerPort.getValue() + "/test500"));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(HTTP_METHOD, "GET"));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(HTTP_FLAVOR, "1.1"));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(HTTP_STATUS_CODE, "500"));
      assertThat(requestExportedSpan.getAttributes().get(SPAN_KIND_ATTRIBUTE), nullValue());
      assertThat(requestExportedSpan.getAttributes().get(SPAN_STATUS_ATTRIBUTE), nullValue());
      assertThat(requestExportedSpan.getSpanKindName(), equalTo("CLIENT"));
      assertThat(requestExportedSpan.hasErrorStatus(), equalTo(true));
      assertThat(requestExportedSpan.getStatusAsString(), equalTo(ERROR_STATUS));
    } finally {
      spanCapturer.dispose();
    }
  }
}
