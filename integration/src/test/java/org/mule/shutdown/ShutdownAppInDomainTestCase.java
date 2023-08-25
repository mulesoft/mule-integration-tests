/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.shutdown;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.privileged.event.PrivilegedEvent.getCurrentEvent;
import static org.mule.runtime.http.api.HttpConstants.Method.GET;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.LIFECYCLE_AND_DEPENDENCY_INJECTION;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.GracefulShutdownStory.GRACEFUL_SHUTDOWN_STORY;

import org.mule.functional.junit4.DomainFunctionalTestCase;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.report.HeapDumpOnFailure;

import java.io.IOException;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Tests that threads in pools defined in a domain do not hold references to objects of the application in their thread locals.
 */
@Ignore("MULE-10335")
@Feature(LIFECYCLE_AND_DEPENDENCY_INJECTION)
@Story(GRACEFUL_SHUTDOWN_STORY)
public class ShutdownAppInDomainTestCase extends DomainFunctionalTestCase {

  private static final int PROBER_POLLING_INTERVAL = 100;
  private static final int PROBER_POLIING_TIMEOUT = 5000;
  private static final int MESSAGE_TIMEOUT = 2000;

  @Rule
  public HeapDumpOnFailure heapDumpOnFailure = new HeapDumpOnFailure();

  private static final Set<PhantomReference<CoreEvent>> requestContextRefs = new HashSet<>();

  public static class RetrieveRequestContext implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      requestContextRefs.add(new PhantomReference<>(getCurrentEvent(), new ReferenceQueue<>()));
      return event;
    }
  }

  @Before
  public void before() {
    requestContextRefs.clear();
  }

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder().build();

  @Rule
  public DynamicPort httpPort = new DynamicPort("httpPort");

  @Override
  protected String getDomainConfig() {
    return "org/mule/shutdown/domain-with-connectors.xml";
  }

  @Override
  public ApplicationConfig[] getConfigResources() {
    return new ApplicationConfig[] {
        new ApplicationConfig("app-with-flows", new String[] {"org/mule/shutdown/app-with-flows.xml"})
    };
  }

  @Test
  public void httpListener() throws IOException, TimeoutException {
    MuleContext muleContextForApp = getMuleContextForApp("app-with-flows");

    HttpRequest request =
        HttpRequest.builder().uri("http://localhost:" + httpPort.getNumber() + "/sync").method(GET).build();
    httpClient.send(request, MESSAGE_TIMEOUT, false, null);

    muleContextForApp.dispose();

    assertEventsUnreferenced();
  }

  @Test
  public void httpListenerNonBlocking() throws IOException, TimeoutException {
    MuleContext muleContextForApp = getMuleContextForApp("app-with-flows");

    HttpRequest request =
        HttpRequest.builder().uri("http://localhost:" + httpPort.getNumber() + "/nonBlocking").method(GET).build();
    httpClient.send(request, MESSAGE_TIMEOUT, false, null);

    muleContextForApp.dispose();

    assertEventsUnreferenced();
  }

  @Test
  public void httpRequest() throws IOException, TimeoutException {
    MuleContext muleContextForApp = getMuleContextForApp("app-with-flows");

    HttpRequest request =
        HttpRequest.builder().uri("http://localhost:" + httpPort.getNumber() + "/request").method(GET).build();
    httpClient.send(request, MESSAGE_TIMEOUT, false, null);
    muleContextForApp.dispose();

    assertEventsUnreferenced();
  }

  @Test
  @Ignore("Reimplement with the new JMS Connector")
  public void jms() throws MuleException {
    final MuleContext muleContextForApp = getMuleContextForApp("app-with-flows");

    // TODO: Replace with JMS connector
    // muleContextForApp.getClient().dispatch("jms://in?connector=sharedJmsConnector", of("payload"));
    // muleContextForApp.getClient().request("jms://out?connector=sharedJmsConnector", MESSAGE_TIMEOUT);

    muleContextForApp.dispose();

    assertEventsUnreferenced();
  }

  private void assertEventsUnreferenced() {
    new PollingProber(PROBER_POLIING_TIMEOUT, PROBER_POLLING_INTERVAL).check(new JUnitProbe() {

      @Override
      protected boolean test() throws Exception {
        System.gc();
        for (PhantomReference<CoreEvent> phantomReference : requestContextRefs) {
          assertThat(phantomReference.isEnqueued(), is(true));
        }
        return true;
      }
    });
  }
}
