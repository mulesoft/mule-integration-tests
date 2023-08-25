/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.el;

import static org.mule.test.allure.AllureConstants.ExpressionLanguageFeature.EXPRESSION_LANGUAGE;

import static java.lang.Thread.currentThread;

import static org.junit.Assert.fail;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.qameta.allure.Feature;

@Feature(EXPRESSION_LANGUAGE)
public class ExpressionLanguageConcurrencyTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/el/expression-language-concurrency-config.xml";
  }

  @Test
  public void testConcurrentEvaluation() throws Exception {
    final int N = 100;
    final CountDownLatch start = new CountDownLatch(1);
    final CountDownLatch end = new CountDownLatch(N);
    final AtomicInteger errors = new AtomicInteger(0);
    for (int i = 0; i < N; i++) {
      new Thread((Runnable) () -> {
        try {
          start.await();
          flowRunner("slowRequestHandler").withPayload("foo").run();
        } catch (Exception e) {
          // A NullPointerException is thrown when a lookup is performed when a registry is
          // added or removed concurrently
          errors.incrementAndGet();
        } finally {
          end.countDown();
        }
      }, "thread-eval-" + i).start();
    }
    start.countDown();
    end.await();
    if (errors.get() > 0) {
      fail();
    }
  }

  public static long sleep(long millis) {
    try {
      Thread.sleep(millis);
      return millis;
    } catch (InterruptedException e) {
      currentThread().interrupt();
      throw new MuleRuntimeException(e);
    }
  }
}
