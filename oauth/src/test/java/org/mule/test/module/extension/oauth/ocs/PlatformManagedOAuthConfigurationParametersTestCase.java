/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.oauth.ocs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mule.test.oauth.ConnectionType.DUO;
import static org.mule.test.oauth.ConnectionType.HYPER;

import org.mule.test.oauth.ConnectionProperties;
import org.mule.test.oauth.ConnectionType;
import org.mule.test.oauth.TestOAuthConnection;
import org.mule.test.oauth.TestOAuthConnectionState;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class PlatformManagedOAuthConfigurationParametersTestCase extends PlatformManagedOAuthConfigurationTestCase {

  private static final String SECURITY_LEVEL_PARAMETER_NAME = "securityLevel";
  private static final String SECURITY_LEVEL_PARAMETER_VALUE = "100";
  private static final Integer SECURITY_LEVEL_PARAMETER_INT_VALUE = 100;
  private static final String COMPLEX_PARAMETER_NAME = "connectionProperties";
  private static final String COMPLEX_PARAMETER_NAME_IN_SHOWINDSL_PARAMETER_GROUP = "profileConnectionProperties";
  private static final String COMPLEX_PARAMETER_NAME_IN_NON_DEFAULT_PARAMETER_GROUP = "anotherConnectionProperties";
  private static final String CONNECTION_DESCRIPTION_FIELD_NAME = "connectionDescription";
  private static final String CONNECTION_DESCRIPTION_FIELD_VALUE = "This is the connection description";
  private static final String CONNECTION_TYPE_FIELD_NAME = "connectionType";
  private static final String CONNECTION_TYPE_FIELD_VALUE = "DUO";
  private static final String PROFILE_LEVEL_PARAMETER_NAME = "profileLevel";
  private static final String PROFILE_LEVEL_PARAMETER_VALUE = "25";
  private static final String DETAILS_PRIORITY_PARAMETER_NAME = "detailsPriority";
  private static final String DETAILS_PRIORITY_PARAMETER_VALUE = "55";
  private static final String OAUTH_CONNECTION_TYPE_PARAMETER_NAME = "oauthConnectionType";
  private static final String OAUTH_CONNECTION_TYPE_PARAMETER_VALUE = "HYPER";
  private static final ConnectionType OAUTH_CONNECTION_TYPE_PARAMETER_ENUM_VALUE = HYPER;
  private static final ConnectionType CONNECTION_TYPE_FIELD_ENUM_VALUE = DUO;
  private static final ConnectionProperties CONNECTION_PROPERTIES =
      new ConnectionProperties(CONNECTION_DESCRIPTION_FIELD_VALUE, CONNECTION_TYPE_FIELD_ENUM_VALUE);

  @Override
  protected Map<String, Object> getDescriptorParameters() {
    Map<String, Object> descriptorParameters = super.getDescriptorParameters();
    Map<String, String> complexParameterMap = new HashMap<>();
    complexParameterMap.put(CONNECTION_DESCRIPTION_FIELD_NAME, CONNECTION_DESCRIPTION_FIELD_VALUE);
    complexParameterMap.put(CONNECTION_TYPE_FIELD_NAME, CONNECTION_TYPE_FIELD_VALUE);
    // descriptorParameters.put(COMPLEX_PARAMETER_NAME, complexParameterMap);
    // descriptorParameters.put(COMPLEX_PARAMETER_NAME_IN_SHOWINDSL_PARAMETER_GROUP, complexParameterMap);
    // descriptorParameters.put(COMPLEX_PARAMETER_NAME_IN_NON_DEFAULT_PARAMETER_GROUP, complexParameterMap);
    // descriptorParameters.put(PROFILE_LEVEL_PARAMETER_NAME, PROFILE_LEVEL_PARAMETER_VALUE);
    // descriptorParameters.put(DETAILS_PRIORITY_PARAMETER_NAME, DETAILS_PRIORITY_PARAMETER_VALUE);
    // descriptorParameters.put(SECURITY_LEVEL_PARAMETER_NAME, SECURITY_LEVEL_PARAMETER_VALUE);
    // descriptorParameters.put(OAUTH_CONNECTION_TYPE_PARAMETER_NAME, OAUTH_CONNECTION_TYPE_PARAMETER_VALUE);
    return descriptorParameters;
  }

  @Test
  public void complexParameter() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    TestOAuthConnectionState connectionState = connection.getState();
    assertThat(connectionState.getConnectionProperties(), equalTo(CONNECTION_PROPERTIES));
  }

  @Test
  public void complexParameterInShowInDslParameterGroup() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    TestOAuthConnectionState connectionState = connection.getState();
    assertThat(connectionState.getConnectionProfile().getProfileConnectionProperties(), equalTo(CONNECTION_PROPERTIES));
  }

  @Test
  public void simpleParameterInShowInDslParameterGroup() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    TestOAuthConnectionState connectionState = connection.getState();
    assertThat(connectionState.getConnectionProfile().getProfileLevel(), equalTo(Integer.valueOf(PROFILE_LEVEL_PARAMETER_VALUE)));
  }

  @Test
  public void complexParameterInShowInNonDefaultParameterGroup() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    TestOAuthConnectionState connectionState = connection.getState();
    assertThat(connectionState.getConnectionDetails().getAnotherConnectionProperties(), equalTo(CONNECTION_PROPERTIES));
  }

  @Test
  public void simpleParameterInShowInNonDefaultParameterGroup() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    TestOAuthConnectionState connectionState = connection.getState();
    assertThat(connectionState.getConnectionDetails().getDetailsPriority(),
               equalTo(Integer.valueOf(DETAILS_PRIORITY_PARAMETER_VALUE)));
  }

  @Test
  public void enumParameter() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    TestOAuthConnectionState connectionState = connection.getState();
    assertThat(connectionState.getOauthConnectionType(),
               equalTo(OAUTH_CONNECTION_TYPE_PARAMETER_ENUM_VALUE));
  }

  @Test
  public void integerParameterAsString() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    TestOAuthConnectionState connectionState = connection.getState();
    assertThat(connectionState.getSecurityLevel(),
               equalTo(SECURITY_LEVEL_PARAMETER_INT_VALUE));
  }

}
