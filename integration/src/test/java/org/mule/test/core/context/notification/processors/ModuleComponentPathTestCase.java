/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.context.notification.processors;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.FLOW;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.OPERATION;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.ROUTER;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.SCOPE;
import static org.mule.runtime.api.component.TypedComponentIdentifier.builder;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.api.util.collection.Collectors.toImmutableList;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.CHOICE_IDENTIFIER;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.FLOW_IDENTIFIER;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.FLOW_REF_IDENTIFIER;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.FOREACH_IDENTIFIER;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.PARALLEL_FOREACH_IDENTIFIER;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.ROUTE_ELEMENT;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.ROUTE_IDENTIFIER;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.SCATTER_GATHER_IDENTIFIER;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.SUBFLOW_IDENTIFIER;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.TRY_IDENTIFIER;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.UNTIL_SUCCESSFUL_IDENTIFIER;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.dsl.api.xml.parser.XmlConfigurationDocumentLoader.noValidationDocumentLoader;
import static org.mule.runtime.dsl.api.xml.parser.XmlConfigurationProcessor.processXmlConfiguration;
import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.createDefaultExtensionManager;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.CONFIGURATION_COMPONENT_LOCATOR;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.ConfigurationComponentLocationStory.COMPONENT_LOCATION;
import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.notification.MessageProcessorNotification;
import org.mule.runtime.api.util.ResourceLocator;
import org.mule.runtime.config.api.dsl.processor.ArtifactConfig;
import org.mule.runtime.config.internal.ModuleDelegatingEntityResolver;
import org.mule.runtime.config.internal.model.ApplicationModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.registry.SpiServiceRegistry;
import org.mule.runtime.core.api.util.xmlsecurity.XMLSecureFactories;
import org.mule.runtime.dsl.api.ConfigResource;
import org.mule.runtime.dsl.api.component.config.DefaultComponentLocation;
import org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.DefaultLocationPart;
import org.mule.runtime.dsl.api.xml.XmlNamespaceInfo;
import org.mule.runtime.dsl.api.xml.XmlNamespaceInfoProvider;
import org.mule.runtime.dsl.api.xml.parser.ConfigFile;
import org.mule.runtime.dsl.api.xml.parser.ConfigLine;
import org.mule.runtime.dsl.api.xml.parser.ParsingPropertyResolver;
import org.mule.runtime.dsl.api.xml.parser.XmlConfigurationDocumentLoader;
import org.mule.runtime.dsl.api.xml.parser.XmlParsingConfiguration;
import org.mule.runtime.extension.api.loader.xml.XmlExtensionModelLoader;
import org.mule.test.AbstractIntegrationTestCase;

import com.google.common.collect.ImmutableList;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.xml.parsers.SAXParserFactory;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.xml.sax.EntityResolver;

@DisplayName("XML Connectors Path generation")
@Feature(CONFIGURATION_COMPONENT_LOCATOR)
@Story(COMPONENT_LOCATION)
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
  private static final Optional<TypedComponentIdentifier> SUBFLOW_TYPED_COMPONENT_IDENTIFIER =
      of(builder().identifier(SUBFLOW_IDENTIFIER).type(SCOPE).build());

  private static final DefaultComponentLocation getFlowLocation(final String flowName, final int flowLineNumber) {
    return new DefaultComponentLocation(of(flowName), asList(new DefaultLocationPart(flowName, FLOW_TYPED_COMPONENT_IDENTIFIER,
                                                                                     CONFIG_FILE_NAME, of(flowLineNumber),
                                                                                     of(5))));
  }

  private static final DefaultComponentLocation getSubFlowLocation(final String subFlowName, final int subFlowLineNumber) {
    return new DefaultComponentLocation(of(subFlowName),
                                        asList(new DefaultLocationPart(subFlowName, SUBFLOW_TYPED_COMPONENT_IDENTIFIER,
                                                                       CONFIG_FILE_NAME, of(subFlowLineNumber),
                                                                       of(5))));
  }

  private static final String FLOW_WITH_SINGLE_MP_NAME = "flowWithSingleMp";
  private static final String FLOW_WITH_SET_PAYLOAD_HARDCODED_NAME = "flowWithSetPayloadHardcoded";
  private static final String FLOW_WITH_SET_PAYLOAD_HARDCODED_INSIDE_SUBFLOW_NAME = "flowWithSetPayloadHardcodedInsideSubFlow";
  private static final String SUBFLOW_WITH_SET_PAYLOAD_HARDCODED_NAME = "subFlowWithSetPayloadHardcoded";
  private static final String FLOW_WITH_SET_PAYLOAD_TWO_TIMES_NAME = "flowWithSetPayloadTwoTimes";
  private static final String FLOW_WITH_SET_PAYLOAD_HARDCODED_TWICE_NAME = "flowWithSetPayloadHardcodedTwice";
  private static final String FLOW_WITH_SET_PAYLOAD_PARAM_VALUE_NAME = "flowWithSetPayloadParamValue";
  private static final String FLOW_WITH_SET_PAYLOAD_TWO_TIMES_TWICE_NAME = "flowWithSetPayloadTwoTimesTwice";
  private static final String FLOW_WITH_PROXY_SET_PAYLOAD_HARDCODED_NAME = "flowWithProxySetPayloadHardcoded";
  private static final String FLOW_WITH_PROXY_SET_PAYLOAD_HARDCODED_AND_LOGGER_NAME = "flowWithProxySetPayloadHardcodedAndLogger";
  private static final String FLOW_WITH_PROXY_AND_SIMPLE_MODULE_AND_LOGGER_NAME = "flowWithProxyAndSimpleModuleAndLogger";
  private static final String FLOW_WITH_PROXY_AND_SIMPLE_MODULE_AND_LOGGER_REVERSE_NAME =
      "flowWithProxyAndSimpleModuleAndLoggerReverse";
  private static final String FLOW_WITH_CHOICE_ROUTER_NAME = "flowWithChoiceRouter";
  private static final String FLOW_WITH_UNTIL_SUCCESSFUL_SCOPE_NAME = "flowWithUntilSuccessfulScope";
  private static final String FLOW_WITH_TRY_SCOPE_NAME = "flowWithTryScope";
  private static final String FLOW_WITH_FOREACH_SCOPE_NAME = "flowWithForeachScope";
  private static final String FLOW_WITH_PARALLEL_FOREACH_SCOPE_NAME = "flowWithParallelForeachScope";
  private static final String FLOW_WITH_SCATTER_GATHER_NAME = "flowWithScatterGather";

  /**
   * "flows-using-modules.xml" flows defined below
   */
  private static final DefaultComponentLocation FLOW_WITH_SINGLE_MP_LOCATION =
      getFlowLocation(FLOW_WITH_SINGLE_MP_NAME, 15);
  private static final DefaultComponentLocation FLOW_WITH_SET_PAYLOAD_HARDCODED =
      getFlowLocation(FLOW_WITH_SET_PAYLOAD_HARDCODED_NAME, 19);
  private static final DefaultComponentLocation FLOW_WITH_SET_PAYLOAD_HARDCODED_INSIDE_SUBFLOW =
      getFlowLocation(FLOW_WITH_SET_PAYLOAD_HARDCODED_INSIDE_SUBFLOW_NAME, 61);
  private static final DefaultComponentLocation SUBFLOW_WITH_SET_PAYLOAD_HARDCODED =
      getSubFlowLocation(SUBFLOW_WITH_SET_PAYLOAD_HARDCODED_NAME, 65);
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
  private static final DefaultComponentLocation FLOW_WITH_CHOICE_ROUTER =
      getFlowLocation(FLOW_WITH_CHOICE_ROUTER_NAME, 69);
  private static final DefaultComponentLocation FLOW_WITH_UNTIL_SUCCESSFUL_SCOPE =
      getFlowLocation(FLOW_WITH_UNTIL_SUCCESSFUL_SCOPE_NAME, 77);
  private static final DefaultComponentLocation FLOW_WITH_TRY_SCOPE =
      getFlowLocation(FLOW_WITH_TRY_SCOPE_NAME, 83);
  private static final DefaultComponentLocation FLOW_WITH_FOREACH_SCOPE =
      getFlowLocation(FLOW_WITH_FOREACH_SCOPE_NAME, 89);
  private static final DefaultComponentLocation FLOW_WITH_PARALLEL_FOREACH_SCOPE =
      getFlowLocation(FLOW_WITH_PARALLEL_FOREACH_SCOPE_NAME, 95);
  private static final DefaultComponentLocation FLOW_WITH_SCATTER_GATHER =
      getFlowLocation(FLOW_WITH_SCATTER_GATHER_NAME, 101);

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
                                                                       of(operationLineNumber),
                                                                       of(13))));
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

  @Test
  public void validateComponentLocationCreatedFromExtensionModelsWithoutUsingParsers() throws Exception {
    final Set<ExtensionModel> extensionModels = muleContext.getExtensionManager().getExtensions();

    ArtifactConfig.Builder artifactConfigBuilder = new ArtifactConfig.Builder();
    artifactConfigBuilder.addConfigFile(new ConfigFile(CONFIG_FILE_NAME.get(), singletonList(loadConfigLines(extensionModels,
                                                                                                             this.getClass()
                                                                                                                 .getClassLoader()
                                                                                                                 .getResourceAsStream(CONFIG_FILE_NAME
                                                                                                                     .get()))
                                                                                                                         .orElseThrow(() -> new IllegalArgumentException(String
                                                                                                                             .format("Failed to parse %s.",
                                                                                                                                     CONFIG_FILE_NAME
                                                                                                                                         .get()))))));


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
        .add(Location.builder().globalName(FLOW_WITH_SINGLE_MP_NAME).addProcessorsPart().addIndexPart(0).build()
            .toString())

        .add(Location.builder().globalName(FLOW_WITH_SET_PAYLOAD_HARDCODED_NAME).build().toString())
        .add(Location.builder().globalName(FLOW_WITH_SET_PAYLOAD_HARDCODED_NAME).addProcessorsPart().addIndexPart(0)
            .build()
            .toString())

        .add(Location.builder().globalName(FLOW_WITH_SET_PAYLOAD_HARDCODED_TWICE_NAME).build().toString())
        .add(Location.builder().globalName(FLOW_WITH_SET_PAYLOAD_HARDCODED_TWICE_NAME).addProcessorsPart()
            .addIndexPart(0).build()
            .toString())
        .add(Location.builder().globalName(FLOW_WITH_SET_PAYLOAD_HARDCODED_TWICE_NAME).addProcessorsPart()
            .addIndexPart(1).build()
            .toString())

        .add(Location.builder().globalName(FLOW_WITH_SET_PAYLOAD_PARAM_VALUE_NAME).build().toString())
        .add(
             Location.builder().globalName(FLOW_WITH_SET_PAYLOAD_PARAM_VALUE_NAME).addProcessorsPart().addIndexPart(0)
                 .build()
                 .toString())

        .add(Location.builder().globalName(FLOW_WITH_SET_PAYLOAD_TWO_TIMES_NAME).build().toString())
        .add(Location.builder().globalName(FLOW_WITH_SET_PAYLOAD_TWO_TIMES_NAME).addProcessorsPart().addIndexPart(0)
            .build()
            .toString())

        .add(Location.builder().globalName(FLOW_WITH_SET_PAYLOAD_TWO_TIMES_TWICE_NAME).build().toString())
        .add(Location.builder().globalName(FLOW_WITH_SET_PAYLOAD_TWO_TIMES_TWICE_NAME).addProcessorsPart()
            .addIndexPart(0).build()
            .toString())
        .add(Location.builder().globalName(FLOW_WITH_SET_PAYLOAD_TWO_TIMES_TWICE_NAME).addProcessorsPart()
            .addIndexPart(1).build()
            .toString())

        .add(Location.builder().globalName(FLOW_WITH_PROXY_SET_PAYLOAD_HARDCODED_NAME).build().toString())
        .add(Location.builder().globalName(FLOW_WITH_PROXY_SET_PAYLOAD_HARDCODED_NAME).addProcessorsPart()
            .addIndexPart(0).build()
            .toString())

        .add(Location.builder().globalName(FLOW_WITH_PROXY_SET_PAYLOAD_HARDCODED_AND_LOGGER_NAME).build().toString())
        .add(Location.builder().globalName(FLOW_WITH_PROXY_SET_PAYLOAD_HARDCODED_AND_LOGGER_NAME).addProcessorsPart()
            .addIndexPart(0).build().toString())

        .add(Location.builder().globalName(FLOW_WITH_PROXY_AND_SIMPLE_MODULE_AND_LOGGER_NAME).build().toString())
        .add(Location.builder().globalName(FLOW_WITH_PROXY_AND_SIMPLE_MODULE_AND_LOGGER_NAME).addProcessorsPart()
            .addIndexPart(0)
            .build().toString())
        .add(Location.builder().globalName(FLOW_WITH_PROXY_AND_SIMPLE_MODULE_AND_LOGGER_NAME).addProcessorsPart()
            .addIndexPart(1)
            .build().toString())
        .add(Location.builder().globalName(FLOW_WITH_PROXY_AND_SIMPLE_MODULE_AND_LOGGER_NAME).addProcessorsPart()
            .addIndexPart(2)
            .build().toString())

        .add(Location.builder().globalName(FLOW_WITH_PROXY_AND_SIMPLE_MODULE_AND_LOGGER_REVERSE_NAME).build()
            .toString())
        .add(Location.builder().globalName(FLOW_WITH_PROXY_AND_SIMPLE_MODULE_AND_LOGGER_REVERSE_NAME)
            .addProcessorsPart()
            .addIndexPart(0).build().toString())
        .add(Location.builder().globalName(FLOW_WITH_PROXY_AND_SIMPLE_MODULE_AND_LOGGER_REVERSE_NAME)
            .addProcessorsPart()
            .addIndexPart(1).build().toString())
        .add(Location.builder().globalName(FLOW_WITH_PROXY_AND_SIMPLE_MODULE_AND_LOGGER_REVERSE_NAME)
            .addProcessorsPart()
            .addIndexPart(2).build().toString())

        .add(Location.builder().globalName(FLOW_WITH_SET_PAYLOAD_HARDCODED_INSIDE_SUBFLOW_NAME).build().toString())
        .add(Location.builder().globalName(FLOW_WITH_SET_PAYLOAD_HARDCODED_INSIDE_SUBFLOW_NAME).addProcessorsPart()
            .addIndexPart(0).build()
            .toString())

        .add(Location.builder().globalName(SUBFLOW_WITH_SET_PAYLOAD_HARDCODED_NAME).build().toString())
        .add(Location.builder().globalName(SUBFLOW_WITH_SET_PAYLOAD_HARDCODED_NAME).addProcessorsPart()
            .addIndexPart(0).build()
            .toString())

        .add(Location.builder().globalName(FLOW_WITH_CHOICE_ROUTER_NAME).build().toString())
        .add(Location.builder().globalName(FLOW_WITH_CHOICE_ROUTER_NAME).addProcessorsPart().addIndexPart(0).build()
            .toString())
        .add(Location.builder().globalName(FLOW_WITH_CHOICE_ROUTER_NAME).addProcessorsPart().addIndexPart(0)
            .addPart(ROUTE_ELEMENT)
            .addIndexPart(0).build().toString())
        .add(Location.builder().globalName(FLOW_WITH_CHOICE_ROUTER_NAME).addProcessorsPart().addIndexPart(0)
            .addPart(ROUTE_ELEMENT)
            .addIndexPart(0).addProcessorsPart().addIndexPart(0).build().toString())

        .add(Location.builder().globalName(FLOW_WITH_UNTIL_SUCCESSFUL_SCOPE_NAME).build().toString())
        .add(Location.builder().globalName(FLOW_WITH_UNTIL_SUCCESSFUL_SCOPE_NAME).addProcessorsPart().addIndexPart(0)
            .build().toString())
        .add(Location.builder().globalName(FLOW_WITH_UNTIL_SUCCESSFUL_SCOPE_NAME).addProcessorsPart().addIndexPart(0)
            .addProcessorsPart().addIndexPart(0).build().toString())

        .add(Location.builder().globalName(FLOW_WITH_TRY_SCOPE_NAME).build().toString())
        .add(Location.builder().globalName(FLOW_WITH_TRY_SCOPE_NAME).addProcessorsPart().addIndexPart(0)
            .build().toString())
        .add(Location.builder().globalName(FLOW_WITH_TRY_SCOPE_NAME).addProcessorsPart().addIndexPart(0)
            .addProcessorsPart().addIndexPart(0).build().toString())

        .add(Location.builder().globalName(FLOW_WITH_FOREACH_SCOPE_NAME).build().toString())
        .add(Location.builder().globalName(FLOW_WITH_FOREACH_SCOPE_NAME).addProcessorsPart().addIndexPart(0)
            .build().toString())
        .add(Location.builder().globalName(FLOW_WITH_FOREACH_SCOPE_NAME).addProcessorsPart().addIndexPart(0)
            .addProcessorsPart().addIndexPart(0).build().toString())

        .add(Location.builder().globalName(FLOW_WITH_PARALLEL_FOREACH_SCOPE_NAME).build().toString())
        .add(Location.builder().globalName(FLOW_WITH_PARALLEL_FOREACH_SCOPE_NAME).addProcessorsPart().addIndexPart(0)
            .build().toString())
        .add(Location.builder().globalName(FLOW_WITH_PARALLEL_FOREACH_SCOPE_NAME).addProcessorsPart().addIndexPart(0)
            .addProcessorsPart().addIndexPart(0).build().toString())

        .add(Location.builder().globalName(FLOW_WITH_SCATTER_GATHER_NAME).build().toString())
        .add(Location.builder().globalName(FLOW_WITH_SCATTER_GATHER_NAME).addProcessorsPart().addIndexPart(0).build()
            .toString())
        .add(Location.builder().globalName(FLOW_WITH_SCATTER_GATHER_NAME).addProcessorsPart().addIndexPart(0)
            .addPart(ROUTE_ELEMENT)
            .addIndexPart(0).build().toString())
        .add(Location.builder().globalName(FLOW_WITH_SCATTER_GATHER_NAME).addProcessorsPart().addIndexPart(0)
            .addPart(ROUTE_ELEMENT)
            .addIndexPart(0).addProcessorsPart().addIndexPart(0).build().toString())
        .add(Location.builder().globalName(FLOW_WITH_SCATTER_GATHER_NAME).addProcessorsPart().addIndexPart(0)
            .addPart(ROUTE_ELEMENT)
            .addIndexPart(1).build().toString())
        .add(Location.builder().globalName(FLOW_WITH_SCATTER_GATHER_NAME).addProcessorsPart().addIndexPart(0)
            .addPart(ROUTE_ELEMENT)
            .addIndexPart(1).addProcessorsPart().addIndexPart(0).build().toString())

        .build(), componentLocations);
  }

  @Test
  public void flowWithSingleMp() throws Exception {
    // simple test to be sure the macro expansion doesn't mess up the a flow that has no modifications
    flowRunner("flowWithSingleMp").run();
    assertNextProcessorLocationIs(FLOW_WITH_SINGLE_MP_LOCATION
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", LOGGER, CONFIG_FILE_NAME, of(16), of(9)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void subFlowWithSetPayloadHardcoded() throws Exception {
    flowRunner("flowWithSetPayloadHardcodedInsideSubFlow").run();
    assertNextProcessorLocationIs(FLOW_WITH_SET_PAYLOAD_HARDCODED_INSIDE_SUBFLOW
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", of(TypedComponentIdentifier.builder()
            .identifier(FLOW_REF_IDENTIFIER)
            .type(OPERATION).build()), CONFIG_FILE_NAME, of(62),
                            of(9)));
    assertNextProcessorLocationIs(SUBFLOW_WITH_SET_PAYLOAD_HARDCODED
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", MODULE_SET_PAYLOAD_HARDCODED_VALUE, CONFIG_FILE_NAME, of(66),
                            of(9)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_HARDCODED_VALUE_FIRST_MP
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(13), of(13)));
    assertNoNextProcessorNotification();
  }

  @Description("Smart Connector inside a choice router")
  @Issue("MULE-16984")
  @Test
  public void flowWithChoiceRouter() throws Exception {
    flowRunner("flowWithChoiceRouter").run();

    DefaultComponentLocation firstComponentLocation = FLOW_WITH_CHOICE_ROUTER
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", of(builder()
            .identifier(CHOICE_IDENTIFIER)
            .type(ROUTER).build()), CONFIG_FILE_NAME, of(70), of(9));

    assertNextProcessorLocationIs(firstComponentLocation);
    assertNextProcessorLocationIs(firstComponentLocation
        .appendLocationPart(ROUTE_ELEMENT, empty(), empty(), empty(), empty())
        .appendLocationPart("0", of(TypedComponentIdentifier.builder()
            .identifier(ROUTE_IDENTIFIER)
            .type(SCOPE).build()), CONFIG_FILE_NAME, of(71), of(13))
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", MODULE_SET_PAYLOAD_HARDCODED_VALUE, CONFIG_FILE_NAME, of(72),
                            of(17)));

    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_HARDCODED_VALUE_FIRST_MP
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(13), of(13)));
    assertNoNextProcessorNotification();
  }

  @Description("Smart Connector inside a until-successful scope")
  @Issue("MULE-16285")
  @Test
  public void flowWithUntilSuccessfulScope() throws Exception {
    flowRunner("flowWithUntilSuccessfulScope").run();

    DefaultComponentLocation firstComponentLocation = FLOW_WITH_UNTIL_SUCCESSFUL_SCOPE
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", of(builder()
            .identifier(UNTIL_SUCCESSFUL_IDENTIFIER)
            .type(SCOPE).build()), CONFIG_FILE_NAME, of(78), of(9));

    assertNextProcessorLocationIs(firstComponentLocation);
    assertNextProcessorLocationIs(firstComponentLocation
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", MODULE_SET_PAYLOAD_HARDCODED_VALUE, CONFIG_FILE_NAME, of(79),
                            of(13)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_HARDCODED_VALUE_FIRST_MP
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(13), of(13)));
    assertNoNextProcessorNotification();
  }

  @Description("Smart Connector inside a try scope")
  @Issue("MULE-16285")
  @Test
  public void flowWithTryScope() throws Exception {
    flowRunner("flowWithTryScope").run();

    DefaultComponentLocation firstComponentLocation = FLOW_WITH_TRY_SCOPE
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", of(builder()
            .identifier(TRY_IDENTIFIER)
            .type(SCOPE).build()), CONFIG_FILE_NAME, of(84), of(9));

    assertNextProcessorLocationIs(firstComponentLocation);
    assertNextProcessorLocationIs(firstComponentLocation
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", MODULE_SET_PAYLOAD_HARDCODED_VALUE, CONFIG_FILE_NAME, of(85),
                            of(13)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_HARDCODED_VALUE_FIRST_MP
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(13), of(13)));
    assertNoNextProcessorNotification();
  }

  @Description("Smart Connector inside a foreach scope")
  @Issue("MULE-16285")
  @Test
  public void flowWithForeachScope() throws Exception {
    flowRunner("flowWithForeachScope").run();

    DefaultComponentLocation firstComponentLocation = FLOW_WITH_FOREACH_SCOPE
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", of(builder()
            .identifier(FOREACH_IDENTIFIER)
            .type(SCOPE).build()), CONFIG_FILE_NAME, of(90), of(9));

    assertNextProcessorLocationIs(firstComponentLocation);
    assertNextProcessorLocationIs(firstComponentLocation
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", MODULE_SET_PAYLOAD_HARDCODED_VALUE, CONFIG_FILE_NAME, of(91),
                            of(13)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_HARDCODED_VALUE_FIRST_MP
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(13), of(13)));
    assertNoNextProcessorNotification();
  }

  @Description("Smart Connector inside a parallel-foreach scope")
  @Issue("MULE-16285")
  @Test
  public void flowWithParallelForeachScope() throws Exception {
    flowRunner("flowWithParallelForeachScope").run();

    DefaultComponentLocation firstComponentLocation = FLOW_WITH_PARALLEL_FOREACH_SCOPE
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", of(builder()
            .identifier(PARALLEL_FOREACH_IDENTIFIER)
            .type(SCOPE).build()), CONFIG_FILE_NAME, of(96), of(9));

    assertNextProcessorLocationIs(firstComponentLocation);
    assertNextProcessorLocationIs(firstComponentLocation
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", MODULE_SET_PAYLOAD_HARDCODED_VALUE, CONFIG_FILE_NAME, of(97), of(13)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_HARDCODED_VALUE_FIRST_MP
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(13), of(13)));
    assertNoNextProcessorNotification();
  }

  @Description("Smart Connector inside a scatter-gather")
  @Issue("MULE-16285")
  @Ignore("MULE-18878")
  @Test
  public void flowWithScatterGather() throws Exception {
    flowRunner("flowWithScatterGather").run();

    DefaultComponentLocation firstComponentLocation = FLOW_WITH_SCATTER_GATHER
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", of(builder()
            .identifier(SCATTER_GATHER_IDENTIFIER)
            .type(ROUTER).build()), CONFIG_FILE_NAME, of(102), of(9));

    assertNextProcessorLocationIs(firstComponentLocation);

    assertNextProcessorLocationIs(firstComponentLocation
        .appendLocationPart(ROUTE_ELEMENT, empty(), empty(), empty(), empty())
        .appendLocationPart("0", of(builder()
            .identifier(ROUTE_IDENTIFIER)
            .type(SCOPE).build()), CONFIG_FILE_NAME, of(103), of(13))
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", MODULE_SET_PAYLOAD_HARDCODED_VALUE, CONFIG_FILE_NAME, of(104),
                            of(17)));

    DefaultComponentLocation thirdComponentLocation = firstComponentLocation
        .appendLocationPart(ROUTE_ELEMENT, empty(), empty(), empty(), empty())
        .appendLocationPart("1", of(builder()
            .identifier(ROUTE_IDENTIFIER)
            .type(SCOPE).build()), CONFIG_FILE_NAME, of(106), of(13))
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", LOGGER, CONFIG_FILE_NAME, of(107), of(17));

    DefaultComponentLocation fourthComponentLocation = OPERATION_SET_PAYLOAD_HARDCODED_VALUE_FIRST_MP
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(13), of(13));

    ComponentLocation thirdProcessorNotification = getMessageProcessorNotification().getComponent().getLocation();
    ComponentLocation fourthProcessorNotification = getMessageProcessorNotification().getComponent().getLocation();

    List<String> locations = asList(thirdProcessorNotification.getLocation(), fourthProcessorNotification.getLocation());
    List<ComponentLocation> componentLocations = asList(thirdProcessorNotification, fourthProcessorNotification);

    assertThat(locations, hasItems(thirdComponentLocation.getLocation(), fourthComponentLocation.getLocation()));
    assertThat(componentLocations, hasItems(thirdComponentLocation, fourthComponentLocation));

    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithSetPayloadHardcoded() throws Exception {
    flowRunner("flowWithSetPayloadHardcoded").run();
    assertNextProcessorLocationIs(FLOW_WITH_SET_PAYLOAD_HARDCODED
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", MODULE_SET_PAYLOAD_HARDCODED_VALUE, CONFIG_FILE_NAME, of(20),
                            of(9)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_HARDCODED_VALUE_FIRST_MP
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(13), of(13)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithSetPayloadHardcodedTwice() throws Exception {
    flowRunner("flowWithSetPayloadHardcodedTwice").run();
    assertNextProcessorLocationIs(FLOW_WITH_SET_PAYLOAD_HARDCODED_TWICE
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", MODULE_SET_PAYLOAD_HARDCODED_VALUE, CONFIG_FILE_NAME, of(24),
                            of(9)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_HARDCODED_VALUE_FIRST_MP
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(13), of(13)));

    assertNextProcessorLocationIs(FLOW_WITH_SET_PAYLOAD_HARDCODED_TWICE
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("1", MODULE_SET_PAYLOAD_HARDCODED_VALUE, CONFIG_FILE_NAME, of(25),
                            of(9)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_HARDCODED_VALUE_FIRST_MP
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(13), of(13)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithSetPayloadParamValue() throws Exception {
    flowRunner("flowWithSetPayloadParamValue").run();
    assertNextProcessorLocationIs(FLOW_WITH_SET_PAYLOAD_PARAM_VALUE
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", MODULE_SET_PAYLOAD_PARAM_VALUE, CONFIG_FILE_NAME, of(29), of(9)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_PARAM_VALUE_FIRST_MP
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(23), of(13)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithSetPayloadTwoTimes() throws Exception {
    flowRunner("flowWithSetPayloadTwoTimes").run();
    assertNextProcessorLocationIs(FLOW_WITH_SET_PAYLOAD_TWO_TIMES
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", MODULE_SET_PAYLOAD_TWO_TIMES, CONFIG_FILE_NAME, of(33), of(9)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_TWO_TIMES_FIRST_MP
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(30), of(13)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_TWO_TIMES_SECOND_MP
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("1", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(31), of(13)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithSetPayloadTwoTimesTwice() throws Exception {
    flowRunner("flowWithSetPayloadTwoTimesTwice").run();
    assertNextProcessorLocationIs(FLOW_WITH_SET_PAYLOAD_TWO_TIMES_TWICE
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", MODULE_SET_PAYLOAD_TWO_TIMES, CONFIG_FILE_NAME, of(37), of(9)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_TWO_TIMES_FIRST_MP
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(30), of(13)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_TWO_TIMES_SECOND_MP
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("1", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(31), of(13)));
    // assertion on the second call of the OP
    assertNextProcessorLocationIs(FLOW_WITH_SET_PAYLOAD_TWO_TIMES_TWICE
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("1", MODULE_SET_PAYLOAD_TWO_TIMES, CONFIG_FILE_NAME, of(38), of(9)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_TWO_TIMES_FIRST_MP
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(30), of(13)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_TWO_TIMES_SECOND_MP
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("1", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(31), of(13)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithProxySetPayloadHardcoded() throws Exception {
    flowRunner("flowWithProxySetPayloadHardcoded").run();
    // flow assertion
    assertNextProcessorLocationIs(FLOW_WITH_PROXY_SET_PAYLOAD_HARDCODED
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", MODULE_PROXY_SET_PAYLOAD, CONFIG_FILE_NAME, of(42), of(9)));
    assertNextProcessorLocationIs(OPERATION_PROXY_SET_PAYLOAD_FIRST_MP
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", MODULE_SET_PAYLOAD_HARDCODED_VALUE, MODULE_SIMPLE_PROXY_FILE_NAME,
                            of(13), of(13)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_HARDCODED_VALUE_FIRST_MP
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(13), of(13)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithProxySetPayloadHardcodedAndLogger() throws Exception {
    flowRunner("flowWithProxySetPayloadHardcodedAndLogger").run();
    assertNextProcessorLocationIs(FLOW_WITH_PROXY_SET_PAYLOAD_HARDCODED_AND_LOGGER
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", MODULE_PROXY_SET_PAYLOAD_AND_LOGGER, CONFIG_FILE_NAME, of(46),
                            of(9)));
    assertNextProcessorLocationIs(OPERATION_PROXY_SET_PAYLOAD_AND_LOGGER_FIRST_MP
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", MODULE_SET_PAYLOAD_HARDCODED_VALUE, MODULE_SIMPLE_PROXY_FILE_NAME,
                            of(20), of(13)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_HARDCODED_VALUE_FIRST_MP
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(13), of(13)));
    assertNextProcessorLocationIs(OPERATION_PROXY_SET_PAYLOAD_AND_LOGGER_SECOND_MP
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("1", LOGGER, MODULE_SIMPLE_PROXY_FILE_NAME, of(21), of(13)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithProxyAndSimpleModuleAndLogger() throws Exception {
    flowRunner("flowWithProxyAndSimpleModuleAndLogger").run();

    // first MP from within the flow
    assertNextProcessorLocationIs(FLOW_WITH_PROXY_AND_SIMPLE_MODULE_AND_LOGGER
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", MODULE_PROXY_SET_PAYLOAD_AND_LOGGER, CONFIG_FILE_NAME, of(50),
                            of(9)));
    assertNextProcessorLocationIs(OPERATION_PROXY_SET_PAYLOAD_AND_LOGGER_FIRST_MP
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", MODULE_SET_PAYLOAD_HARDCODED_VALUE, MODULE_SIMPLE_PROXY_FILE_NAME,
                            of(20), of(13)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_HARDCODED_VALUE_FIRST_MP
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(13), of(13)));
    assertNextProcessorLocationIs(OPERATION_PROXY_SET_PAYLOAD_AND_LOGGER_SECOND_MP
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("1", LOGGER, MODULE_SIMPLE_PROXY_FILE_NAME, of(21), of(13)));

    // second MP from within the flow
    assertNextProcessorLocationIs(FLOW_WITH_PROXY_AND_SIMPLE_MODULE_AND_LOGGER
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("1", MODULE_SET_PAYLOAD_HARDCODED_VALUE, CONFIG_FILE_NAME, of(51),
                            of(9)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_HARDCODED_VALUE_FIRST_MP
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(13), of(13)));
    // third MP from within the flow
    assertNextProcessorLocationIs(FLOW_WITH_PROXY_AND_SIMPLE_MODULE_AND_LOGGER
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("2", LOGGER, CONFIG_FILE_NAME, of(52), of(9)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithProxyAndSimpleModuleAndLoggerReverse() throws Exception {
    flowRunner("flowWithProxyAndSimpleModuleAndLoggerReverse").run();
    // first MP from within the flow
    assertNextProcessorLocationIs(FLOW_WITH_PROXY_AND_SIMPLE_MODULE_AND_LOGGER_REVERSE
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", LOGGER, CONFIG_FILE_NAME, of(56), of(9)));

    // second MP from within the flow
    assertNextProcessorLocationIs(FLOW_WITH_PROXY_AND_SIMPLE_MODULE_AND_LOGGER_REVERSE
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("1", MODULE_SET_PAYLOAD_HARDCODED_VALUE, CONFIG_FILE_NAME, of(57),
                            of(9)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_HARDCODED_VALUE_FIRST_MP
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(13), of(13)));

    // third MP from within the flow
    assertNextProcessorLocationIs(FLOW_WITH_PROXY_AND_SIMPLE_MODULE_AND_LOGGER_REVERSE
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("2", MODULE_PROXY_SET_PAYLOAD_AND_LOGGER, CONFIG_FILE_NAME, of(58),
                            of(9)));
    assertNextProcessorLocationIs(OPERATION_PROXY_SET_PAYLOAD_AND_LOGGER_FIRST_MP
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", MODULE_SET_PAYLOAD_HARDCODED_VALUE, MODULE_SIMPLE_PROXY_FILE_NAME,
                            of(20), of(13)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_HARDCODED_VALUE_FIRST_MP
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("0", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(13), of(13)));
    assertNextProcessorLocationIs(OPERATION_PROXY_SET_PAYLOAD_AND_LOGGER_SECOND_MP
        .appendLocationPart("processors", empty(), empty(), empty(), empty())
        .appendLocationPart("1", LOGGER, MODULE_SIMPLE_PROXY_FILE_NAME, of(21), of(13)));
    assertNoNextProcessorNotification();
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

  private Optional<ConfigLine> loadConfigLines(Set<ExtensionModel> extensionModels, InputStream inputStream) {
    List<XmlNamespaceInfoProvider> xmlNamespaceInfoProviders =
        ImmutableList.<XmlNamespaceInfoProvider>builder()
            .add(createStaticNamespaceInfoProviders(extensionModels))
            .addAll(discoverRuntimeXmlNamespaceInfoProvider())
            .build();
    List<ConfigFile> configFiles = processXmlConfiguration(new XmlParsingConfiguration() {

      @Override
      public ParsingPropertyResolver getParsingPropertyResolver() {
        return key -> null;
      }

      @Override
      public ConfigResource[] getArtifactConfigResources() {
        return new ConfigResource[] {
            new ConfigResource("config", inputStream)
        };
      }

      @Override
      public ResourceLocator getResourceLocator() {
        return null;
      }

      @Override
      public Supplier<SAXParserFactory> getSaxParserFactory() {
        return () -> XMLSecureFactories.createDefault().getSAXParserFactory();
      }

      @Override
      public XmlConfigurationDocumentLoader getXmlConfigurationDocumentLoader() {
        return noValidationDocumentLoader();
      }

      @Override
      public EntityResolver getEntityResolver() {
        return new ModuleDelegatingEntityResolver(extensionModels);
      }

      @Override
      public List<XmlNamespaceInfoProvider> getXmlNamespaceInfoProvider() {
        return xmlNamespaceInfoProviders;
      }
    });
    return configFiles.isEmpty() ? empty() : of(configFiles.get(0).getConfigLines().get(0));
  }


  private XmlNamespaceInfoProvider createStaticNamespaceInfoProviders(Set<ExtensionModel> extensionModels) {
    List<XmlNamespaceInfoProvider> xmlNamesInfoProviders =
        extensionModels.stream()
            .map(ext -> (XmlNamespaceInfoProvider) () -> singleton(new XmlNamespaceInfo() {

              @Override
              public String getNamespaceUriPrefix() {
                return ext.getXmlDslModel().getNamespace();
              }

              @Override
              public String getNamespace() {
                return ext.getXmlDslModel().getPrefix();
              }
            }))
            .collect(toImmutableList());
    return () -> xmlNamesInfoProviders.stream().map(XmlNamespaceInfoProvider::getXmlNamespacesInfo)
        .flatMap(collection -> collection.stream())
        .collect(Collectors.toCollection(() -> new ArrayList<>()));
  }

  private List<XmlNamespaceInfoProvider> discoverRuntimeXmlNamespaceInfoProvider() {
    ImmutableList.Builder namespaceInfoProvidersBuilder = ImmutableList.builder();
    namespaceInfoProvidersBuilder
        .addAll(new SpiServiceRegistry().lookupProviders(XmlNamespaceInfoProvider.class,
                                                         muleContext.getClass().getClassLoader()));
    return namespaceInfoProvidersBuilder.build();
  }

  private void assertNoNextProcessorNotification() {
    Iterator iterator = listener.getNotifications().iterator();
    assertThat(iterator.hasNext(), is(false));
  }

  private void assertNextProcessorLocationIs(DefaultComponentLocation componentLocation) {
    MessageProcessorNotification processorNotification = getMessageProcessorNotification();
    assertThat(processorNotification.getComponent().getLocation().getLocation(), is(componentLocation.getLocation()));
    assertThat(processorNotification.getComponent().getLocation(), is(componentLocation));
  }

  private MessageProcessorNotification getMessageProcessorNotification() {
    assertThat(listener.getNotifications().isEmpty(), is(false));
    MessageProcessorNotification processorNotification =
        listener.getNotifications().get(0);
    listener.getNotifications().remove(0);
    return processorNotification;
  }

}
