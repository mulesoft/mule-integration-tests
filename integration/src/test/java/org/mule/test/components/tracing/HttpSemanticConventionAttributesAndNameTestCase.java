/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.components.tracing;

import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.privileged.profiling.CapturedExportedSpan;
import org.mule.runtime.core.privileged.profiling.ExportedSpanCapturer;
import org.mule.runtime.core.privileged.profiling.PrivilegedProfilingService;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.Collection;

import javax.inject.Inject;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import junit.framework.AssertionFailedError;
import org.junit.Rule;
import org.junit.Test;
import org.mule.test.infrastructure.profiling.tracing.SpanTestHierarchy;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class HttpSemanticConventionAttributesAndNameTestCase extends AbstractIntegrationTestCase {

  private static final String STARTING_FLOW = "startingFlow";
  public static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  private static final String EXPECTED_HTTP_REQUEST_SPAN_NAME = "HTTP GET";
  private static final String EXPECTED_HTTP_FLOW_SPAN_NAME = "/test";
  private static final String EXPECTED_LOGGER_SPAN_NAME = "mule:logger";
  public static final String NET_PEER_NAME = "net.peer.name";
  public static final String NET_PEER_PORT = "net.peer.port";
  public static final String HTTP_URL = "http.url";
  public static final String HTTP_METHOD = "http.method";
  public static final String HTTP_FLAVOR = "http.flavor";
  public static final String NET_HOST_NAME = "net.host.name";
  public static final String HTTP_TARGET = "http.target";
  public static final String HTTP_USER_AGENT = "http.user_agent";
  public static final String NET_HOST_PORT = "net.host.port";
  @Inject
  PrivilegedProfilingService profilingService;

  @Rule
  public DynamicPort httpPort = new DynamicPort("port");

  @Override
  protected String getConfigFile() {
    return "tracing/http-semantic-conventions-tracing.xml";
  }

  @Test
  public void testFlow() throws Exception {
    ExportedSpanCapturer spanCapturer = profilingService.getSpanExportManager().getExportedSpanCapturer();

    try {
      flowRunner(STARTING_FLOW).run();
      Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();

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

      CapturedExportedSpan listenerExportedSpan =
          exportedSpans.stream().filter(exportedSpan -> exportedSpan.getName().equals(EXPECTED_HTTP_FLOW_SPAN_NAME))
              .findFirst()
              .orElseThrow(() -> new AssertionFailedError("No span for http listener flow found!"));

      assertThat(listenerExportedSpan.getAttributes(), aMapWithSize(12));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(NET_HOST_NAME, "0.0.0.0"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_TARGET, "/test"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_FLAVOR, "1.1"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_USER_AGENT, "AHC/1.0"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(NET_HOST_PORT, httpPort.getValue()));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_METHOD, "GET"));

      CapturedExportedSpan requestExportedSpan =
          exportedSpans.stream().filter(exportedSpan -> exportedSpan.getName().equals(EXPECTED_HTTP_REQUEST_SPAN_NAME))
              .findFirst()
              .orElseThrow(() -> new AssertionFailedError("No span for http request flow found!"));

      assertThat(requestExportedSpan.getAttributes(), aMapWithSize(12));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(NET_PEER_NAME, "localhost"));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(NET_PEER_PORT, httpPort.getValue()));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(HTTP_URL, "http://localhost:" + httpPort.getValue() + "/test"));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(HTTP_METHOD, "GET"));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(HTTP_FLAVOR, "1.1"));

    } finally {
      spanCapturer.dispose();
    }
  }

}
