/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.test;

import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.configurationDeclarationMissingRequiredParameter;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.connectionDeclaration;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.rules.ExpectedException.none;

import org.mule.runtime.app.declaration.api.fluent.ArtifactDeclarer;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.deployment.model.api.DeploymentInitException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DslConfigurationElementDeclarationTestingTestCase extends DeclarationSessionTestCase {

  private static String CONFIG_MISSING_PARAMETERS_NAME = "configMissingParameters";

  @Rule
  public ExpectedException expectedException = none();

  @Override
  protected void declareArtifact(ArtifactDeclarer artifactDeclarer) {
    super.declareArtifact(artifactDeclarer);
    artifactDeclarer.withGlobalElement(configurationDeclarationMissingRequiredParameter(CONFIG_MISSING_PARAMETERS_NAME,
                                                                                        connectionDeclaration(CLIENT_NAME)));
  }

  @Test
  public void testConnectionMissingRequiredParameterOnConfiguration() {
    expectedException.expect(DeploymentInitException.class);
    expectedException.expectCause(instanceOf(ConfigurationException.class));
    expectedException.expectMessage("[unknown:-1]: Element <tst:config> is missing required parameter 'notActingParameter'.");
    session.testConnection(CONFIG_MISSING_PARAMETERS_NAME);
  }

}
