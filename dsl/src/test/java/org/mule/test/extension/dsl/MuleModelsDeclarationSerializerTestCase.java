/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.extension.dsl;

import static java.lang.Thread.currentThread;
import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.ast.api.DependencyResolutionMode.MINIMAL;
import static org.mule.runtime.core.api.util.IOUtils.getResourceAsString;

import org.mule.runtime.api.security.Authentication;
import org.mule.runtime.api.security.SecurityException;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.ast.api.DependencyResolutionMode;
import org.mule.runtime.config.api.dsl.ArtifactDeclarationXmlSerializer;
import org.mule.runtime.core.api.security.AbstractSecurityProvider;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class MuleModelsDeclarationSerializerTestCase extends AbstractElementModelTestCase {

  @Rule
  public SystemProperty minimalDependencies =
      new SystemProperty(SYSTEM_PROPERTY_PREFIX + DependencyResolutionMode.class.getName(), MINIMAL.name());

  private ArtifactDeclarationXmlSerializer serializer;

  @Override
  protected String[] getConfigFiles() {
    return new String[] {};
  }

  @Before
  public void setup() throws Exception {
    super.setup();
    serializer = ArtifactDeclarationXmlSerializer.getDefault(dslContext);
  }

  @Test
  public void loadAndSerializeAppWithParallelForEach() throws Exception {
    String appXmlFile = "app-with-parallel-foreach.xml";
    ArtifactDeclaration artifact = loadApplicationDeclaration(appXmlFile);
    assertApplicationArtifactSerialization(appXmlFile, artifact);
  }

  @Test
  public void loadAndSerializeAppWithAsync() throws Exception {
    String appXmlFile = "app-with-async.xml";
    ArtifactDeclaration artifact = loadApplicationDeclaration(appXmlFile);
    assertApplicationArtifactSerialization(appXmlFile, artifact);
  }

  @Test
  public void loadAndSerializeAppWithUntilSuccessful() throws Exception {
    String appXmlFile =
        "app-with-until-successful.xml";
    ArtifactDeclaration artifact = loadApplicationDeclaration(appXmlFile);
    assertApplicationArtifactSerialization(appXmlFile, artifact);
  }

  @Test
  public void loadAndSerializeAppWithTry() throws Exception {
    String appXmlFile = "app-with-try.xml";
    ArtifactDeclaration artifact = loadApplicationDeclaration(appXmlFile);
    assertApplicationArtifactSerialization(appXmlFile, artifact);
  }

  @Test
  public void loadAndSerializeAppWithErrorHandler() throws Exception {
    String appXmlFile = "app-with-error-handler.xml";
    ArtifactDeclaration artifact = loadApplicationDeclaration(appXmlFile);
    assertApplicationArtifactSerialization(appXmlFile, artifact);
  }

  @Test
  public void loadAndSerializeAppWithObject() throws Exception {
    String appXmlFile = "app-with-object.xml";
    ArtifactDeclaration artifact = loadApplicationDeclaration(appXmlFile);
    assertApplicationArtifactSerialization(appXmlFile, artifact);
  }

  @Test
  public void loadAndSerializeAppWithSecurityManager() throws Exception {
    String appXmlFile = "app-with-security-manager.xml";
    ArtifactDeclaration artifact = loadApplicationDeclaration(appXmlFile);
    assertApplicationArtifactSerialization(appXmlFile, artifact);
  }

  @Test
  public void loadAndSerializeAppWithChoice() throws Exception {
    String appXmlFile = "app-with-choice.xml";
    ArtifactDeclaration artifact = loadApplicationDeclaration(appXmlFile);
    assertApplicationArtifactSerialization(appXmlFile, artifact);
  }

  @Test
  public void loadAndSerializeAppWithForEach() throws Exception {
    String appXmlFile = "app-with-foreach.xml";
    ArtifactDeclaration artifact = loadApplicationDeclaration(appXmlFile);
    assertApplicationArtifactSerialization(appXmlFile, artifact);
  }

  @Test
  public void loadAndSerializeAppWithFirstSuccessful() throws Exception {
    String appXmlFile = "app-with-first-successful.xml";
    ArtifactDeclaration artifact = loadApplicationDeclaration(appXmlFile);
    assertApplicationArtifactSerialization(appXmlFile, artifact);
  }

  @Test
  public void loadAndSerializeAppWithRoundRobin() throws Exception {
    String appXmlFile = "app-with-round-robin.xml";
    ArtifactDeclaration artifact = loadApplicationDeclaration(appXmlFile);
    assertApplicationArtifactSerialization(appXmlFile, artifact);
  }

  @Test
  public void loadAndSerializeAppWithScatterGather() throws Exception {
    String appXmlFile = "app-with-scatter-gather.xml";
    ArtifactDeclaration artifact = loadApplicationDeclaration(appXmlFile);
    assertApplicationArtifactSerialization(appXmlFile, artifact);
  }

  @Test
  public void loadAndSerializeAppWithSubflow() throws Exception {
    String appXmlFile = "app-with-subflow.xml";
    ArtifactDeclaration artifact = loadApplicationDeclaration(appXmlFile);
    assertApplicationArtifactSerialization(appXmlFile, artifact);
  }

  private String getResourceContent(String xmlFile) throws IOException {
    return getResourceAsString(xmlFile, getClass());
  }

  private ArtifactDeclaration loadApplicationDeclaration(String appXmlFile) {
    InputStream appIs = currentThread().getContextClassLoader().getResourceAsStream(appXmlFile);
    return serializer.deserialize(appIs);
  }

  private void assertApplicationArtifactSerialization(String appXmlFile, ArtifactDeclaration artifact) throws Exception {
    String expectedAppXml = getResourceContent(appXmlFile);
    String serializationResult = serializer.serialize(artifact);
    compareXML(expectedAppXml, serializationResult);
  }

  public static class TestSecurityProvider extends AbstractSecurityProvider {

    public TestSecurityProvider() {
      this("test-security-provider");
    }

    public TestSecurityProvider(String name) {
      super(name);
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws SecurityException {
      return null;
    }
  }
}
