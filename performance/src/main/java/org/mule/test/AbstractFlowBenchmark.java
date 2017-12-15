/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test;

import static java.lang.Class.forName;
import static java.lang.Thread.sleep;
import static java.lang.reflect.Proxy.newProxyInstance;
import static java.util.concurrent.locks.LockSupport.parkNanos;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.construct.Flow.builder;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.BLOCKING;
import static org.mule.runtime.core.privileged.registry.LegacyRegistryUtils.lookupObject;
import static org.mule.runtime.core.privileged.registry.LegacyRegistryUtils.registerObject;
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
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.DefaultMuleContextFactory;
import org.mule.runtime.core.api.context.MuleContextFactory;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.internal.config.builders.DefaultsConfigurationBuilder;
import org.mule.runtime.core.internal.processor.strategy.AbstractProcessingStrategyFactory;
import org.mule.runtime.core.internal.processor.strategy.ReactorStreamProcessingStrategyFactory;
import org.mule.service.scheduler.internal.DefaultSchedulerService;
import org.mule.tck.TriggerableMessageSource;
import org.mule.weave.v2.el.WeaveDefaultExpressionLanguageFactoryService;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.LockSupport;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import reactor.core.publisher.Mono;

@State(Benchmark)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
public abstract class AbstractFlowBenchmark extends AbstractBenchmark {

  static final Processor nullProcessor = event -> event;

  static final Processor cpuLightProcessor = event -> {
    // Roughly 20uS on modern CPU.
    consumeCPU(1000);
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
        sleep(20);
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

  static final Processor iorwSmall = new Processor() {

    @Override
    public CoreEvent process(CoreEvent event) {
      consumeCPU(1000);
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
      for (int i = 0; i < 10; i++) {
        consumeCPU(1000);
        parkNanos(50000);
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
      for (int i = 0; i < 50; i++) {
        consumeCPU(1000);
        parkNanos(50000);
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
      // "org.mule.runtime.core.internal.processor.strategy.DirectProcessingStrategyFactory",
      // "org.mule.runtime.core.internal.processor.strategy.DirectStreamPerThreadProcessingStrategyFactory",
      "org.mule.runtime.core.internal.processor.strategy.ReactorProcessingStrategyFactory",
      "org.mule.runtime.core.internal.processor.strategy.ReactorStreamProcessingStrategyFactory", // Stream unico + cpuLight pool
      "org.mule.runtime.core.internal.processor.strategy.ProactorStreamProcessingStrategyFactory", // Stream unico + cpuLight pool
      // + IO pool for blocking
      "org.mule.runtime.core.internal.processor.strategy.WorkQueueProcessingStrategyFactory", // 4.0
      "org.mule.runtime.core.internal.processor.strategy.WorkQueueStreamProcessingStrategyFactory", // Stream unico + IO pool
      "org.mule.runtime.core.internal.processor.strategy.WorkQueueMultiStreamProcessingStrategyFactory"
  })
  public String processingStrategyFactory;

  @Param({"1"})
  public int subscribers;

  @Param({"256"})
  public int bufferSize;

  @Param({"10000"})
  public int maxConcurrency;

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
        DefaultExpressionLanguageFactoryService weaveExpressionExecutor = new WeaveDefaultExpressionLanguageFactoryService();
        registerObject(muleContext, weaveExpressionExecutor.getName(), weaveExpressionExecutor);
      }
    });
    builderList.add(new DefaultsConfigurationBuilder());
    return muleContextFactory.createMuleContext(builderList.toArray(new ConfigurationBuilder[] {}));
  }

  @Setup
  public void setup() throws Exception {
    muleContext = createMuleContextWithServices();
    muleContext.start();

    ProcessingStrategyFactory factory = (ProcessingStrategyFactory) forName(processingStrategyFactory).newInstance();
    if (factory instanceof AbstractProcessingStrategyFactory) {
      ((AbstractProcessingStrategyFactory) factory).setMaxConcurrency(maxConcurrency);
    }
    if (factory instanceof ReactorStreamProcessingStrategyFactory) {
      ((ReactorStreamProcessingStrategyFactory) factory).setBufferSize(bufferSize);
      ((ReactorStreamProcessingStrategyFactory) factory).setSubscriberCount(subscribers);
    }


    source = new TriggerableMessageSource();
    flow = builder(AbstractBenchmark.FLOW_NAME, muleContext).processors(getMessageProcessors()).source(source)
        .processingStrategyFactory(factory).build();
    registerObject(muleContext, AbstractBenchmark.FLOW_NAME, flow, FlowConstruct.class);
  }

  protected abstract List<Processor> getMessageProcessors();

  protected abstract int getStreamIterations();

  @TearDown
  public void teardown() throws MuleException {
    muleContext.dispose();
    schedulerService.stop();
  }

  // @Benchmark
  // public CoreEvent processSourceBlocking() throws MuleException {
  // return source.trigger(CoreEvent.builder(create(flow, AbstractBenchmark.CONNECTOR_LOCATION))
  // .message(of(AbstractBenchmark.PAYLOAD)).build());
  // }

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
