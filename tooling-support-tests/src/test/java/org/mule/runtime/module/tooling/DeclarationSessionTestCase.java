/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mule.maven.client.test.MavenTestUtils.getMavenProperty;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newArtifact;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.TEST_EXTENSION_DECLARER;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.configurationDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.connectionDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.failingConnectionDeclaration;
import static org.mule.test.infrastructure.maven.MavenTestUtils.getMavenLocalRepository;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.value.ResolvingFailure;
import org.mule.runtime.api.value.ValueResult;
import org.mule.runtime.app.declaration.api.ConstructElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterizedElementDeclaration;
import org.mule.runtime.app.declaration.api.fluent.ArtifactDeclarer;
import org.mule.runtime.app.declaration.api.fluent.ElementDeclarer;
import org.mule.runtime.module.tooling.api.artifact.DeclarationSession;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.infrastructure.deployment.AbstractFakeMuleServerTestCase;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.After;
import org.junit.ClassRule;
import org.junit.Rule;

public abstract class DeclarationSessionTestCase extends AbstractFakeMuleServerTestCase {

  private static final String WEAVE_SERVICE_LOCATION_PROPERTY = "weave.service.location";
  private static final File WEAVE_SERVICE_LOCATION;

  static {
    try {
      File targetFolder =
          new File(DeclarationSessionTestCase.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
      String weaveServiceLocation = getMavenProperty(WEAVE_SERVICE_LOCATION_PROPERTY, targetFolder::getParentFile);
      WEAVE_SERVICE_LOCATION = new File(targetFolder, weaveServiceLocation);
    } catch (URISyntaxException e) {
      throw new RuntimeException("Could not find Weave Service Location", e);
    }
  }

  protected static final String EXTENSION_GROUP_ID = "org.mule.tooling";
  protected static final String EXTENSION_ARTIFACT_ID = "tooling-support-test-extension";
  protected static final String EXTENSION_VERSION = getToolingSupportTestExtensionVersion();
  protected static final String EXTENSION_CLASSIFIER = "mule-plugin";
  protected static final String EXTENSION_TYPE = "jar";
  protected static final String EXTENSION_VERSION_MAVEN_PROPERTY_NAME = "testExtensionVersion";

  protected static final String CONFIG_NAME = "dummyConfig";
  protected static final String CONFIG_FAILING_CONNECTION_PROVIDER = "configNameFailingConnectionProvider";

  protected static final String CLIENT_NAME = "client";
  protected static final String PROVIDED_PARAMETER_NAME = "providedParameter";
  protected static final String ERROR_PROVIDED_PARAMETER_NAME = "errorProvidedParameter";
  protected static final String INTERNAL_ERROR_PROVIDED_PARAMETER_NAME = "internalErrorProvidedParameter";
  protected static final String WITH_ACTING_PARAMETER = "WITH-ACTING-PARAMETER-";

  protected DeclarationSession session;

  @ClassRule
  public static SystemProperty artifactsLocation =
      new SystemProperty("mule.test.maven.artifacts.dir", DeclarationSession.class.getResource("/").getPath());

  @Rule
  public SystemProperty repositoryLocation =
      new SystemProperty("muleRuntimeConfig.maven.repositoryLocation", getMavenLocalRepository().getAbsolutePath());

  @Override
  public void setUp() throws Exception {
    ArtifactDeclarer artifactDeclarer = newArtifact();
    declareArtifact(artifactDeclarer);
    super.setUp();
    this.session = this.muleServer
        .toolingService()
        .newDeclarationSessionBuilder()
        .addDependency(EXTENSION_GROUP_ID,
                       EXTENSION_ARTIFACT_ID,
                       EXTENSION_VERSION,
                       EXTENSION_CLASSIFIER,
                       EXTENSION_TYPE)
        .setArtifactDeclaration(artifactDeclarer.getDeclaration())
        .build();
    this.muleServer.start();
  }

  @Override
  protected boolean addExpressionLanguageMetadataService() {
    return false;
  }

  @Override
  protected File getExpressionLanguageService() throws IOException {
    return WEAVE_SERVICE_LOCATION;
  }

  protected void declareArtifact(ArtifactDeclarer artifactDeclarer) {
    artifactDeclarer.withGlobalElement(configurationDeclaration(CONFIG_NAME, connectionDeclaration(CLIENT_NAME)));
    artifactDeclarer.withGlobalElement(configurationDeclaration(CONFIG_FAILING_CONNECTION_PROVIDER,
                                                                failingConnectionDeclaration()));

  }

  @After
  public void disposeSession() {
    if (session != null) {
      session.dispose();
    }
  }

  protected void validateValuesSuccess(DeclarationSession session,
                                       ParameterizedElementDeclaration elementDeclaration,
                                       String parameterName,
                                       String expectedValue) {
    ValueResult providerResult = getValueResult(session, elementDeclaration, parameterName);
    assertThat(providerResult.isSuccess(), equalTo(true));
    assertThat(providerResult.getValues(), hasSize(1));
    assertThat(providerResult.getValues().iterator().next().getId(), is(expectedValue));
  }

  protected ValueResult getValueResult(DeclarationSession session, ParameterizedElementDeclaration elementDeclaration,
                                       String parameterName) {
    return session.getValues(elementDeclaration, parameterName);
  }

  protected void validateValuesFailure(DeclarationSession session,
                                       ParameterizedElementDeclaration elementDeclaration,
                                       String parameterName,
                                       String message,
                                       String code,
                                       String... reason) {
    ValueResult providerResult = getValueResult(session, elementDeclaration, parameterName);
    assertThat(providerResult.isSuccess(), is(false));
    assertThat(providerResult.getFailure().isPresent(), is(true));
    final ResolvingFailure failure = providerResult.getFailure().get();
    assertThat(failure.getFailureCode(), is(code));
    assertThat(failure.getMessage(), is(message));
    if (reason.length > 0) {
      assertThat(failure.getReason(), containsString(reason[0]));
    }
  }

  protected ConstructElementDeclaration invalidComponentDeclaration(String component) {
    return TEST_EXTENSION_DECLARER.newConstruct(component).getDeclaration();
  }

  protected ConstructElementDeclaration invalidExtensionModel(String invalidExtensionModel) {
    return ElementDeclarer.forExtension(invalidExtensionModel).newConstruct("invalid").getDeclaration();
  }

  private static String getToolingSupportTestExtensionVersion() {
    return getMavenProperty(EXTENSION_VERSION_MAVEN_PROPERTY_NAME, () -> {
      try {
        return new File(DeclarationSessionTestCase.class.getProtectionDomain().getCodeSource().getLocation().toURI())
            .getParentFile().getParentFile();
      } catch (URISyntaxException e) {
        throw new MuleRuntimeException(e);
      }
    });
  }

}
