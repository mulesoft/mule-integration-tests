/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.components.tracing;

import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.tck.tracing.ExportedSpansVerifier.getExportedSpansVerifier;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;

import org.mule.tck.junit4.rule.TestExportedSpanCapturer;
import org.mule.test.AbstractIntegrationTestCase;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;
import org.junit.Rule;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class SimpleTracingTestCase extends AbstractIntegrationTestCase {

  private static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  private static final String EXPECTED_SET_PAYLOAD_SPAN_NAME = "mule:set-payload";
  private static final String EXPECTED_TRACING_CORRELATION_ID_SPAN_NAME = "tracing:with-correlation-id";
  private static final String EXPECTED_SET_VARIABLE_SPAN_NAME = "mule:set-variable";
  private static final String EXPECTED_SET_LOGGING_VARIABLE_SPAN_NAME = "tracing:set-logging-variable";
  private static final String SIMPLE_FLOW = "simple-flow";
  private static final String CORRELATION_ID_KEY = "correlationId";
  private static final String ARTIFACT_TYPE_KEY = "artifactType";
  private static final String ARTIFACT_ID_KEY = "artifactId";
  private static final String THREAD_START_ID_KEY = "threadStartName";
  private static final String ARTIFACT_ID = "SimpleTracingTestCase#testSimpleFlow";
  private static final String FLOW_LOCATION = "simple-flow";
  private static final String LOCATION_KEY = "location";
  private static final String SET_PAYLOAD_LOCATION = "simple-flow/processors/2";
  private static final String SET_VARIABLE_LOCATION = "simple-flow/processors/0/processors/0";
  private static final String SET_LOGGGING_VARIABLE_LOCATION = "simple-flow/processors/1";
  private static final String TRACING_SET_CORRELATION_ID_LOCATION = "simple-flow/processors/0";
  private static final String TEST_VAR_NAME = "testVar";
  private static final String TEST_VAR_VALUE = "Hello World!";
  private static final String CORRELATION_ID_CUSTOM_VALUE = "Fua";

  @Override
  protected String getConfigFile() {
    return "tracing/flow.xml";
  }

  @Rule
  public TestExportedSpanCapturer exportedSpanCapturer = new TestExportedSpanCapturer();

  @Test
  public void testSimpleFlow() throws Exception {
    flowRunner(SIMPLE_FLOW).withPayload(TEST_PAYLOAD).run().getMessage();

    getExportedSpansVerifier(EXPECTED_FLOW_SPAN_NAME)
        .withAttribute(LOCATION_KEY, FLOW_LOCATION)
        .withAttributeInAllChildren(ARTIFACT_TYPE_KEY, APP.getAsString())
        .withAttributeInAllChildren(ARTIFACT_ID_KEY, ARTIFACT_ID)
        .hasAttributeInAllChildren(CORRELATION_ID_KEY)
        .hasAttributeInAllChildren(THREAD_START_ID_KEY)
        .withChildExportedSpan(getExportedSpansVerifier(EXPECTED_TRACING_CORRELATION_ID_SPAN_NAME)
            .withAttribute(LOCATION_KEY, TRACING_SET_CORRELATION_ID_LOCATION)
            .withChildExportedSpan(getExportedSpansVerifier(EXPECTED_SET_VARIABLE_SPAN_NAME)
                .withAttribute(LOCATION_KEY, SET_VARIABLE_LOCATION)
                .withAttribute(CORRELATION_ID_KEY, CORRELATION_ID_CUSTOM_VALUE)))
        .withChildExportedSpan(getExportedSpansVerifier(EXPECTED_SET_LOGGING_VARIABLE_SPAN_NAME)
            .withAttribute(LOCATION_KEY, SET_LOGGGING_VARIABLE_LOCATION))
        .withChildExportedSpan(getExportedSpansVerifier(EXPECTED_SET_PAYLOAD_SPAN_NAME)
            .withAttribute(LOCATION_KEY, SET_PAYLOAD_LOCATION)
            .withAttribute(TEST_VAR_NAME, TEST_VAR_VALUE))
        .verify(exportedSpanCapturer);
  }
}
