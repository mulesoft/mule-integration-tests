/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transaction;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.transaction.DelegateTransactionFactory;
import org.mule.runtime.core.api.transaction.MuleTransactionConfig;
import org.mule.test.AbstractIntegrationTestCase;

import java.lang.reflect.Method;

import org.junit.Test;

public class TransactionalTryTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "transactional-try-config.xml";
  }

  @Test
  public void resolvesStandardTransactionFactory() throws Exception {
    Processor processor = ((Flow) getFlowConstruct("standardTry")).getProcessors().get(0);
    assertThat(processor.getClass().getName(), equalTo("org.mule.runtime.core.internal.processor.TryScope"));

    Method getTransactionConfigMethod = processor.getClass().getMethod("getTransactionConfig");
    MuleTransactionConfig transactionConfig = (MuleTransactionConfig) getTransactionConfigMethod.invoke(processor);
    assertThat(transactionConfig.getFactory(), is(instanceOf(DelegateTransactionFactory.class)));
  }

}
