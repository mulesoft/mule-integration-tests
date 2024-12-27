/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.shutdown;

import static org.mule.runtime.core.privileged.event.PrivilegedEvent.getCurrentEvent;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.GracefulShutdownStory.GRACEFUL_SHUTDOWN_STORY;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.LIFECYCLE_AND_DEPENDENCY_INJECTION;

import static org.apache.http.impl.client.HttpClients.createDefault;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.report.HeapDumpOnFailure;
import org.mule.test.AbstractIntegrationTestCase;

import java.io.IOException;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests that threads in pools defined in a domain do not hold references to objects of the application in their thread locals.
 */
@Feature(LIFECYCLE_AND_DEPENDENCY_INJECTION)
@Story(GRACEFUL_SHUTDOWN_STORY)
public class ShutdownAppEventLeakTestCase extends AbstractIntegrationTestCase {

  private static final int PROBER_POLLING_INTERVAL = 100;
  private static final int PROBER_POLIING_TIMEOUT = 5000;
  private static final int MESSAGE_TIMEOUT = 2000;

  @Rule
  public HeapDumpOnFailure heapDumpOnFailure = new HeapDumpOnFailure();

  private CloseableHttpClient httpClient;

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
    httpClient = createDefault();
    requestContextRefs.clear();
  }

  @After
  public void after() throws IOException {
    httpClient.close();
  }

  @Rule
  public DynamicPort httpPort = new DynamicPort("httpPort");

  @Override
  protected String getConfigFile() {
    return "org/mule/shutdown/app-with-flows.xml";
  }

  @Test
  public void httpListener() throws IOException, TimeoutException {
    MuleContext muleContextForApp = muleContext;

    HttpGet request = new HttpGet("http://localhost:" + httpPort.getNumber() + "/sync");
    httpClient.execute(request);

    muleContextForApp.dispose();

    assertEventsUnreferenced();
  }

  @Test
  public void httpListenerNonBlocking() throws IOException, TimeoutException {
    MuleContext muleContextForApp = muleContext;

    HttpGet request = new HttpGet("http://localhost:" + httpPort.getNumber() + "/nonBlocking");
    httpClient.execute(request);


    muleContextForApp.dispose();

    assertEventsUnreferenced();
  }

  @Test
  public void httpRequest() throws IOException, TimeoutException {
    MuleContext muleContextForApp = muleContext;

    HttpGet request = new HttpGet("http://localhost:" + httpPort.getNumber() + "/request");
    httpClient.execute(request);

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
