/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.module.extension.oauth.ocs;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mule.test.oauth.TestOAuthPooledProvider.BORROWED;
import static org.mule.test.oauth.TestOAuthPooledProvider.CALLBACK_ACTIONS;
import static org.mule.test.oauth.TestOAuthPooledProvider.RETURNED;

import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;

public class PlatformManagedOAuthPooledConnectionTestCase extends PlatformManagedOAuthConfigurationTestCase {

  private static int TIMES_TO_TEST_CALLBACKS = 10;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {"Pooled configuration", "ocs/pooled-platform-managed-config.xml", true}
    });
  }

  @Before
  public void resetConnectionCallbackActiones() {
    CALLBACK_ACTIONS.clear();
  }

  @Test
  public void accessTokenRetrieval() throws Exception {
    for (int i = 0; i < TIMES_TO_TEST_CALLBACKS; i++) {
      flowRunner("getConnection").run();
      assertThat(CALLBACK_ACTIONS.poll(), is(BORROWED));
      assertThat(CALLBACK_ACTIONS.poll(), is(RETURNED));
    }
  }

}
