/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.http;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.runtime.api.scheduler.SchedulerConfig.config;
import static org.mule.tck.junit4.matcher.Eventually.eventually;
import org.junit.Before;
import org.junit.Test;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerConfig;
import org.mule.runtime.api.scheduler.SchedulerPoolsConfigFactory;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.api.scheduler.SchedulerView;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.client.HttpClient;
import org.mule.runtime.http.api.client.HttpClientConfiguration;
import org.mule.test.AbstractIntegrationTestCase;

import javax.inject.Inject;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;

public class GrizzlyHttpClientSchedulerTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/http/dummy-app.xml";
  }

  @Inject
  private HttpService httpService;

  @Inject
  private SchedulerService schedulerService;

  private Latch lockLatch;
  private Latch startTestLatch;
  private AtomicReference<Scheduler> schedulerReference;
  private HttpClient httpClient;
  private TestSchedulerService testSchedulerService;

  @Before
  public void initialize() throws Exception {
    lockLatch = new Latch();
    startTestLatch = new Latch();
    schedulerReference = new AtomicReference<>();
    httpClient =
        httpService.getClientFactory().create(new HttpClientConfiguration.Builder().setName("http-client-scheduler").build());
    testSchedulerService = new TestSchedulerService(schedulerService) {

      @Override
      public Scheduler customScheduler(SchedulerConfig config, int queueSize) {
        Scheduler scheduler = schedulerServiceDelegate.customScheduler(config, queueSize);
        schedulerReference.set(scheduler);
        scheduler.execute(() -> {
          try {
            startTestLatch.release();
            lockLatch.await();
          } catch (InterruptedException e) {
            fail("Fail initializing selector pool");
          }
        });
        try {
          startTestLatch.await();
        } catch (InterruptedException e) {
          fail(e.getMessage());
        }
        return scheduler;
      }
    };

    Field schedulerServiceField = httpClient.getClass().getDeclaredField("schedulerService");
    setFinalField(httpClient, schedulerServiceField, testSchedulerService);
  }

  @Test
  public void testSchedulerWithNonFinishTask() throws NoSuchFieldException, IllegalAccessException {
    Future future = schedulerService.customScheduler(config()
        .withDirectRunCpuLightWhenTargetBusy(true)
        .withMaxConcurrentTasks(1)
        .withName("TEST")).submit(() -> {
          httpClient.start();
          lockLatch.release();
        });
    try {
      future.get(5, SECONDS);
    } catch (Exception e) {
      fail(e.getMessage());
    }
    ThreadPoolExecutor executor = (ThreadPoolExecutor) getPrivateField(schedulerReference.get(), "executor");
    assertThat(executor.getQueue(), is(eventually(empty())));

  }

  private static class TestSchedulerService implements SchedulerService {

    protected SchedulerService schedulerServiceDelegate;

    private TestSchedulerService(SchedulerService schedulerServiceDelegate) {
      this.schedulerServiceDelegate = schedulerServiceDelegate;
    }

    @Override
    public String getName() {
      return schedulerServiceDelegate.getName();
    }

    @Override
    public Scheduler cpuLightScheduler() {
      return schedulerServiceDelegate.cpuLightScheduler();
    }

    @Override
    public Scheduler ioScheduler() {
      return schedulerServiceDelegate.ioScheduler();
    }

    @Override
    public Scheduler cpuIntensiveScheduler() {
      return schedulerServiceDelegate.cpuIntensiveScheduler();
    }

    @Override
    public Scheduler cpuLightScheduler(SchedulerConfig config) {
      return schedulerServiceDelegate.cpuLightScheduler(config);
    }

    @Override
    public Scheduler ioScheduler(SchedulerConfig config) {
      return schedulerServiceDelegate.ioScheduler(config);
    }

    @Override
    public Scheduler cpuIntensiveScheduler(SchedulerConfig config) {
      return schedulerServiceDelegate.cpuIntensiveScheduler(config);
    }

    @Override
    public Scheduler cpuLightScheduler(SchedulerConfig config, SchedulerPoolsConfigFactory poolsConfigFactory) {
      return schedulerServiceDelegate.cpuLightScheduler(config, poolsConfigFactory);
    }

    @Override
    public Scheduler ioScheduler(SchedulerConfig config, SchedulerPoolsConfigFactory poolsConfigFactory) {
      return schedulerServiceDelegate.ioScheduler(config, poolsConfigFactory);
    }

    @Override
    public Scheduler cpuIntensiveScheduler(SchedulerConfig config, SchedulerPoolsConfigFactory poolsConfigFactory) {
      return schedulerServiceDelegate.cpuIntensiveScheduler(config, poolsConfigFactory);
    }

    @Override
    public Scheduler customScheduler(SchedulerConfig config) {
      return schedulerServiceDelegate.customScheduler(config);
    }

    @Override
    public Scheduler customScheduler(SchedulerConfig config, int queueSize) {
      return schedulerServiceDelegate.customScheduler(config, queueSize);
    }

    @Override
    public List<SchedulerView> getSchedulers() {
      return schedulerServiceDelegate.getSchedulers();
    }
  }

  private void setFinalField(Object object, Field field, Object newValue) throws Exception {
    field.setAccessible(true);

    Field modifiersField = Field.class.getDeclaredField("modifiers");
    modifiersField.setAccessible(true);
    modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

    field.set(object, newValue);
  }

  private Object getPrivateField(Object obj, String field) throws NoSuchFieldException, IllegalAccessException {
    Field f = obj.getClass().getDeclaredField(field);
    f.setAccessible(true);
    return f.get(obj);
  }

}
