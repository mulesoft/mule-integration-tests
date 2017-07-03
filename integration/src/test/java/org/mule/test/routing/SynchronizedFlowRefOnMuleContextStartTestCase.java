/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.routing;

import static org.mule.runtime.core.DefaultEventContext.create;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.LIFECYCLE_AND_DEPENDENCY_INJECTION;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.MuleContextStartOrderStory.MULE_CONTEXT_START_ORDER_STORY;

import org.mule.functional.api.component.EventCallback;
import org.mule.functional.api.component.SkeletonSource;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.internal.construct.AbstractPipeline;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features(LIFECYCLE_AND_DEPENDENCY_INJECTION)
@Stories(MULE_CONTEXT_START_ORDER_STORY)
public class SynchronizedFlowRefOnMuleContextStartTestCase extends AbstractIntegrationTestCase {

  protected static volatile int processedMessageCounter = 0;
  protected static final String FLOW_UNDER_TESTING = "flow2";

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

      @Override
      public boolean isSatisfied() {
        return processedMessageCounter == 1;
      }

      @Override
      public String describeFailure() {
        return "Did not wait for mule context started before attempting to process event";
      }
    });
  }

  private void prePopulateObjectStore() throws ObjectStoreException {
    ObjectStore<Event> objectStore = muleContext.getRegistry().lookupObject("objectStore");

    Message testMessage = Message.builder().payload(TEST_MESSAGE).build();
    Flow clientFlow = muleContext.getRegistry().get(FLOW_UNDER_TESTING);
    Event testMuleEvent =
        Event.builder(create(clientFlow, fromSingleComponent(clientFlow.getName()))).message(testMessage).build();
    objectStore.store(testMuleEvent.getCorrelationId(), testMuleEvent);
  }

  public static class TestMessageProcessorCallback implements EventCallback {

    @Override
    public void eventReceived(Event event, Object component, MuleContext muleContext) throws Exception {
      if (((SkeletonSource) ((AbstractPipeline) muleContext.getRegistry().lookupFlowConstruct(FLOW_UNDER_TESTING)).getSource())
          .isStarted()) {
        processedMessageCounter++;
      }
    }
  }
}
