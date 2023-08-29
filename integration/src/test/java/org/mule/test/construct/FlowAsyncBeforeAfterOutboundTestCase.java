/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.metadata.DataType.STRING;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.tests.api.TestQueueManager;

import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import org.junit.Test;

public class FlowAsyncBeforeAfterOutboundTestCase extends AbstractIntegrationTestCase {

  @Inject
  private TestQueueManager queueManager;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/construct/flow-async-before-after-outbound.xml";
  }

  @Test
  public void testAsyncBefore() throws Exception {
    Message msgSync = flowRunner("test-async-block-before-outbound").withPayload("message").run().getMessage();
    Message msgAsync = queueManager.read("test.before.async.out", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();
    Message msgOut = queueManager.read("test.before.out", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();

    assertCorrectThreads(msgSync, msgAsync, msgOut);
  }

  @Test
  public void testAsyncAfter() throws Exception {
    Message msgSync = flowRunner("test-async-block-after-outbound").withPayload("message").run().getMessage();
    Message msgAsync = queueManager.read("test.after.async.out", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();
    Message msgOut = queueManager.read("test.after.out", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();

    assertCorrectThreads(msgSync, msgAsync, msgOut);
  }

  private void assertCorrectThreads(Message msgSync, Message msgAsync, Message msgOut) throws Exception {
    assertThat(msgSync, not(nullValue()));
    assertThat(msgAsync, not(nullValue()));
    assertThat(msgOut, not(nullValue()));

    assertThat(msgOut.getPayload().getValue(),
               equalTo(msgSync.getPayload().getValue()));
    assertThat(msgSync.getPayload().getValue(),
               not(equalTo(msgAsync.getPayload().getValue())));
    assertThat(msgOut.getPayload().getValue(),
               not(equalTo(msgAsync.getPayload().getValue())));
    assertThat((String) msgAsync.getPayload().getValue(),
               not(containsString("ring-buffer")));
  }

  public static class ThreadSensingMessageProcessor implements Processor {

    private static final ThreadLocal<String> taskTokenInThread = new ThreadLocal<>();
    private static final AtomicInteger idgenerator = new AtomicInteger();

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      String requestTaskToken;
      if (taskTokenInThread.get() != null) {
        requestTaskToken = taskTokenInThread.get();
      } else {
        requestTaskToken = generateTaskToken();
        taskTokenInThread.set(requestTaskToken);
      }

      return CoreEvent.builder(event)
          .message(Message.builder(event.getMessage()).payload(new TypedValue<>(requestTaskToken, STRING)).build())
          .build();
    }

    protected String generateTaskToken() {
      return currentThread().getName() + " - " + idgenerator.getAndIncrement();
    }
  }
}
