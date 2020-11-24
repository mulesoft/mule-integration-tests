/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Test;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.AbstractIntegrationTestCase;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.privileged.event.PrivilegedEvent.getCurrentEvent;
import static org.mule.test.allure.AllureConstants.RoutersFeature.ParallelForEachStory.PARALLEL_FOR_EACH;
import static org.mule.test.allure.AllureConstants.RoutersFeature.ROUTERS;

@Feature(ROUTERS)
@Story(PARALLEL_FOR_EACH)
public class ParallelForEachWithContextScopesTestCase extends AbstractIntegrationTestCase {

  private static final int PROBER_POLLING_INTERVAL = 100;
  private static final int PROBER_POLLING_TIMEOUT = 5000;
  private static final String[] FRUIT_LIST = new String[] {"apple", "banana", "orange"};

  private static final Set<PhantomReference<CoreEvent>> eventRefs = new HashSet<>();

  @Before
  public void before() {
    eventRefs.clear();
  }

  @Override
  protected String getConfigFile() {
    return "routers/parallel-for-each-context-scopes.xml";
  }

  @Test
  @Issue("MULE-18696")
  @Description("Check that parallel for each is not referencing to the original event prior the error handling")
  public void parallelForEachWithErrorHandling() throws Exception {
    flowRunner("parallelForEachWithErrorHandling").withPayload(FRUIT_LIST).run();
    // to check last event
    assertEventsUnreferenced();
  }

  private static void assertEventsUnreferenced() {
    new PollingProber(PROBER_POLLING_TIMEOUT, PROBER_POLLING_INTERVAL).check(new JUnitProbe() {

      @Override
      protected boolean test() throws Exception {
        System.gc();
        for (PhantomReference<CoreEvent> phantomReference : eventRefs) {
          assertThat(phantomReference.isEnqueued(), is(true));
        }
        return true;
      }
    });
  }

  @Override
  protected boolean isGracefulShutdown() {
    return true;
  }

  public static class EventReferenceProcessor implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      synchronized (eventRefs) {
        // We have to verify that previous references are not being referenced any more (the execution of that parallel
        // foreach route has finished). This is because Parallel For each has a reference to the event to
        // aggregate results after the entire execution is finished, but that final event shouldn't have references
        // to inner references
        if (eventRefs.size() > 0) {
          assertEventsUnreferenced();
        }
        eventRefs.add(new PhantomReference<>(getCurrentEvent(), new ReferenceQueue<>()));
      }
      return event;
    }
  }

}
