/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.ast;

import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.ast.internal.serialization.ArtifactAstSerializerFactory.JSON;
import static org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider.getExtensionModel;
import static org.mule.runtime.core.api.extension.provider.RuntimeExtensionModelProvider.discoverRuntimeExtensionModels;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.test.allure.AllureConstants.ArtifactAst.ARTIFACT_AST;
import static org.mule.test.allure.AllureConstants.ArtifactAst.ParameterAst.PARAMETER_AST;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;

import static org.junit.Assert.fail;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.serialization.ArtifactAstDeserializer;
import org.mule.runtime.ast.api.serialization.ArtifactAstSerializer;
import org.mule.runtime.ast.api.serialization.ArtifactAstSerializerProvider;
import org.mule.runtime.ast.api.xml.AstXmlParser;
import org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider;
import org.mule.runtime.module.extension.internal.loader.java.DefaultJavaExtensionModelLoader;
import org.mule.runtime.module.extension.internal.manager.DefaultExtensionManager;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.test.runner.infrastructure.ExtensionsTestInfrastructureDiscoverer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(ARTIFACT_AST)
@Story(PARAMETER_AST)
@RunWith(Parameterized.class)
public abstract class BaseParameterAstTestCase extends AbstractMuleContextTestCase {

  private static List<ExtensionModel> runtimeExtensionModels;

  @BeforeClass
  public static void beforeClass() throws Exception {
    runtimeExtensionModels = new ArrayList<>(discoverRuntimeExtensionModels());
  }

  @Parameters(name = "serialize: {0}; populateGenerationInformation: {1}")
  public static List<Object[]> params() {
    return asList(new Object[][] {
        {false, true},
        {true, false},
        {true, true}});
  }

  private final boolean serialize;
  private final boolean populateGenerationInformation;

  private DefaultExtensionManager extensionManager;

  public BaseParameterAstTestCase(boolean serialize, boolean populateGenerationInformation) {
    this.serialize = serialize;
    this.populateGenerationInformation = populateGenerationInformation;
  }

  @Before
  public void before() throws Exception {
    extensionManager = new DefaultExtensionManager();
    muleContext.setExtensionManager(extensionManager);
    initialiseIfNeeded(extensionManager, muleContext);
  }

  protected ArtifactAst buildArtifactAst(final String configFile, final Class... extensions) {
    ExtensionsTestInfrastructureDiscoverer discoverer = new ExtensionsTestInfrastructureDiscoverer(extensionManager);

    DefaultJavaExtensionModelLoader extensionModelLoader = new DefaultJavaExtensionModelLoader();
    Set<ExtensionModel> dependencies = new HashSet<>(singleton(getExtensionModel()));
    for (Class<?> annotatedClass : extensions) {
      dependencies.add(discoverer.discoverExtension(annotatedClass, extensionModelLoader, getDefault(dependencies)));
    }

    ArtifactAst parsedAst = AstXmlParser.builder()
        .withExtensionModels(muleContext.getExtensionManager().getExtensions())
        .withExtensionModels(runtimeExtensionModels)
        .withSchemaValidationsDisabled()
        .build()
        .parse(this.getClass().getClassLoader().getResource("ast/" + configFile));

    if (isSerialize()) {
      try {
        return serializeAndDeserialize(parsedAst);
      } catch (IOException e) {
        fail(e.getMessage());
        return null;
      }
    } else {
      return parsedAst;
    }
  }

  protected Optional<ComponentAst> findComponent(Stream<ComponentAst> stream, String componentIdentifier, String componentId) {
    return findComponent(stream, ComponentIdentifier.buildFromStringRepresentation(componentIdentifier), componentId);
  }

  protected Optional<ComponentAst> findComponent(Stream<ComponentAst> stream, ComponentIdentifier identifier,
                                                 String componentId) {
    return stream
        .filter(componentAst -> identifier.equals(componentAst.getIdentifier())
            && componentId.equals(componentAst.getComponentId().orElse(null)))
        .findFirst();
  }

  protected Optional<ComponentAst> findComponent(Stream<ComponentAst> stream, String componentIdentifier) {
    return findComponent(stream, ComponentIdentifier.buildFromStringRepresentation(componentIdentifier));
  }

  protected Optional<ComponentAst> findComponent(Stream<ComponentAst> stream, ComponentIdentifier identifier) {
    return stream
        .filter(componentAst -> identifier.equals(componentAst.getIdentifier()))
        .findFirst();
  }

  protected Optional<ComponentAst> findComponentByComponentId(Stream<ComponentAst> stream, String componentId) {
    return stream.filter(c -> componentId.equals(c.getComponentId().orElse(null))).findFirst();
  }

  private ArtifactAst serializeAndDeserialize(ArtifactAst artifactAst) throws IOException {
    ArtifactAstSerializer jsonArtifactAstSerializer = new ArtifactAstSerializerProvider().getSerializer(JSON, "1.0");
    InputStream inputStream = jsonArtifactAstSerializer.serialize(artifactAst);

    ArtifactAstDeserializer defaultArtifactAstDeserializer =
        new ArtifactAstSerializerProvider().getDeserializer(isPopulateGenerationInformation());
    ArtifactAst deserializedArtifactAst = defaultArtifactAstDeserializer
        .deserialize(inputStream, name -> artifactAst.dependencies().stream()
            .filter(x -> x.getName().equals(name))
            .findFirst()
            .orElse(null));

    return deserializedArtifactAst;
  }

  public boolean isSerialize() {
    return serialize;
  }

  public boolean isPopulateGenerationInformation() {
    return populateGenerationInformation;
  }
}
