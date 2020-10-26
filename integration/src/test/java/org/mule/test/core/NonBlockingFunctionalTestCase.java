/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core;

import static java.util.Arrays.asList;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mule.tck.processor.FlowAssert.verify;

import org.mule.runtime.api.security.SecurityContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.security.AbstractAuthenticationFilter;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunnerDelegateTo(Parameterized.class)
public class NonBlockingFunctionalTestCase extends AbstractIntegrationTestCase {

  @Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {"Local Error Handler with Default PS", "non-blocking-test-config.xml", DEFAULT_PROCESSING_STRATEGY_CLASSNAME},
        {"Global Error Handler with Default PS", "non-blocking-test-config-global-err.xml",
            DEFAULT_PROCESSING_STRATEGY_CLASSNAME},
        {"Local Error Handler with Proactor PS", "non-blocking-test-config.xml", PROACTOR_PROCESSING_STRATEGY_CLASSNAME},
        {"Global Error Handler with Proactor PS", "non-blocking-test-config-global-err.xml",
            PROACTOR_PROCESSING_STRATEGY_CLASSNAME}
    });
  }

  private final String config;
  private final String processingStrategyFactory;

  public NonBlockingFunctionalTestCase(String type, String config, String processingStrategyFactory) {
    this.config = config;
    this.processingStrategyFactory = processingStrategyFactory;
  }

  @Override
  protected String getConfigFile() {
    return config;
  }

  @Override
  protected void doSetUpBeforeMuleContextCreation() {
    setDefaultProcessingStrategyFactory(PROACTOR_PROCESSING_STRATEGY_CLASSNAME);
  }

  @Override
  protected void doTearDownAfterMuleContextDispose() {
    clearDefaultProcessingStrategyFactory();
  }

  @Test
  public void flow() throws Exception {
    flowRunner("flow").withPayload(TEST_MESSAGE).run();
  }

  @Test
  public void subFlow() throws Exception {
    flowRunner("subFlow").withPayload(TEST_MESSAGE).run();
  }

  @Test
  public void childFlow() throws Exception {
    flowRunner("childFlow").withPayload(TEST_MESSAGE).run();
  }

  public void childAsyncFlow() throws Exception {
    flowRunner("childAsyncFlow").withPayload(TEST_MESSAGE).run();
    verify("childAsyncFlowChild");
  }

  @Test
  public void processorChain() throws Exception {
    flowRunner("processorChain").withPayload(TEST_MESSAGE).run();
  }

  @Test
  public void securityFilter() throws Exception {
    flowRunner("security-filter").withPayload(TEST_MESSAGE).run();
  }

  @Test
  public void choice() throws Exception {
    flowRunner("choice").withPayload(TEST_MESSAGE).run();
  }

  @Test
  public void async() throws Exception {
    flowRunner("async").withPayload(TEST_MESSAGE).run();
  }

  @Test
  public void catchExceptionStrategy() throws Exception {
    flowRunner("catchExceptionStrategy").withPayload(TEST_MESSAGE).run();
    verify("catchExceptionStrategyChild");
  }

  @Test
  public void rollbackExceptionStrategy() throws Exception {
    flowRunner("rollbackExceptionStrategy").withPayload(TEST_MESSAGE).run();
    verify("rollbackExceptionStrategyChild");
  }

  @Test
  public void nonTransactionalTry() throws Exception {
    flowRunner("nonTransactionalTry").withPayload(TEST_MESSAGE).run();
  }

  @Test
  public void nonTransactionalTryErrorHandler() throws Exception {
    flowRunner("nonTransactionalTryErrorHandler").withPayload(TEST_MESSAGE).run();
  }

  @Test
  public void transactionalTry() throws Exception {
    String flowName = "transactionalTry";

    if (PROACTOR_PROCESSING_STRATEGY_CLASSNAME.equals(processingStrategyFactory)) {
      flowName += "Proactor";
    } else if (DEFAULT_PROCESSING_STRATEGY_CLASSNAME.equals(processingStrategyFactory)) {
      flowName += "Emitter";
    } else {
      fail("Unknown processingStrategyFactory " + processingStrategyFactory);
    }

    flowRunner(flowName).withPayload(TEST_MESSAGE).run();
  }

  @Test
  public void transactionalTryErrorHandler() throws Exception {
    flowRunner("transactionalTryErrorHandler").withPayload(TEST_MESSAGE).run();
  }

  @Test
  public void childDefaultFlow() throws Exception {
    flowRunner("childDefaultFlow").withPayload(TEST_MESSAGE).run();
    verify("childDefaultFlowChild");
  }

  @Test
  public void untilSuccessfulNoRetry() throws Exception {
    flowRunner("untilSuccessfulNoRetry").withPayload(TEST_MESSAGE).run();
  }

  @Test
  public void untilSuccessfulNoRetryNonBlockingAfterScope() throws Exception {
    flowRunner("untilSuccessfulNoRetryNonBlockingAfterScope").withPayload(TEST_MESSAGE).run();
  }

  @Test
  public void untilSuccessfulWithRetryExceptionBefore() throws Exception {
    flowRunner("untilSuccessfulWithRetryExceptionBefore").withPayload(TEST_MESSAGE).run();
  }

  @Test
  public void untilSuccessfulWithRetryExceptionAfter() throws Exception {
    flowRunner("untilSuccessfulWithRetryExceptionAfter").withPayload(TEST_MESSAGE).run();
  }

  @Test
  public void untilSuccessfulWithRetryNonBlockingAfterScope() throws Exception {
    flowRunner("untilSuccessfulWithRetryNonBlockingAfterScope").withPayload(TEST_MESSAGE).run();
  }

  @Test
  public void untilSuccessfulWithRetryTransactional() throws Exception {
    TransactionCoordination.getInstance().bindTransaction(mock(Transaction.class));
    flowRunner("untilSuccessfulWithRetryTransactional").withPayload(TEST_MESSAGE).run();
  }

  @Test
  public void foreach() throws Exception {
    flowRunner("foreach").withPayload(asList(new String[] {"1", "2", "3"}, new String[] {"a", "b", "c"})).run();
  }

  @Test
  public void untilSuccessful() throws Exception {
    flowRunner("untilSuccessful").withPayload(TEST_MESSAGE).run();
  }

  @Test
  public void forEachAndUntilSuccessful() throws Exception {
    flowRunner("foreach_and_untilSuccessful").withPayload(asList(new String[] {"1", "2", "3"}, new String[] {"a", "b", "c"}))
        .run();
  }

  @Test
  public void untilSuccessfulAndForeach() throws Exception {
    flowRunner("untilSuccessful_and_foreach").withPayload(TEST_MESSAGE).run();
  }

  @Test
  public void scatterGather() throws Exception {
    flowRunner("scatterGather").run();
  }

  @Test
  public void parallelForeach() throws Exception {
    flowRunner("parallelForeach").withPayload(asList(new String[] {"1", "2", "3"}, new String[] {"a", "b", "c"})).run();
  }

  public static class CustomSecurityFilter extends AbstractAuthenticationFilter {

    @Override
    public SecurityContext authenticate(CoreEvent event) throws SecurityException {
      return event.getSecurityContext();
    }
  }
}

