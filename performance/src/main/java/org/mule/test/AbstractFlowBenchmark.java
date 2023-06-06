/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test;

import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.construct.Flow.builder;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.BLOCKING;
import static org.mule.runtime.core.privileged.registry.LegacyRegistryUtils.registerObject;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.from;
import static org.mule.tck.config.WeaveExpressionLanguageFactoryServiceProvider.provideDefaultExpressionLanguageFactoryService;

import static java.lang.Class.forName;
import static java.lang.Thread.sleep;
import static java.lang.reflect.Proxy.newProxyInstance;
import static java.util.Collections.singletonMap;
import static java.util.concurrent.locks.LockSupport.parkNanos;

import static org.openjdk.jmh.annotations.Scope.Benchmark;
import static org.openjdk.jmh.infra.Blackhole.consumeCPU;

import org.mule.AbstractBenchmark;
import org.mule.runtime.api.el.DefaultExpressionLanguageFactoryService;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.context.DefaultMuleContextFactory;
import org.mule.runtime.core.api.context.MuleContextFactory;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.internal.config.builders.MinimalConfigurationBuilder;
import org.mule.runtime.core.internal.processor.strategy.AbstractStreamProcessingStrategyFactory;
import org.mule.service.scheduler.internal.DefaultSchedulerService;
import org.mule.tck.TriggerableMessageSource;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import reactor.core.publisher.Mono;

@State(Benchmark)
@Warmup(iterations = 10)
@Measurement(iterations = 10)
public abstract class AbstractFlowBenchmark extends AbstractBenchmark {

  static final Processor nullProcessor = event -> event;

  static final Processor cpuLightProcessor = event -> {
    // Roughly 20uS on modern CPU.
    consumeCPU(10000);
    return event;
  };

  static final Processor cpuLight2Processor = event -> {
    // Roughly 50uS on modern CPU.
    consumeCPU(25000);
    return event;
  };

  static final Processor cpuIntensiveProcessor = new Processor() {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      // Roughly 5mS on modern CPU.
      consumeCPU(2500000);
      return event;
    }

    @Override
    public ProcessingType getProcessingType() {
      return ProcessingType.CPU_INTENSIVE;
    }
  };

  static final Processor blockingProcessor = new Processor() {

    @Override
    public CoreEvent process(CoreEvent event) {
      try {
        sleep(1);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      return event;
    }

    @Override
    public ProcessingType getProcessingType() {
      return BLOCKING;
    }
  };

  static final Processor blocking2Processor = new Processor() {

    @Override
    public CoreEvent process(CoreEvent event) {
      try {
        sleep(5);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      return event;
    }

    @Override
    public ProcessingType getProcessingType() {
      return BLOCKING;
    }
  };

  static final Processor iorwXSmall = new Processor() {

    @Override
    public CoreEvent process(CoreEvent event) {
      consumeCPU(5000);
      return event;
    }

    @Override
    public ProcessingType getProcessingType() {
      return BLOCKING;
    }
  };

  static final Processor iorwSmall = new Processor() {

    @Override
    public CoreEvent process(CoreEvent event) {
      for (int i = 0; i < 5; i++) {
        // 10uS
        consumeCPU(5000);
        // 0.1ms
        parkNanos(100000);
      }
      return event;
    }

    @Override
    public ProcessingType getProcessingType() {
      return BLOCKING;
    }
  };

  static final Processor iorwMedium = new Processor() {

    @Override
    public CoreEvent process(CoreEvent event) {
      for (int i = 0; i < 20; i++) {
        // 10uS
        consumeCPU(5000);
        // 0.1ms
        parkNanos(100000);
      }
      return event;
    }

    @Override
    public ProcessingType getProcessingType() {
      return BLOCKING;
    }
  };

  static final Processor iorwLarge = new Processor() {

    @Override
    public CoreEvent process(CoreEvent event) {
      for (int i = 0; i < 100; i++) {
        // 10uS
        consumeCPU(5000);
        // 0.1ms
        parkNanos(100000);
      }
      return event;
    }

    @Override
    public ProcessingType getProcessingType() {
      return BLOCKING;
    }
  };


  protected MuleContext muleContext;
  protected Flow flow;
  protected TriggerableMessageSource source;

  private DefaultSchedulerService schedulerService;

  @Param({
      "org.mule.runtime.core.internal.processor.strategy.DirectProcessingStrategyFactory",
      "org.mule.runtime.core.internal.processor.strategy.ReactorProcessingStrategyFactory",
      "org.mule.runtime.core.internal.processor.strategy.ReactorStreamProcessingStrategyFactory",
      "org.mule.runtime.core.internal.processor.strategy.ProactorStreamProcessingStrategyFactory",
      "org.mule.runtime.core.internal.processor.strategy.WorkQueueProcessingStrategyFactory",
      "org.mule.runtime.core.internal.processor.strategy.WorkQueueStreamProcessingStrategyFactory",
  })
  public String processingStrategyFactory;

  @Param({"1"})
  public int subscribers;

  @Param({"256"})
  public int bufferSize;

  @Param({"10000"})
  public int maxConcurrency;

  @Override
  protected MuleContext createMuleContextWithServices() throws MuleException {
    MuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
    List<ConfigurationBuilder> builderList = new ArrayList<>();
    builderList.add(new AbstractConfigurationBuilder() {

      @Override
      protected void doConfigure(MuleContext muleContext) throws Exception {
        schedulerService = new DefaultSchedulerService();
        schedulerService.start();
        registerObject(muleContext, schedulerService.getName(),
                       newProxyInstance(getClass().getClassLoader(), new Class[] {SchedulerService.class},
                                        new PassThroughInvocationHandler(schedulerService)));
        DefaultExpressionLanguageFactoryService weaveExpressionExecutor = provideDefaultExpressionLanguageFactoryService();
        registerObject(muleContext, weaveExpressionExecutor.getName(), weaveExpressionExecutor);
      }
    });
    builderList.add(new MinimalConfigurationBuilder());
    return muleContextFactory.createMuleContext(builderList.toArray(new ConfigurationBuilder[] {}));
  }

  @Setup
  public void setup() throws Exception {
    muleContext = createMuleContextWithServices();
    muleContext.start();

    ProcessingStrategyFactory factory = (ProcessingStrategyFactory) forName(processingStrategyFactory).newInstance();
    if (factory instanceof AbstractStreamProcessingStrategyFactory) {
      ((AbstractStreamProcessingStrategyFactory) factory).setBufferSize(bufferSize);
      ((AbstractStreamProcessingStrategyFactory) factory).setSubscriberCount(subscribers);
    }


    source = new TriggerableMessageSource();
    flow = builder(AbstractBenchmark.FLOW_NAME, muleContext).processors(getMessageProcessors()).source(source)
        .processingStrategyFactory(factory).maxConcurrency(maxConcurrency).build();
    flow.setAnnotations(singletonMap(LOCATION_KEY, from("flow")));
    registerObject(muleContext, AbstractBenchmark.FLOW_NAME, flow);
  }

  protected abstract List<Processor> getMessageProcessors();

  protected abstract int getStreamIterations();

  @TearDown
  public void teardown() throws MuleException {
    muleContext.dispose();
    schedulerService.stop();
  }

  @Benchmark
  public CoreEvent processSourceBlocking() throws MuleException {
    return source.trigger(CoreEvent.builder(create(flow, AbstractBenchmark.CONNECTOR_LOCATION))
        .message(of(AbstractBenchmark.PAYLOAD)).build());
  }

  @Benchmark
  public CountDownLatch processSourceStream() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(getStreamIterations());
    for (int i = 0; i < getStreamIterations(); i++) {
      Mono.just(CoreEvent.builder(create(flow, AbstractBenchmark.CONNECTOR_LOCATION))
          .message(of(AbstractBenchmark.PAYLOAD)).build()).transform(source.getListener()).doOnNext(event -> latch.countDown())
          .subscribe();
    }
    latch.await();
    return latch;
  }

  private static class PassThroughInvocationHandler implements InvocationHandler {

    private final Object target;

    public PassThroughInvocationHandler(Object target) {
      this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      return method.invoke(target, args);
    }
  }

}
