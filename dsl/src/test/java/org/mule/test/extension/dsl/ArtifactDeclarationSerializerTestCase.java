/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.extension.dsl;

import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.CONNECTION;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newArtifact;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newListValue;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newObjectValue;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newParameterGroup;
import static org.mule.runtime.app.declaration.api.fluent.ParameterSimpleValue.cdata;
import static org.mule.runtime.app.declaration.api.fluent.ParameterSimpleValue.plain;
import static org.mule.runtime.app.declaration.api.fluent.SimpleValueType.BOOLEAN;
import static org.mule.runtime.app.declaration.api.fluent.SimpleValueType.NUMBER;
import static org.mule.runtime.app.declaration.api.fluent.SimpleValueType.STRING;
import static org.mule.runtime.core.api.util.IOUtils.getResourceAsString;
import static org.mule.runtime.extension.api.ExtensionConstants.EXPIRATION_POLICY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.POOLING_PROFILE_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.PRIMARY_NODE_ONLY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.RECONNECTION_CONFIG_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.RECONNECTION_STRATEGY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.REDELIVERY_POLICY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.STREAMING_STRATEGY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_VALUE_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TLS_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TRANSACTIONAL_ACTION_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TRANSACTIONAL_TYPE_PARAMETER_NAME;
import static org.mule.runtime.extension.api.declaration.type.ReconnectionStrategyTypeBuilder.COUNT;
import static org.mule.runtime.extension.api.declaration.type.ReconnectionStrategyTypeBuilder.FREQUENCY;
import static org.mule.runtime.extension.api.declaration.type.ReconnectionStrategyTypeBuilder.RECONNECTION_CONFIG;
import static org.mule.runtime.extension.api.declaration.type.ReconnectionStrategyTypeBuilder.RECONNECT_ALIAS;
import static org.mule.runtime.extension.api.declaration.type.RedeliveryPolicyTypeBuilder.MAX_REDELIVERY_COUNT;
import static org.mule.runtime.extension.api.declaration.type.RedeliveryPolicyTypeBuilder.USE_SECURE_HASH;
import static org.mule.runtime.extension.api.declaration.type.StreamingStrategyTypeBuilder.REPEATABLE_IN_MEMORY_BYTES_STREAM_ALIAS;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.compareXML;
import org.mule.extensions.jms.api.connection.caching.NoCachingConfiguration;
import org.mule.runtime.api.app.declaration.serialization.ArtifactDeclarationJsonSerializer;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.app.declaration.api.ParameterElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterValue;
import org.mule.runtime.app.declaration.api.fluent.ElementDeclarer;
import org.mule.runtime.app.declaration.api.fluent.SimpleValueType;
import org.mule.runtime.config.api.dsl.ArtifactDeclarationXmlSerializer;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.retry.policy.RetryPolicyExhaustedException;
import org.mule.runtime.extension.api.runtime.ExpirationPolicy;
import org.mule.test.runner.RunnerDelegateTo;

import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.skyscreamer.jsonassert.JSONAssert;

@RunnerDelegateTo(Parameterized.class)
public class ArtifactDeclarationSerializerTestCase extends AbstractElementModelTestCase {

  private String expectedAppXml;
  private String expectedAppJson;

  @Parameterized.Parameter(0)
  public String configFile;

  @Parameterized.Parameter(1)
  public ArtifactDeclaration applicationDeclaration;

  @Parameterized.Parameter(2)
  public String declarationFile;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {"full-artifact-config-dsl-app.xml", createFullArtifactDeclaration(), "full-artifact-config-dsl-app.json"},
        {"multi-flow-dsl-app.xml", createMultiFlowArtifactDeclaration(), "multi-flow-dsl-app.json"},
        {"no-mule-components-dsl-app.xml", createNoMuleComponentsArtifactDeclaration(), "no-mule-components-dsl-app.json"}
    });
  }

  @Before
  public void loadExpectedResult() throws IOException {
    expectedAppXml = getResourceAsString(configFile, getClass());

    JsonParser parser = new JsonParser();
    JsonReader reader = new JsonReader(new InputStreamReader(currentThread().getContextClassLoader()
        .getResourceAsStream(declarationFile)));
    expectedAppJson = parser.parse(reader).toString();
  }

  @Test
  public void serialize() throws Exception {
    String serializationResult = ArtifactDeclarationXmlSerializer.getDefault(dslContext).serialize(applicationDeclaration);
    compareXML(expectedAppXml, serializationResult);
  }

  @Test
  public void loadCustomConfigParameters() {
    InputStream configIs = currentThread().getContextClassLoader().getResourceAsStream(configFile);
    ArtifactDeclarationXmlSerializer serializer = ArtifactDeclarationXmlSerializer.getDefault(dslContext);

    ArtifactDeclaration artifact = serializer.deserialize(configIs);

    List<String> expectedCustomParams = asList("xmlns", "xmlns:xsi", "xsi:schemaLocation");
    List<ParameterElementDeclaration> customParameters = artifact.getCustomConfigurationParameters();
    expectedCustomParams
        .forEach(custom -> assertThat("Missing parameter: " + custom,
                                      customParameters.stream().anyMatch(p -> p.getName().equals(custom)), is(true)));
  }

  @Test
  public void loadAndSerialize() throws Exception {
    InputStream configIs = currentThread().getContextClassLoader().getResourceAsStream(configFile);
    ArtifactDeclarationXmlSerializer serializer = ArtifactDeclarationXmlSerializer.getDefault(dslContext);

    ArtifactDeclaration artifact = serializer.deserialize(configIs);

    String serializationResult = serializer.serialize(artifact);

    compareXML(expectedAppXml, serializationResult);
  }

  @Test
  public void serializesToJson() {
    InputStream configIs = currentThread().getContextClassLoader().getResourceAsStream(configFile);
    ArtifactDeclarationXmlSerializer serializer = ArtifactDeclarationXmlSerializer.getDefault(dslContext);
    ArtifactDeclaration expectedDeclaration = serializer.deserialize(configIs);

    ArtifactDeclarationJsonSerializer jsonSerializer = ArtifactDeclarationJsonSerializer.getDefault(true);
    String actualAppJson = jsonSerializer.serialize(expectedDeclaration);

    JSONAssert.assertEquals(expectedAppJson, actualAppJson, true);
  }

  @Override
  protected String[] getConfigFiles() {
    return new String[] {};
  }

  private static ArtifactDeclaration createMultiFlowArtifactDeclaration() {
    ElementDeclarer jms = ElementDeclarer.forExtension("JMS");
    ElementDeclarer core = ElementDeclarer.forExtension("mule");

    return newArtifact()
        .withGlobalElement(jms.newConfiguration("config")
            .withRefName("config")
            .withConnection(jms.newConnection("active-mq")
                .withParameterGroup(newParameterGroup()
                    .withParameter("cachingStrategy",
                                   newObjectValue()
                                       .ofType(NoCachingConfiguration.class.getName())
                                       .build())
                    .getDeclaration())
                .getDeclaration())
            .getDeclaration())
        .withGlobalElement(core.newConstruct("flow")
            .withRefName("send-payload")
            .withComponent(jms.newOperation("publish")
                .withConfig("config")
                .withParameterGroup(newParameterGroup()
                    .withParameter("destination", createStringParameter("#[initialDestination]"))
                    .getDeclaration())
                .withParameterGroup(newParameterGroup("Message")
                    .withParameter("body", createStringParameter("#[payload]"))
                    .withParameter("properties", createStringParameter("#[{(initialProperty): propertyValue}]"))
                    .getDeclaration())
                .getDeclaration())
            .getDeclaration())
        .withGlobalElement(core.newConstruct("flow").withRefName("bridge")
            .withComponent(jms.newOperation("consume")
                .withConfig("config")
                .withParameterGroup(newParameterGroup()
                    .withParameter("destination", createStringParameter("#[initialDestination]"))
                    .withParameter("maximumWait", createNumberParameter("1000"))
                    .getDeclaration())
                .getDeclaration())
            .withComponent(core.newConstruct("foreach")
                .withComponent(jms.newOperation("publish")
                    .withConfig("config")
                    .withParameterGroup(newParameterGroup()
                        .withParameter("destination", createStringParameter("#[finalDestination]"))
                        .getDeclaration())
                    .withParameterGroup(newParameterGroup("Message")
                        .withParameter("jmsxProperties",
                                       createStringParameter("#[attributes.properties.jmsxProperties]"))
                        .withParameter("body",
                                       createStringParameter("#[bridgePrefix ++ payload]"))
                        .withParameter("properties",
                                       createStringParameter("#[attributes.properties.userProperties]"))
                        .getDeclaration())
                    .getDeclaration())
                .withComponent(core
                    .newOperation("logger")
                    .withParameterGroup(newParameterGroup()
                        .withParameter("message", createStringParameter("Message Sent"))
                        .getDeclaration())
                    .getDeclaration())
                .getDeclaration())
            .getDeclaration())
        .withGlobalElement(core.newConstruct("flow").withRefName("jmsListener")
            .withParameterGroup(group -> group.withParameter("initialState", createStringParameter("stopped")))
            .withComponent(jms.newSource("listener")
                .withConfig("config")
                .withParameterGroup(newParameterGroup()
                    .withParameter("destination", "listen-queue")
                    .withParameter(TRANSACTIONAL_TYPE_PARAMETER_NAME, createStringParameter("LOCAL"))
                    .withParameter(PRIMARY_NODE_ONLY_PARAMETER_NAME, createBooleanParameter("false"))
                    .getDeclaration())
                .getDeclaration())
            .withComponent(jms.newOperation("consume")
                .withConfig("config")
                .withParameterGroup(newParameterGroup()
                    .withParameter("destination", createStringParameter("#[finalDestination]"))
                    .withParameter("maximumWait", createNumberParameter("1000"))
                    .getDeclaration())
                .getDeclaration())
            .getDeclaration())
        .withGlobalElement(core.newConstruct("flow").withRefName("bridge-receiver")
            .withComponent(jms
                .newOperation("consume")
                .withConfig("config")
                .withParameterGroup(newParameterGroup()
                    .withParameter("destination", createStringParameter("#[finalDestination]"))
                    .withParameter("maximumWait", createNumberParameter("1000"))
                    .getDeclaration())
                .getDeclaration())
            .getDeclaration())
        .getDeclaration();
  }

  private static ArtifactDeclaration createFullArtifactDeclaration() {

    ElementDeclarer core = ElementDeclarer.forExtension("mule");
    ElementDeclarer db = ElementDeclarer.forExtension("Database");
    ElementDeclarer http = ElementDeclarer.forExtension("HTTP");
    ElementDeclarer sockets = ElementDeclarer.forExtension("Sockets");
    ElementDeclarer wsc = ElementDeclarer.forExtension("Web Service Consumer");
    ElementDeclarer file = ElementDeclarer.forExtension("File");
    ElementDeclarer os = ElementDeclarer.forExtension("ObjectStore");

    return newArtifact()
        .withCustomParameter("xmlns:doc", "http://www.mulesoft.org/schema/mule/documentation")
        .withGlobalElement(core.newConstruct("configuration")
            .withParameterGroup(group -> group.withParameter("defaultErrorHandler-ref",
                                                             createStringParameter("referableHandler")))
            .getDeclaration())
        .withGlobalElement(core.newConstruct("object")
            .withParameterGroup(group -> group
                .withParameter("name", createStringParameter("failingProcessor"))
                .withParameter("class",
                               createStringParameter("org.mule.test.extension.dsl.ArtifactDeclarationSerializerTestCase$FailingProcessor")))
            .getDeclaration())
        .withGlobalElement(core.newConstruct("errorHandler")
            .withRefName("referableHandler")
            .withComponent(core.newRoute("onErrorContinue")
                .withParameterGroup(group -> group
                    .withParameter("type", createStringParameter("MULE:SOURCE_RESPONSE"))
                    .withParameter("logException", createBooleanParameter("false"))
                    .withParameter("enableNotifications", createBooleanParameter("false")))
                .withComponent(core.newOperation("logger")
                    .withParameterGroup(group -> group
                        .withParameter("level", createStringParameter("TRACE")))
                    .getDeclaration())
                .getDeclaration())
            .getDeclaration())
        .withGlobalElement(os.newGlobalParameter("objectStore")
            .withRefName("persistentStore")
            .withValue(newObjectValue()
                .ofType("org.mule.extension.objectstore.api.TopLevelObjectStore")
                .withParameter("entryTtl", createNumberParameter("1"))
                .withParameter("entryTtlUnit", createStringParameter("HOURS"))
                .withParameter("maxEntries", createNumberParameter("10"))
                .withParameter("persistent", createBooleanParameter("true"))
                .withParameter("expirationInterval", createNumberParameter("2"))
                .withParameter("expirationIntervalUnit", createStringParameter("HOURS"))
                .withParameter("config-ref", createStringParameter("persistentConfig"))
                .build())
            .getDeclaration())
        .withGlobalElement(os.newConfiguration("config")
            .withRefName("persistentConfig")
            .getDeclaration())
        .withGlobalElement(db.newConfiguration("config")
            .withRefName("dbConfig")
            .withConnection(db
                .newConnection("derby").withParameterGroup(newParameterGroup()
                    .withParameter(POOLING_PROFILE_PARAMETER_NAME, newObjectValue()
                        .withParameter("maxPoolSize", createNumberParameter("10"))
                        .build())
                    .getDeclaration())
                .withParameterGroup(newParameterGroup(CONNECTION)
                    .withParameter("connectionProperties", newObjectValue()
                        .withParameter("first", createStringParameter("propertyOne"))
                        .withParameter("second", createStringParameter("propertyTwo"))
                        .build())
                    .withParameter("database", createStringParameter("target/muleEmbeddedDB"))
                    .withParameter("create", createBooleanParameter("true"))
                    .getDeclaration())
                .getDeclaration())
            .getDeclaration())
        .withGlobalElement(file.newConfiguration("config")
            .withRefName("fileConfig")
            .withConnection(file.newConnection("connection").getDeclaration())
            .getDeclaration())
        .withGlobalElement(wsc.newConfiguration("config")
            .withRefName("wscConfig")
            .withParameterGroup(newParameterGroup()
                .withParameter(EXPIRATION_POLICY_PARAMETER_NAME, newObjectValue()
                    .ofType(ExpirationPolicy.class.getName())
                    .withParameter("maxIdleTime", createNumberParameter("1"))
                    .withParameter("timeUnit", createStringParameter(MINUTES.name()))
                    .build())
                .getDeclaration())
            .withConnection(wsc.newConnection("connection")
                .withParameterGroup(newParameterGroup()
                    .withParameter("soapVersion", createStringParameter("SOAP11"))
                    .withParameter("mtomEnabled", createBooleanParameter("false"))
                    .getDeclaration())
                .withParameterGroup(newParameterGroup("Connection")
                    .withParameter("wsdlLocation", createStringParameter("http://www.webservicex.com/globalweather.asmx?WSDL"))
                    .withParameter("address", createStringParameter("http://www.webservicex.com/globalweather.asmx"))
                    .withParameter("service", createStringParameter("GlobalWeather"))
                    .withParameter("port", createStringParameter("GlobalWeatherSoap"))
                    .getDeclaration())
                .getDeclaration())
            .getDeclaration())
        .withGlobalElement(db.newConfiguration("config")
            .withRefName("dbConfig")
            .withConnection(db
                .newConnection("derby").withParameterGroup(newParameterGroup()
                    .withParameter(POOLING_PROFILE_PARAMETER_NAME, newObjectValue()
                        .withParameter("maxPoolSize", createNumberParameter("10"))
                        .build())
                    .getDeclaration())
                .withParameterGroup(newParameterGroup(CONNECTION)
                    .withParameter("connectionProperties", newObjectValue()
                        .withParameter("first", createStringParameter("propertyOne"))
                        .withParameter("second", createStringParameter("propertyTwo"))
                        .build())
                    .withParameter(RECONNECTION_CONFIG_PARAMETER_NAME, newObjectValue()
                        .ofType(RECONNECTION_CONFIG)
                        .withParameter("failsDeployment", createBooleanParameter("true"))
                        .withParameter("reconnectionStrategy", newObjectValue()
                            .ofType(RECONNECT_ALIAS)
                            .withParameter(COUNT, createNumberParameter("1"))
                            .withParameter(FREQUENCY, createNumberParameter("0"))
                            .build())
                        .build())
                    .withParameter("database", createStringParameter("target/muleEmbeddedDB"))
                    .withParameter("create", createBooleanParameter("true"))
                    .getDeclaration())
                .getDeclaration())
            .getDeclaration())
        .withGlobalElement(http.newConfiguration("listenerConfig")
            .withRefName("httpListener")
            .withParameterGroup(newParameterGroup()
                .withParameter("basePath", createStringParameter("/"))
                .getDeclaration())
            .withConnection(http.newConnection("listener")
                .withParameterGroup(newParameterGroup()
                    .withParameter(TLS_PARAMETER_NAME, newObjectValue()
                        .withParameter("key-store", newObjectValue()
                            .withParameter("path", createStringParameter("ssltest-keystore.jks"))
                            .withParameter("password", createStringParameter("changeit"))
                            .withParameter("keyPassword", createStringParameter("changeit"))
                            .build())
                        .withParameter("trust-store", newObjectValue()
                            .withParameter("insecure", createBooleanParameter("true"))
                            .build())
                        .withParameter("revocation-check",
                                       newObjectValue()
                                           .ofType("standard-revocation-check")
                                           .withParameter("onlyEndEntities", createBooleanParameter("true"))
                                           .build())
                        .build())
                    .getDeclaration())
                .withParameterGroup(group -> group.withName(CONNECTION)
                    .withParameter("host", createStringParameter("localhost"))
                    .withParameter("port", createNumberParameter("49019"))
                    .withParameter("protocol", createStringParameter("HTTPS")))
                .getDeclaration())
            .getDeclaration())
        .withGlobalElement(http.newConfiguration("requestConfig")
            .withRefName("httpRequester")
            .withParameterGroup(group -> group.withName("Request Settings")
                .withParameter("requestStreamingMode", createStringParameter("ALWAYS"))
                .withParameter("defaultHeaders", newListValue().withValue(newObjectValue()
                    .withParameter("key", createStringParameter("testDefault"))
                    .withParameter("value", createStringParameter("testDefaultValue"))
                    .build())
                    .build()))
            .withConnection(http.newConnection("request")
                .withParameterGroup(group -> group.withParameter("authentication",
                                                                 newObjectValue()
                                                                     .ofType("org.mule.extension.http.api.request.authentication.BasicAuthentication")
                                                                     .withParameter("username", createStringParameter("user"))
                                                                     .withParameter("password", createStringParameter("pass"))
                                                                     .build()))
                .withParameterGroup(newParameterGroup(CONNECTION)
                    .withParameter("host", createStringParameter("localhost"))
                    .withParameter("port", createNumberParameter("49020"))
                    .withParameter("clientSocketProperties",
                                   newObjectValue()
                                       .withParameter("connectionTimeout", createNumberParameter("1000"))
                                       .withParameter("keepAlive", createBooleanParameter("true"))
                                       .withParameter("receiveBufferSize", createNumberParameter("1024"))
                                       .withParameter("sendBufferSize", createNumberParameter("1024"))
                                       .withParameter("clientTimeout", createNumberParameter("1000"))
                                       .withParameter("linger", createNumberParameter("1000"))
                                       .build())
                    .getDeclaration())
                .getDeclaration())
            .getDeclaration())
        .withGlobalElement(core.newConstruct("flow")
            .withRefName("testFlow")
            .withCustomParameter("doc:id", "docUUID")
            .withParameterGroup(group -> group.withParameter("initialState", createStringParameter("stopped")))
            .withComponent(http.newSource("listener")
                .withConfig("httpListener")
                .withCustomParameter("doc:id", "docUUID")
                .withParameterGroup(newParameterGroup()
                    .withParameter("path", createStringParameter("testBuilder"))
                    .withParameter(REDELIVERY_POLICY_PARAMETER_NAME,
                                   newObjectValue()
                                       .withParameter(MAX_REDELIVERY_COUNT, createNumberParameter("2"))
                                       .withParameter(USE_SECURE_HASH, createBooleanParameter("true"))
                                       .build())
                    .getDeclaration())
                .withParameterGroup(group -> group.withName(CONNECTION)
                    .withParameter(RECONNECTION_STRATEGY_PARAMETER_NAME,
                                   newObjectValue()
                                       .ofType(RECONNECT_ALIAS)
                                       .withParameter(COUNT, createNumberParameter("1"))
                                       .withParameter(FREQUENCY, createNumberParameter("0"))
                                       .build()))
                .withParameterGroup(newParameterGroup("Response")
                    .withParameter("headers", createStringCDataParameter("<![CDATA[#[{{'content-type' : 'text/plain'}}]]]>"))
                    .withParameter("body",
                                   createStringCDataParameter("<![CDATA[#[\n"
                                       + "                    %dw 2.0\n"
                                       + "                    output application/json\n"
                                       + "                    input payload application/xml\n"
                                       + "                    var baseUrl=\"http://sample.cloudhub.io/api/v1.0/\"\n"
                                       + "                    ---\n"
                                       + "                    using (pageSize = payload.getItemsResponse.PageInfo.pageSize) {\n"
                                       + "                         links: [\n"
                                       + "                            {\n"
                                       + "                                href: fullUrl,\n"
                                       + "                                rel : \"self\"\n"
                                       + "                            }\n"
                                       + "                         ],\n"
                                       + "                         collection: {\n"
                                       + "                            size: pageSize,\n"
                                       + "                            items: payload.getItemsResponse.*Item map {\n"
                                       + "                                id: $.id,\n"
                                       + "                                type: $.type,\n"
                                       + "                                name: $.name\n"
                                       + "                            }\n"
                                       + "                         }\n"
                                       + "                    }\n"
                                       + "                ]]>"))
                    .getDeclaration())
                .getDeclaration())
            .withComponent(core.newConstruct("choice")
                .withRoute(core.newRoute("when")
                    .withParameterGroup(newParameterGroup()
                        .withParameter("expression", createStringParameter("#[true]"))
                        .getDeclaration())
                    .withComponent(db.newOperation("bulkInsert")
                        .withParameterGroup(newParameterGroup()
                            .withParameter(TRANSACTIONAL_ACTION_PARAMETER_NAME,
                                           createStringParameter("ALWAYS_JOIN"))
                            .getDeclaration())
                        .withParameterGroup(newParameterGroup("Query")
                            .withParameter("sql",
                                           createStringParameter("INSERT INTO PLANET(POSITION, NAME) VALUES (:position, :name)"))
                            .withParameter("parameterTypes",
                                           newListValue()
                                               .withValue(newObjectValue()
                                                   .withParameter("key", createStringParameter("name"))
                                                   .withParameter("type", createStringParameter("VARCHAR"))
                                                   .build())
                                               .withValue(newObjectValue()
                                                   .withParameter("key", createStringParameter("position"))
                                                   .withParameter("type", createStringParameter("INTEGER"))
                                                   .build())
                                               .build())
                            .getDeclaration())
                        .getDeclaration())
                    .getDeclaration())
                .withRoute(core.newRoute("otherwise")
                    .withComponent(core.newConstruct("foreach")
                        .withParameterGroup(newParameterGroup()
                            .withParameter("collection", createStringParameter("#[myCollection]"))
                            .getDeclaration())
                        .withComponent(core.newOperation("logger")
                            .withParameterGroup(newParameterGroup()
                                .withParameter("message", createStringParameter("#[payload]"))
                                .getDeclaration())
                            .getDeclaration())
                        .getDeclaration())
                    .getDeclaration())
                .getDeclaration())
            .withComponent(db.newOperation("bulkInsert")
                .withParameterGroup(newParameterGroup("General")
                    .withParameter("bulkInputParameters", createStringParameter("#[payload.changes]"))
                    .getDeclaration())
                .withParameterGroup(newParameterGroup("Query")
                    .withParameter("sql", createStringParameter("INSERT INTO PLANET(POSITION, NAME) VALUES (:position, :name)"))
                    .withParameter("parameterTypes",
                                   newListValue()
                                       .withValue(newObjectValue()
                                           .withParameter("key", createStringParameter("name"))
                                           .withParameter("type", createStringParameter("VARCHAR")).build())
                                       .withValue(newObjectValue()
                                           .withParameter("key", createStringParameter("position"))
                                           .withParameter("type", createStringParameter("INTEGER")).build())
                                       .build())
                    .getDeclaration())
                .getDeclaration())
            .withComponent(http.newOperation("request")
                .withConfig("httpRequester")
                .withParameterGroup(newParameterGroup("URI Settings")
                    .withParameter("path", createStringParameter("/nested"))
                    .getDeclaration())
                .withParameterGroup(newParameterGroup()
                    .withParameter("method", createStringParameter("POST"))
                    .getDeclaration())
                .getDeclaration())
            .withComponent(db.newOperation("insert")
                .withConfig("dbConfig")
                .withParameterGroup(newParameterGroup("Query")
                    .withParameter("sql",
                                   createStringParameter("INSERT INTO PLANET(POSITION, NAME, DESCRIPTION) VALUES (777, 'Pluto', :description)"))
                    .withParameter("parameterTypes",
                                   newListValue()
                                       .withValue(newObjectValue()
                                           .withParameter("key", createStringParameter("description"))
                                           .withParameter("type", createStringParameter("CLOB")).build())
                                       .build())
                    .withParameter("inputParameters", createStringParameter("#[{{'description' : payload}}]"))
                    .getDeclaration())
                .getDeclaration())
            .withComponent(sockets.newOperation("sendAndReceive")
                .withParameterGroup(newParameterGroup()
                    .withParameter(STREAMING_STRATEGY_PARAMETER_NAME,
                                   newObjectValue()
                                       .ofType(REPEATABLE_IN_MEMORY_BYTES_STREAM_ALIAS)
                                       .withParameter("bufferSizeIncrement", createNumberParameter("8"))
                                       .withParameter("bufferUnit", createStringParameter("KB"))
                                       .withParameter("initialBufferSize", createNumberParameter("51"))
                                       .withParameter("maxBufferSize", createNumberParameter("1000"))
                                       .build())
                    .getDeclaration())
                .withParameterGroup(newParameterGroup("Output")
                    .withParameter(TARGET_PARAMETER_NAME, createStringParameter("myVar"))
                    .withParameter(TARGET_VALUE_PARAMETER_NAME, createStringParameter("#[message]"))
                    .getDeclaration())
                .getDeclaration())
            .withComponent(core.newOperation("flowRef")
                .withParameterGroup(newParameterGroup()
                    .withParameter("name", createStringParameter("testSubFlow"))
                    .getDeclaration())
                .getDeclaration())
            .withComponent(core.newConstruct("try")
                .withComponent(wsc.newOperation("consume")
                    .withParameterGroup(newParameterGroup()
                        .withParameter("operation", createStringParameter("GetCitiesByCountry"))
                        .getDeclaration())
                    .withParameterGroup(newParameterGroup("Message")
                        .withParameter("attachments", createStringParameter("#[{}]"))
                        .withParameter("headers", createStringParameter(
                                                                        "#[{\"headers\": {con#headerIn: \"Header In Value\",con#headerInOut: \"Header In Out Value\"}]"))
                        .withParameter("body", createStringParameter("#[payload]"))
                        .getDeclaration())
                    .getDeclaration())
                .withComponent(core.newConstruct("errorHandler")
                    .withComponent(core.newRoute("onErrorContinue")
                        .withParameterGroup(newParameterGroup()
                            .withParameter("type", createStringParameter("MULE:ANY"))
                            .getDeclaration())
                        .withComponent(core.newOperation("logger").getDeclaration())
                        .getDeclaration())
                    .withComponent(core.newRoute("onErrorPropagate")
                        .withParameterGroup(newParameterGroup()
                            .withParameter("type", createStringParameter("WSC:CONNECTIVITY"))
                            .withParameter("when", createStringParameter("#[e.cause == null]"))
                            .getDeclaration())
                        .getDeclaration())
                    .getDeclaration())
                .getDeclaration())
            .getDeclaration())
        .withGlobalElement(core.newConstruct("flow").withRefName("schedulerFlow")
            .withComponent(core.newSource("scheduler")
                .withParameterGroup(newParameterGroup()
                    .withParameter("schedulingStrategy", newObjectValue()
                        .ofType("org.mule.runtime.core.api.source.scheduler.FixedFrequencyScheduler")
                        .withParameter("frequency", createNumberParameter("50"))
                        .withParameter("startDelay", createNumberParameter("20"))
                        .withParameter("timeUnit", createStringParameter("SECONDS"))
                        .build())
                    .getDeclaration())
                .getDeclaration())
            .withComponent(core.newOperation("logger")
                .withParameterGroup(newParameterGroup()
                    .withParameter("message", createStringParameter("#[payload]")).getDeclaration())
                .getDeclaration())
            .getDeclaration())
        .withGlobalElement(core.newConstruct("flow").withRefName("cronSchedulerFlow")
            .withComponent(core.newSource("scheduler")
                .withParameterGroup(newParameterGroup()
                    .withParameter("schedulingStrategy", newObjectValue()
                        .ofType("org.mule.runtime.core.api.source.scheduler.CronScheduler")
                        .withParameter("expression", createStringParameter("0/1 * * * * ?"))
                        .build())
                    .getDeclaration())
                .getDeclaration())
            .withComponent(core.newOperation("logger")
                .withParameterGroup(newParameterGroup()
                    .withParameter("message", createStringParameter("#[payload]")).getDeclaration())
                .getDeclaration())
            .getDeclaration())
        .withGlobalElement(core.newConstruct("flow").withRefName("fileListenerToObjectStore")
            .withComponent(file.newSource("listener")
                .withConfig("fileConfig")
                .withParameterGroup(newParameterGroup()
                    .withParameter("schedulingStrategy", newObjectValue()
                        .ofType("org.mule.runtime.core.api.source.scheduler.FixedFrequencyScheduler")
                        .withParameter("frequency", "1")
                        .withParameter("timeUnit", "MINUTES")
                        .build())
                    .getDeclaration())
                .getDeclaration())
            .withComponent(os.newOperation("store")
                .withParameterGroup(newParameterGroup()
                    .withParameter("key", createStringParameter("key"))
                    .withParameter("failOnNullValue", createBooleanParameter("#[vars.failOnNullValue]"))
                    .withParameter("objectStore", createStringParameter("persistentStore"))
                    .withParameter("value", createStringParameter("#[payload]"))
                    .getDeclaration())
                .getDeclaration())
            .getDeclaration())
        .withGlobalElement(core.newConstruct("flow").withRefName("fileListenerToObjectStoreCron")
            .withComponent(file.newSource("listener")
                .withConfig("fileConfig")
                .withParameterGroup(newParameterGroup()
                    .withParameter("schedulingStrategy", newObjectValue()
                        .ofType("org.mule.runtime.core.api.source.scheduler.CronScheduler")
                        .withParameter("expression", createStringParameter("0,4,25,26,53 0 0 ? * * *"))
                        .build())
                    .getDeclaration())
                .getDeclaration())
            .withComponent(os.newOperation("store")
                .withParameterGroup(newParameterGroup()
                    .withParameter("key", createStringParameter("key"))
                    .withParameter("failOnNullValue", createBooleanParameter("#[vars.failOnNullValue]"))
                    .withParameter("objectStore", createStringParameter("persistentStore"))
                    .withParameter("value", createStringParameter("#[payload]"))
                    .getDeclaration())
                .getDeclaration())
            .getDeclaration())
        .withGlobalElement(core.newConstruct("flow").withRefName("dbListenerToObjectStoreCron")
            .withComponent(db.newSource("listener")
                .withConfig("dbConfig")
                .withParameterGroup(newParameterGroup()
                    .withParameter("schedulingStrategy", newObjectValue()
                        .ofType("org.mule.runtime.core.api.source.scheduler.CronScheduler")
                        .withParameter("expression", createStringParameter("0,4,25,26,51 0 0 ? * * *"))
                        .build())
                    .withParameter("table", createStringParameter("person"))
                    .withParameter("watermarkColumn", createStringParameter("timestamp"))
                    .withParameter("idColumn", createStringParameter("id"))
                    .getDeclaration())
                .getDeclaration())
            .withComponent(os.newOperation("store")
                .withParameterGroup(newParameterGroup()
                    .withParameter("key", createStringParameter("key"))
                    .withParameter("failOnNullValue", createBooleanParameter("#[vars.failOnNullValue]"))
                    .withParameter("objectStore", createStringParameter("persistentStore"))
                    .withParameter("value", createStringParameter("#[payload]"))
                    .getDeclaration())
                .getDeclaration())
            .getDeclaration())
        .withGlobalElement(core.newConstruct("flow").withRefName("testSubFlow")
            .withComponent(core
                .newOperation("logger")
                .withParameterGroup(newParameterGroup()
                    .withParameter("message", createStringParameter("onTestSubFlow"))
                    .getDeclaration())
                .getDeclaration())
            .getDeclaration())
        .getDeclaration();
  }

  private static Object createNoMuleComponentsArtifactDeclaration() {
    ElementDeclarer core = ElementDeclarer.forExtension("mule");
    ElementDeclarer jms = ElementDeclarer.forExtension("JMS");
    ElementDeclarer http = ElementDeclarer.forExtension("HTTP");

    return newArtifact()
        .withGlobalElement(jms.newConfiguration("config")
            .withRefName("config")
            .withConnection(jms.newConnection("active-mq")
                .withParameterGroup(newParameterGroup()
                    .withParameter("cachingStrategy",
                                   newObjectValue()
                                       .ofType(NoCachingConfiguration.class.getName())
                                       .build())
                    .getDeclaration())
                .getDeclaration())
            .getDeclaration())
        .withGlobalElement(http.newConfiguration("requestConfig")
            .withRefName("httpRequester")
            .withConnection(http.newConnection("request").getDeclaration()).getDeclaration())
        .withGlobalElement(core.newConstruct("flow")
            .withRefName("send-payload")
            .withComponent(jms.newOperation("publish")
                .withConfig("config")
                .withParameterGroup(newParameterGroup()
                    .withParameter("destination", createStringParameter("#[initialDestination]"))
                    .getDeclaration())
                .withParameterGroup(newParameterGroup("Message")
                    .withParameter("body", createStringParameter("#[payload]"))
                    .withParameter("properties", createStringParameter("#[{(initialProperty): propertyValue}]"))
                    .getDeclaration())
                .getDeclaration())
            .withComponent(http.newOperation("request")
                .withConfig("httpRequester")
                .withParameterGroup(newParameterGroup("URI Settings")
                    .withParameter("path", createStringParameter("/nested"))
                    .getDeclaration())
                .withParameterGroup(newParameterGroup()
                    .withParameter("method", createStringParameter("POST"))
                    .getDeclaration())
                .getDeclaration())
            .getDeclaration())
        .withGlobalElement(core.newConstruct("flow").withRefName("bridge")
            .withComponent(jms.newOperation("consume")
                .withConfig("config")
                .withParameterGroup(newParameterGroup()
                    .withParameter("destination", createStringParameter("#[initialDestination]"))
                    .withParameter("maximumWait", createNumberParameter("1000"))
                    .getDeclaration())
                .getDeclaration())
            .withComponent(http.newOperation("request")
                .withConfig("httpRequester")
                .withParameterGroup(newParameterGroup("URI Settings")
                    .withParameter("path", createStringParameter("/nested"))
                    .getDeclaration())
                .getDeclaration())
            .getDeclaration())
        .getDeclaration();
  }

  private static ParameterValue createNumberParameter(String value) {
    return createParameter(value, NUMBER);
  }

  private static ParameterValue createStringParameter(String value) {
    return createParameter(value, STRING);
  }

  private static ParameterValue createBooleanParameter(String value) {
    return createParameter(value, BOOLEAN);
  }

  private static ParameterValue createParameter(String value, SimpleValueType type) {
    return plain(value, type);
  }

  private static ParameterValue createStringCDataParameter(String value) {
    return cdata(value, STRING);
  }

  public static class FailingProcessor implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      throw new RetryPolicyExhaustedException(createStaticMessage("Error."), mock(Initialisable.class));
    }

  }

}
