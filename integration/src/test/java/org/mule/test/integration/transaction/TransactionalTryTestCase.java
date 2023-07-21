/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration.transaction;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.runners.Parameterized;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.transaction.DelegateTransactionFactory;
import org.mule.runtime.core.api.transaction.MuleTransactionConfig;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;
import org.mule.test.runner.RunnerDelegateTo;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;

@RunnerDelegateTo(Parameterized.class)
public class TransactionalTryTestCase extends AbstractIntegrationTestCase {

  @Inject
  @Named("standardTry")
  private Flow standardTryFlow;

  private String config;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
        {"Local Error Handler", "transactional-try-config.xml"},
        {"Global Error Handler", "transactional-try-config-global-err.xml"}
    });
  }


  public TransactionalTryTestCase(String type, String config) {
    this.config = config;
  }

  @Override
  protected String getConfigFile() {
    return config;
  }

  @Test
  public void resolvesStandardTransactionFactory() throws Exception {
    Processor processor = standardTryFlow.getProcessors().get(0);
    assertThat(processor.getClass().getName(), equalTo("org.mule.runtime.core.internal.processor.TryScope"));

    Method getTransactionConfigMethod = processor.getClass().getMethod("getTransactionConfig");
    MuleTransactionConfig transactionConfig = (MuleTransactionConfig) getTransactionConfigMethod.invoke(processor);
    assertThat(transactionConfig.getFactory(), is(instanceOf(DelegateTransactionFactory.class)));
    assertThat(transactionConfig.getTimeout(), is(30000));
  }

}
