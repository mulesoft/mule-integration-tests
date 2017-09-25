/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.service.scheduler;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.currentThread;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mule.runtime.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.event.BaseEventContext.create;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;
import static org.mule.test.allure.AllureConstants.SchedulerServiceFeature.SCHEDULER_SERVICE;

import org.mule.functional.api.component.SkeletonSource;
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
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.exception.NullExceptionHandler;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.test.AbstractIntegrationTestCase;

import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;

@Feature(SCHEDULER_SERVICE)
public class SchedulerServiceTestCase extends AbstractIntegrationTestCase {

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
               startsWith("[SchedulerServiceTestCase#schedulerDefaultName].io@" + SchedulerServiceTestCase.class.getName()
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

    assertThat("gracefultShutdown flag in test not honored", muleContext.getConfiguration().getShutdownTimeout(), is(0L));
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
    MessagingException exception =
        flowRunner("delaySchedule").runExpectingException();

    assertThat(exception.getEvent().getError().isPresent(), is(true));
    assertThat(exception.getEvent().getError().get().getErrorType().getIdentifier(), is("OVERLOAD"));
    assertThat(exception.getCause(), instanceOf(SchedulerBusyException.class));

  }

  @Rule
  public ExpectedException expected = none();

  @Test
  @Description("Tests that an OVERLOAD error is handled only by the message source."
      + " This assumes org.mule.test.integration.exceptions.ErrorHandlerTestCase#criticalNotHandled")
  public void overloadErrorHandlingFromSource() throws Throwable {
    SkeletonSource messageSource =
        (SkeletonSource) locator.find(builderFromStringRepresentation("delaySchedule/source").build()).get();

    expected.expect(MessagingException.class);
    expected.expect(new TypeSafeMatcher<MessagingException>() {

      private String errorTypeId;

      @Override
      public void describeTo(org.hamcrest.Description description) {
        description.appendValue(errorTypeId);
      }

      @Override
      protected boolean matchesSafely(MessagingException item) {
        errorTypeId = item.getEvent().getError().get().getErrorType().getIdentifier();
        return "OVERLOAD".equals(errorTypeId);
      }
    });

    expected.expectCause(instanceOf(SchedulerBusyException.class));

    messageSource.getListener()
        .process(CoreEvent
            .builder(create("id", "serverd", fromSingleComponent(SchedulerServiceTestCase.class.getSimpleName()),
                            NullExceptionHandler.getInstance()))
            .message(of(null))
            .build());

  }

  @Test
  @Description("Test that the name of a thread executing a processor has excution context information about its flow")
  public void flowProcessingThreadName() throws Exception {
    flowRunner("willSchedule").run();

    assertThat(RecordThreadName.threadName,
               allOf(startsWith("[MuleRuntime].io."),
                     // [appName].flowName.processingStrategy
                     containsString("[SchedulerServiceTestCase#flowProcessingThreadName].willSchedule.BLOCKING @")));
  }

  public static class HasSchedulingService implements Processor, Initialisable, Disposable {

    @Inject
    private SchedulerService schedulerService;

    private Scheduler scheduler;

    @Override
    public void initialise() throws InitialisationException {
      scheduler = schedulerService.cpuLightScheduler();
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

}
