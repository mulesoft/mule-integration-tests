/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.context.notification.processors;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.FLOW;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.OPERATION;
import static org.mule.runtime.api.component.TypedComponentIdentifier.builder;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.api.util.collection.Collectors.toImmutableList;
import static org.mule.runtime.config.api.XmlConfigurationDocumentLoader.noValidationDocumentLoader;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.FLOW_IDENTIFIER;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.createDefaultExtensionManager;
import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.notification.MessageProcessorNotification;
import org.mule.runtime.config.api.dsl.processor.ArtifactConfig;
import org.mule.runtime.config.api.dsl.processor.ConfigFile;
import org.mule.runtime.config.api.dsl.processor.ConfigLine;
import org.mule.runtime.config.api.dsl.processor.xml.XmlApplicationParser;
import org.mule.runtime.config.api.dsl.xml.StaticXmlNamespaceInfo;
import org.mule.runtime.config.api.dsl.xml.StaticXmlNamespaceInfoProvider;
import org.mule.runtime.config.internal.model.ApplicationModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.registry.SpiServiceRegistry;
import org.mule.runtime.dsl.api.component.config.DefaultComponentLocation;
import org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.DefaultLocationPart;
import org.mule.runtime.dsl.api.xml.XmlNamespaceInfo;
import org.mule.runtime.dsl.api.xml.XmlNamespaceInfoProvider;
import org.mule.runtime.extension.api.loader.xml.XmlExtensionModelLoader;
import org.mule.test.AbstractIntegrationTestCase;

import com.google.common.collect.ImmutableList;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.qameta.allure.junit4.DisplayName;

import org.junit.After;
import org.junit.Test;
import org.w3c.dom.Document;

@DisplayName("XML Connectors Path generation")
public class ModuleComponentPathTestCase extends AbstractIntegrationTestCase {

  private static final String COLON_SEPARATOR = ":";
  private static final String MODULE_SIMPLE_XML = "module-simple.xml";
  private static final String MODULE_SIMPLE_PROXY_XML = "module-simple-proxy.xml";
  private static final String FLOWS_USING_MODULE_SIMPLE_XML = "flows-using-modules.xml";
  private static final String BASE_PATH_XML_MODULES = "org/mule/test/integration/notifications/modules/";

  @Override
  protected String getConfigFile() {
    return CONFIG_FILE_NAME.get();
  }

  private static final Optional<String> CONFIG_FILE_NAME = of(BASE_PATH_XML_MODULES + FLOWS_USING_MODULE_SIMPLE_XML);
  private static final Optional<String> MODULE_SIMPLE_FILE_NAME = of(BASE_PATH_XML_MODULES + MODULE_SIMPLE_XML);
  private static final Optional<String> MODULE_SIMPLE_PROXY_FILE_NAME = of(BASE_PATH_XML_MODULES + MODULE_SIMPLE_PROXY_XML);
  private static final Optional<TypedComponentIdentifier> FLOW_TYPED_COMPONENT_IDENTIFIER =
      of(builder().identifier(FLOW_IDENTIFIER).type(FLOW).build());

  private static final DefaultComponentLocation getFlowLocation(final String flowName, final int flowLineNumber) {
    return new DefaultComponentLocation(of(flowName), asList(new DefaultLocationPart(flowName, FLOW_TYPED_COMPONENT_IDENTIFIER,
                                                                                     CONFIG_FILE_NAME, of(flowLineNumber))));
  }

  private static final String FLOW_WITH_SINGLE_MP_NAME = "flowWithSingleMp";
  private static final String FLOW_WITH_SET_PAYLOAD_HARDCODED_NAME = "flowWithSetPayloadHardcoded";
  private static final String FLOW_WITH_SET_PAYLOAD_TWO_TIMES_NAME = "flowWithSetPayloadTwoTimes";
  private static final String FLOW_WITH_SET_PAYLOAD_HARDCODED_TWICE_NAME = "flowWithSetPayloadHardcodedTwice";
  private static final String FLOW_WITH_SET_PAYLOAD_PARAM_VALUE_NAME = "flowWithSetPayloadParamValue";
  private static final String FLOW_WITH_SET_PAYLOAD_TWO_TIMES_TWICE_NAME = "flowWithSetPayloadTwoTimesTwice";
  private static final String FLOW_WITH_PROXY_SET_PAYLOAD_HARDCODED_NAME = "flowWithProxySetPayloadHardcoded";
  private static final String FLOW_WITH_PROXY_SET_PAYLOAD_HARDCODED_AND_LOGGER_NAME = "flowWithProxySetPayloadHardcodedAndLogger";
  private static final String FLOW_WITH_PROXY_AND_SIMPLE_MODULE_AND_LOGGER_NAME = "flowWithProxyAndSimpleModuleAndLogger";
  private static final String FLOW_WITH_PROXY_AND_SIMPLE_MODULE_AND_LOGGER_REVERSE_NAME =
      "flowWithProxyAndSimpleModuleAndLoggerReverse";

  /**
   * "flows-using-modules.xml" flows defined below
   */
  private static final DefaultComponentLocation FLOW_WITH_SINGLE_MP_LOCATION =
      getFlowLocation(FLOW_WITH_SINGLE_MP_NAME, 15);
  private static final DefaultComponentLocation FLOW_WITH_SET_PAYLOAD_HARDCODED =
      getFlowLocation(FLOW_WITH_SET_PAYLOAD_HARDCODED_NAME, 19);
  private static final DefaultComponentLocation FLOW_WITH_SET_PAYLOAD_HARDCODED_TWICE =
      getFlowLocation(FLOW_WITH_SET_PAYLOAD_HARDCODED_TWICE_NAME, 23);
  private static final DefaultComponentLocation FLOW_WITH_SET_PAYLOAD_PARAM_VALUE =
      getFlowLocation(FLOW_WITH_SET_PAYLOAD_PARAM_VALUE_NAME, 28);
  private static final DefaultComponentLocation FLOW_WITH_SET_PAYLOAD_TWO_TIMES =
      getFlowLocation(FLOW_WITH_SET_PAYLOAD_TWO_TIMES_NAME, 32);
  private static final DefaultComponentLocation FLOW_WITH_SET_PAYLOAD_TWO_TIMES_TWICE =
      getFlowLocation(FLOW_WITH_SET_PAYLOAD_TWO_TIMES_TWICE_NAME, 36);
  private static final DefaultComponentLocation FLOW_WITH_PROXY_SET_PAYLOAD_HARDCODED =
      getFlowLocation(FLOW_WITH_PROXY_SET_PAYLOAD_HARDCODED_NAME, 41);
  private static final DefaultComponentLocation FLOW_WITH_PROXY_SET_PAYLOAD_HARDCODED_AND_LOGGER =
      getFlowLocation(FLOW_WITH_PROXY_SET_PAYLOAD_HARDCODED_AND_LOGGER_NAME, 45);
  private static final DefaultComponentLocation FLOW_WITH_PROXY_AND_SIMPLE_MODULE_AND_LOGGER =
      getFlowLocation(FLOW_WITH_PROXY_AND_SIMPLE_MODULE_AND_LOGGER_NAME, 49);
  private static final DefaultComponentLocation FLOW_WITH_PROXY_AND_SIMPLE_MODULE_AND_LOGGER_REVERSE =
      getFlowLocation(FLOW_WITH_PROXY_AND_SIMPLE_MODULE_AND_LOGGER_REVERSE_NAME, 55);

  private static Optional<TypedComponentIdentifier> getModuleOperationIdentifier(final String namespace,
                                                                                 final String identifier) {
    return of(builder()
        .identifier(buildFromStringRepresentation(namespace + COLON_SEPARATOR + identifier))
        .type(OPERATION).build());
  }

  private static DefaultComponentLocation getModuleOperationLocation(final String operationName,
                                                                     final Optional<TypedComponentIdentifier> operationIdentifier,
                                                                     final Optional<String> moduleFilename,
                                                                     final int operationLineNumber) {
    return new DefaultComponentLocation(of(operationName),
                                        asList(new DefaultLocationPart(operationName,
                                                                       operationIdentifier,
                                                                       moduleFilename,
                                                                       of(operationLineNumber))));
  }

  /**
   * "module-simple" operations defined below
   */
  private static final String MODULE_SIMPLE_NAMESPACE_IN_APP = "simple-prefix";
  private static final String SET_PAYLOAD_HARDCODED_VALUE_NAME = "set-payload-hardcoded-value";
  private static final Optional<TypedComponentIdentifier> MODULE_SET_PAYLOAD_HARDCODED_VALUE =
      getModuleOperationIdentifier(MODULE_SIMPLE_NAMESPACE_IN_APP, SET_PAYLOAD_HARDCODED_VALUE_NAME);
  private static final DefaultComponentLocation OPERATION_SET_PAYLOAD_HARDCODED_VALUE_FIRST_MP =
      getModuleOperationLocation(SET_PAYLOAD_HARDCODED_VALUE_NAME, MODULE_SET_PAYLOAD_HARDCODED_VALUE, MODULE_SIMPLE_FILE_NAME,
                                 13);

  private static final String SET_PAYLOAD_PARAM_VALUE_NAME = "set-payload-param-value";
  private static final Optional<TypedComponentIdentifier> MODULE_SET_PAYLOAD_PARAM_VALUE =
      getModuleOperationIdentifier(MODULE_SIMPLE_NAMESPACE_IN_APP, SET_PAYLOAD_PARAM_VALUE_NAME);
  private static final DefaultComponentLocation OPERATION_SET_PAYLOAD_PARAM_VALUE_FIRST_MP =
      getModuleOperationLocation(SET_PAYLOAD_PARAM_VALUE_NAME, MODULE_SET_PAYLOAD_PARAM_VALUE, MODULE_SIMPLE_FILE_NAME, 23);

  private static final String SET_PAYLOAD_TWO_TIMES_NAME = "set-payload-two-times";
  private static final Optional<TypedComponentIdentifier> MODULE_SET_PAYLOAD_TWO_TIMES =
      getModuleOperationIdentifier(MODULE_SIMPLE_NAMESPACE_IN_APP, SET_PAYLOAD_TWO_TIMES_NAME);
  private static final DefaultComponentLocation OPERATION_SET_PAYLOAD_TWO_TIMES_FIRST_MP =
      getModuleOperationLocation(SET_PAYLOAD_TWO_TIMES_NAME, MODULE_SET_PAYLOAD_TWO_TIMES, MODULE_SIMPLE_FILE_NAME, 30);
  private static final DefaultComponentLocation OPERATION_SET_PAYLOAD_TWO_TIMES_SECOND_MP =
      getModuleOperationLocation(SET_PAYLOAD_TWO_TIMES_NAME, MODULE_SET_PAYLOAD_TWO_TIMES, MODULE_SIMPLE_FILE_NAME, 31);

  /**
   * "module-simple-proxy" operations defined below
   */
  private static final String MODULE_SIMPLE_PROXY_NAMESPACE_IN_APP = "module-simple-proxy";
  private static final String PROXY_SET_PAYLOAD_NAME = "proxy-set-payload-hardcoded-value";
  private static final Optional<TypedComponentIdentifier> MODULE_PROXY_SET_PAYLOAD =
      getModuleOperationIdentifier(MODULE_SIMPLE_PROXY_NAMESPACE_IN_APP, PROXY_SET_PAYLOAD_NAME);
  private static final DefaultComponentLocation OPERATION_PROXY_SET_PAYLOAD_FIRST_MP =
      getModuleOperationLocation(PROXY_SET_PAYLOAD_NAME, MODULE_PROXY_SET_PAYLOAD, MODULE_SIMPLE_PROXY_FILE_NAME, 13);

  private static final String PROXY_SET_PAYLOAD_AND_LOGGER_NAME = "proxy-set-payload-hardcoded-value-and-logger";
  private static final Optional<TypedComponentIdentifier> MODULE_PROXY_SET_PAYLOAD_AND_LOGGER =
      getModuleOperationIdentifier(MODULE_SIMPLE_PROXY_NAMESPACE_IN_APP, PROXY_SET_PAYLOAD_AND_LOGGER_NAME);
  private static final DefaultComponentLocation OPERATION_PROXY_SET_PAYLOAD_AND_LOGGER_FIRST_MP =
      getModuleOperationLocation(PROXY_SET_PAYLOAD_AND_LOGGER_NAME, MODULE_PROXY_SET_PAYLOAD_AND_LOGGER,
                                 MODULE_SIMPLE_PROXY_FILE_NAME, 20);
  private static final DefaultComponentLocation OPERATION_PROXY_SET_PAYLOAD_AND_LOGGER_SECOND_MP =
      getModuleOperationLocation(PROXY_SET_PAYLOAD_AND_LOGGER_NAME, MODULE_PROXY_SET_PAYLOAD_AND_LOGGER,
                                 MODULE_SIMPLE_PROXY_FILE_NAME, 21);

  /**
   * runtime provided MPs
   */
  private static final Optional<TypedComponentIdentifier> LOGGER =
      of(builder().identifier(buildFromStringRepresentation("mule:logger"))
          .type(OPERATION).build());


  private static final Optional<TypedComponentIdentifier> SET_PAYLOAD =
      of(builder().identifier(buildFromStringRepresentation("mule:set-payload"))
          .type(OPERATION).build());

  final ProcessorNotificationStore listener = new ProcessorNotificationStore();

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    listener.setLogSingleNotification(true);
    muleContext.getNotificationManager().addListener(listener);
  }

  @After
  public void clearNotifications() {
    if (listener != null) {
      listener.getNotifications().clear();
    }
  }

  private Optional<ConfigLine> loadConfigLines(Set<ExtensionModel> extensionModels, InputStream inputStream) {
    List<XmlNamespaceInfoProvider> xmlNamespaceInfoProviders =
        ImmutableList.<XmlNamespaceInfoProvider>builder()
            .add(createStaticNamespaceInfoProviders(extensionModels))
            .addAll(discoverRuntimeXmlNamespaceInfoProvider())
            .build();
    XmlApplicationParser xmlApplicationParser = new XmlApplicationParser(xmlNamespaceInfoProviders);
    Document document = noValidationDocumentLoader().loadDocument("config", inputStream);
    return xmlApplicationParser.parse(document.getDocumentElement());
  }

  private XmlNamespaceInfoProvider createStaticNamespaceInfoProviders(Set<ExtensionModel> extensionModels) {
    List<XmlNamespaceInfo> extensionNamespaces = extensionModels.stream()
        .map(ext -> new StaticXmlNamespaceInfo(ext.getXmlDslModel().getNamespace(), ext.getXmlDslModel().getPrefix()))
        .collect(toImmutableList());

    return new StaticXmlNamespaceInfoProvider(extensionNamespaces);
  }

  private List<XmlNamespaceInfoProvider> discoverRuntimeXmlNamespaceInfoProvider() {
    ImmutableList.Builder namespaceInfoProvidersBuilder = ImmutableList.builder();
    namespaceInfoProvidersBuilder
        .addAll(new SpiServiceRegistry().lookupProviders(XmlNamespaceInfoProvider.class,
                                                         muleContext.getClass().getClassLoader()));
    return namespaceInfoProvidersBuilder.build();
  }

  @Test
  public void validateComponentLocationCreatedFromExtensionModelsWithoutUsingParsers() throws Exception {
    final Set<ExtensionModel> extensionModels = muleContext.getExtensionManager().getExtensions();

    ArtifactConfig.Builder artifactConfigBuilder = new ArtifactConfig.Builder();
    artifactConfigBuilder.addConfigFile(new ConfigFile(CONFIG_FILE_NAME.get(), Collections
        .singletonList(loadConfigLines(extensionModels,
                                       this.getClass().getClassLoader().getResourceAsStream(CONFIG_FILE_NAME.get()))
                                           .orElseThrow(() -> new IllegalArgumentException(String
                                               .format("Failed to parse %s.", CONFIG_FILE_NAME.get()))))));


    ApplicationModel toolingApplicationModel = new ApplicationModel(artifactConfigBuilder.build(), null,
                                                                    extensionModels, emptyMap(),
                                                                    empty(),
                                                                    empty(),
                                                                    false,
                                                                    uri -> {
                                                                      throw new UnsupportedOperationException();
                                                                    });

    List<String> componentLocations = new ArrayList<>();
    toolingApplicationModel.executeOnEveryComponentTree(componentModel -> {
      final String componentIdentifierName = componentModel.getIdentifier().getName();
      if (componentIdentifierName.equals("notification") || componentIdentifierName.equals("notifications")) {
        return;
      }
      final ComponentLocation componentLocation = componentModel.getComponentLocation();
      if (componentLocation != null) {
        componentLocations.add(componentLocation.getLocation());
      }
    });

    assertEquals(ImmutableList.builder()
        .add(Location.builder().globalName(FLOW_WITH_SINGLE_MP_NAME).build().toString())
        .add(Location.builder().globalName(FLOW_WITH_SINGLE_MP_NAME).addProcessorsPart().addIndexPart(0).build().toString())

        .add(Location.builder().globalName(FLOW_WITH_SET_PAYLOAD_HARDCODED_NAME).build().toString())
        .add(Location.builder().globalName(FLOW_WITH_SET_PAYLOAD_HARDCODED_NAME).addProcessorsPart().addIndexPart(0).build()
            .toString())

        .add(Location.builder().globalName(FLOW_WITH_SET_PAYLOAD_HARDCODED_TWICE_NAME).build().toString())
        .add(Location.builder().globalName(FLOW_WITH_SET_PAYLOAD_HARDCODED_TWICE_NAME).addProcessorsPart().addIndexPart(0).build()
            .toString())
        .add(Location.builder().globalName(FLOW_WITH_SET_PAYLOAD_HARDCODED_TWICE_NAME).addProcessorsPart().addIndexPart(1).build()
            .toString())

        .add(Location.builder().globalName(FLOW_WITH_SET_PAYLOAD_PARAM_VALUE_NAME).build().toString())
        .add(Location.builder().globalName(FLOW_WITH_SET_PAYLOAD_PARAM_VALUE_NAME).addProcessorsPart().addIndexPart(0).build()
            .toString())

        .add(Location.builder().globalName(FLOW_WITH_SET_PAYLOAD_TWO_TIMES_NAME).build().toString())
        .add(Location.builder().globalName(FLOW_WITH_SET_PAYLOAD_TWO_TIMES_NAME).addProcessorsPart().addIndexPart(0).build()
            .toString())

        .add(Location.builder().globalName(FLOW_WITH_SET_PAYLOAD_TWO_TIMES_TWICE_NAME).build().toString())
        .add(Location.builder().globalName(FLOW_WITH_SET_PAYLOAD_TWO_TIMES_TWICE_NAME).addProcessorsPart().addIndexPart(0).build()
            .toString())
        .add(Location.builder().globalName(FLOW_WITH_SET_PAYLOAD_TWO_TIMES_TWICE_NAME).addProcessorsPart().addIndexPart(1).build()
            .toString())

        .add(Location.builder().globalName(FLOW_WITH_PROXY_SET_PAYLOAD_HARDCODED_NAME).build().toString())
        .add(Location.builder().globalName(FLOW_WITH_PROXY_SET_PAYLOAD_HARDCODED_NAME).addProcessorsPart().addIndexPart(0).build()
            .toString())


        .add(Location.builder().globalName(FLOW_WITH_PROXY_SET_PAYLOAD_HARDCODED_AND_LOGGER_NAME).build().toString())
        .add(Location.builder().globalName(FLOW_WITH_PROXY_SET_PAYLOAD_HARDCODED_AND_LOGGER_NAME).addProcessorsPart()
            .addIndexPart(0).build().toString())

        .add(Location.builder().globalName(FLOW_WITH_PROXY_AND_SIMPLE_MODULE_AND_LOGGER_NAME).build().toString())
        .add(Location.builder().globalName(FLOW_WITH_PROXY_AND_SIMPLE_MODULE_AND_LOGGER_NAME).addProcessorsPart().addIndexPart(0)
            .build().toString())
        .add(Location.builder().globalName(FLOW_WITH_PROXY_AND_SIMPLE_MODULE_AND_LOGGER_NAME).addProcessorsPart().addIndexPart(1)
            .build().toString())
        .add(Location.builder().globalName(FLOW_WITH_PROXY_AND_SIMPLE_MODULE_AND_LOGGER_NAME).addProcessorsPart().addIndexPart(2)
            .build().toString())

        .add(Location.builder().globalName(FLOW_WITH_PROXY_AND_SIMPLE_MODULE_AND_LOGGER_REVERSE_NAME).build().toString())
        .add(Location.builder().globalName(FLOW_WITH_PROXY_AND_SIMPLE_MODULE_AND_LOGGER_REVERSE_NAME).addProcessorsPart()
            .addIndexPart(0).build().toString())
        .add(Location.builder().globalName(FLOW_WITH_PROXY_AND_SIMPLE_MODULE_AND_LOGGER_REVERSE_NAME).addProcessorsPart()
            .addIndexPart(1).build().toString())
        .add(Location.builder().globalName(FLOW_WITH_PROXY_AND_SIMPLE_MODULE_AND_LOGGER_REVERSE_NAME).addProcessorsPart()
            .addIndexPart(2).build().toString())
        .build(), componentLocations);
  }

  @Test
  public void flowWithSingleMp() throws Exception {
    // simple test to be sure the macro expansion doesn't mess up the a flow that has no modifications
    flowRunner("flowWithSingleMp").run();
    assertNextProcessorLocationIs(FLOW_WITH_SINGLE_MP_LOCATION
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", LOGGER, CONFIG_FILE_NAME, of(16)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithSetPayloadHardcoded() throws Exception {
    flowRunner("flowWithSetPayloadHardcoded").run();
    assertNextProcessorLocationIs(FLOW_WITH_SET_PAYLOAD_HARDCODED
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", MODULE_SET_PAYLOAD_HARDCODED_VALUE, CONFIG_FILE_NAME, of(20)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_HARDCODED_VALUE_FIRST_MP
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(13)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithSetPayloadHardcodedTwice() throws Exception {
    flowRunner("flowWithSetPayloadHardcodedTwice").run();
    assertNextProcessorLocationIs(FLOW_WITH_SET_PAYLOAD_HARDCODED_TWICE
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", MODULE_SET_PAYLOAD_HARDCODED_VALUE, CONFIG_FILE_NAME, of(24)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_HARDCODED_VALUE_FIRST_MP
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(13)));

    assertNextProcessorLocationIs(FLOW_WITH_SET_PAYLOAD_HARDCODED_TWICE
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("1", MODULE_SET_PAYLOAD_HARDCODED_VALUE, CONFIG_FILE_NAME, of(25)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_HARDCODED_VALUE_FIRST_MP
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(13)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithSetPayloadParamValue() throws Exception {
    flowRunner("flowWithSetPayloadParamValue").run();
    assertNextProcessorLocationIs(FLOW_WITH_SET_PAYLOAD_PARAM_VALUE
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", MODULE_SET_PAYLOAD_PARAM_VALUE, CONFIG_FILE_NAME, of(29)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_PARAM_VALUE_FIRST_MP
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(23)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithSetPayloadTwoTimes() throws Exception {
    flowRunner("flowWithSetPayloadTwoTimes").run();
    assertNextProcessorLocationIs(FLOW_WITH_SET_PAYLOAD_TWO_TIMES
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", MODULE_SET_PAYLOAD_TWO_TIMES, CONFIG_FILE_NAME, of(33)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_TWO_TIMES_FIRST_MP
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(30)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_TWO_TIMES_SECOND_MP
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("1", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(31)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithSetPayloadTwoTimesTwice() throws Exception {
    flowRunner("flowWithSetPayloadTwoTimesTwice").run();
    assertNextProcessorLocationIs(FLOW_WITH_SET_PAYLOAD_TWO_TIMES_TWICE
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", MODULE_SET_PAYLOAD_TWO_TIMES, CONFIG_FILE_NAME, of(37)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_TWO_TIMES_FIRST_MP
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(30)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_TWO_TIMES_SECOND_MP
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("1", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(31)));
    // assertion on the second call of the OP
    assertNextProcessorLocationIs(FLOW_WITH_SET_PAYLOAD_TWO_TIMES_TWICE
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("1", MODULE_SET_PAYLOAD_TWO_TIMES, CONFIG_FILE_NAME, of(38)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_TWO_TIMES_FIRST_MP
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(30)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_TWO_TIMES_SECOND_MP
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("1", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(31)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithProxySetPayloadHardcoded() throws Exception {
    flowRunner("flowWithProxySetPayloadHardcoded").run();
    // flow assertion
    assertNextProcessorLocationIs(FLOW_WITH_PROXY_SET_PAYLOAD_HARDCODED
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", MODULE_PROXY_SET_PAYLOAD, CONFIG_FILE_NAME, of(42)));
    assertNextProcessorLocationIs(OPERATION_PROXY_SET_PAYLOAD_FIRST_MP
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", MODULE_SET_PAYLOAD_HARDCODED_VALUE, MODULE_SIMPLE_PROXY_FILE_NAME, of(13)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_HARDCODED_VALUE_FIRST_MP
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(13)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithProxySetPayloadHardcodedAndLogger() throws Exception {
    flowRunner("flowWithProxySetPayloadHardcodedAndLogger").run();
    assertNextProcessorLocationIs(FLOW_WITH_PROXY_SET_PAYLOAD_HARDCODED_AND_LOGGER
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", MODULE_PROXY_SET_PAYLOAD_AND_LOGGER, CONFIG_FILE_NAME, of(46)));
    assertNextProcessorLocationIs(OPERATION_PROXY_SET_PAYLOAD_AND_LOGGER_FIRST_MP
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", MODULE_SET_PAYLOAD_HARDCODED_VALUE, MODULE_SIMPLE_PROXY_FILE_NAME, of(20)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_HARDCODED_VALUE_FIRST_MP
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(13)));
    assertNextProcessorLocationIs(OPERATION_PROXY_SET_PAYLOAD_AND_LOGGER_SECOND_MP
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("1", LOGGER, MODULE_SIMPLE_PROXY_FILE_NAME, of(21)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithProxyAndSimpleModuleAndLogger() throws Exception {
    flowRunner("flowWithProxyAndSimpleModuleAndLogger").run();

    // first MP from within the flow
    assertNextProcessorLocationIs(FLOW_WITH_PROXY_AND_SIMPLE_MODULE_AND_LOGGER
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", MODULE_PROXY_SET_PAYLOAD_AND_LOGGER, CONFIG_FILE_NAME, of(50)));
    assertNextProcessorLocationIs(OPERATION_PROXY_SET_PAYLOAD_AND_LOGGER_FIRST_MP
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", MODULE_SET_PAYLOAD_HARDCODED_VALUE, MODULE_SIMPLE_PROXY_FILE_NAME, of(20)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_HARDCODED_VALUE_FIRST_MP
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(13)));
    assertNextProcessorLocationIs(OPERATION_PROXY_SET_PAYLOAD_AND_LOGGER_SECOND_MP
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("1", LOGGER, MODULE_SIMPLE_PROXY_FILE_NAME, of(21)));

    // second MP from within the flow
    assertNextProcessorLocationIs(FLOW_WITH_PROXY_AND_SIMPLE_MODULE_AND_LOGGER
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("1", MODULE_SET_PAYLOAD_HARDCODED_VALUE, CONFIG_FILE_NAME, of(51)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_HARDCODED_VALUE_FIRST_MP
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(13)));
    // third MP from within the flow
    assertNextProcessorLocationIs(FLOW_WITH_PROXY_AND_SIMPLE_MODULE_AND_LOGGER
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("2", LOGGER, CONFIG_FILE_NAME, of(52)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithProxyAndSimpleModuleAndLoggerReverse() throws Exception {
    flowRunner("flowWithProxyAndSimpleModuleAndLoggerReverse").run();
    // first MP from within the flow
    assertNextProcessorLocationIs(FLOW_WITH_PROXY_AND_SIMPLE_MODULE_AND_LOGGER_REVERSE
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", LOGGER, CONFIG_FILE_NAME, of(56)));

    // second MP from within the flow
    assertNextProcessorLocationIs(FLOW_WITH_PROXY_AND_SIMPLE_MODULE_AND_LOGGER_REVERSE
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("1", MODULE_SET_PAYLOAD_HARDCODED_VALUE, CONFIG_FILE_NAME, of(57)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_HARDCODED_VALUE_FIRST_MP
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(13)));

    // third MP from within the flow
    assertNextProcessorLocationIs(FLOW_WITH_PROXY_AND_SIMPLE_MODULE_AND_LOGGER_REVERSE
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("2", MODULE_PROXY_SET_PAYLOAD_AND_LOGGER, CONFIG_FILE_NAME, of(58)));
    assertNextProcessorLocationIs(OPERATION_PROXY_SET_PAYLOAD_AND_LOGGER_FIRST_MP
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", MODULE_SET_PAYLOAD_HARDCODED_VALUE, MODULE_SIMPLE_PROXY_FILE_NAME, of(20)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_HARDCODED_VALUE_FIRST_MP
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(13)));
    assertNextProcessorLocationIs(OPERATION_PROXY_SET_PAYLOAD_AND_LOGGER_SECOND_MP
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("1", LOGGER, MODULE_SIMPLE_PROXY_FILE_NAME, of(21)));
    assertNoNextProcessorNotification();
  }

  private void assertNoNextProcessorNotification() {
    Iterator iterator = listener.getNotifications().iterator();
    assertThat(iterator.hasNext(), is(false));
  }

  private void assertNextProcessorLocationIs(DefaultComponentLocation componentLocation) {
    assertThat(listener.getNotifications().isEmpty(), is(false));
    MessageProcessorNotification processorNotification =
        (MessageProcessorNotification) listener.getNotifications().get(0);
    listener.getNotifications().remove(0);
    assertThat(processorNotification.getComponent().getLocation().getLocation(), is(componentLocation.getLocation()));
    assertThat(processorNotification.getComponent().getLocation(), is(componentLocation));
  }

  private String[] getModulePaths() {
    return new String[] {BASE_PATH_XML_MODULES + MODULE_SIMPLE_XML,
        BASE_PATH_XML_MODULES + MODULE_SIMPLE_PROXY_XML};
  }

  // TODO(fernandezlautaro): MULE-10982 implement a testing framework for XML based connectors
  @Override
  protected void addBuilders(List<ConfigurationBuilder> builders) {
    super.addBuilders(builders);
    builders.add(0, new AbstractConfigurationBuilder() {

      @Override
      protected void doConfigure(MuleContext muleContext) throws Exception {
        ExtensionManager extensionManager;
        if (muleContext.getExtensionManager() == null) {
          extensionManager = createDefaultExtensionManager();
          muleContext.setExtensionManager(extensionManager);
        }
        extensionManager = muleContext.getExtensionManager();
        initialiseIfNeeded(extensionManager, muleContext);

        registerXmlExtensions(extensionManager);
      }

      private void registerXmlExtensions(ExtensionManager extensionManager) {
        final Set<ExtensionModel> extensions = new HashSet<>();
        for (String modulePath : getModulePaths()) {
          Map<String, Object> params = new HashMap<>();
          params.put(XmlExtensionModelLoader.RESOURCE_XML, modulePath);
          final DslResolvingContext dslResolvingContext = getDefault(extensions);
          final ExtensionModel extensionModel =
              new XmlExtensionModelLoader().loadExtensionModel(getClass().getClassLoader(), dslResolvingContext, params);
          extensions.add(extensionModel);
        }
        for (ExtensionModel extension : extensions) {
          extensionManager.registerExtension(extension);
        }
      }
    });
  }
}
