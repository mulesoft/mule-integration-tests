/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.ast;

import static java.lang.Thread.currentThread;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.test.allure.AllureConstants.ArtifactAst.ARTIFACT_AST;
import static org.mule.test.allure.AllureConstants.ArtifactAst.ParameterAst.PARAMETER_AST;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.xml.AstXmlParser;
import org.mule.runtime.core.api.extension.RuntimeExtensionModelProvider;
import org.mule.runtime.core.api.registry.SpiServiceRegistry;
import org.mule.runtime.module.extension.api.loader.java.DefaultJavaExtensionModelLoader;
import org.mule.runtime.module.extension.internal.manager.DefaultExtensionManager;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.test.runner.infrastructure.ExtensionsTestInfrastructureDiscoverer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.BeforeClass;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(ARTIFACT_AST)
@Story(PARAMETER_AST)
public abstract class BaseParameterAstTestCase extends AbstractMuleContextTestCase {

  private static List<ExtensionModel> runtimeExtensionModels;
  private DefaultExtensionManager extensionManager;

  @BeforeClass
  public static void beforeClass() throws Exception {
    runtimeExtensionModels = new ArrayList<>();
    Collection<RuntimeExtensionModelProvider> runtimeExtensionModelProviders = new SpiServiceRegistry()
        .lookupProviders(RuntimeExtensionModelProvider.class, currentThread().getContextClassLoader());
    for (RuntimeExtensionModelProvider runtimeExtensionModelProvider : runtimeExtensionModelProviders) {
      runtimeExtensionModels.add(runtimeExtensionModelProvider.createExtensionModel());
    }
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
    for (Class<?> annotatedClass : extensions) {
      discoverer.discoverExtension(annotatedClass, extensionModelLoader);
    }

    return AstXmlParser.builder()
        .withExtensionModels(muleContext.getExtensionManager().getExtensions())
        .withExtensionModels(runtimeExtensionModels)
        .withSchemaValidationsDisabled()
        .build()
        .parse(this.getClass().getClassLoader().getResource("ast/" + configFile));
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

}
