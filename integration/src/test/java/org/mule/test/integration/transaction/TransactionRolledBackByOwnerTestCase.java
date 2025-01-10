/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transaction;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.junit.Assert.fail;

import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.TX_COMMIT;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.TX_CONTINUE;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.TX_ROLLBACK;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.TX_START;
import static org.mule.runtime.api.util.MuleSystemProperties.DEFAULT_ERROR_HANDLER_NOT_ROLLBACK_IF_NOT_CORRESPONDING_PROPERTY;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunnerDelegateTo(Parameterized.class)
public class TransactionRolledBackByOwnerTestCase extends AbstractIntegrationTestCase {

  private static final int POLL_DELAY_MILLIS = 100;
  private final FlowExecutions flowExecutions;

  @Inject
  private PrivilegedProfilingService service;

  private List<String> states;
  private final Object statesLock = new Object();
  private Map<String, List<String>> statesPerLocation;
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
            "no-rollback",
            new FlowExecutions("no-rollback", false, "commit", null)},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "rollback",
            new FlowExecutions("rollback", true, "rollback", null)},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "no-rollback-outside-try",
            new FlowExecutions("no-rollback-outside-try", true, "commit", null)},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "no-rollback-flowref",
            new FlowExecutions("no-rollback-flowref", false, "commit", null)},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "no-rollback-error-in-flow-ref",
            new FlowExecutions("no-rollback-error-in-flow-ref", false, "commit", null)},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "rollback-error-in-flow-ref-with-try",
            new FlowExecutions("rollback-error-in-flow-ref-with-try", true, "rollback", null)},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "no-rollback-error-in-flow-ref-with-try",
            new FlowExecutions("no-rollback-error-in-flow-ref-with-try", false, "commit", null)},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "no-rollback-error-in-flow-ref-with-try-join-tx",
            new FlowExecutions("no-rollback-error-in-flow-ref-with-try-join-tx", false, "commit", null)},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "with-implicit-default-EH-executed-commits",
            new FlowExecutions("with-implicit-default-EH-executed-commits", false, "commit", null)},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "with-implicit-default-EH-executed-rollback",
            new FlowExecutions("with-implicit-default-EH-executed-rollback", true, "rollback", null)},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "with-default-EH-executed-commits",
            new FlowExecutions("with-default-EH-executed-commits", false, "commit", null)},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "with-default-EH-executed-rollback",
            new FlowExecutions("with-default-EH-executed-rollback", false, "rollback", null)},

        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner-subflow.xml",
            "rollback-on-error-prop",
            new FlowExecutions("rollback-on-error-prop", false, "rollback", null)},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner-subflow.xml",
            "rollback-default-on-error-prop",
            new FlowExecutions("rollback-default-on-error-prop", true, "rollback", null)},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner-subflow.xml",
            "rollback-in-flow",
            new FlowExecutions("rollback-in-flow", true, "rollback", null)},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner-subflow.xml",
            "commit-flow-on-error-continue",
            new FlowExecutions("commit-flow-on-error-continue", false, "commit", null)},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner-subflow.xml",
            "rollback-nested-subflows",
            new FlowExecutions("rollback-nested-subflows", true, "rollback", null)},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner-subflow.xml",
            "commit-nested-subflows",
            new FlowExecutions("commit-nested-subflows", false, "commit", null)},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner-subflow.xml",
            "rollback-nested-flows",
            new FlowExecutions("rollback-nested-flows", true, "rollback", null)},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner-subflow.xml",
            "commit-nested-flows",
            new FlowExecutions("commit-nested-flows", false, "commit", null)},

        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "no-rollback",
            new FlowExecutions("no-rollback", false, "commit", null)},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "rollback",
            new FlowExecutions("rollback", false, "rollback", 1)},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "other-rollback",
            new FlowExecutions("other-rollback", true, "rollback", 1)},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "other-other-rollback",
            new FlowExecutions("other-other-rollback", true, "rollback", 2)},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "contained-flow-name",
            new FlowExecutions("contained-flow-name", true, "rollback", 2)},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "rollback-with-error-handler-reference-at-inner-transaction-location",
            new FlowExecutions("rollback-with-error-handler-reference-at-inner-transaction-location", true, "rollback", 1)},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "rollback-in-flowref",
            new FlowExecutions("rollback-in-flowref", true, "rollback", 1)},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "no-rollback-outside-try",
            new FlowExecutions("no-rollback-outside-try", true, "commit", null)},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "no-rollback-flowref",
            new FlowExecutions("no-rollback-flowref", false, "commit", null)},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "no-rollback-error-in-flowref",
            new FlowExecutions("no-rollback-error-in-flowref", false, "commit", null)},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "rollback-error-in-flowref-with-try",
            new FlowExecutions("rollback-error-in-flowref-with-try", true, "rollback", 3)},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "no-rollback-error-in-flowref-with-try",
            new FlowExecutions("no-rollback-error-in-flowref-with-try", false, "commit", null)},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "rollback-error-in-flowref-with-nested-try",
            new FlowExecutions("rollback-error-in-flowref-with-nested-try", false, "rollback", 3)},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "no-rollback-error-in-flowref-with-nested-try",
            new FlowExecutions("no-rollback-error-in-flowref-with-nested-try", false, "commit", null)},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "no-rollback-error-in-flowref-with-try-join-tx",
            new FlowExecutions("no-rollback-error-in-flowref-with-try-join-tx", false, "commit", null)},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "rollback-error-in-nested-try",
            new FlowExecutions("rollback-error-in-nested-try", true, "rollback", 1)},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "rollback-error-in-nested-try-with-same-error-handler",
            new FlowExecutions("rollback-error-in-nested-try-with-same-error-handler", true, "rollback", 2)},
        // Extra states might be received for this test since one of the flows involved has a polling source, which starts a
        // transaction every time it polls. The polling might happen before the flow that publishes content to the polling source
        // (the one starting the actual test) is run, and it continues after the test scenario is finished. Those extra states
        // have to be ignored.
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-source-owner-global-err.xml",
            "rollback-error-in-nested-flow",
            new FlowExecutions("rollback-error-in-nested-flow", true, "rollback", 2, true)},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "rollback-error-in-flowref-with-try-3-levels",
            new FlowExecutions("rollback-error-in-flowref-with-try-3-levels", true, "rollback", 5)},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "rollback-error-in-flowref-with-try-4-levels",
            new FlowExecutions("rollback-error-in-flowref-with-try-4-levels", true, "rollback", 7)},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "rollback-error-start-tx-in-flowref-with-try-3-levels",
            new FlowExecutions("rollback-error-start-tx-in-flowref-with-try-3-levels", true, "rollback", 3)},
        // Tests that the error handler count started by the first globalPropagate execution and left dangling by the
        // on-error-continue is correctly handled and doesn't affect the next execution of the flow, avoiding an unexpected
        // rollback. Both flow executions need to have the same transaction location and failing component to properly test
        // this scenario.
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "no-rollback-second-execution",
            new FlowExecutions(new FlowExecution("commit-or-rollback-after-error", "commit", false, "commit", null),
                               new FlowExecution("commit-or-rollback-after-error", "commit", false, "commit", null))},
        // Tests that the error handler count started by the first globalPropagate execution and left dangling by the
        // on-error-continue is correctly handled and doesn't affect the next execution of the flow, avoiding the rollback
        // to be done before than when it should. Both flow executions need to have the same transaction location and failing
        // component to properly test this scenario.
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "rollback-second-execution",
            new FlowExecutions(new FlowExecution("commit-or-rollback-after-error", "commit", false, "commit", null),
                               new FlowExecution("commit-or-rollback-after-error", true, "rollback", 4))},

        new Object[] {"Default Error Handler", "org/mule/test/integration/transaction/transaction-owner-default-err.xml",
            "rollback",
            new FlowExecutions("rollback", true, "rollback", 1)},
        new Object[] {"Default Error Handler", "org/mule/test/integration/transaction/transaction-owner-default-err.xml",
            "other-rollback",
            new FlowExecutions("other-rollback", true, "rollback", 1)},
        new Object[] {"Default Error Handler", "org/mule/test/integration/transaction/transaction-owner-default-err.xml",
            "other-other-rollback",
            new FlowExecutions("other-other-rollback", true, "rollback", 3)},
        new Object[] {"Default Error Handler", "org/mule/test/integration/transaction/transaction-owner-default-err.xml",
            "contained-flow-name",
            new FlowExecutions("contained-flow-name", true, "rollback", 3)},
        new Object[] {"Default Error Handler", "org/mule/test/integration/transaction/transaction-owner-default-err.xml",
            "rollback-with-error-handler-reference-at-inner-transaction-location",
            new FlowExecutions("rollback-with-error-handler-reference-at-inner-transaction-location", true, "rollback", 1)},
        new Object[] {"Default Error Handler", "org/mule/test/integration/transaction/transaction-owner-default-err.xml",
            "rollback-in-flowref",
            new FlowExecutions("rollback-in-flowref", true, "rollback", 1)},
        new Object[] {"Default Error Handler", "org/mule/test/integration/transaction/transaction-owner-default-err.xml",
            "no-rollback-outside-try",
            new FlowExecutions("no-rollback-outside-try", true, "commit", null)},
        new Object[] {"Default Error Handler", "org/mule/test/integration/transaction/transaction-owner-default-err.xml",
            "rollback-error-in-flowref-with-try",
            new FlowExecutions("rollback-error-in-flowref-with-try", true, "rollback", 2)},
        new Object[] {"Default Error Handler", "org/mule/test/integration/transaction/transaction-owner-default-err.xml",
            "rollback-error-in-flowref-with-nested-try",
            new FlowExecutions("rollback-error-in-flowref-with-nested-try", false, "rollback", 2)},
        new Object[] {"Default Error Handler", "org/mule/test/integration/transaction/transaction-owner-default-err.xml",
            "no-rollback-error-in-flowref-with-nested-try",
            new FlowExecutions("no-rollback-error-in-flowref-with-nested-try", false, "commit", null)},
        new Object[] {"Default Error Handler", "org/mule/test/integration/transaction/transaction-owner-default-err.xml",
            "rollback-error-in-nested-try",
            new FlowExecutions("rollback-error-in-nested-try", true, "rollback", 1)},
        new Object[] {"Default Error Handler", "org/mule/test/integration/transaction/transaction-owner-default-err.xml",
            "rollback-error-in-nested-try-with-same-error-handler",
            new FlowExecutions("rollback-error-in-nested-try-with-same-error-handler", true, "rollback", 2)},
        new Object[] {"Default Error Handler", "org/mule/test/integration/transaction/transaction-owner-default-err.xml",
            "rollback-error-in-flowref-with-try-3-levels",
            new FlowExecutions("rollback-error-in-flowref-with-try-3-levels", true, "rollback", 4)},
        new Object[] {"Default Error Handler", "org/mule/test/integration/transaction/transaction-owner-default-err.xml",
            "rollback-error-in-flowref-with-try-4-levels",
            new FlowExecutions("rollback-error-in-flowref-with-try-4-levels", true, "rollback", 6)},
        new Object[] {"Default Error Handler", "org/mule/test/integration/transaction/transaction-owner-default-err.xml",
            "rollback-error-start-tx-in-flowref-with-try-3-levels",
            new FlowExecutions("rollback-error-start-tx-in-flowref-with-try-3-levels", true, "rollback", 2)},
        // Tests that the error handler count started by the first globalPropagate execution and left dangling by the
        // on-error-continue is correctly handled and doesn't affect the next execution of the flow, avoiding an unexpected
        // rollback. Both flow executions need to have the same transaction location and failing component to properly test
        // this scenario.
        new Object[] {"Default Error Handler", "org/mule/test/integration/transaction/transaction-owner-default-err.xml",
            "no-rollback-second-execution",
            new FlowExecutions(new FlowExecution("commit-or-rollback-after-error", "commit", false, "commit", null),
                               new FlowExecution("commit-or-rollback-after-error", "commit", false, "commit", null))},
        // Tests that the error handler count started by the first globalPropagate execution and left dangling by the
        // on-error-continue is correctly handled and doesn't affect the next execution of the flow, avoiding the rollback
        // to be done before than when it should. Both flow executions need to have the same transaction location and failing
        // component to properly test this scenario.
        new Object[] {"Default Error Handler", "org/mule/test/integration/transaction/transaction-owner-default-err.xml",
            "rollback-second-execution",
            new FlowExecutions(new FlowExecution("commit-or-rollback-after-error", "commit", false, "commit", null),
                               new FlowExecution("commit-or-rollback-after-error", true, "rollback", 4))},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "rollbackIfErrorDuringContinue",
            new FlowExecutions(new FlowExecution("rollbackIfErrorDuringContinue", true, "rollback", null))},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "rollbackIfErrorDuringContinueGlobalEH",
            new FlowExecutions(new FlowExecution("rollbackIfErrorDuringContinueGlobalEH", true, "rollback", null))}
    };
  }

  public TransactionRolledBackByOwnerTestCase(String type, String config, String testFlow, FlowExecutions flowExecutions) {
    this.config = config;
    this.flowExecutions = flowExecutions;
  }

  @Override
  protected String getConfigFile() {
    return config;
  }

  @Override
  protected void doSetUp() throws Exception {
    cleanStates();
    service.registerProfilingDataConsumer(new ProfilingDataConsumer<TransactionProfilingEventContext>() {

      @Override
      public void onProfilingEvent(ProfilingEventType<TransactionProfilingEventContext> profilingEventType,
                                   TransactionProfilingEventContext profilingEventContext) {
        synchronized (statesLock) {
          states.add(profilingEventType.toString());
          String originatingLocation = profilingEventContext.getEventOrginatingLocation().getLocation();
          statesPerLocation.computeIfAbsent(originatingLocation, k -> new ArrayList<>());
          statesPerLocation.get(originatingLocation).add(profilingEventType.toString());
        }
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

  private void cleanStates() {
    synchronized (statesLock) {
      states = new ArrayList<>();
      statesPerLocation = new HashMap<>();
    }
  }

  @Test
  public void checkRollback() {
    flowExecutions.forEach(flowExecution -> {
      cleanStates();
      try {
        flowRunner(flowExecution.flowName).withPayload(flowExecution.payload).run();
        if (flowExecution.throwsMessagingException) {
          fail("Should have thrown Exception from unhandled error");
        }
      } catch (Exception e) {
        if (!flowExecution.throwsMessagingException) {
          fail("Should have not thrown Exception from handled error: " + e);
        }
      }

      synchronized (statesLock) {
        assertStatesArrived();
        assertCorrectStates(flowExecution);
        if (flowExecution.globalHandlerExecutionsBeforeRollback != null) {
          assertTransactionRolledBackAtTheRightHandler(flowExecution);
        }
      }
    });
  }

  private void assertStatesArrived() {
    PollingProber pollingProber = new PollingProber(RECEIVE_TIMEOUT, POLL_DELAY_MILLIS);
    pollingProber.check(new JUnitProbe() {

      @Override
      protected boolean test() {
        assertThat(states.size(), greaterThanOrEqualTo(2));
        return true;
      }

      @Override
      public String describeFailure() {
        return "States did not arrive";
      }
    });
  }

  private void assertCorrectStates(FlowExecution flowExecution) {
    if (flowExecution.ignoreExtraStates) {
      List<String> filteredStates =
          states.stream().filter(state -> state.equals(flowExecution.expectedFinalState)).collect(toList());
      assertThat("Expected final state " + flowExecution.expectedFinalState + " to be received only once: " + states,
                 filteredStates, iterableWithSize(1));
    } else {
      assertThat("Expected final state from " + states + " to be " + flowExecution.expectedFinalState,
                 states.get(states.size() - 1),
                 is(flowExecution.expectedFinalState));
    }
  }

  private void assertTransactionRolledBackAtTheRightHandler(FlowExecution flowExecution) {
    List<String> states = statesPerLocation.get("globalPropagate/0");
    String reason = "Expected 'globalPropagate' handler to be executed " + flowExecution.globalHandlerExecutionsBeforeRollback
        + " times, but it got executed with states %s";
    if (flowExecution.ignoreExtraStates) {
      // We are only interested in the calls to the handler made up to the final state
      states = states.subList(0, states.indexOf(flowExecution.expectedFinalState));
      assertThat(format(reason, states), states, iterableWithSize(1));
    } else {
      assertThat(format(reason, states), states, iterableWithSize(flowExecution.globalHandlerExecutionsBeforeRollback));
    }
  }

  private static class FlowExecutions {

    final List<FlowExecution> flowExecutions;

    public FlowExecutions(String flowName, boolean throwsMessagingException, String expectedFinalState,
                          Integer globalHandlerExecutionsBeforeRollback) {
      this(flowName, throwsMessagingException, expectedFinalState, globalHandlerExecutionsBeforeRollback, false);
    }

    public FlowExecutions(String flowName, boolean throwsMessagingException, String expectedFinalState,
                          Integer globalHandlerExecutionsBeforeRollback, boolean ignoreExtraStates) {
      flowExecutions = singletonList(new FlowExecution(flowName, throwsMessagingException, expectedFinalState,
                                                       globalHandlerExecutionsBeforeRollback, ignoreExtraStates));
    }

    public FlowExecutions(FlowExecution... flowExecutions) {
      this.flowExecutions = asList(flowExecutions);
    }

    public void forEach(Consumer<FlowExecution> action) {
      flowExecutions.forEach(action);
    }

  }

  private static class FlowExecution {

    final String flowName;
    final Object payload;
    final boolean throwsMessagingException;
    final String expectedFinalState;
    final Integer globalHandlerExecutionsBeforeRollback;
    final boolean ignoreExtraStates;

    public FlowExecution(String flowName, boolean throwsMessagingException, String expectedFinalState,
                         Integer globalHandlerExecutionsBeforeRollback) {
      this(flowName, throwsMessagingException, expectedFinalState, globalHandlerExecutionsBeforeRollback, false);
    }

    public FlowExecution(String flowName, boolean throwsMessagingException, String expectedFinalState,
                         Integer globalHandlerExecutionsBeforeRollback, boolean ignoreExtraStates) {
      this(flowName, "message", throwsMessagingException, expectedFinalState, globalHandlerExecutionsBeforeRollback,
           ignoreExtraStates);
    }

    public FlowExecution(String flowName, Object payload, boolean throwsMessagingException, String expectedFinalState,
                         Integer globalHandlerExecutionsBeforeRollback) {
      this(flowName, payload, throwsMessagingException, expectedFinalState, globalHandlerExecutionsBeforeRollback, false);
    }

    public FlowExecution(String flowName, Object payload, boolean throwsMessagingException, String expectedFinalState,
                         Integer globalHandlerExecutionsBeforeRollback, boolean ignoreExtraStates) {
      this.flowName = flowName;
      this.payload = payload;
      this.throwsMessagingException = throwsMessagingException;
      this.expectedFinalState = expectedFinalState;
      this.globalHandlerExecutionsBeforeRollback = globalHandlerExecutionsBeforeRollback;
      this.ignoreExtraStates = ignoreExtraStates;
    }
  }

}
