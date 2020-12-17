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
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.configurationDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.configurationDeclarationMissingRequiredParameter;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.connectionDeclaration;

import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.app.declaration.api.fluent.ArtifactDeclarer;

import org.junit.Test;

public class DslConnectionElementDeclarationTestingTestCase extends DeclarationSessionTestCase {

  private static final String CONFIG_CONNECTION_MISSING_REQUIRED_PARAM_NAME = "CONFIG_CONNECTION_MISSING_REQUIRED_PARAM_NAME";

  @Override
  protected void declareArtifact(ArtifactDeclarer artifactDeclarer) {
    super.declareArtifact(artifactDeclarer);
    artifactDeclarer.withGlobalElement(configurationDeclaration(CONFIG_CONNECTION_MISSING_REQUIRED_PARAM_NAME,
                                                                connectionDeclaration(CLIENT_NAME, CLIENT_NAME, null)));
  }

  @Test
  public void testConnectionMissingRequiredParameterOnConnectionProvider() {
    ConnectionValidationResult connectionValidationResult = session.testConnection(CONFIG_CONNECTION_MISSING_REQUIRED_PARAM_NAME);
    assertThat(connectionValidationResult.isValid(), equalTo(false));
    assertThat(connectionValidationResult.getMessage(),
               containsString("RequiredParameterNotSetException: Parameter 'acting-parameter' is required but was not found"));
  }

}
