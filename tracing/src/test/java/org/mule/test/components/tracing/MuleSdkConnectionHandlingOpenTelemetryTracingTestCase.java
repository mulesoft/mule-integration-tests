/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.components.tracing;

import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_LAZY_CONNECTIONS_DEPLOYMENT_PROPERTY;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;

import static java.util.Arrays.asList;

import org.junit.runners.Parameterized;
import org.mule.runtime.api.config.custom.ServiceConfigurator;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.tracer.api.sniffer.CapturedExportedSpan;
import org.mule.runtime.tracer.api.sniffer.ExportedSpanSniffer;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.core.privileged.profiling.PrivilegedProfilingService;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.matcher.ErrorTypeMatcher;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.infrastructure.profiling.tracing.SpanTestHierarchy;
import org.mule.test.runner.RunnerDelegateTo;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
@RunnerDelegateTo(Parameterized.class)
public class MuleSdkConnectionHandlingOpenTelemetryTracingTestCase extends MuleArtifactFunctionalTestCase
    implements OpenTelemetryTracingTestRunnerConfigAnnotation {

  private static final int TIMEOUT_MILLIS = 30000;
  private static final int POLL_DELAY_MILLIS = 100;
  public static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  public static final String EXPECTED_PETSTORE_GET_CONNECTION_AGE_SPAN = "petstore:get-connection-age";
  public static final String EXPECTED_MULE_GET_CONNECTION_SPAN = "mule:get-connection";
  private static final String OPERATION_WITH_SIMPLE_CONNECTION = "operation-with-simple-connection";

  private static final String OPERATION_WITH_SIMPLE_FAILING_CONNECTION = "operation-with-simple-failing-connection";
  private ExportedSpanSniffer spanCapturer;
  @Inject
  PrivilegedProfilingService profilingService;
  public final String lazyConnections;

  public MuleSdkConnectionHandlingOpenTelemetryTracingTestCase(String lazyConnections) {
    this.lazyConnections = lazyConnections;
  }

  @Override
  protected String getConfigFile() {
    return "tracing/mule-sdk-connection-handling.xml";
  }

  @Parameterized.Parameters(name = "lazyConnections")
  public static Collection<String> parameters() {
    return asList("true", "false");
  }

  @Before
  public void initialize() {
    spanCapturer = profilingService.getSpanExportManager().getExportedSpanSniffer();
  }

  @After
  public void dispose() {
    spanCapturer.dispose();
  }

  @Override
  protected void addBuilders(List<ConfigurationBuilder> builders) {
    super.addBuilders(builders);
    builders.add(
                 new ConfigurationBuilder() {

                   @Override
                   public void configure(MuleContext muleContext) {
                     muleContext.getDeploymentProperties().setProperty(MULE_LAZY_CONNECTIONS_DEPLOYMENT_PROPERTY,
                                                                       lazyConnections);
                   }

                   @Override
                   public void addServiceConfigurator(ServiceConfigurator serviceConfigurator) {
                     // Nothing to do
                   }
                 });
  }

  @Test
  public void testOperationWithSimpleConnection() throws Exception {

    flowRunner(OPERATION_WITH_SIMPLE_CONNECTION).withPayload(AbstractMuleTestCase.TEST_PAYLOAD)
        .run();

    PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);

    prober.check(new JUnitProbe() {

      @Override
      protected boolean test() {
        Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();;
        return exportedSpans.size() == 3;
      }

      @Override
      public String describeFailure() {
        return "The exact amount of spans was not captured";
      }
    });

    Collection<CapturedExportedSpan> capturedExportedSpans = spanCapturer.getExportedSpans();

    SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(capturedExportedSpans);
    expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
        .beginChildren()
        .child(EXPECTED_PETSTORE_GET_CONNECTION_AGE_SPAN)
        .beginChildren()
        .child(EXPECTED_MULE_GET_CONNECTION_SPAN);
    expectedSpanHierarchy.assertSpanTree();
  }

  @Test
  public void testOperationWithFailingSimpleConnection() throws Exception {

    flowRunner(OPERATION_WITH_SIMPLE_FAILING_CONNECTION).withPayload(AbstractMuleTestCase.TEST_PAYLOAD)
        .runExpectingException(ErrorTypeMatcher.errorType("PETSTORE", "CONNECTIVITY"));

    PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);

    prober.check(new JUnitProbe() {

      @Override
      protected boolean test() {
        Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();;
        return exportedSpans.size() == 4;
      }

      @Override
      public String describeFailure() {
        return "The exact amount of spans was not captured";
      }
    });

    Collection<CapturedExportedSpan> capturedExportedSpans = spanCapturer.getExportedSpans();

    SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(capturedExportedSpans);
    expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME).addExceptionData("PETSTORE:CONNECTIVITY")
        .beginChildren()
        .child(EXPECTED_PETSTORE_GET_CONNECTION_AGE_SPAN).addExceptionData("PETSTORE:CONNECTIVITY")
        .beginChildren()
        .child(EXPECTED_MULE_GET_CONNECTION_SPAN);
    expectedSpanHierarchy.assertSpanTree();
  }

}
