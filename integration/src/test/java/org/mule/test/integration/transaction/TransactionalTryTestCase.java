/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transaction;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.processor.DelegateTransactionFactory;
import org.mule.runtime.core.processor.TryMessageProcessor;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class TransactionalTryTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "transactional-try-config.xml";
  }

  @Test
  public void resolvesStandardTransactionFactory() throws Exception {
    Processor processor = ((Flow) getFlowConstruct("standardTry")).getMessageProcessors().get(0);
    assertThat(processor,
               is(instanceOf(TryMessageProcessor.class)));

    assertThat(((TryMessageProcessor) processor).getTransactionConfig().getFactory(),
               is(instanceOf(DelegateTransactionFactory.class)));
  }

}
