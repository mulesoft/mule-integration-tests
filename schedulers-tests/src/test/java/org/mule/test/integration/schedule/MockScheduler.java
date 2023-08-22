/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.schedule;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.NameableObject;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class MockScheduler implements NameableObject {

  private final AtomicInteger count = new AtomicInteger(0);
  private ScheduledExecutorService executorService;
  private String name;
  private Runnable task;

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }

  public void start() throws MuleException {
    executorService = newSingleThreadScheduledExecutor();
    task = () -> count.incrementAndGet();
    executorService.scheduleAtFixedRate(task, 1000, 2000, MILLISECONDS);
  }

  public void stop() throws MuleException {
    executorService.shutdown();
  }

  public int getCount() {
    return count.get();
  }

}
