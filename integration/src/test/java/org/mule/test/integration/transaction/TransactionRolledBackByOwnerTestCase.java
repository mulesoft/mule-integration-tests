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
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.TX_COMMIT;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.TX_CONTINUE;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.TX_ROLLBACK;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.TX_START;
import static org.mule.runtime.api.util.MuleSystemProperties.DEFAULT_ERROR_HANDLER_NOT_ROLLBACK_IF_NOT_CORRESPONDING_PROPERTY;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

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
import org.mule.tests.api.TestQueueManager;

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
public class TransactionRolledBackByOwnerTestCase extends AbstractIntegrationTestCase {

  private static final int POLL_DELAY_MILLIS = 100;

  @Inject
  private PrivilegedProfilingService service;

  @Inject
  private TestQueueManager queueManager;

  private List<String> states;
  private final String flowName;
  private final List<String> expectedStates;
  private final boolean throwsMessagingException;
  private final String config;
  private final boolean ignoreExtraStates;

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
            "no-rollback", false, asList("start", "commit"), false},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "rollback", true, asList("start", "rollback"), false},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "no-rollback-outside-try", true, asList("start", "commit"), false},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "no-rollback-flowref", false, asList("start", "continue", "commit"), false},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "no-rollback-error-in-flow-ref", false, asList("start", "commit"), false},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "rollback-error-in-flow-ref-with-try", true, asList("start", "continue", "rollback"), false},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "no-rollback-error-in-flow-ref-with-try", false, asList("start", "continue", "commit"), false},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "no-rollback-error-in-flow-ref-with-try-join-tx", false,
            asList("start", "continue", "continue", "continue", "commit"), false},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "with-implicit-default-EH-executed-commits", false, asList("start", "continue", "continue", "commit"), false},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "with-implicit-default-EH-executed-rollback", true, asList("start", "continue", "continue", "rollback"), false},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "with-default-EH-executed-commits", false, asList("start", "continue", "continue", "commit"), false},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "with-default-EH-executed-rollback", false, asList("start", "rollback"), false},

        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner-subflow.xml",
            "rollback-on-error-prop", false, asList("start", "rollback"), false},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner-subflow.xml",
            "rollback-default-on-error-prop", true, asList("start", "rollback"), false},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner-subflow.xml",
            "rollback-in-flow", true, asList("start", "rollback"), false},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner-subflow.xml",
            "commit-flow-on-error-continue", false, asList("start", "commit"), false},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner-subflow.xml",
            "rollback-nested-subflows", true, asList("start", "rollback"), false},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner-subflow.xml",
            "commit-nested-subflows", false, asList("start", "commit"), false},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner-subflow.xml",
            "rollback-nested-flows", true, asList("start", "continue", "rollback"), false},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner-subflow.xml",
            "commit-nested-flows", false, asList("start", "continue", "commit"), false},

        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "no-rollback", false, asList("start", "commit"), false},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "rollback", false, asList("start", "rollback"), false},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "rollback-with-error-handler-reference-at-inner-transaction-location", true, asList("start", "rollback"), false},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "rollback-in-flowref", true, asList("start", "rollback"), false},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "no-rollback-outside-try", true, asList("start", "commit"), false},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "no-rollback-flowref", false, asList("start", "continue", "commit"), false},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "no-rollback-error-in-flow-ref", false, asList("start", "commit"), false},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "rollback-error-in-flow-ref-with-try", true, asList("start", "continue", "rollback"), false},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "no-rollback-error-in-flow-ref-with-try", false, asList("start", "continue", "commit"), false},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "rollback-error-in-flow-ref-with-nested-try", false, asList("start", "continue", "rollback"), false},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "no-rollback-error-in-flow-ref-with-nested-try", false,
            asList("start", "continue", "continue", "continue", "commit"), false},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "no-rollback-error-in-flow-ref-with-try-join-tx", false,
            asList("start", "continue", "continue", "continue", "commit"), false},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "rollback-error-in-nested-try", true, asList("start", "continue", "continue", "rollback"), false},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "rollback-error-in-nested-try-with-same-error-handler", true, asList("start", "continue", "continue", "rollback"),
            false},
        // The `start` state is omitted from the expected states since given the test source is a polling source, it starts a
        // transaction every time it polls, so it might have already started a transaction right before this specific test case is
        // run and the first state registered by the profiling data consumer would be a `continue` in that case. Anyway, the goal
        // of the test is to check that the state sequence shows a rollback after the `continue` states.
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-source-owner-global-err.xml",
            "rollback-error-in-nested-flow", true, asList("continue", "continue", "rollback"), true},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "rollback-error-in-flow-ref-with-try-3-levels", true,
            asList("start", "continue", "continue", "continue", "rollback"), false},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "rollback-error-start-tx-in-flow-ref-with-try-3-levels", true,
            asList("start", "continue", "rollback"), false},

        new Object[] {"Default Error Handler", "org/mule/test/integration/transaction/transaction-owner-default-err.xml",
            "rollback", true, asList("start", "rollback"), false},
        new Object[] {"Default Error Handler", "org/mule/test/integration/transaction/transaction-owner-default-err.xml",
            "rollback-with-error-handler-reference-at-inner-transaction-location", true, asList("start", "rollback"), false},
        new Object[] {"Default Error Handler", "org/mule/test/integration/transaction/transaction-owner-default-err.xml",
            "rollback-in-flowref", true, asList("start", "rollback"), false},
        new Object[] {"Default Error Handler", "org/mule/test/integration/transaction/transaction-owner-default-err.xml",
            "no-rollback-outside-try", true, asList("start", "commit"), false},
        new Object[] {"Default Error Handler", "org/mule/test/integration/transaction/transaction-owner-default-err.xml",
            "rollback-error-in-flow-ref-with-try", true, asList("start", "continue", "rollback"), false},
        new Object[] {"Default Error Handler", "org/mule/test/integration/transaction/transaction-owner-default-err.xml",
            "rollback-error-in-flow-ref-with-nested-try", false, asList("start", "continue", "rollback"), false},
        new Object[] {"Default Error Handler", "org/mule/test/integration/transaction/transaction-owner-default-err.xml",
            "no-rollback-error-in-flow-ref-with-nested-try", false, asList("start", "continue", "continue", "continue", "commit"),
            false},
        new Object[] {"Default Error Handler", "org/mule/test/integration/transaction/transaction-owner-default-err.xml",
            "rollback-error-in-nested-try", true, asList("start", "continue", "continue", "rollback"), false},
        new Object[] {"Default Error Handler", "org/mule/test/integration/transaction/transaction-owner-default-err.xml",
            "rollback-error-in-nested-try-with-same-error-handler", true, asList("start", "continue", "continue", "rollback"),
            false},
        new Object[] {"Default Error Handler", "org/mule/test/integration/transaction/transaction-owner-default-err.xml",
            "rollback-error-in-flow-ref-with-try-3-levels", true,
            asList("start", "continue", "continue", "continue", "rollback"), false},
        new Object[] {"Default Error Handler", "org/mule/test/integration/transaction/transaction-owner-default-err.xml",
            "rollback-error-start-tx-in-flow-ref-with-try-3-levels", true,
            asList("start", "continue", "rollback"), false},
    };
  }

  public TransactionRolledBackByOwnerTestCase(String type, String config, String flowName, boolean throwsMessagingException,
                                              List<String> expectedStates, boolean ignoreExtraStates) {
    this.flowName = flowName;
    this.expectedStates = expectedStates;
    this.throwsMessagingException = throwsMessagingException;
    this.config = config;
    this.ignoreExtraStates = ignoreExtraStates;
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

    System.out.println("PENDING EVENTS " + queueManager.countPendingEvents("globalPropagateQueue"));
    System.out.println("QUEUE ELEMENT 1 " + queueManager.read("globalPropagateQueue", RECEIVE_TIMEOUT, MILLISECONDS));
    System.out.println("QUEUE ELEMENT 2 " + queueManager.read("globalPropagateQueue", RECEIVE_TIMEOUT, MILLISECONDS));
    System.out.println("QUEUE ELEMENT 3 " + queueManager.read("globalPropagateQueue", RECEIVE_TIMEOUT, MILLISECONDS));
    System.out.println("QUEUE ELEMENT 4 " + queueManager.read("globalPropagateQueue", RECEIVE_TIMEOUT, MILLISECONDS));
    System.out.println("QUEUE ELEMENT 5 " + queueManager.read("globalPropagateQueue", RECEIVE_TIMEOUT, MILLISECONDS));
    System.out.println("QUEUE ELEMENT 6 " + queueManager.read("globalPropagateQueue", RECEIVE_TIMEOUT, MILLISECONDS));
    System.out.println("QUEUE ELEMENT 7 " + queueManager.read("globalPropagateQueue", RECEIVE_TIMEOUT, MILLISECONDS));
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
    String reason = "Expected " + expectedStates + " but obtained " + states;
    if (ignoreExtraStates) {
      assertThat(reason, containsSequence(states, expectedStates), is(true));
    } else {
      assertThat(reason, states, contains(expectedStates.toArray()));
    }
  }

  private boolean containsSequence(List<String> obtainedStates, List<String> expectedStates) {
    for (int i = 0; i <= obtainedStates.size() - expectedStates.size(); i++) {
      if (obtainedStates.subList(i, i + expectedStates.size()).equals(expectedStates)) {
        return true;
      }
    }
    return false;
  }

}
