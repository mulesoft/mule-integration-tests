/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.rules.ExpectedException.none;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.actingParameterOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.configLessConnectionLessOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.configLessOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.configurationDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.failingOnInitConnectionDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.sourceDeclaration;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.app.declaration.api.ComponentElementDeclaration;
import org.mule.runtime.app.declaration.api.fluent.ArtifactDeclarer;
import org.mule.runtime.deployment.model.api.DeploymentInitException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ConnectionProviderFailingOnInitTestingTestCase extends DeclarationSessionTestCase {

  private static final String CONFIG_FAILING_ON_INIT_CONNECTION_PROVIDER = "configNameFailingOnInitConnectionProvider";

  @Rule
  public ExpectedException expectedException = none();

  @Override
  protected void declareArtifact(ArtifactDeclarer artifactDeclarer) {
    artifactDeclarer.withGlobalElement(configurationDeclaration(CONFIG_FAILING_ON_INIT_CONNECTION_PROVIDER,
                                                                failingOnInitConnectionDeclaration()));
  }

  @Test
  public void testConnectionProviderThatFailsOnInit() {
    expectedException.expect(DeploymentInitException.class);
    expectedException.expectCause(instanceOf(InitialisationException.class));
    session.testConnection(CONFIG_FAILING_ON_INIT_CONNECTION_PROVIDER);
  }

  @Test
  public void valuesConnectionProviderThatFailsOnInit() {
    expectedException.expect(DeploymentInitException.class);
    expectedException.expectCause(instanceOf(InitialisationException.class));
    session.getValues(actingParameterOPDeclaration(CONFIG_FAILING_ON_INIT_CONNECTION_PROVIDER, "actingParameter"), PROVIDED_PARAMETER_NAME);
  }

  @Test
  public void metadataKeysConnectionProviderThatFailsOnInit() {
    expectedException.expect(DeploymentInitException.class);
    expectedException.expectCause(instanceOf(InitialisationException.class));
    session.getMetadataKeys(configLessOPDeclaration(CONFIG_FAILING_ON_INIT_CONNECTION_PROVIDER));
  }

  @Test
  public void componentMetadataConnectionProviderThatFailsOnInit() {
    expectedException.expect(DeploymentInitException.class);
    expectedException.expectCause(instanceOf(InitialisationException.class));
    session.resolveComponentMetadata(sourceDeclaration(CONFIG_FAILING_ON_INIT_CONNECTION_PROVIDER, null, "America", "USA", "SFO"));
  }

  @Test
  public void sampleDataConnectionProviderThatFailsOnInit() {
    expectedException.expect(DeploymentInitException.class);
    expectedException.expectCause(instanceOf(InitialisationException.class));
    session.getSampleData(configLessConnectionLessOPDeclaration(CONFIG_FAILING_ON_INIT_CONNECTION_PROVIDER));
  }

}
