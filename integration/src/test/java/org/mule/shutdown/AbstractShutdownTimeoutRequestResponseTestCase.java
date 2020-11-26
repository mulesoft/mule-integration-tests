/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.shutdown;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.concurrent.ExecutorService;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;

public abstract class AbstractShutdownTimeoutRequestResponseTestCase extends AbstractIntegrationTestCase {

  protected static Latch waitLatch;
  protected static Latch contextStopLatch;

  protected ExecutorService executor;

  @Before
  public void before() {
    executor = newSingleThreadExecutor();
  }

  @After
  public void after() {
    executor.shutdownNow();
  }

  @Rule
  public DynamicPort httpPort = new DynamicPort("httpPort");

  @Before
  public void setUpWaitLatch() throws Exception {
    waitLatch = new Latch();
    contextStopLatch = new Latch();
  }

  public static class BlockMessageProcessor extends AbstractComponent implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      waitLatch.release();

      try {
        contextStopLatch.await();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new DefaultMuleException(e);
      }

      return event;
    }
  }
}
