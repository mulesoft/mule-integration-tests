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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.functional.api.flow.FlowRunner;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.tests.api.TestQueueManager;

import jakarta.inject.Inject;

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
  private static Latch subflowIsInitializingLatch;
  private static Latch muleContextIsStoppingLatch;
  private static Latch eventHasBeenProcessedLatch;

  @Inject
  private TestQueueManager queueManager;

  private static final Logger LOGGER = getLogger(ImplicitConfigurationLifeCycleTestCase.class);

  @Before
  public void before() {
    subflowIsInitializingLatch = new Latch();
    muleContextIsStoppingLatch = new Latch();
    eventHasBeenProcessedLatch = new Latch();
    scheduler =
        muleContext.getSchedulerService().ioScheduler(muleContext.getSchedulerBaseConfig().withShutdownTimeout(10, SECONDS));
  }

  @After
  public void after() throws Exception {
    muleContext.dispose();
    scheduler.shutdown();
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
    // Send an event asynchronously (this allows stopping the mule context in the middle of its processing).
    flowRunner.dispatchAsync(scheduler);
    // Wait until the sub flow signals its initialization to start stopping the mule context.
    if (!subflowIsInitializingLatch.await(RECEIVE_TIMEOUT, MILLISECONDS)) {
      LOGGER.warn("subflowIsInitializingLatch timed out.");
    }
    scheduler.submit(() -> {
      try {
        muleContext.stop();
      } catch (MuleException e) {
        throw new RuntimeException(e);
      }
    });
    // Retrieve the expected event processing error.
    CoreEvent errorResponse = queueManager.read("flowErrorQueue", RECEIVE_TIMEOUT, MILLISECONDS);
    if (errorResponse == null) {
      fail("Timeout while waiting for the event error response");
    }
    eventHasBeenProcessedLatch.release();
    // Assert that the event processing failed with the expected message.
    Error processingError = (Error) errorResponse.getMessage().getPayload().getValue();
    assertThat(processingError.getDescription(),
               is("Could not add entry with key 'implicit-config-implicit': Registry has been stopped."));
    assertThat(processingError.getDetailedDescription(),
               is("Found exception while registering configuration provider 'implicit-config-implicit'"));
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
          LOGGER.warn("eventHasBeenProcessedLatch timed out.");
        }
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
          LOGGER.warn("muleContextIsStoppingLatch timed out.");
        }
      } catch (InterruptedException e) {
        throw new MuleRuntimeException(e);
      }
    }
  }

}
