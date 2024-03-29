/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.test;

import static org.mule.runtime.api.metadata.resolving.FailureCode.COMPONENT_NOT_FOUND;
import static org.mule.runtime.api.metadata.resolving.FailureCode.UNKNOWN;
import static org.mule.runtime.api.metadata.resolving.MetadataComponent.KEYS;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.configLessConnectionLessOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.configLessOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.internalErrorMetadataResolverOP;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.multiLevelOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.multiLevelOPDeclarationPartialTypeKeys;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.multiLevelShowInDslGroupOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.requiresConfigurationOutputTypeKeyResolverOP;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.sourceDeclaration;
import static org.mule.tck.junit4.matcher.MetadataKeyMatcher.metadataKeyWithId;

import static java.lang.String.format;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.resolving.FailureCode;
import org.mule.runtime.api.metadata.resolving.MetadataFailure;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.app.declaration.api.ComponentElementDeclaration;

import java.util.Set;

import org.junit.Test;

import org.hamcrest.Matchers;
import org.hamcrest.collection.IsCollectionWithSize;

public class MetadataKeysTestCase extends DeclarationSessionTestCase {

  private static final String CONFIG_LESS_CONNECTION_METADATA_RESOLVER = "ConfigLessConnectionLessMetadataResolver";
  private static final String CONFIG_LESS_METADATA_RESOLVER = "ConfigLessMetadataResolver";
  private static final String MULTI_LEVEL_PARTIAL_TYPE_KEYS_OUTPUT_RESOLVER = "MultiLevelPartialTypeKeysOutputTypeResolver";
  private static final String MULTI_LEVEL_TYPE_KEYS_OUTPUT_RESOLVER = "MultiLevelTypeKeysOutputTypeResolver";

  @Test
  public void preserveOrder() {
    ComponentElementDeclaration elementDeclaration = multiLevelOPDeclaration(CONFIG_NAME, null, null);
    MetadataResult<MetadataKeysContainer> metadataKeys = session.getMetadataKeys(elementDeclaration);
    assertThat(metadataKeys.isSuccess(), Matchers.is(true));

    Set<MetadataKey> keys = metadataKeys.get().getKeysByCategory().get(MULTI_LEVEL_TYPE_KEYS_OUTPUT_RESOLVER);
    assertThat(keys, contains(hasProperty("id", equalTo("EUROPE")),
                              hasProperty("id", equalTo("AMERICA"))));
  }

  @Test
  public void configLessConnectionLessOnOperationMetadataKeys() {
    MetadataResult<MetadataKeysContainer> metadataKeys =
        session.getMetadataKeys(configLessConnectionLessOPDeclaration(CONFIG_NAME));
    assertThat(metadataKeys.isSuccess(), is(true));

    Set<MetadataKey> keys = metadataKeys.get().getKeysByCategory().get(CONFIG_LESS_CONNECTION_METADATA_RESOLVER);
    assertThat(keys, hasSize(1));
    assertThat(keys.stream().findFirst().map(metadataKey -> metadataKey.getId())
        .orElseThrow(() -> new AssertionError("MetadataKey not resolved")), is(CONFIG_LESS_CONNECTION_METADATA_RESOLVER));
  }

  @Test
  public void connectionFailure() {
    ComponentElementDeclaration<?> elementDeclaration = configLessOPDeclaration(CONFIG_FAILING_CONNECTION_PROVIDER);
    MetadataResult<MetadataKeysContainer> metadataKeys = session.getMetadataKeys(elementDeclaration);
    assertThat(metadataKeys.isSuccess(), is(false));
    assertThat(metadataKeys.getFailures(), IsCollectionWithSize.hasSize(1));
    MetadataFailure failure = metadataKeys.getFailures().get(0);
    assertThat(failure.getFailureCode(), equalTo(FailureCode.CONNECTION_FAILURE));
    assertThat(failure.getMessage(),
               equalTo("Failed to establish connection: ConnectionException: Expected connection exception"));
    assertThat(failure.getReason(),
               containsString("org.mule.runtime.api.connection.ConnectionException: Expected connection exception"));
  }


  @Test
  public void configLessOPMetadataKeys() {
    ComponentElementDeclaration<?> elementDeclaration = configLessOPDeclaration(CONFIG_NAME);
    MetadataResult<MetadataKeysContainer> metadataKeys = session.getMetadataKeys(elementDeclaration);
    assertThat(metadataKeys.isSuccess(), is(true));

    Set<MetadataKey> keys = metadataKeys.get().getKeysByCategory().get(CONFIG_LESS_METADATA_RESOLVER);
    assertThat(keys, hasSize(1));
    assertThat(keys.stream().findFirst().map(metadataKey -> metadataKey.getId())
        .orElseThrow(() -> new AssertionError("MetadataKey not resolved")), is(CLIENT_NAME));
  }

  @Test
  public void multiLevelOPMetadataKeysPartialEmptyFirstLevel() {
    multiLevelComponentMetadataKeysPartialEmptyFirstLevel(multiLevelOPDeclarationPartialTypeKeys(CONFIG_NAME, null, null));
  }

  @Test
  public void multiLevelSourceMetadataKeysPartialEmptyFirstLevel() {
    multiLevelComponentMetadataKeysPartialEmptyFirstLevel(sourceDeclaration(CONFIG_NAME, null, null, null));
  }

  private void multiLevelComponentMetadataKeysPartialEmptyFirstLevel(ComponentElementDeclaration elementDeclaration) {
    MetadataResult<MetadataKeysContainer> metadataKeys = session.getMetadataKeys(elementDeclaration);
    assertThat(metadataKeys.isSuccess(), is(true));

    Set<MetadataKey> continents = metadataKeys.get().getKeysByCategory().get(MULTI_LEVEL_PARTIAL_TYPE_KEYS_OUTPUT_RESOLVER);
    assertThat(continents, hasSize(2));
    assertThat(continents, hasItem(metadataKeyWithId("AMERICA")));
    assertThat(continents, hasItem(metadataKeyWithId("EUROPE")));
  }

  @Test
  public void expressionRequiresContext() {
    MetadataResult<MetadataKeysContainer> metadataResult =
        session.getMetadataKeys(multiLevelOPDeclarationPartialTypeKeys(CONFIG_NAME, "#[vars.continent]", null));
    assertThat(metadataResult.isSuccess(), is(false));
    assertThat(metadataResult.getFailures(), hasSize(1));
    MetadataFailure metadataFailure = metadataResult.getFailures().get(0);
    assertThat(metadataFailure.getMessage(),
               is("Error resolving value for parameter: 'continent' from declaration, it cannot be an EXPRESSION value"));
    assertThat(metadataFailure.getFailureCode(), is(new FailureCode("INVALID_PARAMETER_VALUE")));
  }

  @Test
  public void multiLevelOPMetadataKeyPartialWithFirstLevel() {
    multiLevelComponentMetadataKeyPartialWithFirstLevel(multiLevelOPDeclarationPartialTypeKeys(CONFIG_NAME, "America", null));
  }

  @Test
  public void multiLevelShowInDslGroupOPMetadataKeyPartialWithFirstLevel() {
    multiLevelComponentMetadataKeyPartialWithFirstLevel(multiLevelShowInDslGroupOPDeclaration(CONFIG_NAME, "America", null));
  }

  @Test
  public void multiLevelSourceMetadataKeyPartialWithFirstLevel() {
    multiLevelComponentMetadataKeyPartialWithFirstLevel(sourceDeclaration(CONFIG_NAME, "America", null));
  }

  private void multiLevelComponentMetadataKeyPartialWithFirstLevel(ComponentElementDeclaration elementDeclaration) {
    MetadataResult<MetadataKeysContainer> metadataKeys = session.getMetadataKeys(elementDeclaration);
    assertThat(metadataKeys.isSuccess(), is(true));

    Set<MetadataKey> continents = metadataKeys.get().getKeysByCategory().get(MULTI_LEVEL_PARTIAL_TYPE_KEYS_OUTPUT_RESOLVER);
    assertThat(continents, hasSize(1));
    final MetadataKey continent =
        continents.stream().findFirst().orElseThrow(() -> new AssertionError("MetadataKey not resolved"));
    assertThat(continent, metadataKeyWithId("AMERICA").withDisplayName("AMERICA").withPartName("continent"));

    Set<MetadataKey> countries = continent.getChilds();
    assertThat(countries, hasSize(2));
    assertThat(countries, hasItem(metadataKeyWithId("USA").withDisplayName("United States").withPartName("country")));
    assertThat(countries, hasItem(metadataKeyWithId("ARGENTINA").withDisplayName("ARGENTINA").withPartName("country")));
  }

  @Test
  public void componentNotFoundOnDeclaration() {
    String invalidComponentName = "invalid";
    MetadataResult<MetadataKeysContainer> metadataKeys =
        session.getMetadataKeys(invalidComponentDeclaration(invalidComponentName));
    assertThat(metadataKeys.isSuccess(), is(false));
    assertThat(metadataKeys.getFailures(), hasSize(1));
    assertThat(metadataKeys.getFailures().get(0).getFailureCode(), is(COMPONENT_NOT_FOUND));
    assertThat(metadataKeys.getFailures().get(0).getFailingComponent(), is(KEYS));
    assertThat(metadataKeys.getFailures().get(0).getMessage(),
               is(format("Could not find component: 'ToolingSupportTest:%s'", invalidComponentName)));
  }

  @Test
  public void extensionModelNotFound() {
    String invalidExtensionModel = "invalidExtension";
    MetadataResult<MetadataKeysContainer> metadataKeys = session.getMetadataKeys(
                                                                                 invalidExtensionModel(invalidExtensionModel));
    assertThat(metadataKeys.isSuccess(), is(false));
    assertThat(metadataKeys.getFailures(), hasSize(1));
    assertThat(metadataKeys.getFailures().get(0).getFailureCode(), is(COMPONENT_NOT_FOUND));
    assertThat(metadataKeys.getFailures().get(0).getFailingComponent(), is(KEYS));
    assertThat(metadataKeys.getFailures().get(0).getMessage(),
               is(format("ElementDeclaration is defined for extension: '%s' which is not part of the context: '[mule, ToolingSupportTest, module, tls]'",
                         invalidExtensionModel)));
  }

  @Test
  public void configRefNotFound() {
    String missingConfigName = "missingConfigName";
    MetadataResult<MetadataKeysContainer> metadataKeys =
        session.getMetadataKeys(multiLevelShowInDslGroupOPDeclaration(missingConfigName, null, null));
    assertThat(metadataKeys.isSuccess(), is(false));
    assertThat(metadataKeys.getFailures(), hasSize(1));
    assertThat(metadataKeys.getFailures().get(0).getFailureCode(), is(COMPONENT_NOT_FOUND));
    assertThat(metadataKeys.getFailures().get(0).getFailingComponent(), is(KEYS));
    assertThat(metadataKeys.getFailures().get(0).getMessage(),
               is(format("The resolver requires a configuration but the one referenced by the component declaration with name: '%s' is not present",
                         missingConfigName)));
  }

  @Test
  public void failOnOperationDoesNotHaveConfigButResolverRequiresConfiguration() {
    MetadataResult<MetadataKeysContainer> metadataKeys =
        session.getMetadataKeys(requiresConfigurationOutputTypeKeyResolverOP("someType"));
    assertThat(metadataKeys.isSuccess(), is(false));
    assertThat(metadataKeys.getFailures(), hasSize(1));
    assertThat(metadataKeys.getFailures().get(0).getFailureCode(), is(UNKNOWN));
    assertThat(metadataKeys.getFailures().get(0).getFailingComponent(), is(KEYS));
    assertThat(metadataKeys.getFailures().get(0).getMessage(),
               is("Configuration is not present, a message from resolver"));
  }

  @Test
  public void internalErrorInsideResolver() {
    MetadataResult<MetadataKeysContainer> metadataKeys =
        session.getMetadataKeys(internalErrorMetadataResolverOP());
    assertThat(metadataKeys.isSuccess(), is(false));
    assertThat(metadataKeys.getFailures(), hasSize(1));
    assertThat(metadataKeys.getFailures().get(0).getFailureCode(), is(UNKNOWN));
    assertThat(metadataKeys.getFailures().get(0).getFailingComponent(), is(KEYS));
    assertThat(metadataKeys.getFailures().get(0).getMessage(),
               is("InternalErrorMetadataResolver has thrown unexpected exception"));
  }

}
