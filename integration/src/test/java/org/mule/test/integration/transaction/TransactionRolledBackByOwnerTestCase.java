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
import static org.hamcrest.Matchers.iterableWithSize;
import static org.junit.Assert.fail;

import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.TX_COMMIT;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.TX_CONTINUE;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.TX_ROLLBACK;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.TX_START;
import static org.mule.runtime.api.util.MuleSystemProperties.DEFAULT_ERROR_HANDLER_NOT_ROLLBACK_IF_NOT_CORRESPONDING_PROPERTY;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

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

  @Inject
  private TestQueueManager queueManager;

  private List<String> states;
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
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-source-owner-global-err.xml",
            "rollback-error-in-nested-flow",
            new FlowExecutions("rollback-error-in-nested-flow", true, "rollback", 2)},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "rollback-error-in-flowref-with-try-3-levels",
            new FlowExecutions("rollback-error-in-flowref-with-try-3-levels", true, "rollback", 5)},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "rollback-error-in-flowref-with-try-4-levels",
            new FlowExecutions("rollback-error-in-flowref-with-try-4-levels", true, "rollback", 7)},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "rollback-error-start-tx-in-flowref-with-try-3-levels",
            new FlowExecutions("rollback-error-start-tx-in-flowref-with-try-3-levels", true, "rollback", 3)},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "no-rollback-after-continue",
            new FlowExecutions(new FlowExecution("no-rollback-after-continue", false, "commit", null),
                               new FlowExecution("no-rollback-after-continue", false, "commit", null))},

        new Object[] {"Default Error Handler", "org/mule/test/integration/transaction/transaction-owner-default-err.xml",
            "rollback",
            new FlowExecutions("rollback", true, "rollback", 1)},
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
    states = new ArrayList<>();
    statesPerLocation = new HashMap<>();
    service.registerProfilingDataConsumer(new ProfilingDataConsumer<TransactionProfilingEventContext>() {

      @Override
      public void onProfilingEvent(ProfilingEventType<TransactionProfilingEventContext> profilingEventType,
                                   TransactionProfilingEventContext profilingEventContext) {
        states.add(profilingEventType.toString());
        String originatingLocation = profilingEventContext.getEventOrginatingLocation().getLocation();
        statesPerLocation.computeIfAbsent(originatingLocation, k -> new ArrayList<>());
        statesPerLocation.get(originatingLocation).add(profilingEventType.toString());
        System.out
            .println("STATE " + profilingEventType + " " + originatingLocation);
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
    flowExecutions.forEach(flowExecution -> {
      try {
        flowRunner(flowExecution.flowName).withPayload("message").run();
        if (flowExecution.throwsMessagingException) {
          fail("Should have thrown Exception from unhandled error");
        }
      } catch (Exception e) {
        if (!flowExecution.throwsMessagingException) {
          fail("Should have not thrown Exception from handled error");
        }
      }

      assertStatesArrived();
      assertCorrectStates(flowExecution);
      if (flowExecution.globalHandlerExecutionsBeforeRollback != null) {
        assertTransactionRolledBackAtTheRightHandler(flowExecution.globalHandlerExecutionsBeforeRollback);
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
    assertThat("Expected final state from " + states + " to be " + flowExecution.expectedFinalState,
               states.get(states.size() - 1),
               is(flowExecution.expectedFinalState));
  }

  private void assertTransactionRolledBackAtTheRightHandler(Integer globalHandlerExecutionsBeforeRollback) {
    List<String> states = statesPerLocation.get("globalPropagate/0");
    String reason = "Expected 'globalPropagate' handler to be executed " + globalHandlerExecutionsBeforeRollback
        + " times, but it got executed with states " + states;
    assertThat(reason, states, iterableWithSize(globalHandlerExecutionsBeforeRollback));
  }

  private static class FlowExecutions {

    final List<FlowExecution> flowExecutions;

    public FlowExecutions(String flowName, boolean throwsMessagingException, String expectedFinalState,
                          Integer globalHandlerExecutionsBeforeRollback) {
      flowExecutions = singletonList(new FlowExecution(flowName, throwsMessagingException, expectedFinalState,
                                                       globalHandlerExecutionsBeforeRollback));
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
    final boolean throwsMessagingException;
    final String expectedFinalState;
    final Integer globalHandlerExecutionsBeforeRollback;

    public FlowExecution(String flowName, boolean throwsMessagingException, String expectedFinalState,
                         Integer globalHandlerExecutionsBeforeRollback) {
      this.flowName = flowName;
      this.throwsMessagingException = throwsMessagingException;
      this.expectedFinalState = expectedFinalState;
      this.globalHandlerExecutionsBeforeRollback = globalHandlerExecutionsBeforeRollback;
    }

  }

}
