/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.scheduler.cron;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.test.allure.AllureConstants.SchedulerFeature.SCHEDULER;

import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.source.SchedulerMessageSource;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.AbstractSchedulerTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import io.qameta.allure.Feature;

import org.junit.Test;

@Feature(SCHEDULER)
public class StoppedCronSchedulerTestCase extends AbstractSchedulerTestCase {

  private static List<String> foo = new ArrayList<>();

  @Override
  protected String getConfigFile() {
    return "cron-scheduler-stopped-config.xml";
  }

  @Test
  public void test() throws Exception {
    runSchedulersOnce(() -> {
      new PollingProber(RECEIVE_TIMEOUT, 200).check(new JUnitLambdaProbe(() -> {
        assertThat(foo, hasSize(greaterThanOrEqualTo(1)));
        return true;
      }));
      return null;
    });
  }

  public static Object addFoo(String payload) {
    synchronized (foo) {
      foo.add(payload);
    }
    return payload;
  }

  private void runSchedulersOnce(Supplier<Void> assertionSupplier) throws Exception {
    registry.<Startable>lookupByName("pollfoo").get().start();
    try {
      locator.find(builderFromStringRepresentation("pollfoo/source").build()).map(source -> (SchedulerMessageSource) source)
          .ifPresent(SchedulerMessageSource::trigger);
      assertionSupplier.get();
    } finally {
      registry.<Stoppable>lookupByName("pollfoo").get().stop();
    }
  }
}
