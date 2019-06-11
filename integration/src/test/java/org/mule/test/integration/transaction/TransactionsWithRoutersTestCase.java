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
    runsInSameThread("txParallelForeach");
  }

  @Test
  @Description("When running inside a tx, every route executes sequentially")
  public void scatterGatherRunsInSameThread() throws Exception {
    runsInSameThread("txScatterGather");
  }

  @Test
  @Description("When running inside a tx, every execution of until successful must be in the same thread")
  public void untilSucessfulRunsInSameThread() throws Exception {
    runsInSameThread("txUntilSuccessful", TX_MESSAGE, TX_MESSAGE, TX_MESSAGE, OTHER_TX_MESSAGE);
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
    runsInSameThread("flowRefWithTx", TX_MESSAGE, OTHER_TX_MESSAGE);
  }

  @Test
  @Description("When running inside tx and use flow-ref, the tx is propagated even in case of subflow")
  public void flowRefWithTxToSubFlow() throws Exception {
    runsInSameThread("flowRefToSubFlowWithTx", TX_MESSAGE, OTHER_TX_MESSAGE);
  }

  private void runsInSameThread(String flowName) throws Exception {
    runsInSameThread(flowName, "apple", "banana", "orange");
  }

  private void runsInSameThread(String flowName, String... expectedPayloads) throws Exception {
    flowRunner(flowName).run();
    assertThat(threads, hasSize(expectedPayloads.length));
    assertThat(runsInTx, everyItem(is(true)));
    // Since there is no concurrency, the element must match in exact order
    assertThat(payloads, contains(expectedPayloads));
    assertThat(threads, everyItem(is(threads.get(0))));
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
