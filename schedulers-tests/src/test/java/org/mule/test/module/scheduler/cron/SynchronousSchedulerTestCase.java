/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.scheduler.cron;


import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import org.mule.functional.api.component.EventCallback;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.AbstractSchedulerTestCase;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * <p>
 * Validates that a synchronous flow processing strategy implies a synchronous poll execution
 * </p>
 */
public class SynchronousSchedulerTestCase extends AbstractSchedulerTestCase {

  private static List<String> foo = new ArrayList<>();

  @Override
  protected String getConfigFile() {
    return "cron-synchronous-scheduler-config.xml";
  }

  @Test
  public void test() throws InterruptedException {
    new PollingProber(7000, 100).check(new JUnitLambdaProbe(() -> {
      assertThat(foo, hasSize(1));
      return true;
    }));
  }

  public static class Foo implements EventCallback {

    @Override
    public void eventReceived(InternalEvent event, Object component, MuleContext muleContext) throws Exception {
      synchronized (foo) {
        foo.add((String) event.getMessage().getPayload().getValue());
        try {
          Thread.sleep(10000);
        } catch (InterruptedException e) {

        }
      }
    }
  }

}
