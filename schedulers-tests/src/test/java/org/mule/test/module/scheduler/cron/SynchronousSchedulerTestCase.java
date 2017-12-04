/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.scheduler.cron;


import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

import org.mule.functional.api.component.EventCallback;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.test.AbstractSchedulerTestCase;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * <p>
 * Validates that a synchronous flow processing strategy implies a synchronous poll execution
 * </p>
 */
public class SynchronousSchedulerTestCase extends AbstractSchedulerTestCase {

  private static List<String> fooSync = new ArrayList<>();
  private static List<String> fooNonSync = new ArrayList<>();

  @Before
  public void before() {
    FooSync.latch = new CountDownLatch(2);
    FooNonSync.latch = new CountDownLatch(2);
  }

  @Override
  protected String getConfigFile() {
    return "cron-synchronous-scheduler-config.xml";
  }

  @Test
  public void testSynchronous() throws InterruptedException {
    assertThat(FooSync.latch.await(7000, MILLISECONDS), is(false));
    assertThat(fooSync, hasSize(1));
  }

  @Test
  public void testNonSynchronous() throws InterruptedException {
    assertThat(FooNonSync.latch.await(7000, MILLISECONDS), is(true));
    assertThat(fooNonSync, hasSize(greaterThanOrEqualTo(2)));
  }

  public static class FooNonSync implements EventCallback {

    public static CountDownLatch latch;

    @Override
    public void eventReceived(CoreEvent event, Object component, MuleContext muleContext) throws Exception {
      latch.countDown();

      synchronized (fooNonSync) {
        fooNonSync.add((String) event.getMessage().getPayload().getValue());
        try {
          Thread.sleep(10000);
        } catch (InterruptedException e) {

        }
      }
    }
  }

  public static class FooSync implements EventCallback {

    public static CountDownLatch latch;

    @Override
    public void eventReceived(CoreEvent event, Object component, MuleContext muleContext) throws Exception {
      latch.countDown();

      synchronized (fooSync) {
        fooSync.add((String) event.getMessage().getPayload().getValue());
        try {
          Thread.sleep(10000);
        } catch (InterruptedException e) {

        }
      }
    }
  }

}
