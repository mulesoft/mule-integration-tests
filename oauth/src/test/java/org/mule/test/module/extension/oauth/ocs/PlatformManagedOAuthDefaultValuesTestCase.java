/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.module.extension.oauth.ocs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.mule.test.oauth.TestOAuthConnection;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class PlatformManagedOAuthDefaultValuesTestCase extends PlatformManagedOAuthConfigurationTestCase {

  private static final boolean PROMT_DEFAULT_VALUE = true;
  private static final boolean IMMEDIATE_DEFAULT_VALUE = false;
  private static final Double API_VERSION_DEFAULT_VALUE = 34d;

  @Override
  protected Map<String, Object> getDescriptorParameters() {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(DISPLAY_PARAMETER_NAME, DISPLAY_TEST);

    return parameters;
  }

  @Test
  public void defaultParameterValuesAreHonored() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    assertThat(connection.getState().isPrompt(), is(PROMT_DEFAULT_VALUE));
    assertThat(connection.getState().isImmediate(), is(IMMEDIATE_DEFAULT_VALUE));
    assertThat(connection.getState().getApiVersion(), is(API_VERSION_DEFAULT_VALUE));
  }

}
