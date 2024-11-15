/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transaction;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.fail;

import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.TX_COMMIT;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.TX_CONTINUE;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.TX_ROLLBACK;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.TX_START;
import static org.mule.runtime.api.util.MuleSystemProperties.DEFAULT_ERROR_HANDLER_NOT_ROLLBACK_IF_NOT_CORRESPONDING_PROPERTY;

import static java.util.Arrays.asList;

import org.junit.ClassRule;
import org.junit.Rule;

import org.mule.runtime.api.profiling.ProfilingDataConsumer;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.api.profiling.type.context.TransactionProfilingEventContext;
import org.mule.runtime.core.privileged.profiling.PrivilegedProfilingService;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunnerDelegateTo(Parameterized.class)
public class TransactionRollbackedByOwnerTestCase extends AbstractIntegrationTestCase {

  private static final int POLL_DELAY_MILLIS = 100;

  @Inject
  private PrivilegedProfilingService service;

  private List<String> states;
  private final String flowName;
  private final List<String> expectedStates;
  private final boolean throwsMessagingException;
  private final String config;

  @ClassRule
  public static SystemProperty enableProfilingService = new SystemProperty("mule.enable.profiling.service", "true");

  @ClassRule
  public static SystemProperty enableProfilingConsumers =
      new SystemProperty("mule.force.runtime.profiling.consumers.enablement", "true");

  @Rule
  public SystemProperty defaultErrorHandler =
      new SystemProperty(DEFAULT_ERROR_HANDLER_NOT_ROLLBACK_IF_NOT_CORRESPONDING_PROPERTY, "true");

  @Parameters(name = "{0} - {2}")
  public static Object[][] params() {
    return new Object[][] {
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "no-rollback", false, asList("start", "commit")},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "rollback", true, asList("start", "rollback")},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "no-rollback-outside-try", true, asList("start", "commit")},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "no-rollback-flowref", false, asList("start", "continue", "commit")},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "no-rollback-error-in-flow-ref", false, asList("start", "commit")},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "rollback-error-in-flow-ref-with-try", true, asList("start", "continue", "rollback")},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "no-rollback-error-in-flow-ref-with-try", false, asList("start", "continue", "commit")},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "no-rollback-error-in-flow-ref-with-try-join-tx", false,
            asList("start", "continue", "continue", "continue", "commit")},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "with-implicit-default-EH-executed-commits", false, asList("start", "continue", "continue", "commit")},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "with-implicit-default-EH-executed-rollback", true, asList("start", "continue", "continue", "rollback")},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "with-default-EH-executed-commits", false, asList("start", "continue", "continue", "commit")},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "with-default-EH-executed-rollback", false, asList("start", "rollback")},

        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner-subflow.xml",
            "rollback-on-error-prop", false, asList("start", "rollback")},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner-subflow.xml",
            "rollback-default-on-error-prop", true, asList("start", "rollback")},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner-subflow.xml",
            "rollback-in-flow", true, asList("start", "rollback")},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner-subflow.xml",
            "commit-flow-on-error-continue", false, asList("start", "commit")},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner-subflow.xml",
            "rollback-nested-subflows", true, asList("start", "rollback")},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner-subflow.xml",
            "commit-nested-subflows", false, asList("start", "commit")},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner-subflow.xml",
            "rollback-nested-flows", true, asList("start", "continue", "rollback")},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner-subflow.xml",
            "commit-nested-flows", false, asList("start", "continue", "commit")},

        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "no-rollback", false, asList("start", "commit")},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "rollback", false, asList("start", "rollback")},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "no-rollback-outside-try", true, asList("start", "commit")},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "no-rollback-flowref", false, asList("start", "continue", "commit")},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "no-rollback-error-in-flow-ref", false, asList("start", "commit")},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "rollback-error-in-flow-ref-with-try", true, asList("start", "rollback")},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "no-rollback-error-in-flow-ref-with-try", false, asList("start", "continue", "commit")},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "rollback-error-in-flow-ref-with-nested-try", false, asList("start", "rollback")},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "no-rollback-error-in-flow-ref-with-nested-try", false,
            asList("start", "continue", "continue", "continue", "commit")},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "no-rollback-error-in-flow-ref-with-try-join-tx", false,
            asList("start", "continue", "continue", "continue", "commit")},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "rollback-error-in-nested-try", true, asList("start", "continue", "continue", "rollback")},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "rollback-error-in-nested-try-with-same-error-handler", true, asList("start", "continue", "continue", "rollback")},

        new Object[] {"Default Error Handler", "org/mule/test/integration/transaction/transaction-owner-default-err.xml",
            "rollback", true, asList("start", "rollback")},
        new Object[] {"Default Error Handler", "org/mule/test/integration/transaction/transaction-owner-default-err.xml",
            "no-rollback-outside-try", true, asList("start", "commit")},
        new Object[] {"Default Error Handler", "org/mule/test/integration/transaction/transaction-owner-default-err.xml",
            "rollback-error-in-flow-ref-with-try", true, asList("start", "rollback")},
        new Object[] {"Default Error Handler", "org/mule/test/integration/transaction/transaction-owner-default-err.xml",
            "rollback-error-in-flow-ref-with-nested-try", false, asList("start", "rollback")},
        new Object[] {"Default Error Handler", "org/mule/test/integration/transaction/transaction-owner-default-err.xml",
            "no-rollback-error-in-flow-ref-with-nested-try", false, asList("start", "continue", "continue", "continue", "commit")}
    };
  }

  public TransactionRollbackedByOwnerTestCase(String type, String config, String flowName, boolean throwsMessagingException,
                                              List<String> expectedStates) {
    this.flowName = flowName;
    this.expectedStates = expectedStates;
    this.throwsMessagingException = throwsMessagingException;
    this.config = config;
  }

  @Override
  protected String getConfigFile() {
    return config;
  }

  @Override
  protected void doSetUp() throws Exception {
    states = new ArrayList<>();
    service.registerProfilingDataConsumer(new ProfilingDataConsumer<TransactionProfilingEventContext>() {

      @Override
      public void onProfilingEvent(ProfilingEventType<TransactionProfilingEventContext> profilingEventType,
                                   TransactionProfilingEventContext profilingEventContext) {
        states.add(profilingEventType.toString());
      }

      @Override
      public Set<ProfilingEventType<TransactionProfilingEventContext>> getProfilingEventTypes() {
        Set<ProfilingEventType<TransactionProfilingEventContext>> events = new HashSet<>();
        events.add(TX_START);
        events.add(TX_COMMIT);
        events.add(TX_CONTINUE);
        events.add(TX_ROLLBACK);
        return events;
      }

      @Override
      public Predicate<TransactionProfilingEventContext> getEventContextFilter() {
        return tx -> true;
      }
    });

  }

  @Test
  public void checkRollback() {
    try {
      flowRunner(flowName).withPayload("message").run();
      if (throwsMessagingException) {
        fail("Should have thrown Exception from unhandled error");
      }
    } catch (Exception e) {
      if (!throwsMessagingException) {
        fail("Should have not thrown Exception from handled error");
      }
    }

    states.forEach(s -> System.out.println(s));
    assertStatesArrived();
    assertCorrectStates();
  }

  private void assertStatesArrived() {
    PollingProber pollingProber = new PollingProber(RECEIVE_TIMEOUT, POLL_DELAY_MILLIS);
    pollingProber.check(new JUnitProbe() {

      @Override
      protected boolean test() throws Exception {
        assertThat(states.size(), greaterThanOrEqualTo(2));
        return true;
      }

      @Override
      public String describeFailure() {
        return "States did not arrive";
      }
    });
  }

  private void assertCorrectStates() {
    assertThat(states, contains(expectedStates.toArray()));
  }

}
