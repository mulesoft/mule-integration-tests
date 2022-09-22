/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static org.mule.test.allure.AllureConstants.TransactionFeature.TRANSACTION;
import static org.mule.test.allure.AllureConstants.TransactionFeature.TimeoutStory.TRANSACTION_TIMEOUT;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import io.qameta.allure.Description;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.test.AbstractIntegrationTestCase;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

@Feature(TRANSACTION)
@Story(TRANSACTION_TIMEOUT)
public class TransactionTimeoutTestCase extends AbstractIntegrationTestCase {

  private static AtomicInteger lastTimeout = new AtomicInteger(0);

  @Override
  protected String getConfigFile() {
    return "try-timeout.xml";
  }

  @Test
  @Issue("W-11741912")
  @Description("Checks that the timeout of the transaction is the one defined by the default of the mule configuration")
  public void timeoutIsCorrect() throws Exception {
    flowRunner("withinTransaction").run();
    assertThat(lastTimeout.get(), is(30000));
  }

  public static class ThreadCaptor extends AbstractComponent implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      if (TransactionCoordination.isTransactionActive()) {
        lastTimeout.set(TransactionCoordination.getInstance().getTransaction().getTimeout());
      }
      return event;
    }
  }

}
