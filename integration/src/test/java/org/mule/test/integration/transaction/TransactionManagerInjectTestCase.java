/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transaction;

import static org.apache.commons.lang3.JavaVersion.JAVA_11;
import static org.apache.commons.lang3.SystemUtils.isJavaVersionAtMost;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;

import org.mule.test.AbstractIntegrationTestCase;

import javax.inject.Inject;
import javax.transaction.TransactionManager;

import org.junit.BeforeClass;
import org.junit.Test;

public class TransactionManagerInjectTestCase extends AbstractIntegrationTestCase {

  // TODO W-14338813
  @BeforeClass
  public static void ignoreJava17() {
    assumeTrue(isJavaVersionAtMost(JAVA_11));
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/transaction/transaction-manager-inject.xml";
  }

  @Test
  public void injectTransactionManager() {
    TransactionClient txClient = registry.<TransactionClient>lookupByName("txClient").get();
    assertThat(txClient.getTxMgr(), not(nullValue()));
  }

  public static class TransactionClient {

    private TransactionManager txMgr;

    public TransactionManager getTxMgr() {
      return txMgr;
    }

    @Inject
    public void setTxMgr(TransactionManager txMgr) {
      this.txMgr = txMgr;
    }
  }
}
