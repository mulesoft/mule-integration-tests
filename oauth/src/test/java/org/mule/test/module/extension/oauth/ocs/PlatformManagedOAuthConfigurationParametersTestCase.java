/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.oauth.ocs;

import static java.time.Instant.ofEpochMilli;
import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mule.test.oauth.ConnectionType.DUO;
import static org.mule.test.oauth.ConnectionType.HYPER;

import org.mule.runtime.extension.api.runtime.parameter.Literal;
import org.mule.test.oauth.ConnectionProperties;
import org.mule.test.oauth.ConnectionType;
import org.mule.test.oauth.TestOAuthConnection;
import org.mule.test.oauth.TestOAuthConnectionState;
import org.mule.test.values.extension.MyPojo;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.qameta.allure.Description;
import io.qameta.allure.Issue;
import org.junit.Test;

@Issue("W-10867511")
public class PlatformManagedOAuthConfigurationParametersTestCase extends PlatformManagedOAuthConfigurationTestCase {

  private static final String EXTERNAL_POJO_PARAMETER_NAME_IN_POJO = "importedPojo";
  private static final String EXTERNAL_POJO_PARAMETER_NAME = "externalPojo";
  private static final String EXTERNAL_POJO_ID_FIELD_NAME = "pojoId";
  private static final String EXTERNAL_POJO_ID_FIELD_VALUE = "thePojoId";
  private static final String EXTERNAL_POJO_NAME_FIELD_NAME = "pojoName";
  private static final String EXTERNAL_POJO_NAME_FIELD_VALUE = "thePojoName";
  private static final String EXTERNAL_POJO_NUMBER_FIELD_NAME = "pojoNumber";
  private static final String EXTERNAL_POJO_NUMBER_FIELD_VALUE = "1234";
  private static final String EXTERNAL_POJO_BOOLEAN_FIELD_NAME = "pojoBoolean";
  private static final String EXTERNAL_POJO_BOOLEAN_FIELD_VALUE = "true";
  private static final MyPojo EXTERNAL_POJO_VALUE =
      new MyPojo(EXTERNAL_POJO_ID_FIELD_VALUE, EXTERNAL_POJO_NAME_FIELD_VALUE, Integer.valueOf(EXTERNAL_POJO_NUMBER_FIELD_VALUE),
                 Boolean.valueOf(EXTERNAL_POJO_BOOLEAN_FIELD_VALUE));
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
  private static final String MAP_FIRST_KEY = "first";
  private static final String MAP_SECOND_KEY = "second";
  private static final List<String> SOME_NUMBERS_PARAMETER_VALUE =
      asList(new String[] {FIRST_SOME_NUMBER, SECOND_SOME_NUMBER, THIRD_SOME_NUMBER});
  private static final String SOME_CONNECTION_PROPERTIES_PARAMETER_NAME = "someOauthConnectionProperties";
  private static final ConnectionType OAUTH_CONNECTION_TYPE_PARAMETER_ENUM_VALUE = HYPER;
  private static final ConnectionType CONNECTION_TYPE_FIELD_ENUM_VALUE = DUO;
  private static final ZonedDateTime ZONED_DATE_TIME_VALUE =
      ZonedDateTime.ofInstant(ofEpochMilli(1619535600000l), ZoneId.of("-03:00"));
  private static final ConnectionProperties CONNECTION_PROPERTIES =
      new ConnectionProperties(CONNECTION_DESCRIPTION_FIELD_VALUE, CONNECTION_TYPE_FIELD_ENUM_VALUE, new Literal<String>() {

        @Override
        public Optional<String> getLiteralValue() {
          return of(LITERAL_FIELD_IN_POJO_VALUE);
        }

        @Override
        public Class<String> getType() {
          return null;
        }
      }, ZONED_DATE_TIME_VALUE, EXTERNAL_POJO_VALUE, null);
  private static final String ZONED_DATE_TIME_FIELD_VALUE = "2021-04-27T12:00:00-03:00";
  private static final String ZONED_DATE_TIME_FIELD_NAME = "connectionTime";
  private static final String LITERAL_STRING_PARAMETER_IN_PG_VALUE = "literal1";
  private static final String LITERAL_STRING_PARAMETER_VALUE = "#[expression.in.literal]";
  private static final String PARAMETER_RESOLVER_STRING_PARAMETER_VALUE = "paramResolver1";
  private static final String TYPED_VALUE_INTEGER_PARAMETER_VALUE = "33";
  private static final String LITERAL_FIELD_IN_POJO_VALUE = "someLiteralValue";
  private static final String LITERAL_STRING_PARAMETER_IN_PG_NAME = "profileDescription";
  private static final String LITERAL_STRING_PARAMETER_NAME = "literalSecurityDescription";
  private static final String PARAMETER_RESOLVER_STRING_PARAMETER_NAME = "resolverConnectionDisplayName";
  private static final String TYPED_VALUE_INTEGER_PARAMETER_NAME = "typedSecurityLevel";
  private static final String LITERAL_FIELD_IN_POJO_NAME = "connectionPropertyGrade";
  private static final String STACKED_POJO_PARAMETER = "stackedTypePojoParameter";
  private static final String STACKED_ARRAY_PARAMETER = "stackedTypeArrayParameters";
  private static final String STACKED_MAP_PARAMETER = "stackedTypeMapParameter";

  @Override
  protected Map<String, Object> getDescriptorParameters() {
    Map<String, Object> descriptorParameters = super.getDescriptorParameters();
    Map<String, Object> complexParameterMap = new HashMap<>();
    List<Map<String, Object>> someConnectionPropertiesValue = new ArrayList<>();
    Map<String, Map<String, Object>> someMapOfConnectionPropertiesValue = new HashMap<>();
    Map<String, String> externalPojo = new HashMap<>();
    Map<String, String> mapOfIntegers = new HashMap<>();
    mapOfIntegers.put(MAP_FIRST_KEY, FIRST_SOME_NUMBER);
    mapOfIntegers.put(MAP_SECOND_KEY, SECOND_SOME_NUMBER);
    externalPojo.put(EXTERNAL_POJO_BOOLEAN_FIELD_NAME, EXTERNAL_POJO_BOOLEAN_FIELD_VALUE);
    externalPojo.put(EXTERNAL_POJO_NUMBER_FIELD_NAME, EXTERNAL_POJO_NUMBER_FIELD_VALUE);
    externalPojo.put(EXTERNAL_POJO_ID_FIELD_NAME, EXTERNAL_POJO_ID_FIELD_VALUE);
    externalPojo.put(EXTERNAL_POJO_NAME_FIELD_NAME, EXTERNAL_POJO_NAME_FIELD_VALUE);
    complexParameterMap.put(CONNECTION_DESCRIPTION_FIELD_NAME, CONNECTION_DESCRIPTION_FIELD_VALUE);
    complexParameterMap.put(CONNECTION_TYPE_FIELD_NAME, CONNECTION_TYPE_FIELD_VALUE);
    complexParameterMap.put(LITERAL_FIELD_IN_POJO_NAME, LITERAL_FIELD_IN_POJO_VALUE);
    complexParameterMap.put(ZONED_DATE_TIME_FIELD_NAME, ZONED_DATE_TIME_FIELD_VALUE);
    complexParameterMap.put(EXTERNAL_POJO_PARAMETER_NAME_IN_POJO, externalPojo);
    someConnectionPropertiesValue.add(complexParameterMap);
    someConnectionPropertiesValue.add(complexParameterMap);
    someMapOfConnectionPropertiesValue.put(MAP_FIRST_KEY, complexParameterMap);
    someMapOfConnectionPropertiesValue.put(MAP_SECOND_KEY, complexParameterMap);
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
    descriptorParameters.put(LITERAL_STRING_PARAMETER_NAME, LITERAL_STRING_PARAMETER_VALUE);
    descriptorParameters.put(LITERAL_STRING_PARAMETER_IN_PG_NAME, LITERAL_STRING_PARAMETER_IN_PG_VALUE);
    descriptorParameters.put(PARAMETER_RESOLVER_STRING_PARAMETER_NAME, PARAMETER_RESOLVER_STRING_PARAMETER_VALUE);
    descriptorParameters.put(TYPED_VALUE_INTEGER_PARAMETER_NAME, TYPED_VALUE_INTEGER_PARAMETER_VALUE);
    descriptorParameters.put(ZONED_DATE_TIME_FIELD_NAME, ZONED_DATE_TIME_FIELD_VALUE);
    descriptorParameters.put(EXTERNAL_POJO_PARAMETER_NAME, externalPojo);
    descriptorParameters.put(STACKED_POJO_PARAMETER, externalPojo);
    descriptorParameters.put(STACKED_ARRAY_PARAMETER, SOME_NUMBERS_PARAMETER_VALUE);
    descriptorParameters.put(STACKED_MAP_PARAMETER, mapOfIntegers);
    return descriptorParameters;
  }

  @Test
  @Description("Validates that the PlatformManagedConnectionDescriptor can describe parameters of complex types.")
  public void complexParameter() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    TestOAuthConnectionState connectionState = connection.getState();
    assertThat(connectionState.getConnectionProperties(), is(CONNECTION_PROPERTIES));
  }

  @Test
  @Description("Validates that the PlatformManagedConnectionDescriptor can describe parameters of complex types that belongs to a parameter group that is shownInDsl.")

  public void complexParameterInShowInDslParameterGroup() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    TestOAuthConnectionState connectionState = connection.getState();
    assertThat(connectionState.getConnectionProfile().getProfileConnectionProperties(), is(CONNECTION_PROPERTIES));
  }

  @Test
  @Description("Validates that the PlatformManagedConnectionDescriptor can describe parameters of simple types that belongs to a parameter group that is shownInDsl")

  public void simpleParameterInShowInDslParameterGroup() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    TestOAuthConnectionState connectionState = connection.getState();
    assertThat(connectionState.getConnectionProfile().getProfileLevel(), is(Integer.valueOf(PROFILE_LEVEL_PARAMETER_VALUE)));
  }

  @Test
  @Description("Validates that the PlatformManagedConnectionDescriptor can describe parameters of complex types that belongs to the default parameter group.")
  public void complexParameterInNonShowDslInNonDefaultParameterGroup() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    TestOAuthConnectionState connectionState = connection.getState();
    assertThat(connectionState.getConnectionDetails().getAnotherConnectionProperties(), is(CONNECTION_PROPERTIES));
  }

  @Test
  @Description("Validates that the PlatformManagedConnectionDescriptor can describe simple parameters that belongs to a non-default parameter group that is not shownInDsl.")

  public void simpleParameterInNonShowInDslNonDefaultParameterGroup() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    TestOAuthConnectionState connectionState = connection.getState();
    assertThat(connectionState.getConnectionDetails().getDetailsPriority(),
               is(Integer.valueOf(DETAILS_PRIORITY_PARAMETER_VALUE)));
  }

  @Test
  @Description("Validates that the PlatformManagedConnectionDescriptor can describe enum parameters.")
  public void enumParameter() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    TestOAuthConnectionState connectionState = connection.getState();
    assertThat(connectionState.getOauthConnectionType(),
               is(OAUTH_CONNECTION_TYPE_PARAMETER_ENUM_VALUE));
  }

  @Test
  @Description("Validates that the PlatformManagedConnectionDescriptor can describe integer parameters that needs to be transformed from String.")

  public void integerParameterAsString() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    TestOAuthConnectionState connectionState = connection.getState();
    assertThat(connectionState.getSecurityLevel(),
               is(SECURITY_LEVEL_PARAMETER_INT_VALUE));
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
        .forEach(someOauthConnectionProperty -> assertThat(someOauthConnectionProperty, is(CONNECTION_PROPERTIES)));
  }

  @Test
  @Description("Validates that the PlatformManagedConnectionDescriptor can describe a parameter whose type is a map with a complex value.")
  public void mapOfComplexValueTypeParameter() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    TestOAuthConnectionState connectionState = connection.getState();
    Map<String, ConnectionProperties> someOauthMapConnectionProperties = connectionState.getSomeMapOfConnectionProperties();
    assertThat(someOauthMapConnectionProperties.entrySet(), hasSize(2));
    assertThat(someOauthMapConnectionProperties.get(MAP_FIRST_KEY), is(CONNECTION_PROPERTIES));
    assertThat(someOauthMapConnectionProperties.get(MAP_SECOND_KEY), is(CONNECTION_PROPERTIES));
  }

  @Test
  @Description("Validates that the PlatformManagedConnectionDescriptor can describe a parameter whose type is a Literal.")
  public void literalTypeParameter() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    TestOAuthConnectionState connectionState = connection.getState();
    assertThat(connectionState.getLiteralSecurityDescription().getLiteralValue().get(), is(LITERAL_STRING_PARAMETER_VALUE));
  }

  @Test
  @Description("Validates that the PlatformManagedConnectionDescriptor can describe a parameter whose type is a TypedValue.")
  public void typedValueTypeParameter() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    TestOAuthConnectionState connectionState = connection.getState();
    assertThat(connectionState.getTypedSecurityLevel().getValue(),
               is(Integer.valueOf(TYPED_VALUE_INTEGER_PARAMETER_VALUE)));
  }

  @Test
  @Description("Validates that the PlatformManagedConnectionDescriptor can describe a parameter whose type is a ParameterResolver.")
  public void parameterResolverTypeParameter() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    TestOAuthConnectionState connectionState = connection.getState();
    assertThat(connectionState.getResolverConnectionDisplayName().resolve(),
               is(PARAMETER_RESOLVER_STRING_PARAMETER_VALUE));
  }

  @Test
  @Description("Validates that the PlatformManagedConnectionDescriptor can describe a parameter of Literal type inside a showInDsl parameter group.")
  public void literalTypeParameterInParameterGroupShowInDsl() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    TestOAuthConnectionState connectionState = connection.getState();
    assertThat(connectionState.getConnectionProfile().getProfileDescription().getLiteralValue().get(),
               is(LITERAL_STRING_PARAMETER_IN_PG_VALUE));
  }

  @Test
  @Description("Validates that the PlatformManagedConnectionDescriptor can describe a field of Literal type inside a complex parameter.")
  public void literalTypeParameterInPojo() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    TestOAuthConnectionState connectionState = connection.getState();
    assertThat(connectionState.getConnectionDetails().getAnotherConnectionProperties().getConnectionPropertyGrade()
        .getLiteralValue().get(),
               is(LITERAL_FIELD_IN_POJO_VALUE));
  }

  @Test
  @Description("Validates that the PlatformManagedConnectionDescriptor handles a complex parameter that injects a mule dependency.")
  public void complexParameterHasInjectedDependency() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    TestOAuthConnectionState connectionState = connection.getState();
    assertThat(connectionState.getConnectionProperties().getExtensionManager(), is(notNullValue()));
  }

  @Test
  @Description("Validates that the PlatformManagedConnectionDescriptor handles a map parameter with complex values that inject a mule dependency.")
  public void complexParameterInMapValueHasInjectedDependency() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    TestOAuthConnectionState connectionState = connection.getState();
    assertThat(connectionState.getSomeMapOfConnectionProperties().get(MAP_FIRST_KEY).getExtensionManager(),
               is(notNullValue()));
  }

  @Test
  @Description("Validates that the PlatformManagedConnectionDescriptor handles a list parameter with complex items that inject a mule dependency.")
  public void complexParameterInListHasInjectedDependency() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    TestOAuthConnectionState connectionState = connection.getState();
    assertThat(connectionState.getSomeOauthConnectionProperties().get(0).getExtensionManager(), is(notNullValue()));
  }

  @Test
  @Description("Validates that the PlatformManagedConnectionDescriptor handles a map parameter of complex type that inject a mule dependency and belongs to a showInDsl parameter group.")
  public void complexParameterInShowInDslParameterGroupHasInjectedDependency() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    TestOAuthConnectionState connectionState = connection.getState();
    assertThat(connectionState.getConnectionProfile().getProfileConnectionProperties().getExtensionManager(),
               is(notNullValue()));
  }

  @Test
  @Description("Validates that the PlatformManagedConnectionDescriptor handles a complex parameter with a Date type field.")
  public void complexParameterWithDateField() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    TestOAuthConnectionState connectionState = connection.getState();
    assertThat(connectionState.getConnectionProperties().getConnectionTime(),
               is(ZONED_DATE_TIME_VALUE));
  }

  @Test
  @Description("Validates that the PlatformManagedConnectionDescriptor handles a list parameter whose items are of complex type with a Date type field.")
  public void listParameterOfComplexTypeWithDateField() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    TestOAuthConnectionState connectionState = connection.getState();
    assertThat(connectionState.getSomeOauthConnectionProperties().get(0).getConnectionTime(),
               is(ZONED_DATE_TIME_VALUE));
  }

  @Test
  @Description("Validates that the PlatformManagedConnectionDescriptor handles a Date type parameter")
  public void parameterOfDateType() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    TestOAuthConnectionState connectionState = connection.getState();
    assertThat(connectionState.getConnectionTime(),
               is(ZONED_DATE_TIME_VALUE));
  }

  @Test
  @Description("Validates that the PlatformManagedConnectionDescriptor handles a parameter of a type that belongs to another extension.")
  public void externalTypeParameter() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    TestOAuthConnectionState connectionState = connection.getState();
    assertThat(connectionState.getExternalPojo(),
               is(EXTERNAL_POJO_VALUE));
  }

  @Test
  @Description("Validates that the PlatformManagedConnectionDescriptor handles a complex parameter parameter with a field of a type that belongs to another extension.")
  public void externalTypeInsidePojoParameter() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    TestOAuthConnectionState connectionState = connection.getState();
    assertThat(connectionState.getConnectionProperties().getImportedPojo(),
               is(EXTERNAL_POJO_VALUE));
  }

  @Test
  @Description("Validates that the PlatformManagedConnectionDescriptor can describe a parameter whose type is a custom pojo wrapped in a TypedValue wrapped in a ParameterResolver.")
  public void complexTypeInATypedValueInAParameterResolver() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    TestOAuthConnectionState connectionState = connection.getState();
    assertThat(connectionState.getStackedTypePojoParameter().resolve().getValue(),
               is(EXTERNAL_POJO_VALUE));
  }

  @Test
  @Description("Validates that the PlatformManagedConnectionDescriptor can describe a parameter whose type is an arrylist wrapped in a TypedValue wrapped in a ParameterResolver.")
  public void listInATypedValueInAParameterResolver() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    TestOAuthConnectionState connectionState = connection.getState();
    assertThat(connectionState.getStackedTypeArrayParameters().resolve().getValue(),
               hasSize(SOME_NUMBERS_PARAMETER_VALUE.size()));
    assertThat(connectionState.getStackedTypeArrayParameters().resolve().getValue(),
               containsInAnyOrder(Integer.valueOf(FIRST_SOME_NUMBER), Integer.valueOf(SECOND_SOME_NUMBER),
                                  Integer.valueOf(THIRD_SOME_NUMBER)));
  }

  @Test
  @Description("Validates that the PlatformManagedConnectionDescriptor can describe a parameter whose type is a map wrapped in a TypedValue wrapped in a ParameterResolver.")
  public void mapInATypedValueInAParameterResolver() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    TestOAuthConnectionState connectionState = connection.getState();
    Map<String, Integer> mapParameter = connectionState.getStackedTypeMapParameter().resolve().getValue();
    assertThat(mapParameter.values(),
               hasSize(2));
    assertThat(mapParameter.get(MAP_FIRST_KEY), is(Integer.valueOf(FIRST_SOME_NUMBER)));
    assertThat(mapParameter.get(MAP_SECOND_KEY), is(Integer.valueOf(SECOND_SOME_NUMBER)));
  }

}
