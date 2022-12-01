/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.components.tracing;

import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.OPEN_TELEMETRY_TRACING_EXPORT;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.mule.tests.api.TestQueueManager;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import javax.inject.Inject;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;

@Feature(PROFILING)
@Story(OPEN_TELEMETRY_TRACING_EXPORT)
public class HTTPExportTestCase extends AbstractIntegrationTestCase {

  private static final String SIMPLE_FLOW = "simpleFlow";

  @Inject
  private TestQueueManager queueManager;

  @Override
  protected String getConfigFile() {
    return "tracing/tracing-http-export.xml";
  }

  @Rule
  public SystemProperty exportEnabled =
          new SystemProperty("mule.openetelemetry.export.enabled", "true");
  @Rule
  public SystemProperty exportBatchSize =
          new SystemProperty("mule.opentelemetry.export.batch.size", "1");
  @Rule
  public SystemProperty exportProtocol =
          new SystemProperty("mule.opentelemetry.export.protocol", "HTTP");
  @Rule
  public SystemProperty exportEndpoint = new SystemProperty("mule.opentelemetry.endpoint", "http://localhost:8080/v1/traces");

  @Test
  public void testSimpleFlow() throws Exception {
    flowRunner(SIMPLE_FLOW).run();
    assertThat(queueManager.read("exportedSpans", RECEIVE_TIMEOUT, MILLISECONDS), notNullValue());
  }

}
