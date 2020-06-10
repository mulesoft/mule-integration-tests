/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.extension.dsl;

import static java.util.Arrays.asList;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newArtifact;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newParameterGroup;
import static org.mule.runtime.extension.internal.ocs.OCSConstants.OCS_ENABLED;

import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.app.declaration.api.fluent.ElementDeclarer;

import java.util.Collection;

import org.junit.runners.Parameterized;

public class OcsArtifactDeclarationSerializerTestCase extends ArtifactDeclarationSerializerTestCase {

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    System.setProperty(OCS_ENABLED, "true");
    return asList(new Object[][] {
        {"ocs-artifact-config-dsl-app.xml", createOcsArtifactDeclaration(), "ocs-artifact-config-dsl-app.json"},
    });
  }

  private static ArtifactDeclaration createOcsArtifactDeclaration() {
    ElementDeclarer oauth = ElementDeclarer.forExtension("Test OAuth Extension");
    return newArtifact()
        .withGlobalElement(oauth.newConfiguration("mixed")
            .withRefName("oauth-platform")
            .withConnection(oauth.newConnection("platformManagedOauth")
                .withParameterGroup(newParameterGroup()
                    .withParameter("connectionId",
                                   createStringParameter("ocs:348573-495273958273-924852945/salesforce/john-sfdc-1k87kmjt"))
                    .getDeclaration())
                .getDeclaration())
            .getDeclaration())
        .getDeclaration();
  }

}
