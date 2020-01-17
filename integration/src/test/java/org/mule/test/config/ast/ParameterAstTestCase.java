/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.ast;

import static java.lang.Thread.currentThread;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsArrayContaining.hasItemInArray;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.FLOW_IDENTIFIER;
import static org.mule.runtime.core.api.construct.Flow.INITIAL_STATE_STARTED;
import static org.mule.runtime.core.api.construct.Flow.INITIAL_STATE_STOPPED;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.dsl.api.xml.parser.XmlConfigurationDocumentLoader.noValidationDocumentLoader;

import org.mule.extension.http.internal.temporary.HttpConnector;
import org.mule.extension.socket.api.SocketsExtension;
import org.mule.metadata.api.annotation.EnumAnnotation;
import org.mule.metadata.api.annotation.IntAnnotation;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.construct.ConstructModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
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
import org.mule.test.runner.infrastructure.ExtensionsTestInfrastructureDiscoverer;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.xml.parsers.SAXParserFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.helpers.DefaultHandler;

public class ParameterAstTestCase extends AbstractMuleContextTestCase {

  public static final String NAME = "name";
  private ArtifactAst artifactAst;

  @Before
  public void before() throws Exception {
    ArtifactConfig.Builder artifactConfigBuilder = new ArtifactConfig.Builder();

    URL resource = this.getClass().getClassLoader().getResource("org/mule/test/config/ast/parameters-test-config.xml");

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
                                                                                                         .orElseThrow(() -> new IllegalArgumentException(String
                                                                                                             .format("Failed to parse %s.",
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
    for (Class<?> annotatedClass : new Class[] {HttpConnector.class, SocketsExtension.class}) {
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
                                            uri -> muleContext.getExecutionClassLoader().getResourceAsStream(uri));

  }

  @Test
  public void constructParameters() {
    Optional<ComponentAst> optionalFlowParameters = artifactAst.topLevelComponentsStream()
        .filter(componentAst -> componentAst.getIdentifier().equals(FLOW_IDENTIFIER) &&
            "flowParameters".equals(componentAst.getComponentId().orElse(null)))
        .findFirst();
    assertThat(optionalFlowParameters, not(empty()));

    ComponentAst componentAst = optionalFlowParameters.get();
    Optional<ConstructModel> optionalConstructModel = componentAst.getModel(ConstructModel.class);
    assertThat(optionalConstructModel, not(empty()));

    ConstructModel constructModel = optionalConstructModel.get();

    ComponentParameterAst componentParameterAst = componentAst.getParameter("initialState");
    assertThat(componentParameterAst.getRawValue(), equalTo("stopped"));
    assertThat(findParameterModel(constructModel, componentParameterAst), not(empty()));
    String[] values = (String[]) componentParameterAst.getModel().getType().getAnnotation(EnumAnnotation.class).get().getValues();
    assertThat(values, allOf(hasItemInArray(INITIAL_STATE_STARTED), hasItemInArray(INITIAL_STATE_STOPPED)));

    componentParameterAst = componentAst.getParameter("maxConcurrency");
    assertThat(findParameterModel(constructModel, componentParameterAst), not(empty()));
    assertThat(componentParameterAst.getModel().getType().getAnnotation(IntAnnotation.class), not(empty()));

    assertThat(componentAst.getComponentId(), not(empty()));
    componentParameterAst = componentAst.getParameter(NAME);
    assertThat(findParameterModel(constructModel, componentParameterAst), not(empty()));
    assertThat(componentParameterAst.getValue().getRight(), equalTo(componentAst.getComponentId().get()));
  }

  private Optional<ParameterModel> findParameterModel(ParameterizedModel constructModel,
                                                      ComponentParameterAst componentParameterAst) {
    return constructModel.getAllParameterModels().stream()
        .filter(parameterModel -> parameterModel.equals(componentParameterAst.getModel())).findFirst();
  }

  @Test
  public void configurationParameters() {
    Optional<ComponentAst> optionalHttpListenerConfig = artifactAst.topLevelComponentsStream()
        .filter(componentAst -> componentAst.getIdentifier().getName().equals("listener-config") &&
            "HTTP_Listener_config".equals(componentAst.getComponentId().orElse(null)))
        .findFirst();
    assertThat(optionalHttpListenerConfig, not(empty()));

    ComponentAst componentAst = optionalHttpListenerConfig.get();
    assertThat(componentAst.getModel(ConfigurationModel.class), not(empty()));

    assertThat(componentAst.getComponentId(), not(empty()));
    ComponentParameterAst componentParameterAst = componentAst.getParameter(NAME);
    assertThat(componentParameterAst.getValue().getRight(), equalTo(componentAst.getComponentId().get()));

    Optional<ComponentAst> opetionalConnectionProviderAst =
        componentAst.recursiveStream().filter(inner -> inner.getModel(ConnectionProviderModel.class).isPresent()).findFirst();
    assertThat(opetionalConnectionProviderAst, not(empty()));

    ComponentAst connectionProviderAst = opetionalConnectionProviderAst.get();
    assertThat(connectionProviderAst.getParameter("tlsContext").getValue().getRight(), equalTo("listenerTlsContext"));

    componentParameterAst = componentAst.getParameter("basePath");
    assertThat(componentParameterAst.getValue().getRight(), nullValue());

    componentParameterAst = componentAst.getParameter("listenerInterceptors");
    assertThat(componentParameterAst.getValue().getRight(), not(nullValue()));

    ComponentAst nestedComponentAst = (ComponentAst) componentParameterAst.getValue().getRight();
    // TODO should be accessed as parameter!
    ComponentParameterAst nestedComponentParameterAst = nestedComponentAst.getParameter("corsInterceptor");
    assertThat(nestedComponentParameterAst.getValue().getRight(), nullValue());
    //assertThat(nestedComponentParameterAst.getValue().getLeft(), is());

    ComponentAst other =
        nestedComponentAst.recursiveStream().findFirst().orElseThrow(() -> new AssertionError("Should have a nested parameter"));
  }

}
