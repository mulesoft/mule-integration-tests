/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.configurationDeclarationMissingRequiredParameter;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.connectionDeclaration;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.app.declaration.api.fluent.ArtifactDeclarer;

import org.junit.Test;

public class DslConfigurationElementDeclarationTestingTestCase extends DeclarationSessionTestCase {

  private static String CONFIG_MISSING_PARAMETERS_NAME = "configMissingParameters";

  @Override
  protected void declareArtifact(ArtifactDeclarer artifactDeclarer) {
    super.declareArtifact(artifactDeclarer);
    artifactDeclarer.withGlobalElement(configurationDeclarationMissingRequiredParameter(CONFIG_MISSING_PARAMETERS_NAME,
                                                                                        connectionDeclaration(CLIENT_NAME)));
  }

  @Test
  public void testConnectionMissingRequiredParameterOnConfiguration() {
    ConnectionValidationResult connectionValidationResult = session.testConnection(CONFIG_MISSING_PARAMETERS_NAME);
    assertThat(connectionValidationResult.isValid(), equalTo(false));
    assertThat(connectionValidationResult.getMessage(),
               containsString("RequiredParameterNotSetException: Parameter 'not-acting-parameter' is required but was not found"));
  }

}
