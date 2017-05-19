/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.routing;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.core.DefaultEventContext.create;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.Map;

import javax.xml.namespace.QName;

import org.junit.Test;

public class SynchronizedFlowRefOnMuleContextStartTestCase extends AbstractIntegrationTestCase {

  protected static final Latch waitMessageInProgress = new Latch();
  protected static volatile int processedMessageCounter = 0;

  public SynchronizedFlowRefOnMuleContextStartTestCase() {
    setStartContext(false);
  }

  @Override
  protected String getConfigFile() {
    return "synchronized-flowref-mule-context-start-config.xml";
  }

  @Test
  public void waitsForStartedMuleContextBeforeAttemptingToSendMessageToEndpoint() throws Exception {
    prePopulateObjectStore();

    muleContext.start();

    Prober prober = new PollingProber(RECEIVE_TIMEOUT, 50);

    prober.check(new Probe() {

      public boolean isSatisfied() {
        return processedMessageCounter == 1;
      }

      public String describeFailure() {
        return "Did not wait for mule context started before attempting to process event";
      }
    });
  }

  private void prePopulateObjectStore() throws ObjectStoreException {
    ObjectStore<Event> objectStore = muleContext.getRegistry().lookupObject("objectStore");

    Message testMessage = Message.builder().payload(TEST_MESSAGE).build();
    Flow clientFlow = muleContext.getRegistry().get("flow2");
    Event testMuleEvent =
        Event.builder(create(clientFlow, fromSingleComponent(clientFlow.getName()))).message(testMessage).build();
    objectStore.store(testMuleEvent.getCorrelationId(), testMuleEvent);
  }

  public static class UnblockProcessingSource implements MessageSource, Startable {

    @Override
    public void start() throws MuleException {
      waitMessageInProgress.release();
    }

    @Override
    public Object getAnnotation(QName name) {
      return null;
    }

    @Override
    public Map<QName, Object> getAnnotations() {
      return null;
    }

    @Override
    public void setAnnotations(Map<QName, Object> annotations) {

    }

    @Override
    public ComponentLocation getLocation() {
      return null;
    }

    @Override
    public void setListener(Processor listener) {

    }
  }

  public static class TestMessageProcessor {

    public String count(String value) throws InterruptedException {
      if (waitMessageInProgress.await(0, MILLISECONDS)) {
        processedMessageCounter++;
      }

      return value;
    }
  }
}
