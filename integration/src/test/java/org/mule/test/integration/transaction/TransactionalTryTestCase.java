/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transaction;

import static org.apache.commons.lang3.JavaVersion.JAVA_1_8;
import static org.apache.commons.lang3.SystemUtils.isJavaVersionAtMost;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

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
    // TODO (W-14226830): remove assumeThat
    assumeThat(isJavaVersionAtMost(JAVA_1_8), is(true));
    Processor processor = standardTryFlow.getProcessors().get(0);
    assertThat(processor.getClass().getName(), equalTo("org.mule.runtime.core.internal.processor.TryScope"));

    Method getTransactionConfigMethod = processor.getClass().getMethod("getTransactionConfig");
    MuleTransactionConfig transactionConfig = (MuleTransactionConfig) getTransactionConfigMethod.invoke(processor);
    assertThat(transactionConfig.getFactory(), is(instanceOf(DelegateTransactionFactory.class)));
    assertThat(transactionConfig.getTimeout(), is(30000));
  }

}
