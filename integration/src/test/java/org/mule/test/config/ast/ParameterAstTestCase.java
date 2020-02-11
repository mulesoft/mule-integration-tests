/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.ast;

import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsArrayContaining.hasItemInArray;
import static org.hamcrest.collection.IsArrayContainingInOrder.arrayContaining;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mule.metadata.api.utils.MetadataTypeUtils.getTypeId;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.FLOW_IDENTIFIER;
import static org.mule.runtime.core.api.construct.Flow.INITIAL_STATE_STARTED;
import static org.mule.runtime.core.api.construct.Flow.INITIAL_STATE_STOPPED;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.dsl.api.xml.parser.XmlConfigurationDocumentLoader.noValidationDocumentLoader;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isMap;

import org.mule.extension.db.internal.DbConnector;
import org.mule.extension.http.internal.temporary.HttpConnector;
import org.mule.extension.socket.api.SocketsExtension;
import org.mule.metadata.api.annotation.EnumAnnotation;
import org.mule.metadata.api.annotation.IntAnnotation;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.NumberType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.component.ComponentIdentifier;
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
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.heisenberg.extension.model.RecursivePojo;
import org.mule.test.heisenberg.extension.model.Weapon;
import org.mule.test.runner.infrastructure.ExtensionsTestInfrastructureDiscoverer;
import org.mule.test.subtypes.extension.SubTypesMappingConnector;
import org.mule.test.vegan.extension.VeganExtension;

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

  private static final String NAME = "name";

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
    for (Class<?> annotatedClass : new Class[] {HttpConnector.class, SocketsExtension.class, DbConnector.class,
        HeisenbergExtension.class, SubTypesMappingConnector.class, VeganExtension.class}) {
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
                                            false);
  }

  @Test
  public void recursivePojoOperationParameter() {
    Optional<ComponentAst> optionalFlowRecursivePojo = artifactAst.topLevelComponentsStream()
        .filter(componentAst -> componentAst.getIdentifier().equals(FLOW_IDENTIFIER) &&
            "recursivePojo".equals(componentAst.getComponentId().orElse(null)))
        .findFirst();
    assertThat(optionalFlowRecursivePojo, not(empty()));

    ComponentAst heisenbergApprove = optionalFlowRecursivePojo.map(flow -> flow.directChildrenStream().findFirst().get())
        .orElseThrow(() -> new AssertionError("Couldn't find heisenberg approve operation"));

    ComponentParameterAst recursivePojoParameter = heisenbergApprove.getParameter("recursivePojo");
    ComponentAst recursivePojo = (ComponentAst) recursivePojoParameter.getValue().getRight();
    assertThat(recursivePojo, not(nullValue()));

    ComponentParameterAst recursivePojoNextParameter = recursivePojo.getParameter("next");
    assertThat(getTypeId(recursivePojoNextParameter.getModel().getType()), equalTo(of(RecursivePojo.class.getName())));
    assertThat(recursivePojoNextParameter.getValue().getRight(), is(nullValue()));

    ComponentParameterAst recursivePojoChildsParameter = recursivePojo.getParameter("childs");
    assertThat(recursivePojoChildsParameter.getModel().getType(), instanceOf(ArrayType.class));
    assertThat(getTypeId(((ArrayType) recursivePojoChildsParameter.getModel().getType()).getType()),
               equalTo(of(RecursivePojo.class.getName())));
    assertThat(recursivePojoChildsParameter.getValue().getRight(), not(nullValue()));

    ComponentAst childRecursivePojo = ((List<ComponentAst>) recursivePojoChildsParameter.getValue().getRight()).stream()
        .findFirst().orElseThrow(() -> new AssertionError("Couldn't find child declaration"));
    ComponentParameterAst childRecursivePojoNextParameter = childRecursivePojo.getParameter("next");
    assertThat(getTypeId(childRecursivePojoNextParameter.getModel().getType()), equalTo(of(RecursivePojo.class.getName())));
    ComponentAst childRecursivePojoNext = (ComponentAst) childRecursivePojoNextParameter.getValue().getRight();
    assertThat(childRecursivePojoNext, not(nullValue()));

    ComponentParameterAst childRecursivePojoNextMappedChildsParameter = childRecursivePojoNext.getParameter("mappedChilds");
    assertThat(childRecursivePojoNextMappedChildsParameter.getModel().getType(), instanceOf(ObjectType.class));
    Optional<MetadataType> openRestrictionChildRecursivePojoNextMappedChildsParameter =
        ((ObjectType) childRecursivePojoNextMappedChildsParameter.getModel().getType()).getOpenRestriction();
    assertThat(openRestrictionChildRecursivePojoNextMappedChildsParameter, not(empty()));
    assertThat(getTypeId(openRestrictionChildRecursivePojoNextMappedChildsParameter.get()),
               equalTo(of(RecursivePojo.class.getName())));
    // TODO DslSyntax is missing containedElements when recursive-pojo/next/recursive-pojo is used... componentModel should reuse
    // "parser" definition for types...
    //assertThat(childRecursivePojoNextMappedChildsParameter.getValue().getRight(), not(nullValue()));

    ComponentParameterAst recursivePojoMappedChildsParameter = recursivePojo.getParameter("mappedChilds");
    assertThat(recursivePojoMappedChildsParameter.getModel().getType(), instanceOf(ObjectType.class));
    Optional<MetadataType> openRestrictionRecursivePojoMappedChildsParameter =
        ((ObjectType) recursivePojoMappedChildsParameter.getModel().getType()).getOpenRestriction();
    assertThat(openRestrictionRecursivePojoMappedChildsParameter, not(empty()));
    assertThat(getTypeId(openRestrictionRecursivePojoMappedChildsParameter.get()),
               equalTo(of(RecursivePojo.class.getName())));
    assertThat(recursivePojoMappedChildsParameter.getValue().getRight(), not(nullValue()));

    List<ComponentAst> recursivePojoMappedChilds = (List<ComponentAst>) recursivePojoMappedChildsParameter.getValue().getRight();

    ComponentAst recursivePojoMappedChild = recursivePojoMappedChilds.stream().findFirst().get();
    ParameterizedModel recursivePojoMappedChildModel = recursivePojoMappedChild.getModel(ParameterizedModel.class)
        .orElseThrow(() -> new AssertionError("Model is missing for mapped-childs"));
    ParameterModel keyParameterModel =
        recursivePojoMappedChildModel.getAllParameterModels().stream().filter(paramModel -> paramModel.getName().equals("key"))
            .findFirst().orElseThrow(() -> new AssertionError("mapped-childs model is missing key parameter"));
    assertThat(getTypeId(keyParameterModel.getType()), equalTo(of(String.class.getName())));
    ParameterModel valueParameterModel =
        recursivePojoMappedChildModel.getAllParameterModels().stream().filter(paramModel -> paramModel.getName().equals("value"))
            .findFirst().orElseThrow(() -> new AssertionError("mapped-childs model is missing key parameter"));
    assertThat(getTypeId(valueParameterModel.getType()), equalTo(of(RecursivePojo.class.getName())));
  }

  @Test
  public void mapListOfSimpleValueType() {
    ComponentAst heisenbergConfig = getHeisenbergConfiguration();

    ComponentParameterAst deathsBySeasonsParam = heisenbergConfig.getParameter("deathsBySeasons");
    assertThat(isMap(deathsBySeasonsParam.getModel().getType()), is(true));
    Optional<MetadataType> optionalOpenRestriction =
        ((ObjectType) deathsBySeasonsParam.getModel().getType()).getOpenRestriction();
    assertThat(optionalOpenRestriction, not(empty()));
    assertThat(optionalOpenRestriction.get(), instanceOf(ArrayType.class));
    assertThat(getTypeId(((ArrayType) optionalOpenRestriction.get()).getType()), equalTo(of(String.class.getName())));

    List<ComponentAst> deathsBySeasons = (List<ComponentAst>) deathsBySeasonsParam.getValue().getRight();
    assertThat(deathsBySeasons, hasSize(1));

    ComponentAst deathBySeason = deathsBySeasons.stream().findFirst().get();
    ComponentParameterAst keyParameter = deathBySeason.getParameter("key");
    assertThat(keyParameter.getValue().getRight(), is("s01"));
    ComponentParameterAst valueParameter = deathBySeason.getParameter("value");
    List<ComponentAst> values = (List<ComponentAst>) valueParameter.getValue().getRight();
    assertThat(values, hasSize(2));

    assertThat(values.get(0).getParameter("value").getValue().getRight(), is("emilio"));
    assertThat(values.get(1).getParameter("value").getValue().getRight(), is("domingo"));
  }

  @Test
  public void mapListOfComplexValueType() {
    ComponentAst heisenbergConfig = getHeisenbergConfiguration();

    ComponentParameterAst weaponValueMapsParam = heisenbergConfig.getParameter("weaponValueMap");
    assertThat(isMap(weaponValueMapsParam.getModel().getType()), is(true));
    Optional<MetadataType> optionalOpenRestriction =
        ((ObjectType) weaponValueMapsParam.getModel().getType()).getOpenRestriction();
    assertThat(optionalOpenRestriction, not(empty()));
    assertThat(getTypeId(optionalOpenRestriction.get()), equalTo(of(Weapon.class.getName())));

    List<ComponentAst> weaponValueMaps = (List<ComponentAst>) weaponValueMapsParam.getValue().getRight();
    assertThat(weaponValueMaps, hasSize(2));

    ComponentAst weaponValueMap = weaponValueMaps.stream().findFirst().get();
    ComponentParameterAst keyParameter = weaponValueMap.getParameter("key");
    assertThat(keyParameter.getValue().getRight(), is("first"));
    ComponentParameterAst valueParameter = weaponValueMap.getParameter("value");
    ComponentAst ricinValue = (ComponentAst) valueParameter.getValue().getRight();
    assertThat(ricinValue.getParameter("microgramsPerKilo").getValue().getRight(), is(Long.valueOf(22)));
    ComponentAst destination = (ComponentAst) ricinValue.getParameter("destination").getValue().getRight();
    assertThat(destination, not(nullValue()));
    assertThat(destination.getParameter("victim").getValue().getRight(), equalTo("Lidia"));
    assertThat(destination.getParameter("address").getValue().getRight(), equalTo("Stevia coffe shop"));

    weaponValueMap = weaponValueMaps.stream().skip(1).findFirst().get();
    keyParameter = weaponValueMap.getParameter("key");
    assertThat(keyParameter.getValue().getRight(), is("second"));
    valueParameter = weaponValueMap.getParameter("value");
    ComponentAst revolver = (ComponentAst) valueParameter.getValue().getRight();
    assertThat(revolver.getParameter("name").getValue().getRight(), is("sledgeHammer's"));
    assertThat(revolver.getParameter("bullets").getValue().getRight(), is(1));
  }

  @Test
  public void mapSimpleValueType() {
    ComponentAst heisenbergConfig = getHeisenbergConfiguration();

    ComponentParameterAst recipesParam = heisenbergConfig.getParameter("recipe");
    assertThat(isMap(recipesParam.getModel().getType()), is(true));
    Optional<MetadataType> optionalOpenRestriction = ((ObjectType) recipesParam.getModel().getType()).getOpenRestriction();
    assertThat(optionalOpenRestriction, not(empty()));
    assertThat(optionalOpenRestriction.get(), instanceOf(NumberType.class));

    List<ComponentAst> recipes = (List<ComponentAst>) recipesParam.getValue().getRight();
    assertThat(recipes, hasSize(3));

    ComponentAst recipe = recipes.stream().findFirst().get();
    ComponentParameterAst keyParameter = recipe.getParameter("key");
    assertThat(keyParameter.getValue().getRight(), is("methylamine"));
    ComponentParameterAst valueParameter = recipe.getParameter("value");
    assertThat(valueParameter.getValue().getRight(), is(Long.valueOf(75)));
  }

  private ComponentAst getHeisenbergConfiguration() {
    Optional<ComponentAst> optionalHeisenbergConfig = artifactAst.topLevelComponentsStream()
        .filter(componentAst -> componentAst.getIdentifier()
            .equals(ComponentIdentifier.buildFromStringRepresentation("heisenberg:config")) &&
            "heisenberg".equals(componentAst.getComponentId().orElse(null)))
        .findFirst();
    assertThat(optionalHeisenbergConfig, not(empty()));

    return optionalHeisenbergConfig.get();
  }

  @Test
  public void listSimpleValueType() {
    ComponentAst heisenbergConfig = getHeisenbergConfiguration();

    ComponentParameterAst enemiesParam = heisenbergConfig.getParameter("enemies");
    List<ComponentAst> enemies = (List<ComponentAst>) enemiesParam.getValue().getRight();
    assertThat(enemies, not(nullValue()));
    assertThat(enemies, hasSize(2));

    assertThat(enemies.get(0).getParameter("value").getValue().getRight(), equalTo("Gustavo Fring"));
    assertThat(enemies.get(1).getParameter("value").getValue().getRight(), equalTo("Hank"));
  }

  @Test
  public void simpleParameters() {
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
  public void wrappedElementSimpleMapType() {
    Optional<ComponentAst> optionalDbConfig = artifactAst.topLevelComponentsStream()
        .filter(componentAst -> componentAst.getIdentifier().getNamespace().equals("db") &&
            componentAst.getIdentifier().getName().equals("config") &&
            "dbConfig".equals(componentAst.getComponentId().orElse(null)))
        .findFirst();
    assertThat(optionalDbConfig, not(empty()));
    ComponentAst dbConfig = optionalDbConfig.get();

    Optional<ComponentAst> optionalConnectionProvider =
        dbConfig.recursiveStream().filter(inner -> inner.getModel(ConnectionProviderModel.class).isPresent())
            .findFirst();
    assertThat(optionalConnectionProvider, not(empty()));

    ComponentAst connectionProvider = optionalConnectionProvider.get();
    ComponentParameterAst connectionPropertiesParameterAst = connectionProvider.getParameter("connectionProperties");
    assertThat(connectionPropertiesParameterAst.getModel().getType(), instanceOf(ObjectType.class));
    Optional<MetadataType> openRestrictionTypeForConnectionPropertiesParameter =
        ((ObjectType) connectionPropertiesParameterAst.getModel().getType()).getOpenRestriction();
    assertThat(openRestrictionTypeForConnectionPropertiesParameter, not(empty()));
    assertThat(getTypeId(openRestrictionTypeForConnectionPropertiesParameter.get()), equalTo(of(String.class.getName())));
    List<ComponentAst> connectionProperties = (List<ComponentAst>) connectionPropertiesParameterAst.getValue().getRight();

    ComponentAst connectionProperty = connectionProperties.stream().findFirst()
        .orElseThrow(() -> new AssertionError("Couldn't find connection property entry"));

    ParameterizedModel connectionPropertyModel = connectionProperty.getModel(ParameterizedModel.class)
        .orElseThrow(() -> new AssertionError("Model is missing for connection-properties"));
    ParameterModel keyParameterModel =
        connectionPropertyModel.getAllParameterModels().stream().filter(paramModel -> paramModel.getName().equals("key"))
            .findFirst().orElseThrow(() -> new AssertionError("connection-properties model is missing key parameter"));
    assertThat(getTypeId(keyParameterModel.getType()), equalTo(of(String.class.getName())));
    ParameterModel valueParameterModel =
        connectionPropertyModel.getAllParameterModels().stream().filter(paramModel -> paramModel.getName().equals("value"))
            .findFirst().orElseThrow(() -> new AssertionError("connection-properties model is missing key parameter"));
    assertThat(getTypeId(valueParameterModel.getType()), equalTo(of(String.class.getName())));

    assertThat(connectionProperty.getParameter("key").getValue().getRight(), equalTo("first"));
    assertThat(connectionProperty.getParameter("value").getValue().getRight(), equalTo("propertyOne"));

    connectionProperty = connectionProperties.stream().skip(1).findFirst()
        .orElseThrow(() -> new AssertionError("Couldn't find connection property entry"));

    keyParameterModel =
        connectionPropertyModel.getAllParameterModels().stream().filter(paramModel -> paramModel.getName().equals("key"))
            .findFirst().orElseThrow(() -> new AssertionError("connection-properties model is missing key parameter"));
    assertThat(getTypeId(keyParameterModel.getType()), equalTo(of(String.class.getName())));
    valueParameterModel =
        connectionPropertyModel.getAllParameterModels().stream().filter(paramModel -> paramModel.getName().equals("value"))
            .findFirst().orElseThrow(() -> new AssertionError("connection-properties model is missing key parameter"));
    assertThat(getTypeId(valueParameterModel.getType()), equalTo(of(String.class.getName())));
    assertThat(connectionProperty.getParameter("key").getValue().getRight(), equalTo("second"));

    assertThat(connectionProperty.getParameter("value").getValue().getRight(), equalTo("propertyTwo"));
  }

  @Test
  public void wrappedElementArrayType() {
    Optional<ComponentAst> optionalHttpListenerConfig = artifactAst.topLevelComponentsStream()
        .filter(componentAst -> componentAst.getIdentifier().getNamespace().equals("http") &&
            componentAst.getIdentifier().getName().equals("listener-config") &&
            "HTTP_Listener_config".equals(componentAst.getComponentId().orElse(null)))
        .findFirst();
    assertThat(optionalHttpListenerConfig, not(empty()));

    ComponentAst httpListenerConfig = optionalHttpListenerConfig.get();
    assertThat(httpListenerConfig.getModel(ConfigurationModel.class), not(empty()));

    assertThat(httpListenerConfig.getComponentId(), not(empty()));
    ComponentParameterAst nameComponentParameter = httpListenerConfig.getParameter(NAME);
    assertThat(nameComponentParameter.getValue().getRight(), equalTo(httpListenerConfig.getComponentId().get()));

    Optional<ComponentAst> optionalConnectionProvider =
        httpListenerConfig.recursiveStream().filter(inner -> inner.getModel(ConnectionProviderModel.class).isPresent())
            .findFirst();
    assertThat(optionalConnectionProvider, not(empty()));

    ComponentAst connectionProvider = optionalConnectionProvider.get();
    ComponentParameterAst tlsContextParameter = connectionProvider.getParameter("tlsContext");
    assertThat(tlsContextParameter.getValue().getLeft(), nullValue());
    assertThat(tlsContextParameter.getValue().getRight(), equalTo("listenerTlsContext"));

    ComponentParameterAst basePathParameter = httpListenerConfig.getParameter("basePath");
    assertThat(basePathParameter.getValue().getLeft(), nullValue());
    assertThat(basePathParameter.getValue().getRight(), equalTo("/api"));

    ComponentParameterAst listenerInterceptorsParameter = httpListenerConfig.getParameter("listenerInterceptors");
    assertThat(listenerInterceptorsParameter.getValue().getLeft(), nullValue());
    ComponentAst listenerInterceptors = (ComponentAst) listenerInterceptorsParameter.getValue().getRight();

    ComponentParameterAst corsInterceptorParameter = listenerInterceptors.getParameter("corsInterceptor");
    assertThat(corsInterceptorParameter.getValue().getLeft(), nullValue());
    ComponentAst corsInterceptor = (ComponentAst) corsInterceptorParameter.getValue().getRight();

    ComponentParameterAst allowCredentialsParameter = corsInterceptor.getParameter("allowCredentials");
    assertThat(allowCredentialsParameter.getValue().getLeft(), is(nullValue()));
    assertThat(allowCredentialsParameter.getValue().getRight(), is(TRUE));

    ComponentParameterAst originsParameter = corsInterceptor.getParameter("origins");
    assertThat(originsParameter.getValue().getLeft(), is(nullValue()));
    assertThat(originsParameter.getValue().getRight(), not(nullValue()));

    List<ComponentAst> origins = (List<ComponentAst>) originsParameter.getValue().getRight();
    ComponentAst origin = origins.stream().findFirst().get();
    ComponentParameterAst originUrlParameter = origin.getParameter("url");
    assertThat(originUrlParameter.getValue().getLeft(), nullValue());
    assertThat(originUrlParameter.getValue().getRight(), is("http://www.the-origin-of-time.com"));
    ComponentParameterAst originAccessControlMaxAgeParameter = origin.getParameter("accessControlMaxAge");
    assertThat(originAccessControlMaxAgeParameter.getValue().getLeft(), nullValue());
    assertThat(originAccessControlMaxAgeParameter.getValue().getRight(), is(30l));

    assertParameters(origin, "allowedMethods", "methodName", "POST", "PUT", "GET");
    assertParameters(origin, "allowedHeaders", "headerName", "x-allow-origin", "x-yet-another-valid-header");
    assertParameters(origin, "exposeHeaders", "headerName", "x-forwarded-for");

    origin = origins.stream().skip(1).findFirst().get();
    originUrlParameter = origin.getParameter("url");
    assertThat(originUrlParameter.getValue().getLeft(), nullValue());
    assertThat(originUrlParameter.getValue().getRight(), is("http://www.the-origin-of-life.com"));
    originAccessControlMaxAgeParameter = origin.getParameter("accessControlMaxAge");
    assertThat(originAccessControlMaxAgeParameter.getValue().getLeft(), nullValue());
    assertThat(originAccessControlMaxAgeParameter.getValue().getRight(), is(60l));

    assertParameters(origin, "allowedMethods", "methodName", "POST", "GET");
    assertParameters(origin, "allowedHeaders", "headerName", "x-allow-origin");
    assertParameters(origin, "exposeHeaders", "headerName", "x-forwarded-for");
  }

  private void assertParameters(ComponentAst container, String containerParameterName, String elementParameterName,
                                String... rightValues) {
    ComponentParameterAst containerParameter = container.getParameter(containerParameterName);
    assertThat(containerParameter.getValue().getLeft(), nullValue());
    assertThat(containerParameter.getValue().getRight(), not(nullValue()));

    List<ComponentAst> elementComponents = (List<ComponentAst>) containerParameter.getValue().getRight();
    String[] actual = elementComponents.stream().map(componentAst -> {
      ComponentParameterAst elementParameter = componentAst.getParameter(elementParameterName);
      assertThat(elementParameter.getValue().getLeft(), nullValue());
      return (String) elementParameter.getValue().getRight();
    }).toArray(String[]::new);
    assertThat(actual, arrayContaining(rightValues));
  }

}
