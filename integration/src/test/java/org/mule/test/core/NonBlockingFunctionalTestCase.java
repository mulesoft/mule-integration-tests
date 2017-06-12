/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.functional.api.component.FlowAssert.verify;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.context.MuleContextBuilder;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.config.DefaultMuleConfiguration;
import org.mule.runtime.core.processor.strategy.TransactionAwareWorkQueueProcessingStrategyFactory;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class NonBlockingFunctionalTestCase extends AbstractIntegrationTestCase {

  public static String FOO = "foo";
  private ProcessingStrategyFactory processingStrategyFactory = new TransactionAwareWorkQueueProcessingStrategyFactory();

  @Override
  protected String getConfigFile() {
    return "non-blocking-test-config.xml";
  }

  @Override
  protected void configureMuleContext(MuleContextBuilder contextBuilder) {
    DefaultMuleConfiguration configuration = new DefaultMuleConfiguration();
    configuration.setDefaultProcessingStrategyFactory(processingStrategyFactory);
    contextBuilder.setMuleConfiguration(configuration);
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

  @Test
  public void childSyncFlow() throws Exception {
    flowRunner("childSyncFlow").withPayload(TEST_MESSAGE).run();
    verify("childSyncFlowChild");
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
  public void filterAccepts() throws Exception {
    flowRunner("filterAccepts").withPayload(TEST_MESSAGE).run();
  }

  @Test
  public void filterRejects() throws Exception {
    Event result = flowRunner("filterRejects").withPayload(TEST_MESSAGE).run();
    assertThat(result, is(nullValue()));
  }

  @Test
  public void filterAfterNonBlockingAccepts() throws Exception {
    flowRunner("filterAfterNonBlockingAccepts").withPayload(TEST_MESSAGE).run();
  }

  @Test
  public void filterAfterNonBlockingRejects() throws Exception {
    Event result = flowRunner("filterAfterNonBlockingRejects").withPayload(TEST_MESSAGE).run();
    assertThat(result, is(nullValue()));
  }

  @Test
  public void filterBeforeNonBlockingAccepts() throws Exception {
    flowRunner("filterAfterNonBlockingAccepts").withPayload(TEST_MESSAGE).run();
  }

  @Test
  public void filterBeforeNonBlockingRejects() throws Exception {
    Event result = flowRunner("filterAfterNonBlockingRejects").withPayload(TEST_MESSAGE).run();
    assertThat(result, is(nullValue()));
  }

  @Test
  public void filterAfterEnricherBeforeNonBlocking() throws Exception {
    Event result = flowRunner("filterAfterEnricherBeforeNonBlocking").withPayload(TEST_MESSAGE).run();
    assertThat(result, is(nullValue()));
  }

  @Test
  public void securityFilter() throws Exception {
    flowRunner("security-filter").withPayload(TEST_MESSAGE).run();
  }

  @Test
  public void transformer() throws Exception {
    flowRunner("transformer").withPayload(TEST_MESSAGE).run();
  }

  @Test
  public void choice() throws Exception {
    flowRunner("choice").withPayload(TEST_MESSAGE).run();
  }

  @Test
  public void enricher() throws Exception {
    flowRunner("enricher").withPayload(TEST_MESSAGE).run();
  }

  @Test
  public void response() throws Exception {
    flowRunner("response").withPayload(TEST_MESSAGE).run();
  }

  @Test
  public void responseWithNullEvent() throws Exception {
    Event result = flowRunner("responseWithNullEvent").withPayload(TEST_MESSAGE).run();
    assertThat(result, is(nullValue()));
  }

  @Test
  public void enricherIssue() throws Exception {
    Event result = flowRunner("enricherIssue").withPayload(TEST_MESSAGE).run();
    assertThat(result.getMessageAsString(muleContext), is(equalTo(TEST_MESSAGE)));
  }

  @Test
  public void enricherIssueNonBlocking() throws Exception {
    Event result = flowRunner("enricherIssueNonBlocking").withPayload(TEST_MESSAGE).run();
    assertThat(result.getMessageAsString(muleContext), is(equalTo(TEST_MESSAGE)));
  }

  @Test
  public void enricherFlowVar() throws Exception {
    Event result = flowRunner("enricherFlowVar").withPayload(TEST_MESSAGE).run();
    assertThat(result.getVariable(FOO).getValue(), is(equalTo(TEST_MESSAGE)));
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
  public void tansactionalTry() throws Exception {
    flowRunner("transactionalTry").withPayload(TEST_MESSAGE).run();
  }

  @Test
  public void transactionalTryErrorHandler() throws Exception {
    flowRunner("transactionalTryErrorHandler").withPayload(TEST_MESSAGE).run();
  }

  @Test
  public void wiretap() throws Exception {
    flowRunner("wiretap").withPayload(TEST_MESSAGE).run();
  }

  @Test
  public void childDefaultFlow() throws Exception {
    flowRunner("childDefaultFlow").withPayload(TEST_MESSAGE).run();
    verify("childDefaultFlowChild");
  }

}

