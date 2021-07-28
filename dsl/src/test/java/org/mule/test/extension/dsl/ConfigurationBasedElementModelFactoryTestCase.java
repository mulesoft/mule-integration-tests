/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.extension.dsl;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.RECONNECTION_STRATEGY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TLS_PARAMETER_NAME;
import static org.mule.runtime.internal.dsl.DslConstants.KEY_ATTRIBUTE_NAME;
import static org.mule.runtime.internal.dsl.DslConstants.VALUE_ATTRIBUTE_NAME;

import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.api.dsl.model.DslElementModel;
import org.mule.runtime.dsl.api.component.config.ComponentConfiguration;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ConfigurationBasedElementModelFactoryTestCase extends AbstractElementModelTestCase {

  @Rule
  public DynamicPort port = new DynamicPort("port");

  @Rule
  public DynamicPort otherPort = new DynamicPort("otherPort");

  @Before
  public void initApp() throws Exception {
    applicationModel = loadApplicationModel();
  }

  @Override
  protected String getConfigFile() {
    return "integration-multi-config-dsl-app.xml";
  }

  @Test
  public void defaultGroupResolution() throws Exception {
    ComponentAst flow = getAppElement(applicationModel, COMPONENTS_FLOW);

    DslElementModel<ConfigurationModel> overrideWscConsume = resolve(flow.directChildrenStream().skip(5).findFirst().get());
    DslElementModel<ConfigurationModel> defaultWscConsume = resolve(flow.directChildrenStream().skip(6).findFirst().get());

    assertElementName(overrideWscConsume, "consume");
    assertElementName(defaultWscConsume, "consume");

    assertAttributeIsPresent(overrideWscConsume, "operation");
    assertAttributeIsPresent(defaultWscConsume, "operation");

    assertThat(overrideWscConsume.findElement("transportHeaders").isPresent(), is(false));
    assertThat(defaultWscConsume.findElement("transportHeaders").isPresent(), is(false));

    assertThat(defaultWscConsume.getContainedElements().size(), is(overrideWscConsume.getContainedElements().size()));

    assertThat(overrideWscConsume.findElement("body").isPresent(), is(true));
    assertThat(defaultWscConsume.findElement("body").isPresent(), is(true));

    assertThat(overrideWscConsume.findElement("body").get().getValue().get(), is("#['modified' ++ payload]"));
    assertThat(defaultWscConsume.findElement("body").get().getValue().get(), is("#[payload]"));
  }

  @Test
  public void defaultValueResolution() throws Exception {
    ComponentAst config = getAppElement(applicationModel, DB_CONFIG);
    DslElementModel<ConfigurationModel> configElement = resolve(config);

    DslElementModel<ConnectionProviderModel> connectionElement =
        getChild(configElement, config.directChildrenStream().findFirst().get());
    validateDefaultValueResolution(connectionElement);
  }

  @Test
  public void defaultValueResolutionDirectly() {
    validateDefaultValueResolution(resolve(getAppElement(applicationModel, DB_CONFIG).directChildrenStream().findFirst()
        .get()));
  }

  private void validateDefaultValueResolution(DslElementModel<ConnectionProviderModel> connectionElement) {
    assertElementName(connectionElement, "derby-connection");
    assertHasParameter(connectionElement.getModel(), "database");
    assertAttributeIsPresent(connectionElement, "database");

    assertHasParameter(connectionElement.getModel(), "create");
    assertAttributeIsPresent(connectionElement, "create");
    assertValue(connectionElement.findElement("create").get(), "true");

    assertHasParameter(connectionElement.getModel(), "subsubProtocol");
    assertAttributeIsPresent(connectionElement, "subsubProtocol");
    assertValue(connectionElement.findElement("subsubProtocol").get(), "directory");
  }

  @Test
  public void resolveConnectionWithMapParams() throws Exception {
    ComponentAst config = getAppElement(applicationModel, DB_CONFIG);
    DslElementModel<ConfigurationModel> configElement = resolve(config);
    assertElementName(configElement, "config");

    assertThat(configElement.getIdentifier().isPresent(), is(true));
    assertThat(configElement.getIdentifier().get(), is(equalTo(config.getIdentifier())));

    assertThat(configElement.findElement(newIdentifier("oracle-connection", DB_NS)).isPresent(), is(false));

    ComponentAst connection = config.directChildrenStream().findFirst().get();
    DslElementModel<ConnectionProviderModel> connectionElement = getChild(configElement, connection);
    validateConnectionWithMapParams(connection, connectionElement);
  }

  @Test
  public void resolveConnectionWithMapParamsDirectly() {
    ComponentAst config = getAppElement(applicationModel, DB_CONFIG);
    ComponentAst connection = config.directChildrenStream().findFirst().get();
    validateConnectionWithMapParams(connection, resolve(config.directChildrenStream().findFirst().get()));
  }

  private void validateConnectionWithMapParams(ComponentAst connection,
                                               DslElementModel<ConnectionProviderModel> connectionElement) {
    assertElementName(connectionElement, "derby-connection");
    assertHasParameter(connectionElement.getModel(), "database");
    assertAttributeIsPresent(connectionElement, "database");
    assertHasParameter(connectionElement.getModel(), "create");
    assertAttributeIsPresent(connectionElement, "create");
    assertThat(connectionElement.findElement(newIdentifier("connection-properties", DB_NS)).isPresent(), is(true));

    ComponentAst pooling = (ComponentAst) connection.getParameter(DEFAULT_GROUP_NAME, "poolingProfile").getValue().getRight();
    DslElementModel<ConnectionProviderModel> poolingElement = getChild(connectionElement, pooling);

    assertValue(poolingElement.findElement("maxPoolSize").get(), "10");
    assertValue(poolingElement.findElement("minPoolSize").get(), "0");

    DslElementModel<ConnectionProviderModel> propertiesElement = getChild(connectionElement, ComponentIdentifier.builder()
        .namespace(connection.getIdentifier().getNamespace())
        .namespaceUri(connection.getIdentifier().getNamespaceUri())
        .name("connection-properties").build());

    assertThat(propertiesElement.getContainedElements().size(), is(2));
    Optional<DslElementModel> firstKey = propertiesElement.getContainedElements().get(0).findElement(KEY_ATTRIBUTE_NAME);
    assertValue(firstKey.get(), "first");
    Optional<DslElementModel> firstVal = propertiesElement.getContainedElements().get(0).findElement(VALUE_ATTRIBUTE_NAME);
    assertValue(firstVal.get(), "propertyOne");
    Optional<DslElementModel> secondKey = propertiesElement.getContainedElements().get(1).findElement(KEY_ATTRIBUTE_NAME);
    assertValue(secondKey.get(), "second");
    Optional<DslElementModel> secondVal = propertiesElement.getContainedElements().get(1).findElement(VALUE_ATTRIBUTE_NAME);
    assertValue(secondVal.get(), "propertyTwo");
  }

  @Test
  public void resolveConnectionNoExtraParameters() throws Exception {
    ComponentAst config = getAppElement(applicationModel, DB_CONFIG);
    ComponentAst connection = config.directChildrenStream().findFirst().get();
    DslElementModel<ConfigurationModel> configElement = resolve(config);

    DslElementModel<ConnectionProviderModel> connectionElement = getChild(configElement, connection);
    validateConnectionNoExtraParameters(connectionElement);
  }

  @Test
  public void resolveConnectionNoExtraParametersDirectly() {
    ComponentAst config = getAppElement(applicationModel, DB_CONFIG);
    ComponentAst connection = config.directChildrenStream().findFirst().get();
    validateConnectionNoExtraParameters(resolve(connection));
  }

  private void validateConnectionNoExtraParameters(DslElementModel<ConnectionProviderModel> connectionElement) {
    assertHasParameter(connectionElement.getModel(), "columnTypes");
    assertThat(connectionElement.findElement("columnTypes").isPresent(), is(false));
  }

  @Test
  public void resolveInfrastructureParametersAsElements() throws Exception {
    ComponentAst config = getAppElement(applicationModel, HTTP_LISTENER_CONFIG);
    DslElementModel<ConfigurationModel> configElement = resolve(config);
    assertThat(configElement.findElement(TLS_PARAMETER_NAME).isPresent(), is(true));
    assertThat(configElement.findElement(TLS_PARAMETER_NAME).get().getConfiguration().isPresent(), is(true));

    ComponentAst listener =
        getAppElement(applicationModel, COMPONENTS_FLOW).directChildrenStream().skip(LISTENER_PATH).findFirst().get();
    DslElementModel<SourceModel> listenerElement = resolve(listener);
    assertThat(listenerElement.findElement(RECONNECTION_STRATEGY_PARAMETER_NAME).isPresent(), is(true));
    assertThat(listenerElement.findElement(RECONNECTION_STRATEGY_PARAMETER_NAME).get().getConfiguration().isPresent(), is(true));
  }

  @Test
  public void resolveConfigNoExtraContainedElements() throws Exception {
    ComponentAst config = getAppElement(applicationModel, HTTP_LISTENER_CONFIG);
    DslElementModel<ConfigurationModel> configElement = resolve(config);

    assertThat(configElement.findElement(newIdentifier("request-connection", HTTP_NS)).isPresent(),
               is(false));
  }

  @Test
  public void resolveConfigWithParameters() throws Exception {
    ComponentAst config = getAppElement(applicationModel, HTTP_LISTENER_CONFIG);
    DslElementModel<ConfigurationModel> configElement = resolve(config);

    assertElementName(configElement, "listener-config");
    assertHasParameter(configElement.getModel(), "basePath");
    assertAttributeIsPresent(configElement, "basePath");

    ComponentAst connection = config.directChildrenStream().findFirst().get();
    DslElementModel<ConnectionProviderModel> connectionElement = getChild(configElement, connection);

    validateConfigWithParameters(connectionElement);

    assertThat(configElement.findElement(newIdentifier("request-connection", HTTP_NS)).isPresent(),
               is(false));
  }

  @Test
  public void resolveConfigWithParametersDirectly() {
    ComponentAst config = getAppElement(applicationModel, HTTP_LISTENER_CONFIG);
    validateConfigWithParameters(resolve(config.directChildrenStream().findFirst().get()));
  }

  private void validateConfigWithParameters(DslElementModel<ConnectionProviderModel> connectionElement) {
    assertElementName(connectionElement, "listener-connection");
    assertAttributeIsPresent(connectionElement, "host");
    assertAttributeIsPresent(connectionElement, "port");
  }

  @Test
  public void resolveConnectionWithSubtypes() throws Exception {
    ComponentAst config = getAppElement(applicationModel, HTTP_REQUESTER_CONFIG);
    DslElementModel<ConfigurationModel> configElement = resolve(config);

    assertElementName(configElement, "request-config");

    ComponentAst connection = config.directChildrenStream().findFirst().get();
    DslElementModel<ConnectionProviderModel> connectionElement = getChild(configElement, connection);

    validateConnectionWithSubtypes(connection, connectionElement);

    assertThat(configElement.findElement(newIdentifier("listener-connection", HTTP_NS)).isPresent(),
               is(false));
  }

  @Test
  public void resolveConnectionWithSubtypesDirectly() {
    ComponentAst config = getAppElement(applicationModel, HTTP_REQUESTER_CONFIG);
    ComponentAst connection = config.directChildrenStream().findFirst().get();
    validateConnectionWithSubtypes(connection, resolve(connection));
  }

  private void validateConnectionWithSubtypes(ComponentAst connection,
                                              DslElementModel<ConnectionProviderModel> connectionElement) {
    assertElementName(connectionElement, "request-connection");
    assertHasParameter(connectionElement.getModel(), "host");
    assertAttributeIsPresent(connectionElement, "host");
    assertHasParameter(connectionElement.getModel(), "port");
    assertAttributeIsPresent(connectionElement, "port");

    final ComponentAst authentication =
        (ComponentAst) connection.getParameter(DEFAULT_GROUP_NAME, "authentication").getValue().getRight();
    DslElementModel<ParameterModel> basicAuthElement = getChild(connectionElement, authentication);
    assertElementName(basicAuthElement, "basic-authentication");
    assertThat(basicAuthElement.getDsl().isWrapped(), is(false));
    assertThat(basicAuthElement.getDsl().supportsAttributeDeclaration(), is(false));
  }

  @Test
  public void resolveConnectionWithImportedTypes() throws Exception {
    ComponentAst config = getAppElement(applicationModel, HTTP_REQUESTER_CONFIG);
    DslElementModel<ConfigurationModel> configElement = resolve(config);
    assertElementName(configElement, "request-config");

    ComponentAst connection = config.directChildrenStream().findFirst().get();
    DslElementModel<ConnectionProviderModel> connectionElement = getChild(configElement, connection);

    validateConnectionWithImportedTypes(connection, connectionElement);

    assertValue(configElement.findElement("receiveBufferSize").get(), "1024");
    assertValue(configElement.findElement("sendTcpNoDelay").get(), "true");

    assertThat(configElement.findElement(newIdentifier("listener-connection", HTTP_NS)).isPresent(),
               is(false));
  }

  @Test
  public void resolveConnectionWithImportedTypedDirectly() {
    ComponentAst config = getAppElement(applicationModel, HTTP_REQUESTER_CONFIG);
    ComponentAst connection = config.directChildrenStream().findFirst().get();
    validateConnectionWithImportedTypes(connection, resolve(connection));
  }

  private void validateConnectionWithImportedTypes(ComponentAst connection,
                                                   DslElementModel<ConnectionProviderModel> connectionElement) {
    assertElementName(connectionElement, "request-connection");
    assertHasParameter(connectionElement.getModel(), "host");
    assertAttributeIsPresent(connectionElement, "host");
    assertHasParameter(connectionElement.getModel(), "port");
    assertAttributeIsPresent(connectionElement, "port");

    ComponentAst properties =
        (ComponentAst) connection.getParameter("Connection", "clientSocketProperties").getValue().getRight();
    DslElementModel<ObjectType> propertiesElement = getChild(connectionElement, properties);

    assertElementName(propertiesElement, "tcp-client-socket-properties");
    assertThat(propertiesElement.getDsl().isWrapped(), is(true));
    assertThat(propertiesElement.getDsl().supportsAttributeDeclaration(), is(false));
  }

  @Test
  public void flowElementsResolution() throws Exception {
    ComponentAst flow = getAppElement(applicationModel, COMPONENTS_FLOW);

    ComponentAst listener = flow.directChildrenStream().skip(LISTENER_PATH).findFirst().get();
    assertListenerSourceWithMessageBuilder(listener);

    ComponentAst dbBulkInsert = flow.directChildrenStream().skip(DB_BULK_INSERT_PATH).findFirst().get();
    assertBulkInsertOperationWithNestedList(dbBulkInsert);

    ComponentAst requester = flow.directChildrenStream().skip(REQUESTER_PATH).findFirst().get();
    assertRequestOperationWithFlatParameters(requester);

    ComponentAst dbInsert = flow.directChildrenStream().skip(DB_INSERT_PATH).findFirst().get();
    assertInsertOperationWithMaps(dbInsert);
  }

  @Test
  public void multiFlowModelLoaderFromComponentConfiguration() throws Exception {
    ExtensionModel jmsModel = muleContext.getExtensionManager().getExtension("JMS")
        .orElseThrow(() -> new IllegalStateException("Missing Extension JMS"));
    DslSyntaxResolver jmsDslResolver = DslSyntaxResolver.getDefault(jmsModel, dslContext);

    applicationModel = loadApplicationModel("multi-flow-dsl-app.xml");
    DslElementModel<ConfigurationModel> config = resolve(getAppElement(applicationModel, "config"));
    ConfigurationModel jmsConfigModel = jmsModel.getConfigurationModel("config").get();

    assertConfigLoaded(config, jmsConfigModel, jmsDslResolver);
    assertConnectionLoaded(config);

    OperationModel publishModel = jmsConfigModel.getOperationModel("publish").get();
    OperationModel consumeModel = jmsConfigModel.getOperationModel("consume").get();

    assertSendPayloadLoaded(publishModel, jmsDslResolver);
    assertBridgeLoaded(publishModel, consumeModel, jmsDslResolver);
    assertBridgeReceiverLoaded(consumeModel, jmsDslResolver);
  }

  @Test
  public void schedulerSource() {
    ComponentAst flow = getAppElement(applicationModel, "testFlowScheduler");

    ComponentAst scheduler = flow.directChildrenStream().findFirst().get();

    DslElementModel<SourceModel> schedulerElement = resolve(scheduler);

    ComponentAst schedulingStrategy =
        (ComponentAst) scheduler.getParameter(DEFAULT_GROUP_NAME, "schedulingStrategy").getValue().getRight();

    DslElementModel<ParameterModel> fixedFrequency = getChild(schedulerElement, schedulingStrategy);
    assertElementName(fixedFrequency, "fixed-frequency");

    assertValue(fixedFrequency.findElement("frequency").get(), "5");
  }

  private void assertSendPayloadLoaded(OperationModel publishModel,
                                       DslSyntaxResolver jmsDslResolver) {
    List<ComponentAst> sendOperations = getAppElement(applicationModel, "send-payload").directChildrenStream().collect(toList());
    assertThat(sendOperations.size(), is(1));

    DslElementModel<OperationModel> publishElement = resolve(sendOperations.get(0));
    assertThat(publishElement.getModel(), is(publishModel));
    assertThat(publishElement.getDsl(), is(jmsDslResolver.resolve(publishModel)));

    // attributes are present in the parent and its model is reachable, but no componentConfiguration is required
    assertThat(publishElement.getConfiguration().get().getParameters().get("destination"), is("#[initialDestination]"));
    assertThat(publishElement.findElement("destination").get().getConfiguration().isPresent(), is(false));
    assertThat(publishElement.findElement("destination").get().getModel(), is(findParameter("destination", publishModel)));

    // child element contains its configuration element along with its content
    DslElementModel<Object> builderElement = publishElement.findElement("Message").get();
    assertThat(builderElement.getModel(),
               is(publishModel.getParameterGroupModels().stream().filter(g -> g.getName().equals("Message")).findFirst().get()));
    Optional<ComponentConfiguration> messageBuilder = builderElement.getConfiguration();
    assertThat(messageBuilder.isPresent(), is(true));

    assertThat(messageBuilder.get().getNestedComponents().size(), is(2));
    assertThat(messageBuilder.get().getNestedComponents().get(1).getValue().get().trim(),
               is("#[{(initialProperty): propertyValue}]"));
  }

  private void assertBridgeLoaded(OperationModel publishModel, OperationModel consumeModel,
                                  DslSyntaxResolver jmsDslResolver) {
    List<ComponentAst> bridgeOperation = getAppElement(applicationModel, "bridge").directChildrenStream().collect(toList());
    assertThat(bridgeOperation.size(), is(2));

    DslElementModel<OperationModel> consumeElement = resolve(bridgeOperation.get(0));
    DslElementModel<OperationModel> publishElement =
        resolve(bridgeOperation.get(1).directChildrenStream().findFirst().get());
    assertThat(consumeElement.getModel(), is(consumeModel));
    assertThat(consumeElement.getDsl(), is(jmsDslResolver.resolve(consumeModel)));

    assertThat(consumeElement.getConfiguration().get().getParameters().get("destination"), is("#[initialDestination]"));
    assertThat(consumeElement.findElement("destination").get().getConfiguration().isPresent(), is(false));
    assertThat(consumeElement.findElement("destination").get().getModel(), is(findParameter("destination", consumeModel)));


    assertThat(publishElement.getModel(), is(publishModel));
    assertThat(publishElement.getDsl(), is(jmsDslResolver.resolve(publishModel)));

    assertThat(publishElement.findElement("destination").get().getModel(), is(findParameter("destination", publishModel)));
    assertThat(publishElement.findElement("destination").get().getConfiguration().isPresent(), is(false));
    assertThat(publishElement.getConfiguration().get().getParameters().get("destination"), is("#[finalDestination]"));

    DslElementModel<Object> builderElement = publishElement.findElement("Message").get();
    assertThat(builderElement.getModel(),
               is(publishModel.getParameterGroupModels().stream().filter(g -> g.getName().equals("Message")).findFirst().get()));
    Optional<ComponentConfiguration> messageBuilder = builderElement.getConfiguration();
    assertThat(messageBuilder.isPresent(), is(true));

  }

  private void assertBridgeReceiverLoaded(OperationModel consumeModel,
                                          DslSyntaxResolver jmsDslResolver) {
    List<ComponentAst> consumeOperation =
        getAppElement(applicationModel, "bridge-receiver").directChildrenStream().collect(toList());
    assertThat(consumeOperation.size(), is(1));

    DslElementModel<OperationModel> consumeElement = resolve(consumeOperation.get(0));
    assertThat(consumeElement.getModel(), is(consumeModel));
    assertThat(consumeElement.getDsl(), is(jmsDslResolver.resolve(consumeModel)));

    assertThat(consumeElement.findElement("destination").get().getModel(), is(findParameter("destination", consumeModel)));
    assertThat(consumeElement.findElement("destination").get().getConfiguration().isPresent(), is(false));
    assertThat(consumeElement.getConfiguration().get().getParameters().get("destination"), is("#[finalDestination]"));

    assertThat(consumeElement.findElement("ackMode").isPresent(), is(false));
  }

  private void assertConfigLoaded(DslElementModel<ConfigurationModel> config, ConfigurationModel jmsConfigModel,
                                  DslSyntaxResolver jmsDslResolver) {
    assertThat(config.getModel(), is(jmsConfigModel));
    assertThat(config.getDsl(), is(jmsDslResolver.resolve(jmsConfigModel)));
  }

  private void assertConnectionLoaded(DslElementModel<ConfigurationModel> config) {
    assertThat(config.getContainedElements().size(), is(6));
    assertThat(config.findElement("active-mq").isPresent(), is(true));
    assertThat(config.findElement("active-mq").get().getContainedElements().size(), is(3));

    assertThat(config.findElement(newIdentifier("xa-connection-pool", "jms")).isPresent(), is(true));
    assertThat(config.findElement(newIdentifier("xa-connection-pool", "jms")).get().getContainedElements().size(), is(3));

    assertThat(config.findElement(newIdentifier("consumer-config", "jms")).isPresent(), is(true));
    assertThat(config.findElement(newIdentifier("consumer-config", "jms")).get().getContainedElements().size(), is(2));

    assertThat(config.findElement(newIdentifier("producer-config", "jms")).isPresent(), is(true));
    assertThat(config.findElement(newIdentifier("producer-config", "jms")).get().getContainedElements().size(), is(7));

    assertThat(config.findElement(newIdentifier("no-caching", "jms")).isPresent(), is(true));

    assertThat(config.getContainedElements()
        .stream()
        .filter(element -> element.getDsl().getAttributeName().equals("sendCorrelationId"))
        .findAny()
        .isPresent(), is(true));
  }

  private ParameterModel findParameter(String name, ParameterizedModel model) {
    return model.getAllParameterModels().stream().filter(p -> p.getName().equals(name)).findFirst().get();
  }

  private void assertInsertOperationWithMaps(ComponentAst dbInsert) {
    DslElementModel<OperationModel> dbElement = resolve(dbInsert);

    assertThat(dbElement.getContainedElements().size(), is(9));

    DslElementModel<ParameterModel> sqlElement = getChild(dbElement, ComponentIdentifier.builder()
        .namespace(dbInsert.getIdentifier().getNamespace())
        .namespaceUri(dbInsert.getIdentifier().getNamespaceUri())
        .name("sql").build());
    assertElementName(sqlElement, "sql");

    DslElementModel<ParameterModel> parameterTypesElement = getChild(dbElement, ComponentIdentifier.builder()
        .namespace(dbInsert.getIdentifier().getNamespace())
        .namespaceUri(dbInsert.getIdentifier().getNamespaceUri())
        .name("parameter-types").build());
    assertElementName(parameterTypesElement, "parameter-types");

    DslElementModel<ObjectType> elementOne = parameterTypesElement.getContainedElements().get(0);
    assertElementName(elementOne, "parameter-type");
    assertValue(elementOne.findElement("key").get(), "description");
    assertValue(elementOne.findElement("type").get(), "CLOB");

    assertValue(dbElement.findElement(newIdentifier("input-parameters", DB_NS)).get(), "#[{{'description' : payload}}]");
  }

  protected void assertRequestOperationWithFlatParameters(ComponentAst requester) {
    DslElementModel<OperationModel> requesterElement = resolve(requester);
    assertHasParameter(requesterElement.getModel(), "path");
    assertThat(requesterElement.findElement("path").isPresent(), is(true));
    assertHasParameter(requesterElement.getModel(), "method");
    assertThat(requesterElement.findElement("method").isPresent(), is(true));
  }

  protected void assertBulkInsertOperationWithNestedList(ComponentAst dbInsert) {
    DslElementModel<OperationModel> bulkInsertElement = resolve(dbInsert);

    assertThat(bulkInsertElement.getContainedElements().size(), is(7));

    assertValue(bulkInsertElement.findElement("bulkInputParameters").get(), "#[payload]");

    DslElementModel<ParameterModel> sqlElement = getChild(bulkInsertElement, ComponentIdentifier.builder()
        .namespace(dbInsert.getIdentifier().getNamespace())
        .namespaceUri(dbInsert.getIdentifier().getNamespaceUri())
        .name("sql").build());
    assertElementName(sqlElement, "sql");
    assertValue(sqlElement, "INSERT INTO PLANET(POSITION, NAME) VALUES (:position, :name)");

    List<ComponentAst> parameterTypes =
        (List<ComponentAst>) dbInsert.getParameter("Query", "parameterTypes").getValue().getRight();
    DslElementModel<ParameterModel> parameterTypesElement = getChild(bulkInsertElement, ComponentIdentifier.builder()
        .namespace(dbInsert.getIdentifier().getNamespace())
        .namespaceUri(dbInsert.getIdentifier().getNamespaceUri())
        .name("parameter-types").build());
    assertElementName(parameterTypesElement, "parameter-types");

    ComponentAst parameterOne = parameterTypes.get(0);
    assertThat(parameterOne.getParameter("ParameterType", "key").getValue().getRight(), is("name"));
    DslElementModel<ObjectType> elementOne = parameterTypesElement.getContainedElements().get(0);
    assertElementName(elementOne, parameterOne.getIdentifier().getName());
    assertValue(elementOne.findElement("key").get(), "name");
    assertValue(elementOne.findElement("type").get(), "VARCHAR");

    ComponentAst parameterTwo = parameterTypes.get(1);
    assertThat(parameterTwo.getParameter("ParameterType", "key").getValue().getRight(), is("position"));
    DslElementModel<ObjectType> elementTwo = parameterTypesElement.getContainedElements().get(1);
    assertElementName(elementTwo, parameterTwo.getIdentifier().getName());
    assertValue(elementTwo.findElement("key").get(), "position");
    assertValue(elementTwo.findElement("type").get(), "INTEGER");
  }

  protected void assertListenerSourceWithMessageBuilder(ComponentAst listener) {
    DslElementModel<SourceModel> listenerElement = resolve(listener);

    assertHasParameter(listenerElement.getModel(), "path");

    DslElementModel<ParameterModel> responseBuilderElement = getChild(listenerElement, ComponentIdentifier.builder()
        .namespace(listener.getIdentifier().getNamespace())
        .namespaceUri(listener.getIdentifier().getNamespaceUri())
        .name("response").build());
    assertElementName(responseBuilderElement, "response");

    assertThat(responseBuilderElement.getDsl().getChild("headers").isPresent(), is(true));
    assertValue(responseBuilderElement.findElement(newIdentifier("headers", HTTP_NS)).get(),
                "#[{{'content-type' : 'text/plain'}}]");

    assertValue(listenerElement.findElement("path").get(), "testBuilder");
  }

}
