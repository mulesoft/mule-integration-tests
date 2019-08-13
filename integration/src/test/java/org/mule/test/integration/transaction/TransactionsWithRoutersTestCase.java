/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transaction;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.Every.everyItem;
import static org.hamcrest.core.IsNot.not;
import static org.mule.runtime.core.api.transaction.TransactionCoordination.isTransactionActive;

import io.qameta.allure.Description;
import org.junit.Rule;
import org.junit.Test;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


public class TransactionsWithRoutersTestCase extends AbstractIntegrationTestCase {

  private static final String TX_MESSAGE = "Kangaroo";
  private static final String OTHER_TX_MESSAGE = "Uruguayan";
  private static final String CPU_LIGHT = "cpuLight";
  private static final String IO = "io";
  private static List<Thread> threads;
  private static List<String> payloads;
  private static List<Boolean> runsInTx;
  private static Latch latch;

  @Rule
  public SystemProperty message = new SystemProperty("firstValue", TX_MESSAGE);

  @Rule
  public SystemProperty otherMessage = new SystemProperty("otherValue", OTHER_TX_MESSAGE);

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    threads = new ArrayList<>();
    payloads = new ArrayList<>();
    latch = new Latch();
    runsInTx = new CopyOnWriteArrayList<>();
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/transaction/transaction-routers.xml";
  }

  @Test
  @Description("When running inside a tx, parallel foreach should work as common foreach")
  public void parallelForeachInSameThread() throws Exception {
    runsInSameTransaction("txParallelForeach", TX_MESSAGE, OTHER_TX_MESSAGE);
  }

  @Test
  public void parallelForeachInsideParallelForeachInSameThread() throws Exception {
    runsInSameTransaction("txParallelForeachInsideParallelForeach", TX_MESSAGE, OTHER_TX_MESSAGE, OTHER_TX_MESSAGE, TX_MESSAGE);
  }

  @Test
  @Description("Error handling of parallel foreach does not change even in context of transactions, where execution is sequential")
  public void parallelForEachHasSameErrorHandling() throws Exception {
    runsInSameTransactionWithErrors("txParallelForeachWithErrors");
  }

  @Test
  @Description("When running inside a tx, every route executes sequentially")
  public void scatterGatherRunsInSameThread() throws Exception {
    runsInSameTransaction("txScatterGather", TX_MESSAGE, OTHER_TX_MESSAGE);
  }

  @Test
  @Description("Error handling of scatter gather does not change even in context of transactions, where execution is sequential")
  public void scatterGatherHasSameErrorHandling() throws Exception {
    runsInSameTransactionWithErrors("txScatterGatherWithErrors");
  }

  @Test
  @Description("When running inside a tx, every execution of until successful must be in the same thread")
  public void untilSucessfulRunsInSameThread() throws Exception {
    runsInSameTransaction("txUntilSuccessful", TX_MESSAGE, TX_MESSAGE, TX_MESSAGE, OTHER_TX_MESSAGE);
  }

  @Test
  @Description("When running inside a tx, every execution of until successful must be in the same thread")
  public void untilSucessfulWithErrorHandlerWithRouterRunsInSameThread() throws Exception {
    runsInSameTransaction("txUntilSuccessfulOtherError", TX_MESSAGE, TX_MESSAGE, TX_MESSAGE, OTHER_TX_MESSAGE, TX_MESSAGE,
                          TX_MESSAGE,
                          TX_MESSAGE, OTHER_TX_MESSAGE);
  }

  @Test
  @Description("When running inside a tx, async runs in another thread and it's not part of the tx")
  public void asyncNotInTx() throws Exception {
    flowRunner("txAsync").run();
    latch.await();
    assertThat(runsInTx, containsInAnyOrder(true, false));
    assertThat(payloads, hasSize(1));
    assertThat(payloads.get(0), is(TX_MESSAGE));
  }

  @Test
  @Description("When running in non-tx and use flow-ref, even if the source of such flow begins a tx, the flow runs without tx")
  public void flowRefWithoutTx() throws Exception {
    flowRunner("flowRefWithoutTx").run();
    assertThat(threads, hasSize(2));
    assertThat(runsInTx, everyItem(is(false)));
    assertThat(payloads, contains(TX_MESSAGE, OTHER_TX_MESSAGE));
  }

  @Test
  @Description("When running inside tx and use flow-ref, the tx is propagated")
  public void flowRefWithTx() throws Exception {
    runsInSameTransaction("flowRefWithTx", TX_MESSAGE, OTHER_TX_MESSAGE);
  }

  @Test
  @Description("When running inside tx and use flow-ref, the tx is propagated even in case of subflow")
  public void flowRefWithTxToSubFlow() throws Exception {
    runsInSameTransaction("flowRefToSubFlowWithTx", TX_MESSAGE, OTHER_TX_MESSAGE);
  }

  @Test
  public void flowRefWithTxToFlowWithError() throws Exception {
    runsInSameTransaction("flowRefToFlowWithErrorTx", TX_MESSAGE, TX_MESSAGE, OTHER_TX_MESSAGE);
  }

  @Test
  public void flowReDynamicWithTxToFlowWithError() throws Exception {
    runsInSameTransaction("flowRefDynamicToFlowWithErrorTx", TX_MESSAGE, TX_MESSAGE, OTHER_TX_MESSAGE);
  }

  @Test
  @Description("When Flow that creates tx has flow-ref to flow that raises error and handles it with on-error-continue, then tx must go on in the first flow")
  public void flowRefToFlowWithErrorAndOnErrorContinue() throws Exception {
    runsInSameTransaction("flowRefToFlowWithErrorAndOnErrorContinue", TX_MESSAGE, TX_MESSAGE, OTHER_TX_MESSAGE);
  }

  @Test
  public void flowReDynamicToFlowWithTxAndErrorWithOnErrorPropagate() throws Exception {
    flowRunner("flowRefToTxFlowWithError").withVariable("errorType", "raise-propagate-error").run();
    assertThat(threads, hasSize(4));
    // when executing the on-error-propagate, tx has already been rolled back
    assertThat(runsInTx, contains(false, true, false, false));
    assertThat(payloads, contains(TX_MESSAGE, OTHER_TX_MESSAGE, OTHER_TX_MESSAGE, TX_MESSAGE));
  }

  @Test
  public void flowReDynamicToFlowWithTxAndErrorWithOnErrorContinue() throws Exception {
    flowRunner("flowRefToTxFlowWithError").withVariable("errorType", "raise-continue-error").run();
    assertThat(threads, hasSize(3));
    // when executing the on-error-continue, tx has not yet been committed (it still runs as part of the tx)
    assertThat(runsInTx, contains(false, true, true));
    assertThat(payloads, contains(TX_MESSAGE, OTHER_TX_MESSAGE, OTHER_TX_MESSAGE));
  }

  @Test
  public void flowRefWithTxToFlowWithErrorAndFlowRefInErrorHandler() throws Exception {
    runsInSameTransaction("flowRefToFlowWithErrorTxAndFlowRef", TX_MESSAGE, TX_MESSAGE, OTHER_TX_MESSAGE, TX_MESSAGE);
  }

  @Test
  public void nestedTries() throws Exception {
    runsInSameTransaction("nestedTries", TX_MESSAGE, TX_MESSAGE, TX_MESSAGE);
  }

  @Test
  public void nestedTriesContinuesTx() throws Exception {
    runsInSameTransaction("nestedTriesContinuesTx", TX_MESSAGE, OTHER_TX_MESSAGE);
  }

  @Test
  public void flowWithTxSourceWithTryContinuesTx() throws Exception {
    runsInSameTransactionAsync("toQueueFlowWithTxSourceWithTryContinuesTx", TX_MESSAGE, OTHER_TX_MESSAGE);
  }

  @Test
  public void nestedTriesWithIndifferentInTheMiddle() throws Exception {
    runsInSameTransaction("nestedTriesWithIndifferentInTheMiddle", TX_MESSAGE, TX_MESSAGE, TX_MESSAGE, OTHER_TX_MESSAGE);
  }

  @Test
  public void flowWithTxSourceAndFlowRef() throws Exception {
    runsInSameTransactionAsync("toQueue", OTHER_TX_MESSAGE, TX_MESSAGE, OTHER_TX_MESSAGE);
  }

  @Test
  public void flowRefToFlowWithErrorAndContinue() throws Exception {
    runsInSameTransaction("flowRefToFlowWithErrorAndContinue", TX_MESSAGE, TX_MESSAGE, OTHER_TX_MESSAGE, OTHER_TX_MESSAGE);
  }

  @Test
  public void flowRefToFlowWithErrorAndPropagate() throws Exception {
    // Since the on-error-propagate is not in the flow/try that created the tx, it should not rollback it. Thus, it should
    // still run in the same thread (and within a tx)
    runsInSameTransaction("flowRefToFlowWithErrorAndPropagate", TX_MESSAGE, TX_MESSAGE, OTHER_TX_MESSAGE, OTHER_TX_MESSAGE);
  }

  @Test
  public void nestedTriesWithOnErrorPropagates() throws Exception {
    runsInSameTransaction("nestedTriesWithOnErrorPropagate", TX_MESSAGE, TX_MESSAGE, OTHER_TX_MESSAGE, OTHER_TX_MESSAGE);
  }

  @Test
  public void innerTryWithOnErrorPropagate() throws Exception {
    runsInSameTransaction("tryWithInnerTryWithOnErrorPropagate", TX_MESSAGE, TX_MESSAGE, OTHER_TX_MESSAGE, OTHER_TX_MESSAGE);
  }

  @Test
  public void nestedTriesWithOnErrorContinue() throws Exception {
    runsInSameTransaction("nestedTriesWithOnErrorContinue", TX_MESSAGE, TX_MESSAGE, OTHER_TX_MESSAGE, OTHER_TX_MESSAGE);
  }

  @Test
  public void onErrorPropagateRaisesError() throws Exception {
    runsInSameTransaction("onErrorPropagateRaisesError", TX_MESSAGE, TX_MESSAGE, OTHER_TX_MESSAGE, OTHER_TX_MESSAGE);
  }

  @Test
  public void onErrorContinueRaisesError() throws Exception {
    runsInSameTransaction("onErrorContinueRaisesError", TX_MESSAGE, TX_MESSAGE, OTHER_TX_MESSAGE, OTHER_TX_MESSAGE);
  }

  @Test
  public void onErrorContinueAndPropagateRaiseError() throws Exception {
    runsInSameTransaction("onErrorContinueAndPropagateRaiseError", TX_MESSAGE, TX_MESSAGE, OTHER_TX_MESSAGE, OTHER_TX_MESSAGE,
                          OTHER_TX_MESSAGE);
  }

  @Test
  public void nestedTryRollbacksTxInInnerTry() throws Exception {
    flowRunner("nestedTryRollbacksTxInInnerTry").run();
    assertThat(runsInTx, contains(false, true, true, false));
  }

  @Test
  public void tryWithinTryDoesNotFinishTx() throws Exception {
    runsInSameTransaction("tryWithinTryDoesNotFinishTx", TX_MESSAGE, OTHER_TX_MESSAGE, OTHER_TX_MESSAGE);
  }

  @Test
  public void flowRefToFlowWithErrorPropagateWithError() throws Exception {
    runsInSameTransaction("flowRefToFlowWithErrorPropagateWithError", TX_MESSAGE, OTHER_TX_MESSAGE, OTHER_TX_MESSAGE,
                          OTHER_TX_MESSAGE);
  }

  @Test
  public void tryRunsInSameThreadAsBeforeExecuting() throws Exception {
    flowRunner("tryRunsInSameThreadAsBeforeExecuting").run();
    assertThat(threads.get(1), is(threads.get(0)));
    assertThreadType(threads.get(0), CPU_LIGHT);
  }

  @Test
  public void tryWithAlwaysBegin() throws Exception {
    flowRunner("tryWithAlwaysBegin").run();
    assertThat(threads.get(1), not(threads.get(0)));
    assertThreadType(threads.get(0), CPU_LIGHT);
    assertThreadType(threads.get(1), IO);
  }

  @Test
  public void tryWithBeginOrJoin() throws Exception {
    flowRunner("tryWithBeginOrJoin").run();
    assertThat(threads.get(1), not(threads.get(0)));
    assertThreadType(threads.get(0), CPU_LIGHT);
    assertThreadType(threads.get(1), IO);
  }

  @Test
  public void tryWithBeginOrJoinNestedIndifferent() throws Exception {
    flowRunner("tryWithBeginOrJoinNestedIndifferent").run();
    assertThat(threads.get(1), not(threads.get(0)));
    assertThat(threads.get(2), is(threads.get(1)));
    assertThreadType(threads.get(0), CPU_LIGHT);
    assertThreadType(threads.get(1), IO);
  }

  @Test
  public void tryWithBeginOrJoinNestedBeginOrJoin() throws Exception {
    flowRunner("tryWithBeginOrJoinNestedBeginOrJoin").run();
    assertThat(threads.get(1), not(threads.get(0)));
    assertThat(threads.get(2), is(threads.get(1)));
    assertThreadType(threads.get(0), CPU_LIGHT);
    assertThreadType(threads.get(1), IO);
  }

  private void runsInSameTransaction(String flowName, String... expectedPayloads) throws Exception {
    runsInSameTransaction(flowName, false, expectedPayloads);
  }

  private void runsInSameTransactionAsync(String flowName, String... expectedPayloads) throws Exception {
    runsInSameTransaction(flowName, true, expectedPayloads);
  }

  private void runsInSameTransaction(String flowName, boolean async, String... expectedPayloads) throws Exception {
    // Checks that all the points captured by the ThreadCaptor are processing within the Transaction, the threads
    // are (as should be) the same, and the payloads are the expected ones
    flowRunner(flowName).run();
    if (async) {
      latch.await();
    }
    assertThat(threads, hasSize(expectedPayloads.length));
    assertThat(threads, everyItem(is(threads.get(0))));
    assertThat(runsInTx, everyItem(is(true)));
    // Since there is no concurrency, the element must match in exact order
    assertThat(payloads, contains(expectedPayloads));
  }

  private void runsInSameTransactionWithErrors(String flow) throws Exception {
    runsInSameTransaction(flow, TX_MESSAGE, OTHER_TX_MESSAGE, "Error with " + TX_MESSAGE, OTHER_TX_MESSAGE);
  }

  private void assertThreadType(Thread thread, String type) {
    assertThat(thread.getName().matches("^\\[MuleRuntime\\]\\." + type + ".*"), is(true));
  }

  public static class ThreadCaptor implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      threads.add(Thread.currentThread());
      payloads.add(event.getMessage().getPayload().getValue().toString());
      runsInTx.add(isTransactionActive());
      return event;
    }

  }

  public static class ThreadCaptorAsync implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      runsInTx.add(isTransactionActive());
      latch.release();
      return event;
    }

  }

}
