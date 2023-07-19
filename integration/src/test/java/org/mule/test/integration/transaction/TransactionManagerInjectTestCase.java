/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration.transaction;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

import javax.inject.Inject;
import javax.transaction.TransactionManager;

public class TransactionManagerInjectTestCase extends AbstractIntegrationTestCase {

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
