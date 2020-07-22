/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.service.scheduler;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.currentThread;
import static java.util.Optional.empty;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.from;
import static org.mule.test.allure.AllureConstants.SchedulerServiceFeature.SCHEDULER_SERVICE;

import org.mule.functional.api.exception.ExpectedError;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerBusyException;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.test.AbstractIntegrationTestCase;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;

@Feature(SCHEDULER_SERVICE)
public class SchedulerServiceTestCase extends AbstractIntegrationTestCase {

  @Rule
  public ExpectedError expectedError = ExpectedError.none();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/service/scheduler/scheduler-service.xml";
  }

  @Test
  @Description("Test that the scheduler service is properly injected into a Mule component")
  public void useSchedulingService() throws Exception {
    flowRunner("willSchedule").run();
  }

  @Test
  public void schedulerDefaultName() {
    SchedulerService schedulerService = muleContext.getSchedulerService();
    final Scheduler ioScheduler = schedulerService.ioScheduler();
    assertThat(ioScheduler.getName(),
               startsWith("[SchedulerServiceTestCase#schedulerDefaultName].uber@" + SchedulerServiceTestCase.class.getName()
                   + ".schedulerDefaultName:"));
    ioScheduler.shutdownNow();
  }

  @Test
  public void customSchedulerDefaultName() {
    SchedulerService schedulerService = muleContext.getSchedulerService();
    final Scheduler ioScheduler =
        schedulerService.customScheduler(muleContext.getSchedulerBaseConfig().withMaxConcurrentTasks(1));
    assertThat(ioScheduler.getName(),
               startsWith("[SchedulerServiceTestCase#customSchedulerDefaultName].custom@"
                   + SchedulerServiceTestCase.class.getName()
                   + ".customSchedulerDefaultName:"));
    ioScheduler.shutdownNow();
  }

  @Test
  public void schedulerCustomName() {
    SchedulerService schedulerService = muleContext.getSchedulerService();
    final Scheduler ioScheduler =
        schedulerService.ioScheduler(muleContext.getSchedulerBaseConfig().withName("myPreciousScheduler"));
    assertThat(ioScheduler.getName(), startsWith("[SchedulerServiceTestCase#schedulerCustomName].myPreciousScheduler"));
    ioScheduler.shutdownNow();
  }

  @Test
  public void configTimeoutChange() {
    Scheduler scheduler = muleContext.getSchedulerService().cpuLightScheduler();

    Latch testLatch = new Latch();
    scheduler.submit(() -> {
      try {
        testLatch.await();
      } catch (InterruptedException e) {
        currentThread().interrupt();
      }
    });

    final long stopRequestTime = currentTimeMillis();
    scheduler.stop();

    assertThat("gracefultShutdown flag in test not honored", muleContext.getConfiguration().getShutdownTimeout(),
               is(NON_GRACEFUL_SHUTDOWN_TIMEOUT));
    // check for the actual timeout plus some margin
    assertThat(currentTimeMillis() - stopRequestTime, lessThan(1000L));
  }

  @Override
  protected void addBuilders(List<ConfigurationBuilder> builders) {
    super.addBuilders(builders);
    builders.add(new AbstractConfigurationBuilder() {

      @Override
      protected void doConfigure(MuleContext muleContext) throws Exception {
        // Set an arbitrarily high value. This will be overridden by the context builder wen checking for the graceful shutdown
        // flag of the test.
        ((DefaultMuleConfiguration) muleContext.getConfiguration()).setShutdownTimeout(10000);
      }
    });
  }

  @Test
  @Description("Tests that the exception that happens when a thread pool is full is properly handled.")
  public void overloadErrorHandling() throws Exception {
    expectedError.expectErrorType(any(String.class), is("OVERLOAD"));
    expectedError.expectCause(instanceOf(RejectedExecutionException.class));

    flowRunner("delaySchedule").run();
  }

  @Test
  @Description("Tests that an OVERLOAD error is handled only by the message source."
      + " This assumes org.mule.test.integration.exceptions.ErrorHandlerTestCase#criticalNotHandled")
  public void overloadErrorHandlingFromSource() throws Throwable {
    MessageSource messageSource =
        (MessageSource) locator.find(builderFromStringRepresentation("delaySchedule/source").build()).get();

    expectedError.expectErrorType("MULE", "OVERLOAD");
    expectedError.expectCause(instanceOf(RejectedExecutionException.class));

    final Field messageProcessorField = messageSource.getClass().getDeclaredField("messageProcessor");
    messageProcessorField.setAccessible(true);
    final Processor listener = (Processor) messageProcessorField.get(messageSource);
    listener
        .process(CoreEvent
            .builder(create("id", "serverd", from(SchedulerServiceTestCase.class.getSimpleName()), null, empty()))
            .message(of(null))
            .build());
  }

  @Test
  @Description("Test that the name of a thread executing a processor has excution context information about its flow")
  public void flowProcessingThreadName() throws Exception {
    flowRunner("willSchedule").run();

    assertThat(RecordThreadName.threadName,
               allOf(startsWith("[MuleRuntime].uber."),
                     // [appName].flowName.processingStrategy
                     containsString("[SchedulerServiceTestCase#flowProcessingThreadName].willSchedule.CPU_LITE @")));
  }

  public static class HasSchedulingService implements Processor, Initialisable, Disposable {

    @Inject
    private SchedulerService schedulerService;

    private Scheduler scheduler;

    @Override
    public void initialise() throws InitialisationException {
      if (scheduler == null) {
        scheduler = schedulerService.cpuLightScheduler();
      }
    }

    @Override
    public void dispose() {
      scheduler.shutdownNow();
    }

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      try {
        // just exercise the scheduler.
        return scheduler.submit(() -> event).get();
      } catch (InterruptedException e) {
        currentThread().interrupt();
        return event;
      } catch (ExecutionException e) {
        throw new MuleRuntimeException(e.getCause());
      }
    }
  }

  public static class RecordThreadName implements Processor {

    public static String threadName;

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      threadName = currentThread().getName();
      return event;
    }

  }

  public static class RaiseBusy implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      throw new SchedulerBusyException("JustToBeAbleToInstantiateException");
    }

  }
}
