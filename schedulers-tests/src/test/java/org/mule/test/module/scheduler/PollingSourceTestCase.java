/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.scheduler;

import static java.util.stream.Collectors.toList;

import static org.mule.tck.probe.PollingProber.check;
import static org.mule.test.petstore.extension.PetAdoptionSource.ALL_PETS;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.test.AbstractSchedulerTestCase;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

public class PollingSourceTestCase extends AbstractSchedulerTestCase {

  @Override
  protected String getConfigFile() {
    return "polling-source-config.xml";
  }

  private static final List<CoreEvent> ADOPTION_EVENTS = new LinkedList<>();

  public static class AdoptionProcessor implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      synchronized (ADOPTION_EVENTS) {
        ADOPTION_EVENTS.add(event);
      }
      return event;
    }
  }

  /* This test checks that when a polling source with a fixed frequency scheduler with start delay is restarted, the
  start delay is not applied again. The polling source of this test is set to fail midway through populating the pet
  adoption list, which will provoke a restart. Without the changes made in MULE-16974, this test would fail, because the
  start delay would be re-applied on the restart and the probe would timeout. */
  @Test
  public void whenReconnectingAfterConnectionExceptionSchedulerRunsWithoutStartDelay() throws Exception {
    startFlow("fixedFrequencyReconnectingPoll");
    assertAllPetsAdopted();
  }

  private void assertAllPetsAdopted() {
    check(5000, 300, () -> {
      synchronized (ADOPTION_EVENTS) {
        return ADOPTION_EVENTS.size() >= ALL_PETS.size() &&
            ADOPTION_EVENTS.stream().map(e -> e.getMessage().getPayload().getValue().toString()).collect(toList())
                .containsAll(ALL_PETS);
      }
    });
  }

  private void startFlow(String flowName) throws Exception {
    ((Startable) getFlowConstruct(flowName)).start();
  }
}
