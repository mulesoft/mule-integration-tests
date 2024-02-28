/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.lifecycle;

import static org.mule.test.allure.AllureConstants.RegistryFeature.DomainObjectRegistrationStory.OBJECT_REGISTRATION;
import static org.mule.test.allure.AllureConstants.RegistryFeature.REGISTRY;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.functional.api.flow.FlowRunner;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.tests.api.TestQueueManager;

import javax.inject.Inject;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

@Feature(REGISTRY)
@Story(OBJECT_REGISTRATION)
public class ImplicitConfigurationLifeCycleTestCase extends AbstractIntegrationTestCase {

  private Scheduler scheduler;
  private static final Latch subflowIsInitializingLatch = new Latch();
  private static final Latch muleContextIsStoppingLatch = new Latch();
  private static final Latch eventHasBeenProcessedLatch = new Latch();
  private static final Logger LOGGER = getLogger(ImplicitConfigurationLifeCycleTestCase.class);

  @Inject
  private TestQueueManager queueManager;

  @Before
  public void before() {
    scheduler =
        muleContext.getSchedulerService().ioScheduler(muleContext.getSchedulerBaseConfig().withShutdownTimeout(10, SECONDS));
    LOGGER.info("Starting was started.");
  }

  @After
  public void after() throws Exception {
    scheduler.shutdown();
    LOGGER.info("Scheduler was shut down.");
  }

  @Override
  public String getConfigFile() {
    return "org/mule/test/integration/lifecycle/implicit-configuration-lifecycle.xml";
  }

  @Override
  protected boolean isGracefulShutdown() {
    return true;
  }

  @Test
  @Issue("W-14722908")
  public void flowThatRegistersImplicitConfigurationDuringMuleContextStop() throws Exception {
    FlowRunner flowRunner = flowRunner("flowThatAddsRegistryEntryDuringFirstEventProcessing");
    flowRunner.dispatchAsync(scheduler);
    // Wait until the sub flow signals it's initialization to start stopping the mule context.
    if (!subflowIsInitializingLatch.await(RECEIVE_TIMEOUT, MILLISECONDS)) {
      LOGGER.info("Subflow initializing latch was not released in time");
    }
    scheduler.submit(() -> {
      try {
        muleContext.stop();
      } catch (MuleException e) {
        throw new RuntimeException(e);
      }
    });
    LOGGER.info("Initial event dispatched.");
    // Check that the expected registration error happens
    Message responseMessage = queueManager.read("flowErrorQueue", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();
    eventHasBeenProcessedLatch.release();
    LOGGER.info("Initial event processed.");
    Error processingError = (Error) responseMessage.getPayload().getValue();
    assertThat(processingError.getDescription(),
               is("Could not add entry with key 'implicit-config-implicit': Registry was shutting down."));
    assertThat(processingError.getDetailedDescription(),
               is("Found exception while registering configuration provider 'implicit-config-implicit'"));
    LOGGER.info("Assertions done.");
    // Restart the mule context and check that the flow can process an event without issues.
    muleContext.start();
    LOGGER.info("Mule context restarted.");
    flowRunner.reset();
    flowRunner.run();
  }

  public static class SignalMuleContextIsStopping implements Processor, Stoppable {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      return event;
    }

    @Override
    public void stop() throws MuleException {
      muleContextIsStoppingLatch.release();
      try {
        // Defer the rest of the flow under test stop until the event processing is done.
        if (!eventHasBeenProcessedLatch.await(RECEIVE_TIMEOUT, MILLISECONDS)) {
          LOGGER.info("Event processed latch was not released in time");
        } ;
        LOGGER.info("Stopping mule context.");
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public static class TriggerMuleContextStopWhileSubFlowIsBeingInitialized implements Processor, Initialisable {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      return event;
    }

    @Override
    public void initialise() {
      subflowIsInitializingLatch.release();
      try {
        // Defer the rest of the initialization until the mule context is being stopped.
        if (!muleContextIsStoppingLatch.await(RECEIVE_TIMEOUT, MILLISECONDS)) {
          LOGGER.info("Mule context is stopping latch latch was not released in time");
        } ;
        LOGGER.info("Initializing mule context.");
      } catch (InterruptedException e) {
        throw new MuleRuntimeException(e);
      }
    }
  }
}
