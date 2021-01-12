/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.ast;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.util.Optional.of;
import static org.apache.commons.lang3.ArrayUtils.addAll;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.dsl.api.xml.parser.XmlConfigurationDocumentLoader.noValidationDocumentLoader;
import static org.mule.test.allure.AllureConstants.ArtifactAst.ARTIFACT_AST;
import static org.mule.test.allure.AllureConstants.ArtifactAst.ParameterAst.PARAMETER_AST;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Before;
import org.mule.extension.http.internal.temporary.HttpConnector;
import org.mule.extension.socket.api.SocketsExtension;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.api.dsl.model.ComponentBuildingDefinitionRegistry;
import org.mule.runtime.config.api.dsl.processor.ArtifactConfig;
import org.mule.runtime.config.internal.model.ApplicationModel;
import org.mule.runtime.core.api.extension.RuntimeExtensionModelProvider;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.runtime.core.api.registry.SpiServiceRegistry;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider;
import org.mule.runtime.dsl.api.xml.XmlNamespaceInfoProvider;
import org.mule.runtime.dsl.api.xml.parser.ConfigFile;
import org.mule.runtime.dsl.api.xml.parser.ConfigLine;
import org.mule.runtime.dsl.internal.xml.parser.XmlApplicationParser;
import org.mule.runtime.module.extension.api.loader.java.DefaultJavaExtensionModelLoader;
import org.mule.runtime.module.extension.internal.config.ExtensionBuildingDefinitionProvider;
import org.mule.runtime.module.extension.internal.manager.DefaultExtensionManager;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.runner.infrastructure.ExtensionsTestInfrastructureDiscoverer;
import org.mule.test.vegan.extension.VeganExtension;
import org.w3c.dom.Document;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Feature(ARTIFACT_AST)
@Story(PARAMETER_AST)
public abstract class AbstractParameterAstTestCase extends AbstractMuleContextTestCase {

  protected ArtifactAst artifactAst;

  protected abstract boolean runtimeMode();

  protected abstract String getConfig();

  protected abstract Class[] getExtensions();

  @Before
  public void before() throws Exception {
    ArtifactConfig.Builder artifactConfigBuilder = new ArtifactConfig.Builder();

    URL resource = this.getClass().getClassLoader().getResource(getConfig());

    Optional<ConfigLine> configLine;
    ServiceRegistry serviceRegistry = new SpiServiceRegistry();
    try (InputStream configFileStream = resource.openStream()) {
      Document document =
          noValidationDocumentLoader().loadDocument(SAXParserFactory::newInstance, "config", configFileStream,
                                                    new DefaultHandler());

      ImmutableList.Builder<XmlNamespaceInfoProvider> namespaceInfoProvidersBuilder = ImmutableList.builder();
      namespaceInfoProvidersBuilder
          .addAll(serviceRegistry.lookupProviders(XmlNamespaceInfoProvider.class, currentThread().getContextClassLoader()));
      ImmutableList<XmlNamespaceInfoProvider> xmlNamespaceInfoProviders = namespaceInfoProvidersBuilder.build();

      XmlApplicationParser xmlApplicationParser = new XmlApplicationParser(xmlNamespaceInfoProviders);
      configLine = xmlApplicationParser.parse(document.getDocumentElement());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    artifactConfigBuilder.addConfigFile(new ConfigFile(resource.getFile(), Collections.singletonList(
                                                                                                     configLine
                                                                                                         .orElseThrow(() -> new IllegalArgumentException(format("Failed to parse %s.",
                                                                                                                                                                resource))))));

    ArtifactConfig artifactConfig = artifactConfigBuilder.build();

    List<ExtensionModel> runtimeExtensionModels = new ArrayList<>();
    Collection<RuntimeExtensionModelProvider> runtimeExtensionModelProviders = new SpiServiceRegistry()
        .lookupProviders(RuntimeExtensionModelProvider.class, Thread.currentThread().getContextClassLoader());
    for (RuntimeExtensionModelProvider runtimeExtensionModelProvider : runtimeExtensionModelProviders) {
      runtimeExtensionModels.add(runtimeExtensionModelProvider.createExtensionModel());
    }

    DefaultExtensionManager extensionManager = new DefaultExtensionManager();
    muleContext.setExtensionManager(extensionManager);
    initialiseIfNeeded(extensionManager, muleContext);

    ExtensionsTestInfrastructureDiscoverer discoverer = new ExtensionsTestInfrastructureDiscoverer(extensionManager);

    DefaultJavaExtensionModelLoader extensionModelLoader = new DefaultJavaExtensionModelLoader();

    Class[] extensions =
        new Class[] {HttpConnector.class, SocketsExtension.class, HeisenbergExtension.class, VeganExtension.class};
    for (Class<?> annotatedClass : addAll(extensions, getExtensions())) {
      discoverer.discoverExtension(annotatedClass, extensionModelLoader);
    }

    ImmutableSet<ExtensionModel> extensionModels = ImmutableSet.<ExtensionModel>builder()
        .addAll(muleContext.getExtensionManager().getExtensions())
        .addAll(runtimeExtensionModels)
        .build();

    final ComponentBuildingDefinitionRegistry componentBuildingDefinitionRegistry =
        new ComponentBuildingDefinitionRegistry();
    serviceRegistry
        .lookupProviders(ComponentBuildingDefinitionProvider.class, ComponentBuildingDefinitionProvider.class.getClassLoader())
        .forEach(componentBuildingDefinitionProvider -> {
          if (componentBuildingDefinitionProvider instanceof ExtensionBuildingDefinitionProvider) {
            // Ignore extensions building definition provider, we have to test this works fine without parsers
          }
          componentBuildingDefinitionProvider.init();
          componentBuildingDefinitionProvider.getComponentBuildingDefinitions()
              .forEach(componentBuildingDefinitionRegistry::register);
        });

    this.artifactAst = new ApplicationModel(artifactConfig, null, extensionModels, Collections.emptyMap(),
                                            Optional.empty(), of(componentBuildingDefinitionRegistry),
                                            uri -> muleContext.getExecutionClassLoader().getResourceAsStream(uri),
                                            runtimeMode(), getFeatureFlaggingService());
  }

  protected Optional<ComponentAst> findComponent(Stream<ComponentAst> stream, String componentIdentifier, String componentId) {
    return findComponent(stream, ComponentIdentifier.buildFromStringRepresentation(componentIdentifier), componentId);
  }

  protected Optional<ComponentAst> findComponent(Stream<ComponentAst> stream, ComponentIdentifier componentIdentifier,
                                                 String componentId) {
    return stream
        .filter(c -> componentIdentifier.equals(c.getIdentifier()) && componentId.equals(c.getComponentId().orElse(null)))
        .findFirst();
  }

  protected Optional<ComponentAst> findComponent(Stream<ComponentAst> stream, String identifier) {
    return findComponent(stream, ComponentIdentifier.buildFromStringRepresentation(identifier));
  }

  protected Optional<ComponentAst> findComponent(Stream<ComponentAst> stream, ComponentIdentifier identifier) {
    return stream.filter(componentAst -> identifier.equals(componentAst.getIdentifier())).findFirst();
  }

  protected Optional<ComponentAst> findComponentByComponentId(Stream<ComponentAst> stream, String componentId) {
    return stream.filter(c -> componentId.equals(c.getComponentId().orElse(null))).findFirst();
  }
}
