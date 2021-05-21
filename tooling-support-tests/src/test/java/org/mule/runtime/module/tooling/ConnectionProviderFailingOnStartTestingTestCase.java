/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mule.runtime.api.metadata.resolving.FailureCode.UNKNOWN;
import static org.mule.runtime.api.metadata.resolving.MetadataComponent.KEYS;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.actingParameterOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.configLessConnectionLessOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.configLessOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.configurationDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.failingOnStartConnectionDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.sourceDeclaration;

import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataTypesDescriptor;
import org.mule.runtime.api.metadata.resolving.FailureCode;
import org.mule.runtime.api.metadata.resolving.MetadataComponent;
import org.mule.runtime.api.metadata.resolving.MetadataFailure;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.app.declaration.api.fluent.ArtifactDeclarer;
import org.mule.runtime.deployment.model.api.DeploymentInitException;
import org.mule.runtime.deployment.model.api.DeploymentStartException;

import org.hamcrest.CoreMatchers;
import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.Test;

public class ConnectionProviderFailingOnStartTestingTestCase extends DeclarationSessionTestCase {

  private static final String CONFIG_FAILING_ON_START_CONNECTION_PROVIDER = "configNameFailingOnStartConnectionProvider";

  @Override
  protected void declareArtifact(ArtifactDeclarer artifactDeclarer) {
    artifactDeclarer.withGlobalElement(configurationDeclaration(CONFIG_FAILING_ON_START_CONNECTION_PROVIDER,
                                                                failingOnStartConnectionDeclaration()));
  }

  @Test
  public void testConnectionThatFailsOnStart() {
    ConnectionValidationResult connectionValidationResult = session.testConnection(CONFIG_FAILING_ON_START_CONNECTION_PROVIDER);
    assertThat(connectionValidationResult.isValid(), equalTo(false));
    assertThat(connectionValidationResult.getMessage(), equalTo("RuntimeException: Expected start error"));
    assertThat(connectionValidationResult.getException(), instanceOf(DeploymentStartException.class));
  }

  @Test
  public void valuesConnectionProviderThatFailsOnInit() {
    validateValuesFailure(session, actingParameterOPDeclaration(CONFIG_FAILING_ON_START_CONNECTION_PROVIDER, "actingParameter"),
            PROVIDED_PARAMETER_NAME, "Couldn't start configuration(s) while resolving values on component: 'ToolingSupportTest:actingParameterOP' for providerName: 'providedParameter'",
            "UNKNOWN", "RuntimeException: Expected start error");
  }

  @Test
  public void metadataKeysConnectionProviderThatFailsOnInit() {
    MetadataResult<MetadataKeysContainer> metadataKeys = session.getMetadataKeys(configLessOPDeclaration(CONFIG_FAILING_ON_START_CONNECTION_PROVIDER));
    assertThat(metadataKeys.isSuccess(), is(false));
    assertThat(metadataKeys.getFailures(), hasSize(1));
    MetadataFailure metadataFailure = metadataKeys.getFailures().get(0);
    assertThat(metadataFailure.getMessage(), is("Couldn't start configuration(s) while resolving metadata keys on component: 'ToolingSupportTest:configLessOP'"));
    assertThat(metadataFailure.getFailureCode(), is(UNKNOWN));
    assertThat(metadataFailure.getReason(), is("RuntimeException: Expected start error"));
    assertThat(metadataFailure.getFailingComponent(), is(KEYS));
  }

  @Test
  public void componentMetadataConnectionProviderThatFailsOnInit() {
    MetadataResult<ComponentMetadataTypesDescriptor> componentMetadataTypesDescriptorMetadataResult =
            session.resolveComponentMetadata(sourceDeclaration(CONFIG_FAILING_ON_START_CONNECTION_PROVIDER, null, "America", "USA", "SFO"));
    assertThat(componentMetadataTypesDescriptorMetadataResult.isSuccess(), is(false));
    assertThat(componentMetadataTypesDescriptorMetadataResult.getFailures(), hasSize(1));
  }

  @Test
  public void sampleDataConnectionProviderThatFailsOnInit() {
//    expectedException.expect(DeploymentInitException.class);
//    expectedException.expectCause(instanceOf(InitialisationException.class));
//    session.getSampleData(configLessConnectionLessOPDeclaration(CONFIG_FAILING_ON_START_CONNECTION_PROVIDER));
  }

}
