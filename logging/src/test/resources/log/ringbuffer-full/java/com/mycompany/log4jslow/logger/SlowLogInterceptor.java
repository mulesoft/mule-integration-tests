/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mycompany.log4jslow.logger;

import static java.lang.Thread.currentThread;

import java.util.concurrent.CountDownLatch;

import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.rewrite.RewritePolicy;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

@Plugin(name = "SlowLogInterceptor", category = Core.CATEGORY_NAME, elementType = "rewritePolicy", printObject = true)
public final class SlowLogInterceptor implements RewritePolicy {

  private static final CountDownLatch slowDownLatch = new CountDownLatch(1);

  @Override
  public LogEvent rewrite(final LogEvent event) {
    try {
      slowDownLatch.await();
    } catch(InterruptedException e) {
      currentThread().interrupt();
      return event;
    }
    return event;
  }

  @PluginFactory
  public static SlowLogInterceptor createPolicy() {
    return new SlowLogInterceptor();
  }

  public static boolean releaseLatch() {
    slowDownLatch.countDown();
    return true;
  }
}
