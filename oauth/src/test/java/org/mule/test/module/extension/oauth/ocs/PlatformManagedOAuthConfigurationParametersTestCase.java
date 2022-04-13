/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.oauth.ocs;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mule.test.oauth.ConnectionType.DUO;
import static org.mule.test.oauth.ConnectionType.HYPER;

import org.mule.test.oauth.ConnectionProperties;
import org.mule.test.oauth.ConnectionType;
import org.mule.test.oauth.TestOAuthConnection;
import org.mule.test.oauth.TestOAuthConnectionState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.qameta.allure.Description;
import io.qameta.allure.Issue;
import org.junit.Test;

@Issue("W-10867511")
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
  private static final String FIRST_SOME_NUMBER = "1";
  private static final String SECOND_SOME_NUMBER = "2";
  private static final String THIRD_SOME_NUMBER = "3";
  private static final String SOME_NUMBERS_PARAMETER_NAME = "someConnectionNumbers";
  private static final String CONNECTION_PROPERTIES_MAP_PARAMETER_NAME = "someMapOfConnectionProperties";
  private static final String CONNECTION_PROPERTIES_MAP_FIRST_KEY = "first";
  private static final String CONNECTION_PROPERTIES_MAP_SECOND_KEY = "second";
  private static final List<String> SOME_NUMBERS_PARAMETER_VALUE =
      asList(new String[] {FIRST_SOME_NUMBER, SECOND_SOME_NUMBER, THIRD_SOME_NUMBER});
  private static final String SOME_CONNECTION_PROPERTIES_PARAMETER_NAME = "someOauthConnectionProperties";
  private static final ConnectionType OAUTH_CONNECTION_TYPE_PARAMETER_ENUM_VALUE = HYPER;
  private static final ConnectionType CONNECTION_TYPE_FIELD_ENUM_VALUE = DUO;
  private static final ConnectionProperties CONNECTION_PROPERTIES =
      new ConnectionProperties(CONNECTION_DESCRIPTION_FIELD_VALUE, CONNECTION_TYPE_FIELD_ENUM_VALUE);

  @Override
  protected Map<String, Object> getDescriptorParameters() {
    Map<String, Object> descriptorParameters = super.getDescriptorParameters();
    Map<String, String> complexParameterMap = new HashMap<>();
    List<Map<String, String>> someConnectionPropertiesValue = new ArrayList<>();
    Map<String, Map<String, String>> someMapOfConnectionPropertiesValue = new HashMap<>();
    complexParameterMap.put(CONNECTION_DESCRIPTION_FIELD_NAME, CONNECTION_DESCRIPTION_FIELD_VALUE);
    complexParameterMap.put(CONNECTION_TYPE_FIELD_NAME, CONNECTION_TYPE_FIELD_VALUE);
    someConnectionPropertiesValue.add(complexParameterMap);
    someConnectionPropertiesValue.add(complexParameterMap);
    someMapOfConnectionPropertiesValue.put(CONNECTION_PROPERTIES_MAP_FIRST_KEY, complexParameterMap);
    someMapOfConnectionPropertiesValue.put(CONNECTION_PROPERTIES_MAP_SECOND_KEY, complexParameterMap);
    descriptorParameters.put(CONNECTION_PROPERTIES_MAP_PARAMETER_NAME, someMapOfConnectionPropertiesValue);
    descriptorParameters.put(SOME_CONNECTION_PROPERTIES_PARAMETER_NAME, someConnectionPropertiesValue);
    descriptorParameters.put(COMPLEX_PARAMETER_NAME, complexParameterMap);
    descriptorParameters.put(COMPLEX_PARAMETER_NAME_IN_SHOWINDSL_PARAMETER_GROUP, complexParameterMap);
    descriptorParameters.put(COMPLEX_PARAMETER_NAME_IN_NON_DEFAULT_PARAMETER_GROUP, complexParameterMap);
    descriptorParameters.put(PROFILE_LEVEL_PARAMETER_NAME, PROFILE_LEVEL_PARAMETER_VALUE);
    descriptorParameters.put(DETAILS_PRIORITY_PARAMETER_NAME, DETAILS_PRIORITY_PARAMETER_VALUE);
    descriptorParameters.put(SECURITY_LEVEL_PARAMETER_NAME, SECURITY_LEVEL_PARAMETER_VALUE);
    descriptorParameters.put(OAUTH_CONNECTION_TYPE_PARAMETER_NAME, OAUTH_CONNECTION_TYPE_PARAMETER_VALUE);
    descriptorParameters.put(SOME_NUMBERS_PARAMETER_NAME, SOME_NUMBERS_PARAMETER_VALUE);
    return descriptorParameters;
  }

  @Test
  @Description("Validates that the PlatformManagedConnectionDescriptor can describe parameters of complex types.")
  public void complexParameter() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    TestOAuthConnectionState connectionState = connection.getState();
    assertThat(connectionState.getConnectionProperties(), equalTo(CONNECTION_PROPERTIES));
  }

  @Test

  @Description("Validates that the PlatformManagedConnectionDescriptor can describe parameters of complex types that belong to a parameter group that is shownInDsl.")
  public void complexParameterInShowInDslParameterGroup() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    TestOAuthConnectionState connectionState = connection.getState();
    assertThat(connectionState.getConnectionProfile().getProfileConnectionProperties(), equalTo(CONNECTION_PROPERTIES));
  }

  @Test
  @Description("Validates that the PlatformManagedConnectionDescriptor can describe parameters of simple types that belong to a parameter group that is shownInDsl")
  public void simpleParameterInShowInDslParameterGroup() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    TestOAuthConnectionState connectionState = connection.getState();
    assertThat(connectionState.getConnectionProfile().getProfileLevel(), equalTo(Integer.valueOf(PROFILE_LEVEL_PARAMETER_VALUE)));
  }

  @Test
  @Description("Validates that the PlatformManagedConnectionDescriptor can describe parameters of complex types that belong to the default parameter group.")
  public void complexParameterInNonShowDslInNonDefaultParameterGroup() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    TestOAuthConnectionState connectionState = connection.getState();
    assertThat(connectionState.getConnectionDetails().getAnotherConnectionProperties(), equalTo(CONNECTION_PROPERTIES));
  }

  @Test
  @Description("Validates that the PlatformManagedConnectionDescriptor can describe simple parameters that belong to a non-default parameter group that is not shownInDsl.")
  public void simpleParameterInNonShowInDslNonDefaultParameterGroup() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    TestOAuthConnectionState connectionState = connection.getState();
    assertThat(connectionState.getConnectionDetails().getDetailsPriority(),
               equalTo(Integer.valueOf(DETAILS_PRIORITY_PARAMETER_VALUE)));
  }

  @Test
  @Description("Validates that the PlatformManagedConnectionDescriptor can describe enum parameters.")
  public void enumParameter() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    TestOAuthConnectionState connectionState = connection.getState();
    assertThat(connectionState.getOauthConnectionType(),
               equalTo(OAUTH_CONNECTION_TYPE_PARAMETER_ENUM_VALUE));
  }

  @Test
  @Description("Validates that the PlatformManagedConnectionDescriptor can describe integer parameters that needs to be transformed from String.")
  public void integerParameterAsString() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    TestOAuthConnectionState connectionState = connection.getState();
    assertThat(connectionState.getSecurityLevel(),
               equalTo(SECURITY_LEVEL_PARAMETER_INT_VALUE));
  }

  @Test
  @Description("Validates that the PlatformManagedConnectionDescriptor can describe a parameter whose time is List<Integer>.")
  public void arrayOfIntegerParameter() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    TestOAuthConnectionState connectionState = connection.getState();
    List<Integer> someConnectionNumbers = connectionState.getSomeConnectionNumbers();
    assertThat(someConnectionNumbers, hasSize(SOME_NUMBERS_PARAMETER_VALUE.size()));
    assertThat(someConnectionNumbers, containsInAnyOrder(Integer.valueOf(FIRST_SOME_NUMBER), Integer.valueOf(SECOND_SOME_NUMBER),
                                                         Integer.valueOf(THIRD_SOME_NUMBER)));
  }

  @Test
  @Description("Validates that the PlatformManagedConnectionDescriptor can describe a parameter whose type is a list of a complex type.")
  public void arrayOfComplexTypeParameter() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    TestOAuthConnectionState connectionState = connection.getState();
    List<ConnectionProperties> someOauthConnectionProperties = connectionState.getSomeOauthConnectionProperties();
    assertThat(someOauthConnectionProperties, hasSize(2));
    someOauthConnectionProperties
        .forEach(someOauthConnectionProperty -> assertThat(someOauthConnectionProperty, equalTo(CONNECTION_PROPERTIES)));
  }

  @Test
  @Description("Validates that the PlatformManagedConnectionDescriptor can describe a parameter whose type is a map with a complex value.")
  public void mapOfComplexValueTypeParameter() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    TestOAuthConnectionState connectionState = connection.getState();
    Map<String, ConnectionProperties> someOauthMapConnectionProperties = connectionState.getSomeMapOfConnectionProperties();
    assertThat(someOauthMapConnectionProperties.entrySet(), hasSize(2));
    assertThat(someOauthMapConnectionProperties.get(CONNECTION_PROPERTIES_MAP_FIRST_KEY), equalTo(CONNECTION_PROPERTIES));
    assertThat(someOauthMapConnectionProperties.get(CONNECTION_PROPERTIES_MAP_SECOND_KEY), equalTo(CONNECTION_PROPERTIES));
  }

}
