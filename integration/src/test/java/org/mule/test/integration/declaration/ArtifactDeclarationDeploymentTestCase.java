/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.declaration;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newArtifact;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newParameterGroup;
import static org.mule.runtime.app.declaration.api.fluent.ParameterSimpleValue.plain;
import static org.mule.runtime.app.declaration.api.fluent.SimpleValueType.STRING;
import static org.mule.test.allure.AllureConstants.DeploymentConfiguration.ArtifactDeclarationStory.ARTIFACT_DECLARATION;
import static org.mule.test.allure.AllureConstants.DeploymentConfiguration.DEPLOYMENT_CONFIGURATION;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.app.declaration.api.ParameterValue;
import org.mule.runtime.app.declaration.api.fluent.ElementDeclarer;
import org.mule.runtime.app.declaration.api.fluent.SimpleValueType;
import org.mule.test.AbstractIntegrationTestCase;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(DEPLOYMENT_CONFIGURATION)
@Story(ARTIFACT_DECLARATION)
public class ArtifactDeclarationDeploymentTestCase extends AbstractIntegrationTestCase {

  @Override
  protected ArtifactDeclaration getArtifactDeclaration() {
    ElementDeclarer core = ElementDeclarer.forExtension("mule");

    return newArtifact()
        .withGlobalElement(core.newConstruct("flow").withRefName("testFlow")
            .withComponent(core.newOperation("setPayload")
                .withParameterGroup(newParameterGroup()
                    .withParameter("value", createStringParameter("#[\"foo\"]")).getDeclaration())
                .getDeclaration())
            .getDeclaration())
        .getDeclaration();
  }

  @Test
  public void testFlowExecutionUsingArtifactDeclaration() throws Exception {
    assertThat(flowRunner("testFlow").run().getMessage().getPayload().getValue(), equalTo("foo"));
  }


  private static ParameterValue createStringParameter(String value) {
    return createParameter(value, STRING);
  }

  private static ParameterValue createParameter(String value, SimpleValueType type) {
    return plain(value, type);
  }

}
