/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.configurationDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.connectionDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.connectionDeclarationWithResource;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.app.declaration.api.fluent.ArtifactDeclarer;

import java.util.Optional;

import org.junit.Test;

public class DeclarationSessionWithResourcesFailureTestCase extends DeclarationSessionTestCase {

  private String nonExistentResourcePath = "nonexistent/file.txt";

  @Override
  protected Optional<Pair<String, byte[]>> getResource() {
    return of(new Pair("connection/secrets/keyStore.jks", "someValue".getBytes()));
  }

  protected void declareArtifact(ArtifactDeclarer artifactDeclarer) {
    artifactDeclarer.withGlobalElement(configurationDeclaration(CONFIG_NAME, getResource()
        .map(resource -> connectionDeclarationWithResource(CLIENT_NAME, nonExistentResourcePath, empty()))
        .orElse(connectionDeclaration(CLIENT_NAME))));
  }

  @Test
  public void declaringIncorrectResourceShouldFail() {
    ConnectionValidationResult connectionValidationResult = session.testConnection(CONFIG_NAME);
    assertThat(connectionValidationResult.isValid(), is(false));
    assertThat(connectionValidationResult.getMessage(), equalTo("Could not access resource: " + nonExistentResourcePath));
  }
}
