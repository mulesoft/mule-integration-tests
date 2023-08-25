/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.logging;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.mule.test.AbstractIntegrationTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.qameta.allure.Issue;
import org.junit.Test;

/**
 * When setting logging variables with tracing module, race conditions can occur. This test will ensure that there are no race
 * conditions when setting logging variables, with the fix for (W-12206167).
 */
@Issue("W-12206167")
public class TracingLoggingVariableTestCase extends AbstractIntegrationTestCase {

  private static final int NUMBER_OF_THREADS = 100;

  @Override
  protected String getConfigFile() {
    return "org/mule/logging/async-tracing-set-logging-variable.xml";
  }

  @Test
  public void tracingSetLoggingVariableWithRaceCondition() throws Exception {
    List<Thread> threads = new ArrayList<>();
    AtomicBoolean testFailed = new AtomicBoolean(false);
    for (int i = 0; i < NUMBER_OF_THREADS; i++) {
      Thread t = new Thread(() -> {
        try {
          flowRunner("async-set-logging-variable-flow").run();
        } catch (Exception e) {
          testFailed.set(true);
        }
      });
      threads.add(t);
      t.start();
    }
    for (Thread t : threads) {
      t.join();
    }
    assertThat(testFailed.get(), is(false));
  }
}
