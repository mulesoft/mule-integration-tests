/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling;

import static org.mule.metadata.api.utils.MetadataTypeUtils.getTypeId;
import static org.mule.runtime.api.metadata.resolving.FailureCode.COMPONENT_NOT_FOUND;
import static org.mule.runtime.api.metadata.resolving.FailureCode.INVALID_METADATA_KEY;
import static org.mule.runtime.api.metadata.resolving.FailureCode.UNKNOWN;
import static org.mule.runtime.api.metadata.resolving.MetadataComponent.COMPONENT;
import static org.mule.runtime.api.metadata.resolving.MetadataComponent.INPUT;
import static org.mule.runtime.api.metadata.resolving.MetadataComponent.OUTPUT_ATTRIBUTES;
import static org.mule.runtime.api.metadata.resolving.MetadataComponent.OUTPUT_PAYLOAD;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.configLessConnectionLessOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.configLessMetadataKeyExpressionDefaultValueOP;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.configLessOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.internalErrorMetadataResolverOP;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.metadataKeyWithOptionalsAndPartialKeyOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.metadataKeyWithOptionalsOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.multiLevelCompleteOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.multiLevelOPDeclarationPartialTypeKeys;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.multiLevelShowInDslGroupOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.multiLevelTypeKeyMetadataKeyWithDefaultsOP;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.requiresConfigurationOutputTypeKeyResolverOP;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.resolverUsesResourcesCacheOP;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.sourceDeclaration;

import static java.lang.String.format;
import static java.util.Optional.of;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import org.mule.metadata.api.annotation.DefaultValueAnnotation;
import org.mule.metadata.internal.utils.MetadataTypeWriter;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataTypesDescriptor;
import org.mule.runtime.api.metadata.resolving.FailureCode;
import org.mule.runtime.api.metadata.resolving.MetadataFailure;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.app.declaration.api.OperationElementDeclaration;
import org.mule.runtime.app.declaration.api.SourceElementDeclaration;

import org.junit.Test;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;

public class MetadataTypesTestCase extends DeclarationSessionTestCase {

  @Test
  public void sourceDynamicTypes() {
    SourceElementDeclaration sourceElementDeclaration = sourceDeclaration(CONFIG_NAME, null, "America", "USA", "SFO");
    MetadataResult<ComponentMetadataTypesDescriptor> containerTypeMetadataResult =
        session.resolveComponentMetadata(sourceElementDeclaration);
    assertThat(containerTypeMetadataResult.isSuccess(), is(true));

    // input parameters
    assertThat(containerTypeMetadataResult.get().getInputMetadata().size(), is(1));
    assertThat(new MetadataTypeWriter().toString(containerTypeMetadataResult.get().getInputMetadata().get("onSuccessParameter")),
               equalTo("%type _:Java = @default(\"value\" : \"America|USA|SFO\") String"));

    // output
    assertThat(containerTypeMetadataResult.get().getOutputMetadata().isPresent(), is(true));
    assertThat(new MetadataTypeWriter().toString(containerTypeMetadataResult.get().getOutputMetadata().get()),
               equalTo("%type _:Java = @default(\"value\" : \"America|USA|SFO\") String"));

    // output attributes
    assertThat(containerTypeMetadataResult.get().getOutputAttributesMetadata().isPresent(), is(true));
    assertThat(new MetadataTypeWriter().toString(containerTypeMetadataResult.get().getOutputAttributesMetadata().get()),
               equalTo("%type _:Java = @typeId(\"value\" : \"org.mule.tooling.extensions.metadata.api.source.StringAttributes\") {\n"
                   +
                   "  \"value\"? : @default(\"value\" : \"America|USA|SFO\") String\n" +
                   "}"));

  }

  @Test
  public void operationDynamicTypes() {
    OperationElementDeclaration operationElementDeclaration =
        multiLevelCompleteOPDeclaration(CONFIG_NAME, "America", "USA", "SFO");
    MetadataResult<ComponentMetadataTypesDescriptor> containerTypeMetadataResult =
        session.resolveComponentMetadata(operationElementDeclaration);
    assertThat(containerTypeMetadataResult.isSuccess(), is(true));
    assertAmericaUsaSfoMetadata(containerTypeMetadataResult);
  }

  private void assertAmericaUsaSfoMetadata(MetadataResult<ComponentMetadataTypesDescriptor> containerTypeMetadataResult) {
    // input parameters
    assertInputDynamicTypeAmericaUsaSfoMetadata(containerTypeMetadataResult);

    // output
    assertThat(containerTypeMetadataResult.get().getOutputMetadata().isPresent(), is(true));
    assertThat(new MetadataTypeWriter().toString(containerTypeMetadataResult.get().getOutputMetadata().get()),
               equalTo("%type _:Java = @default(\"value\" : \"America|USA|SFO\") String"));

    // output attributes
    assertThat(containerTypeMetadataResult.get().getOutputAttributesMetadata().isPresent(), is(true));
    assertThat(new MetadataTypeWriter().toString(containerTypeMetadataResult.get().getOutputAttributesMetadata().get()),
               equalTo("%type _:Java = @typeId(\"value\" : \"org.mule.tooling.extensions.metadata.api.source.StringAttributes\") {\n"
                   +
                   "  \"value\"? : @default(\"value\" : \"America|USA|SFO\") String\n" +
                   "}"));
  }

  private void assertInputDynamicTypeAmericaUsaSfoMetadata(MetadataResult<ComponentMetadataTypesDescriptor> containerTypeMetadataResult) {
    assertThat(containerTypeMetadataResult.get().getInputMetadata().size(), is(1));
    assertThat(new MetadataTypeWriter().toString(containerTypeMetadataResult.get().getInputMetadata().get("dynamicParam")),
               equalTo("%type _:Java = @default(\"value\" : \"America|USA|SFO\") String"));
  }

  @Test
  public void operationDynamicTypesPartialKey() {
    OperationElementDeclaration operationElementDeclaration =
        multiLevelTypeKeyMetadataKeyWithDefaultsOP(CONFIG_NAME, "America", "USA", null);
    MetadataResult<ComponentMetadataTypesDescriptor> containerTypeMetadataResult =
        session.resolveComponentMetadata(operationElementDeclaration);
    assertThat(containerTypeMetadataResult.isSuccess(), is(true));
    assertInputDynamicTypeAmericaUsaSfoMetadata(containerTypeMetadataResult);
  }

  @Test
  public void operationDynamicTypesNoKey() {
    OperationElementDeclaration operationElementDeclaration =
        multiLevelTypeKeyMetadataKeyWithDefaultsOP(CONFIG_NAME, null, null, null);
    MetadataResult<ComponentMetadataTypesDescriptor> containerTypeMetadataResult =
        session.resolveComponentMetadata(operationElementDeclaration);
    assertThat(containerTypeMetadataResult.isSuccess(), is(true));
    assertInputDynamicTypeAmericaUsaSfoMetadata(containerTypeMetadataResult);
  }

  @Test
  public void expressionNotSupportedMetadataKey() {
    OperationElementDeclaration operationElementDeclaration =
        multiLevelOPDeclarationPartialTypeKeys(CONFIG_NAME, "America", "#['USA']");
    MetadataResult<ComponentMetadataTypesDescriptor> containerTypeMetadataResult =
        session.resolveComponentMetadata(operationElementDeclaration);
    assertThat(containerTypeMetadataResult.isSuccess(), is(false));
    assertThat(containerTypeMetadataResult.getFailures(), hasSize(1));
    assertThat(containerTypeMetadataResult.getFailures().get(0).getFailureCode(), is(INVALID_METADATA_KEY));
    assertThat(containerTypeMetadataResult.getFailures().get(0).getMessage(),
               containsString("Error resolving value for parameter: 'country' from declaration, it cannot be an EXPRESSION value"));
  }

  @Test
  public void operationDynamicTypesSingleLevelKey() {
    OperationElementDeclaration operationElementDeclaration = configLessOPDeclaration(CONFIG_NAME, "item");
    MetadataResult<ComponentMetadataTypesDescriptor> metadataTypes =
        session.resolveComponentMetadata(operationElementDeclaration);
    assertThat(metadataTypes.isSuccess(), is(true));
    assertThat(metadataTypes.get().getOutputMetadata().isPresent(), is(true));
    assertThat(getTypeId(metadataTypes.get().getOutputMetadata().get()),
               is(of("org.mule.tooling.extensions.metadata.api.parameters.ItemOutput")));
  }

  @Test
  public void operationDynamicTypesSingleLevelKeyDefaultValue() {
    OperationElementDeclaration operationElementDeclaration = configLessOPDeclaration(CONFIG_NAME, null);
    MetadataResult<ComponentMetadataTypesDescriptor> metadataTypes =
        session.resolveComponentMetadata(operationElementDeclaration);
    assertThat(metadataTypes.isSuccess(), is(true));
    assertThat(metadataTypes.get().getOutputMetadata().isPresent(), is(true));
    assertThat(getTypeId(metadataTypes.get().getOutputMetadata().get()),
               is(of("org.mule.tooling.extensions.metadata.api.parameters.ItemOutput")));
  }

  @Test
  public void connectionFailure() {
    OperationElementDeclaration operationElementDeclaration = configLessOPDeclaration(CONFIG_FAILING_CONNECTION_PROVIDER, "item");
    MetadataResult<ComponentMetadataTypesDescriptor> metadataTypes =
        session.resolveComponentMetadata(operationElementDeclaration);
    assertThat(metadataTypes.isSuccess(), is(false));
    assertThat(metadataTypes.getFailures(), hasSize(1));
    MetadataFailure failure = metadataTypes.getFailures().get(0);
    assertThat(failure.getFailureCode(), equalTo(FailureCode.CONNECTION_FAILURE));
    assertThat(failure.getMessage(),
               Matchers.equalTo("Failed to establish connection: ConnectionException: Expected connection exception"));
    assertThat(failure.getReason(),
               CoreMatchers.containsString("org.mule.runtime.api.connection.ConnectionException: Expected connection exception"));
  }

  @Test
  public void operationDynamicTypesSingleLevelKeyRequiredNotProvided() {
    OperationElementDeclaration operationElementDeclaration = configLessConnectionLessOPDeclaration(CONFIG_NAME);
    MetadataResult<ComponentMetadataTypesDescriptor> metadataTypes =
        session.resolveComponentMetadata(operationElementDeclaration);
    assertThat(metadataTypes.isSuccess(), is(false));
    assertThat(metadataTypes.getFailures(), hasSize(1));
    assertThat(metadataTypes.getFailures().get(0).getFailingComponent(), is(COMPONENT));
    assertThat(metadataTypes.getFailures().get(0).getFailureCode(), is(INVALID_METADATA_KEY));
    assertThat(metadataTypes.getFailures().get(0).getMessage(), containsString("Missing MetadataKey: metadataKey"));
  }

  @Test
  public void simpleMetadataKeyWithDefaultValue() {
    OperationElementDeclaration operationElementDeclaration = configLessOPDeclaration(CONFIG_NAME);
    MetadataResult<ComponentMetadataTypesDescriptor> metadataTypes =
        session.resolveComponentMetadata(operationElementDeclaration);
    assertThat(metadataTypes.isSuccess(), is(true));
    assertThat(metadataTypes.get().getOutputMetadata().isPresent(), is(true));
    assertThat(getTypeId(metadataTypes.get().getOutputMetadata().get()),
               is(of("org.mule.tooling.extensions.metadata.api.parameters.ItemOutput")));
  }

  @Test
  public void simpleMetadataKeyWithNullValue() {
    OperationElementDeclaration operationElementDeclaration = configLessMetadataKeyExpressionDefaultValueOP(CONFIG_NAME, null);
    MetadataResult<ComponentMetadataTypesDescriptor> metadataTypes =
        session.resolveComponentMetadata(operationElementDeclaration);
    assertThat(metadataTypes.isSuccess(), is(false));
    assertThat(metadataTypes.getFailures(), hasSize(1));
    assertThat(metadataTypes.getFailures().get(0).getFailureCode(), is(INVALID_METADATA_KEY));
    assertThat(metadataTypes.getFailures().get(0).getFailingComponent(), is(OUTPUT_PAYLOAD));
    assertThat(metadataTypes.getFailures().get(0).getMessage(),
               is("Null key provided to the resolver, this resolver doesn't support null values"));
  }

  @Test
  public void componentNotFoundOnDeclaration() {
    String invalidComponentName = "invalid";
    MetadataResult<ComponentMetadataTypesDescriptor> metadataTypes =
        session.resolveComponentMetadata(invalidComponentDeclaration(invalidComponentName));
    assertThat(metadataTypes.isSuccess(), is(false));
    assertThat(metadataTypes.getFailures(), hasSize(1));
    assertThat(metadataTypes.getFailures().get(0).getFailureCode(), is(COMPONENT_NOT_FOUND));
    assertThat(metadataTypes.getFailures().get(0).getFailingComponent(), is(COMPONENT));
    assertThat(metadataTypes.getFailures().get(0).getMessage(),
               is(format("Could not find component: 'ToolingSupportTest:%s'", invalidComponentName)));
  }

  @Test
  public void extensionModelNotFound() {
    String invalidExtensionModel = "invalidExtensionName";
    MetadataResult<ComponentMetadataTypesDescriptor> metadataTypes =
        session.resolveComponentMetadata(invalidExtensionModel(invalidExtensionModel));
    assertThat(metadataTypes.isSuccess(), is(false));
    assertThat(metadataTypes.getFailures(), hasSize(1));
    assertThat(metadataTypes.getFailures().get(0).getFailureCode(), is(COMPONENT_NOT_FOUND));
    assertThat(metadataTypes.getFailures().get(0).getFailingComponent(), is(COMPONENT));
    assertThat(metadataTypes.getFailures().get(0).getMessage(),
               is(format("ElementDeclaration is defined for extension: '%s' which is not part of the context: '[mule, ToolingSupportTest, module, tls]'",
                         invalidExtensionModel)));
  }

  @Test
  public void configRefNotFound() {
    String missingConfigName = "missingConfigName";
    MetadataResult<ComponentMetadataTypesDescriptor> metadataTypes =
        session.resolveComponentMetadata(multiLevelShowInDslGroupOPDeclaration(missingConfigName, null, null));
    assertThat(metadataTypes.isSuccess(), is(false));
    assertThat(metadataTypes.getFailures(), hasSize(1));
    assertThat(metadataTypes.getFailures().get(0).getFailureCode(), is(COMPONENT_NOT_FOUND));
    assertThat(metadataTypes.getFailures().get(0).getFailingComponent(), is(COMPONENT));
    assertThat(metadataTypes.getFailures().get(0).getMessage(),
               is(format("The resolver requires a configuration but the one referenced by the component declaration with name: '%s' is not present",
                         missingConfigName)));
  }

  @Test
  public void failOnOperationDoesNotHaveConfigButResolverRequiresConfiguration() {
    MetadataResult<ComponentMetadataTypesDescriptor> metadataTypes =
        session.resolveComponentMetadata(requiresConfigurationOutputTypeKeyResolverOP("someType"));
    assertThat(metadataTypes.isSuccess(), is(false));
    assertThat(metadataTypes.getFailures(), hasSize(1));
    assertThat(metadataTypes.getFailures().get(0).getFailureCode(), is(UNKNOWN));
    assertThat(metadataTypes.getFailures().get(0).getFailingComponent(), is(OUTPUT_PAYLOAD));
    assertThat(metadataTypes.getFailures().get(0).getMessage(),
               is("Configuration is not present, a message from resolver"));
  }

  @Test
  public void internalErrorInsideResolver() {
    MetadataResult<ComponentMetadataTypesDescriptor> metadataTypes =
        session.resolveComponentMetadata(internalErrorMetadataResolverOP());
    assertThat(metadataTypes.isSuccess(), is(false));
    assertThat(metadataTypes.getFailures(), hasSize(3));
    for (MetadataFailure metadataFailure : metadataTypes.getFailures()) {
      assertThat(metadataFailure.getFailureCode(), is(UNKNOWN));
      assertThat(metadataFailure.getFailingComponent(), anyOf(is(INPUT), is(OUTPUT_PAYLOAD), is(OUTPUT_ATTRIBUTES)));
      assertThat(metadataFailure.getMessage(),
                 is("InternalErrorMetadataResolver has thrown unexpected exception"));
    }
  }

  @Test
  public void metadataCache() {
    OperationElementDeclaration operationElementDeclaration = resolverUsesResourcesCacheOP(CONFIG_NAME, "KEY_1");
    // We need to clean up cache first...
    session.disposeMetadataCache(operationElementDeclaration);

    // First call
    validateMetadataCacheResolve(operationElementDeclaration, "KEY_1");

    // Second call
    validateMetadataCacheResolve(resolverUsesResourcesCacheOP(CONFIG_NAME, "KEY_2"), "KEY_1 KEY_2");

    // Dispose
    session.disposeMetadataCache(operationElementDeclaration);

    // New call again
    validateMetadataCacheResolve(resolverUsesResourcesCacheOP(CONFIG_NAME, "KEY_3"), "KEY_3");
  }

  @Test
  public void multiLevelKeyWithAllOptionalParametersProvidingSome() {
    final OperationElementDeclaration operationElementDeclaration = metadataKeyWithOptionalsOPDeclaration("America", "USA", null);
    MetadataResult<ComponentMetadataTypesDescriptor> result = session.resolveComponentMetadata(operationElementDeclaration);
    assertThat(result.isSuccess(), is(true));

    // output
    assertThat(result.get().getOutputMetadata().isPresent(), is(true));
    assertThat(result.get().getOutputMetadata().get().getAnnotation(DefaultValueAnnotation.class).get().getValue(),
               equalTo("America|USA|null"));

    // output attributes
    assertThat(result.get().getOutputAttributesMetadata().isPresent(), is(true));
    assertThat(result.get().getOutputAttributesMetadata().get().getAnnotation(DefaultValueAnnotation.class).get().getValue(),
               equalTo("America|USA|null"));
  }

  @Test
  public void multiLevelKeyWithAllOptionalParametersProvidingNone() {
    final OperationElementDeclaration operationElementDeclaration = metadataKeyWithOptionalsOPDeclaration(null, null, null);
    MetadataResult<ComponentMetadataTypesDescriptor> result = session.resolveComponentMetadata(operationElementDeclaration);
    assertThat(result.isSuccess(), is(true));

    // output
    assertThat(result.get().getOutputMetadata().isPresent(), is(true));
    assertThat(result.get().getOutputMetadata().get().getAnnotation(DefaultValueAnnotation.class).get().getValue(),
               equalTo("null|null|null"));

    // output attributes
    assertThat(result.get().getOutputAttributesMetadata().isPresent(), is(true));
    assertThat(result.get().getOutputAttributesMetadata().get().getAnnotation(DefaultValueAnnotation.class).get().getValue(),
               equalTo("null|null|null"));
  }

  @Test
  public void multiLevelKeyWithAllOptionalParametersAndPartialKeyProvidingSome() {
    final OperationElementDeclaration operationElementDeclaration =
        metadataKeyWithOptionalsAndPartialKeyOPDeclaration("America", "USA", null);
    MetadataResult<ComponentMetadataTypesDescriptor> result = session.resolveComponentMetadata(operationElementDeclaration);
    assertThat(result.isSuccess(), is(true));

    // output
    assertThat(result.get().getOutputMetadata().isPresent(), is(true));
    assertThat(result.get().getOutputMetadata().get().getAnnotation(DefaultValueAnnotation.class).get().getValue(),
               equalTo("America|USA|null"));

    // output attributes
    assertThat(result.get().getOutputAttributesMetadata().isPresent(), is(true));
    assertThat(result.get().getOutputAttributesMetadata().get().getAnnotation(DefaultValueAnnotation.class).get().getValue(),
               equalTo("America|USA|null"));
  }

  @Test
  public void multiLevelKeyWithAllOptionalParametersAndPartialKeyProvidingNone() {
    final OperationElementDeclaration operationElementDeclaration =
        metadataKeyWithOptionalsAndPartialKeyOPDeclaration(null, null, null);
    MetadataResult<ComponentMetadataTypesDescriptor> result = session.resolveComponentMetadata(operationElementDeclaration);
    assertThat(result.isSuccess(), is(true));

    // output
    assertThat(result.get().getOutputMetadata().isPresent(), is(true));
    assertThat(result.get().getOutputMetadata().get().getAnnotation(DefaultValueAnnotation.class).get().getValue(),
               equalTo("null|null|null"));

    // output attributes
    assertThat(result.get().getOutputAttributesMetadata().isPresent(), is(true));
    assertThat(result.get().getOutputAttributesMetadata().get().getAnnotation(DefaultValueAnnotation.class).get().getValue(),
               equalTo("null|null|null"));
  }

  private void validateMetadataCacheResolve(OperationElementDeclaration operationElementDeclaration,
                                            String expectedDefaultValue) {
    MetadataResult<ComponentMetadataTypesDescriptor> metadataTypes =
        session.resolveComponentMetadata(operationElementDeclaration);
    assertThat(metadataTypes.isSuccess(), is(true));
    assertThat(metadataTypes.get().getOutputMetadata().isPresent(), is(true));
    assertThat(new MetadataTypeWriter().toString(metadataTypes.get().getOutputMetadata().get()),
               equalTo("%type _:Java = @default(\"value\" : \"" + expectedDefaultValue + "\") String"));
  }

}
