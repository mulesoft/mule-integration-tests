/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static org.mule.runtime.core.api.transaction.TransactionCoordination.getInstance;
import static org.mule.runtime.core.api.transaction.TransactionCoordination.isTransactionActive;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ErrorHandlingStory.GLOBAL_ERROR_HANDLER;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.AbstractIntegrationTestCase;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

@Feature(ERROR_HANDLING)
@Story(GLOBAL_ERROR_HANDLER)
public class TransactionWithGlobalErrorHandlerTestCase extends AbstractIntegrationTestCase {

  private static final int EXECUTIONS = 10;
  private static final CountDownLatch latch = new CountDownLatch(EXECUTIONS);
  private static final List<Transaction> transactions = new CopyOnWriteArrayList<>();
  private static final int PROBER_POLLING_INTERVAL = 100;
  private static final int PROBER_POLLING_TIMEOUT = 2000;

  @Override
  protected String getConfigFile() {
    return "routers/transaction-global-eh.xml";
  }

  @Test
  public void commitsTransaction() throws Exception {
    for (int i = 0; i < EXECUTIONS; i++) {
      flowRunner("execute").run();
    }
    latch.await();

    // all the latches would have finished but the commit operation happens after the error handler finishes
    // to execute, so it may happen that even though the operators finished executing, the last one (or more) didn't
    // make it to commit the transaction
    new PollingProber(PROBER_POLLING_TIMEOUT, PROBER_POLLING_INTERVAL).check(new JUnitProbe() {

      @Override
      protected boolean test() throws Exception {
        assertThat(transactions, hasSize(EXECUTIONS));
        for (Transaction tx : transactions) {
          assertThat(tx.isCommitted(), is(true));
        }
        return true;
      }
    });
  }

  public static class Operation implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      if (isTransactionActive()) {
        transactions.add(getInstance().getTransaction());
      }
      latch.countDown();
      return event;
    }
  }

}
