/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.ast;

import static java.lang.Boolean.TRUE;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
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
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.FLOW_IDENTIFIER;
import static org.mule.runtime.core.api.construct.Flow.INITIAL_STATE_STARTED;
import static org.mule.runtime.core.api.construct.Flow.INITIAL_STATE_STOPPED;
import static org.mule.runtime.extension.api.ExtensionConstants.ERROR_MAPPINGS_PARAMETER_NAME;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isMap;
import static org.mule.test.allure.AllureConstants.ArtifactAst.ARTIFACT_AST;
import static org.mule.test.allure.AllureConstants.ArtifactAst.ParameterAst.PARAMETER_AST;
import static org.mule.test.allure.AllureConstants.SourcesFeature.SOURCES;

import org.mule.extension.aggregator.internal.AggregatorsExtension;
import org.mule.extension.db.internal.DbConnector;
import org.mule.extension.http.api.request.proxy.HttpProxyConfig;
import org.mule.extension.http.internal.temporary.HttpConnector;
import org.mule.extension.oauth2.OAuthExtension;
import org.mule.extension.socket.api.SocketsExtension;
import org.mule.metadata.api.annotation.EnumAnnotation;
import org.mule.metadata.api.annotation.IntAnnotation;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.NumberType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.construct.ConstructModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.extension.api.error.ErrorMapping;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.heisenberg.extension.model.RecursivePojo;
import org.mule.test.heisenberg.extension.model.Weapon;
import org.mule.test.petstore.extension.PetStoreConnector;
import org.mule.test.subtypes.extension.SubTypesMappingConnector;
import org.mule.test.vegan.extension.VeganExtension;

import java.util.List;
import java.util.Optional;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(ARTIFACT_AST)
@Story(PARAMETER_AST)
public class ParameterAstTestCase extends BaseParameterAstTestCase {

  private static final String NAME = "name";

  @Issue("MULE-18564")
  @Test
  public void oauthCredentialThroughProxyInlineDefinition() {
    ArtifactAst artifactAst = buildArtifactAst("parameters-test-http-oauth-proxy-config.xml",
                                               HttpConnector.class, SocketsExtension.class, OAuthExtension.class);

    ComponentAst httpRequestConfigWithOAuthProxyInline =
        findComponentByComponentId(artifactAst.topLevelComponentsStream(), "httpRequestConfigWithOAuthProxyInline")
            .orElseThrow(() -> new AssertionError("Couldn't find 'httpRequestConfigWithOAuthProxyInline'"));

    ComponentAst oAuthHttpRequestConnection =
        findComponent(httpRequestConfigWithOAuthProxyInline.directChildrenStream(), "http:request-connection")
            .orElseThrow(() -> new AssertionError("Couldn't find 'http:request-connection'"));

    ComponentParameterAst proxyConfig = oAuthHttpRequestConnection.getParameter("proxyConfig");
    assertThat(proxyConfig.getRawValue(), is(nullValue()));
    assertThat(proxyConfig.getValue(), is(Either.empty()));
    assertThat(getTypeId(proxyConfig.getModel().getType()), equalTo(of(HttpProxyConfig.class.getName())));

    ComponentAst grantType = (ComponentAst) oAuthHttpRequestConnection.getParameter("authentication").getValue().getRight();

    ComponentParameterAst proxyConfigParameter = grantType.getParameter("proxyConfig");
    assertThat(proxyConfigParameter.getRawValue(), is(nullValue()));
    assertThat(proxyConfigParameter.getValue().getRight(), not(nullValue()));

    ComponentAst oauthHttpProxy = (ComponentAst) proxyConfigParameter.getValue().getRight();
    assertThat(oauthHttpProxy.getIdentifier().toString(), is("http:proxy"));
    ComponentParameterAst portParameter = oauthHttpProxy.getParameter("port");
    assertThat(portParameter.getValue().getRight(), is(8083));
    ComponentParameterAst hostParameter = oauthHttpProxy.getParameter("host");
    assertThat(hostParameter.getValue().getRight(), is("localhost"));
  }

  @Test
  public void defaultComponentHttpParameterAst() {
    ArtifactAst artifactAst = buildArtifactAst("parameters-test-http-oauth-proxy-config.xml",
                                               HttpConnector.class, SocketsExtension.class, OAuthExtension.class);

    // Default flow parameters
    ComponentAst defaultParametersFlow =
        findComponent(artifactAst.topLevelComponentsStream(), FLOW_IDENTIFIER, "defaultParametersFlow")
            .orElseThrow(() -> new AssertionError("Couldn't find 'defaultParametersFlow' flow"));
    assertThat(defaultParametersFlow.getParameter("initialState").isDefaultValue(), is(true));
    assertThat(defaultParametersFlow.getParameter("initialState").getValue().getRight(), is("started"));
    assertThat(defaultParametersFlow.getParameter("maxConcurrency").isDefaultValue(), is(true));
    assertThat(defaultParametersFlow.getParameter("maxConcurrency").getValue(), is(Either.empty()));

    // Non default flow parameters
    ComponentAst flowParameters = findComponent(artifactAst.topLevelComponentsStream(), FLOW_IDENTIFIER, "flowParameters")
        .orElseThrow(() -> new AssertionError("Couldn't find 'flowParameters' flow"));
    assertThat(flowParameters.getParameter("initialState").isDefaultValue(), is(false));
    assertThat(flowParameters.getParameter("initialState").getValue().getRight(), is("stopped"));
    assertThat(flowParameters.getParameter("maxConcurrency").isDefaultValue(), is(false));
    assertThat(flowParameters.getParameter("maxConcurrency").getValue().getRight(), is(2));

    // HTTP listener parameters
    ComponentAst httpListener = findComponent(defaultParametersFlow.directChildrenStream(), "http:listener")
        .orElseThrow(() -> new AssertionError("Couldn't find 'http:listener'"));
    assertThat(httpListener.getParameter("path").isDefaultValue(), is(false));
    assertThat(httpListener.getParameter("path").getValue().getRight(), is("/run"));
    assertThat(httpListener.getParameter("config-ref").isDefaultValue(), is(false));
    assertThat(httpListener.getParameter("config-ref").getValue().getRight(), is("defaultHttpListenerConfig"));
    assertThat(httpListener.getParameter("allowedMethods").isDefaultValue(), is(true));
    assertThat(httpListener.getParameter("allowedMethods").getValue(), is(Either.empty()));

    // HTTP listener config parameters
    ComponentAst httpListenerConfig =
        findComponent(artifactAst.topLevelComponentsStream(), "http:listener-config", "defaultHttpListenerConfig")
            .orElseThrow(() -> new AssertionError("Couldn't find 'defaultHttpListenerConfig' http:listener-config"));
    ComponentAst httpConnectionConfig = httpListenerConfig.directChildrenStream().findFirst().get();
    assertThat(httpConnectionConfig.getParameter("protocol").isDefaultValue(), is(true));
    assertThat(httpConnectionConfig.getParameter("protocol").getValue().getRight(), is("HTTP"));
    assertThat(httpConnectionConfig.getParameter("port").isDefaultValue(), is(false));
    assertThat(httpConnectionConfig.getParameter("port").getValue().getRight(), is(8081));
    assertThat(httpConnectionConfig.getParameter("host").isDefaultValue(), is(false));
    assertThat(httpConnectionConfig.getParameter("host").getValue().getRight(), is("localhost"));
    assertThat(httpConnectionConfig.getParameter("usePersistentConnections").isDefaultValue(), is(true));
    assertThat(httpConnectionConfig.getParameter("usePersistentConnections").getValue().getRight(), is(true));
  }

  @Test
  public void defaultComponentAggregatorsParameterAst() {
    ArtifactAst artifactAst = buildArtifactAst("parameters-test-aggregators-config.xml", AggregatorsExtension.class);

    // Aggregator default parameters
    ComponentAst timeBasedAggregatorFlow =
        findComponent(artifactAst.topLevelComponentsStream(), FLOW_IDENTIFIER, "defaultContentAggregatorFlow")
            .orElseThrow(() -> new AssertionError("Couldn't find 'defaultContentAggregatorFlow' flow"));
    ComponentAst timeBasedAggregator =
        findComponent(timeBasedAggregatorFlow.directChildrenStream(), "aggregators:time-based-aggregator")
            .orElseThrow(() -> new AssertionError("Couldn't find 'aggregators:time-based-aggregator'"));

    assertThat(timeBasedAggregator.getParameter("period").isDefaultValue(), is(false));
    assertThat(timeBasedAggregator.getParameter("period").getValue().getRight(), is(1));
    assertThat(timeBasedAggregator.getParameter("periodUnit").isDefaultValue(), is(true));
    assertThat(timeBasedAggregator.getParameter("periodUnit").getValue().getRight(), is("SECONDS"));
    // Expression not defined should return default
    assertThat(timeBasedAggregator.getParameter("content").isDefaultValue(), is(true));
    assertThat(timeBasedAggregator.getParameter("content").getValue().getLeft(), is("payload"));

    // Aggregator default value expression parameter
    timeBasedAggregatorFlow =
        findComponent(artifactAst.topLevelComponentsStream(), FLOW_IDENTIFIER, "payloadContentAggregatorFlow")
            .orElseThrow(() -> new AssertionError("Couldn't find 'payloadContentAggregatorFlow' flow"));
    timeBasedAggregator = findComponent(timeBasedAggregatorFlow.directChildrenStream(), "aggregators:time-based-aggregator")
        .orElseThrow(() -> new AssertionError("Couldn't find 'aggregators:time-based-aggregator'"));

    assertThat(timeBasedAggregator.getParameter("period").isDefaultValue(), is(false));
    assertThat(timeBasedAggregator.getParameter("period").getValue().getRight(), is(10));
    assertThat(timeBasedAggregator.getParameter("periodUnit").isDefaultValue(), is(true));
    assertThat(timeBasedAggregator.getParameter("periodUnit").getValue().getRight(), is("SECONDS"));
    // Expression same as default value
    assertThat(timeBasedAggregator.getParameter("content").isDefaultValue(), is(true));
    assertThat(timeBasedAggregator.getParameter("content").getValue().getLeft(), is("payload"));

    // Aggregator non default value expression parameter
    timeBasedAggregatorFlow =
        findComponent(artifactAst.topLevelComponentsStream(), FLOW_IDENTIFIER, "customContentAggregatorFlow")
            .orElseThrow(() -> new AssertionError("Couldn't find 'customContentAggregatorFlow' flow"));
    timeBasedAggregator = findComponent(timeBasedAggregatorFlow.directChildrenStream(), "aggregators:time-based-aggregator")
        .orElseThrow(() -> new AssertionError("Couldn't find 'aggregators:time-based-aggregator'"));

    assertThat(timeBasedAggregator.getParameter("period").isDefaultValue(), is(false));
    assertThat(timeBasedAggregator.getParameter("period").getValue().getRight(), is(20));
    assertThat(timeBasedAggregator.getParameter("periodUnit").isDefaultValue(), is(false));
    assertThat(timeBasedAggregator.getParameter("periodUnit").getValue().getRight(), is("MINUTES"));
    // Non default value expression
    assertThat(timeBasedAggregator.getParameter("content").isDefaultValue(), is(false));
    assertThat(timeBasedAggregator.getParameter("content").getValue().getLeft(), is("message"));
  }

  @Test
  @Issue("MULE-18619")
  public void infrastructureParameters() {
    ArtifactAst artifactAst = buildArtifactAst("parameters-test-http-oauth-proxy-config.xml",
                                               HttpConnector.class, SocketsExtension.class, OAuthExtension.class);

    Optional<ComponentAst> clientGlobalConfig =
        findComponentByComponentId(artifactAst.topLevelComponentsStream(), "clientGlobalConfig");
    assertThat(clientGlobalConfig, not(empty()));

    ComponentAst clientGlobalConfigConnection =
        findComponent(clientGlobalConfig.get().directChildrenStream(), "http:request-connection")
            .orElseThrow(() -> new AssertionError("Couldn't find 'http:request-connection'"));

    final ComponentAst tlsContext = (ComponentAst) clientGlobalConfigConnection.getParameter("tlsContext").getValue().getRight();

    final ComponentAst trustStore = (ComponentAst) tlsContext.getParameter("trust-store").getValue().getRight();
    assertThat(trustStore.getParameter("path").getValue().getRight(), is("tls/ssltest-cacerts.jks"));
    assertThat(trustStore.getParameter("password").getValue().getRight(), is("changeit"));
    final ComponentAst keyStore = (ComponentAst) tlsContext.getParameter("key-store").getValue().getRight();
    assertThat(keyStore.getParameter("path").getValue().getRight(), is("tls/ssltest-keystore.jks"));
    assertThat(keyStore.getParameter("keyPassword").getValue().getRight(), is("changeit"));
    assertThat(keyStore.getParameter("password").getValue().getRight(), is("changeit"));

    Optional<ComponentAst> withInfrastructureParametersFlow =
        findComponent(artifactAst.topLevelComponentsStream(), FLOW_IDENTIFIER, "withInfrastructureParametersFlow");
    assertThat(withInfrastructureParametersFlow, not(empty()));

    final List<ComponentAst> flowChildren = withInfrastructureParametersFlow.get().directChildrenStream().collect(toList());

    final ComponentAst source = flowChildren.get(0);

    final ComponentParameterAst primaryNodeOnly = source.getParameter("primaryNodeOnly");
    assertThat(primaryNodeOnly.getValue().getRight(), is(true));

    final ComponentAst redeliveryPolicy = (ComponentAst) (source.getParameter("redeliveryPolicy").getValue().getRight());
    assertThat(redeliveryPolicy.getModel(NamedObject.class).get().getName(),
               is("RedeliveryPolicy"));
    assertThat(redeliveryPolicy.getIdentifier().getName(),
               is("redelivery-policy"));
    assertThat(redeliveryPolicy.getParameter("maxRedeliveryCount").getValue().getRight(),
               is(4));
    assertThat(redeliveryPolicy.getParameter("idExpression").getValue().getLeft(),
               is("payload.id"));

    final ComponentParameterAst streamingStrategyParameter = source.getParameter("streamingStrategy");
    assertThat(getTypeId(streamingStrategyParameter.getModel().getType()).get(),
               is("ByteStreamingStrategy"));
    final ComponentAst streamingStrategy = (ComponentAst) (streamingStrategyParameter.getValue().getRight());
    assertThat(streamingStrategy.getModel(NamedObject.class).get().getName(),
               is("non-repeatable-stream"));
    assertThat(streamingStrategy.getIdentifier().getName(),
               is("non-repeatable-stream"));

    final ComponentParameterAst reconnectionStrategyParam = source.getParameter("reconnectionStrategy");
    assertThat(getTypeId(reconnectionStrategyParam.getModel().getType()).get(),
               is("ReconnectionStrategy"));
    final ComponentAst reconnectionStrategy = (ComponentAst) (reconnectionStrategyParam.getValue().getRight());
    assertThat(reconnectionStrategy.getModel(NamedObject.class).get().getName(),
               is("reconnect"));
    assertThat(reconnectionStrategy.getIdentifier().getName(),
               is("reconnect"));

    final ComponentAst operation = flowChildren.get(1);

    final ComponentParameterAst target = operation.getParameter("target");
    assertThat(target.getValue().getRight(), is("response"));
    final ComponentParameterAst targetValue = operation.getParameter("targetValue");
    assertThat(targetValue.getValue().getLeft(), is("payload.body"));

    final List<ErrorMapping> errorMappings =
        (List<ErrorMapping>) (operation.getParameter(ERROR_MAPPINGS_PARAMETER_NAME).getValue().getRight());
    assertThat(errorMappings, hasSize(1));
    assertThat(errorMappings.get(0).getSource(), is("HTTP:SECURITY"));
    assertThat(errorMappings.get(0).getTarget(), is("APP:GET_OUT"));

    final ComponentParameterAst streamingStrategyOpParam = operation.getParameter("streamingStrategy");
    assertThat(getTypeId(streamingStrategyOpParam.getModel().getType()).get(),
               is("ByteStreamingStrategy"));
    final ComponentAst streamingStrategyOp = (ComponentAst) (streamingStrategyOpParam.getValue().getRight());
    assertThat(streamingStrategyOp.getModel(NamedObject.class).get().getName(),
               is("non-repeatable-stream"));
    assertThat(streamingStrategyOp.getIdentifier().getName(),
               is("non-repeatable-stream"));

    final ComponentParameterAst operationReconnectionParam = operation.getParameter("reconnectionStrategy");
    assertThat(getTypeId(operationReconnectionParam.getModel().getType()).get(),
               is("ReconnectionStrategy"));
    final ComponentAst operationReconnection = (ComponentAst) (operationReconnectionParam.getValue().getRight());
    assertThat(operationReconnection.getModel(NamedObject.class).get().getName(),
               is("reconnect"));
    assertThat(operationReconnection.getIdentifier().getName(),
               is("reconnect"));
    assertThat(operationReconnection.getParameter("frequency").getValue().getRight(),
               is(3000L));
    assertThat(operationReconnection.getParameter("count").getValue().getRight(),
               is(3));
  }

  @Test
  @Issue("MULE-19561")
  public void tlsContextParameter() {
    ArtifactAst artifactAst = buildArtifactAst("parameters-test-tls-config.xml", PetStoreConnector.class);

    final ComponentAst petStoreInlineTls = artifactAst.topLevelComponentsStream()
        .filter(componentAst -> componentAst.getComponentId().map(id -> id.equals("inlineTls")).orElse(false))
        .findFirst()
        .get();

    final ComponentParameterAst configTls = petStoreInlineTls.getParameter("tlsContext");
    assertThat(configTls, not(nullValue()));
    assertThat(configTls.getValue().getRight(), not(nullValue()));

    final ComponentParameterAst cageParam = petStoreInlineTls.getParameter("cage");
    final ComponentAst cagePojo = (ComponentAst) cageParam.getValue().getRight();

    // this is the param name as defined in the pojo field, no additional handling is done for pojos
    final ComponentParameterAst pojoTls = cagePojo.getParameter("tls");
    assertThat(pojoTls, not(nullValue()));
    assertThat(pojoTls.getValue().getRight(), not(nullValue()));
  }

  @Test
  @Issue("MULE-18602")
  public void nestedPojoOperationParameter() {
    ArtifactAst artifactAst = buildArtifactAst("parameters-test-pojo-config.xml",
                                               HeisenbergExtension.class, SubTypesMappingConnector.class, VeganExtension.class);

    Optional<ComponentAst> optionalFlowNestedPojo =
        findComponent(artifactAst.topLevelComponentsStream(), FLOW_IDENTIFIER, "nestedPojo");
    assertThat(optionalFlowNestedPojo, not(empty()));

    ComponentAst heisenbergApprove = optionalFlowNestedPojo.map(flow -> flow.directChildrenStream().findFirst().get())
        .orElseThrow(() -> new AssertionError("Couldn't find heisenberg approve operation"));

    ComponentParameterAst investmentParameter = heisenbergApprove.getParameter("investment");
    ComponentAst investmentAst = (ComponentAst) investmentParameter.getValue().getRight();
    assertThat(investmentAst, not(nullValue()));

    ComponentParameterAst commercialName = investmentAst.getParameter("commercialName");
    ComponentParameterAst carsPerMinute = investmentAst.getParameter("carsPerMinute");

    assertThat(investmentAst.getParameters(), is(not(empty())));

    assertThat(commercialName.getValue().getRight(), is("A1"));
    assertThat(carsPerMinute.getValue().getRight(), is(5));
  }

  @Test
  public void recursivePojoOperationParameter() {
    ArtifactAst artifactAst = buildArtifactAst("parameters-test-pojo-config.xml",
                                               HeisenbergExtension.class, SubTypesMappingConnector.class, VeganExtension.class);

    Optional<ComponentAst> optionalFlowRecursivePojo =
        findComponent(artifactAst.topLevelComponentsStream(), FLOW_IDENTIFIER, "recursivePojo");
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
    List<ComponentAst> childRecursivePojoNextMappedChildsParameterComponent =
        (List<ComponentAst>) childRecursivePojoNextMappedChildsParameter.getValue().getRight();
    assertThat(childRecursivePojoNextMappedChildsParameterComponent, hasSize(1));
    ComponentParameterAst childRecursivePojoNextMappedChildsParameterComponentKeyParam =
        childRecursivePojoNextMappedChildsParameterComponent.get(0).getParameter("key");
    assertThat(childRecursivePojoNextMappedChildsParameterComponentKeyParam.getValue().getRight(), equalTo("someKey"));
    assertThat(getTypeId(childRecursivePojoNextMappedChildsParameterComponentKeyParam.getModel().getType()),
               equalTo(of(String.class.getName())));
    ComponentParameterAst childRecursivePojoNextMappedChildsParameterComponentValueParam =
        childRecursivePojoNextMappedChildsParameterComponent.get(0).getParameter("value");
    assertThat(childRecursivePojoNextMappedChildsParameterComponentValueParam.getValue().getLeft(),
               equalTo("{} as Object {class: 'new org.mule.test.heisenberg.extension.model.RecursivePojo'}"));
    assertThat(getTypeId(childRecursivePojoNextMappedChildsParameterComponentValueParam.getModel().getType()),
               equalTo(of(RecursivePojo.class.getName())));

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
    ArtifactAst artifactAst = buildArtifactAst("parameters-test-pojo-config.xml",
                                               HeisenbergExtension.class, SubTypesMappingConnector.class, VeganExtension.class);

    ComponentAst heisenbergConfig = getHeisenbergConfiguration(artifactAst);

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
    ArtifactAst artifactAst = buildArtifactAst("parameters-test-pojo-config.xml",
                                               HeisenbergExtension.class, SubTypesMappingConnector.class, VeganExtension.class);

    ComponentAst heisenbergConfig = getHeisenbergConfiguration(artifactAst);

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
    ArtifactAst artifactAst = buildArtifactAst("parameters-test-pojo-config.xml",
                                               HeisenbergExtension.class, SubTypesMappingConnector.class, VeganExtension.class);

    ComponentAst heisenbergConfig = getHeisenbergConfiguration(artifactAst);

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

  private ComponentAst getHeisenbergConfiguration(ArtifactAst artifactAst) {
    Optional<ComponentAst> optionalHeisenbergConfig =
        findComponent(artifactAst.topLevelComponentsStream(), "heisenberg:config", "heisenberg");
    assertThat(optionalHeisenbergConfig, not(empty()));

    return optionalHeisenbergConfig.get();
  }

  @Test
  public void listSimpleValueType() {
    ArtifactAst artifactAst = buildArtifactAst("parameters-test-pojo-config.xml",
                                               HeisenbergExtension.class, SubTypesMappingConnector.class, VeganExtension.class);

    ComponentAst heisenbergConfig = getHeisenbergConfiguration(artifactAst);

    ComponentParameterAst enemiesParam = heisenbergConfig.getParameter("enemies");
    List<ComponentAst> enemies = (List<ComponentAst>) enemiesParam.getValue().getRight();
    assertThat(enemies, not(nullValue()));
    assertThat(enemies, hasSize(2));

    assertThat(enemies.get(0).getParameter("value").getValue().getRight(), equalTo("Gustavo Fring"));
    assertThat(enemies.get(1).getParameter("value").getValue().getRight(), equalTo("Hank"));
  }

  @Test
  public void simpleParameters() {
    ArtifactAst artifactAst = buildArtifactAst("parameters-test-config.xml", DbConnector.class, PetStoreConnector.class);

    Optional<ComponentAst> optionalFlowParameters =
        findComponent(artifactAst.topLevelComponentsStream(), FLOW_IDENTIFIER, "flowParameters");
    assertThat(optionalFlowParameters, not(empty()));

    ComponentAst componentAst = optionalFlowParameters.get();
    Optional<ConstructModel> optionalConstructModel = componentAst.getModel(ConstructModel.class);
    assertThat(optionalConstructModel, not(empty()));

    ConstructModel constructModel = optionalConstructModel.get();

    ComponentParameterAst componentParameterAst = componentAst.getParameter("initialState");
    assertThat(componentParameterAst.getRawValue(), equalTo("stopped"));
    assertThat(componentParameterAst.isDefaultValue(), is((false)));
    assertThat(findParameterModel(constructModel, componentParameterAst), not(empty()));
    String[] values = (String[]) componentParameterAst.getModel().getType().getAnnotation(EnumAnnotation.class).get().getValues();
    assertThat(values, allOf(hasItemInArray(INITIAL_STATE_STARTED), hasItemInArray(INITIAL_STATE_STOPPED)));

    componentParameterAst = componentAst.getParameter("maxConcurrency");
    assertThat(findParameterModel(constructModel, componentParameterAst), not(empty()));
    assertThat(componentParameterAst.isDefaultValue(), is((false)));
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
    ArtifactAst artifactAst = buildArtifactAst("parameters-test-config.xml", DbConnector.class, PetStoreConnector.class);

    Optional<ComponentAst> optionalDbConfig = findComponent(artifactAst.topLevelComponentsStream(), "db:config", "dbConfig");
    assertThat(optionalDbConfig, not(empty()));
    ComponentAst dbConfig = optionalDbConfig.get();

    Optional<ComponentAst> optionalConnectionProvider =
        dbConfig.recursiveStream().filter(inner -> inner.getModel(ConnectionProviderModel.class).isPresent())
            .findFirst();
    assertThat(optionalConnectionProvider, not(empty()));

    ComponentAst connectionProvider = optionalConnectionProvider.get();
    ComponentParameterAst poolingProfileParameterAst = connectionProvider.getParameter("poolingProfile");
    assertThat(poolingProfileParameterAst, not(empty()));
    Optional<ComponentAst> poolingProfileParameter = poolingProfileParameterAst.getValue().getValue();
    assertThat(poolingProfileParameter, not(empty()));
    ComponentParameterAst maxWaitUnit = poolingProfileParameter.get().getParameter("maxWaitUnit");
    assertThat(maxWaitUnit.getValue().isRight(), is(true));
    assertThat(maxWaitUnit.getValue().getRight(), is("")); // MULE-19183

    Optional<ComponentParameterAst> additionalPropertiesParameterAst = poolingProfileParameter.map(
                                                                                                   ppp -> ppp
                                                                                                       .getParameter("additionalProperties"));
    assertThat(additionalPropertiesParameterAst, not(empty()));
    Optional<List<ComponentParameterAst>> additionalProperties = additionalPropertiesParameterAst.get().getValue().getValue();
    assertThat(additionalProperties, not(empty()));
    assertThat(additionalProperties.get().isEmpty(), is(true));
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
    ArtifactAst artifactAst = buildArtifactAst("parameters-test-http-oauth-proxy-config.xml",
                                               HttpConnector.class, SocketsExtension.class, OAuthExtension.class);

    Optional<ComponentAst> optionalHttpListenerConfig =
        findComponent(artifactAst.topLevelComponentsStream(), "http:listener-config", "HTTP_Listener_config");
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

  @Test
  @Issue("MULE-19563")
  public void complexParamWithDefaultValueFixed() {
    ArtifactAst artifactAst = buildArtifactAst("parameters-test-pojo-config.xml",
                                               HeisenbergExtension.class, SubTypesMappingConnector.class, VeganExtension.class);

    final ComponentAst killWithRicinAsChildElementFlow = artifactAst.topLevelComponentsStream()
        .filter(componentAst -> componentAst.getComponentId()
            .map(id -> id.equals("killWithRicinAsChildElement"))
            .orElse(false))
        .findFirst()
        .get();

    final ComponentAst killWithRicinsOperation = killWithRicinAsChildElementFlow.directChildrenStream().findFirst().get();

    final Either<String, Object> ricinsValue = killWithRicinsOperation.getParameter("ricins").getValue();
    assertThat(ricinsValue.toString(), ricinsValue.isRight(), is(true));
    assertThat(ricinsValue.toString(), ricinsValue.getRight(), instanceOf(List.class));
  }

  @Test
  @Issue("MULE-19563")
  public void complexParamWithDefaultValueExpression() {
    ArtifactAst artifactAst = buildArtifactAst("parameters-test-pojo-config.xml",
                                               HeisenbergExtension.class, SubTypesMappingConnector.class, VeganExtension.class);

    final ComponentAst killWithRicinAsChildElementFlow = artifactAst.topLevelComponentsStream()
        .filter(componentAst -> componentAst.getComponentId()
            .map(id -> id.equals("killWithRicinAsExpression"))
            .orElse(false))
        .findFirst()
        .get();

    final ComponentAst killWithRicinsOperation = killWithRicinAsChildElementFlow.directChildrenStream().findFirst().get();

    final Either<String, Object> ricinsValue = killWithRicinsOperation.getParameter("ricins").getValue();
    assertThat(ricinsValue.toString(), ricinsValue.isLeft(), is(true));
    assertThat(ricinsValue.toString(), ricinsValue.getLeft(), is("{}"));
  }

  @Test
  @Issue("MULE-19563")
  public void complexParamWithDefaultValue() {
    ArtifactAst artifactAst = buildArtifactAst("parameters-test-pojo-config.xml",
                                               HeisenbergExtension.class, SubTypesMappingConnector.class, VeganExtension.class);

    final ComponentAst killWithRicinAsChildElementFlow = artifactAst.topLevelComponentsStream()
        .filter(componentAst -> componentAst.getComponentId()
            .map(id -> id.equals("killWithRicinDefault"))
            .orElse(false))
        .findFirst()
        .get();

    final ComponentAst killWithRicinsOperation = killWithRicinAsChildElementFlow.directChildrenStream().findFirst().get();

    final Either<String, Object> ricinsValue = killWithRicinsOperation.getParameter("ricins").getValue();
    assertThat(ricinsValue.toString(), ricinsValue.isLeft(), is(true));
    assertThat(ricinsValue.toString(), ricinsValue.getLeft(), is("payload"));
  }

  @Test
  @Issue("MULE-19264")
  public void parameterGroupNameWithSpacesIsMatchedWithDslWhenItShowsInTheDsl() {
    ArtifactAst artifactAst = buildArtifactAst("parameters-test-config.xml", DbConnector.class, PetStoreConnector.class);

    ComponentIdentifier PETSTORE_CONFIG_IDENTIFIER = builder().namespace("petstore").name("config").build();

    Optional<ComponentAst> optionalPetstoreConfigComponentAst = artifactAst.topLevelComponentsStream()
        .filter(componentAst -> componentAst.getIdentifier().equals(PETSTORE_CONFIG_IDENTIFIER))
        .findFirst();
    assertThat(optionalPetstoreConfigComponentAst, not(empty()));

    ComponentAst petstoreConfigComponentAst = optionalPetstoreConfigComponentAst.get();

    // The "brand" parameter belongs to the "Advanced Leash Configuration" parameter group which shows in the DSL
    ComponentParameterAst brandParameter =
        petstoreConfigComponentAst.getParameter("brand");
    assertThat(brandParameter, not(nullValue()));
    assertThat(brandParameter.getValue().getLeft(), is(nullValue()));
    assertThat(brandParameter.getValue().getRight(), not(nullValue()));

    // The "material" parameter belongs to the "Advanced Leash Configuration" parameter group which shows in the DSL
    ComponentParameterAst materialParameter = petstoreConfigComponentAst.getParameter("material");
    assertThat(materialParameter, not(nullValue()));
    assertThat(materialParameter.getValue().getLeft(), is(nullValue()));
    assertThat(materialParameter.getValue().getRight(), not(nullValue()));
  }

  @Test
  @Feature(SOURCES)
  @Issue("MULE-19331")
  public void schedulingStrategyParameterSchedulerSource() {
    ArtifactAst artifactAst = buildArtifactAst("parameters-test-config.xml", DbConnector.class, PetStoreConnector.class);

    final ComponentAst schedulerFlowFixedSource = artifactAst.topLevelComponentsStream()
        .filter(componentAst -> componentAst.getComponentId().map(id -> id.equals("schedulerFlowFixed")).orElse(false))
        .map(schedulerFlowFixed -> schedulerFlowFixed.directChildrenStream().findFirst().get())
        .findFirst()
        .get();

    final ComponentParameterAst schedulerFlowFixedSourceSchStrategy = schedulerFlowFixedSource.getParameter("schedulingStrategy");
    assertThat(schedulerFlowFixedSourceSchStrategy, not(nullValue()));
    assertThat(((ComponentAst) (schedulerFlowFixedSourceSchStrategy.getValue().getRight()))
        .getIdentifier().getName(), is("fixed-frequency"));
  }

  @Test
  @Feature(SOURCES)
  @Issue("MULE-19331")
  public void fixedSchedulingStrategyParameterSdkPollingSource() {
    ArtifactAst artifactAst = buildArtifactAst("parameters-test-config.xml", DbConnector.class, PetStoreConnector.class);

    final ComponentAst dbSchedulerFlowFixed = artifactAst.topLevelComponentsStream()
        .filter(componentAst -> componentAst.getComponentId().map(id -> id.equals("dbSchedulerFlowFixed")).orElse(false))
        .map(schedulerFlowFixed -> schedulerFlowFixed.directChildrenStream().findFirst().get())
        .findFirst()
        .get();

    final ComponentParameterAst dbSchedulerFlowFixedSchStrategy = dbSchedulerFlowFixed.getParameter("schedulingStrategy");
    assertThat(dbSchedulerFlowFixedSchStrategy, not(nullValue()));
    assertThat(((ComponentAst) (dbSchedulerFlowFixedSchStrategy.getValue().getRight()))
        .getIdentifier().getName(), is("fixed-frequency"));
  }

  @Test
  @Feature(SOURCES)
  @Issue("MULE-19331")
  public void cronSchedulingStrategyParameterSdkPollingSource() {
    ArtifactAst artifactAst = buildArtifactAst("parameters-test-config.xml", DbConnector.class, PetStoreConnector.class);

    final ComponentAst dbSchedulerFlowCronSource = artifactAst.topLevelComponentsStream()
        .filter(componentAst -> componentAst.getComponentId().map(id -> id.equals("dbSchedulerFlowCron")).orElse(false))
        .map(schedulerFlowFixed -> schedulerFlowFixed.directChildrenStream().findFirst().get())
        .findFirst()
        .get();

    final ComponentParameterAst dbSchedulerFlowCronSchStrategy = dbSchedulerFlowCronSource.getParameter("schedulingStrategy");
    assertThat(dbSchedulerFlowCronSchStrategy, not(nullValue()));
    assertThat(((ComponentAst) (dbSchedulerFlowCronSchStrategy.getValue().getRight()))
        .getIdentifier().getName(), is("cron"));
  }

  @Test
  public void configPojoParameter() {
    ArtifactAst artifactAst = buildArtifactAst("parameters-test-pojo-config.xml",
                                               HeisenbergExtension.class, SubTypesMappingConnector.class, VeganExtension.class);

    final ComponentAst appleConfig = artifactAst.topLevelComponentsStream()
        .filter(componentAst -> componentAst.getComponentId().map(id -> id.equals("apple")).orElse(false))
        .findFirst()
        .get();

    final ComponentParameterAst cookBookParam = appleConfig.getParameter("cookBook");
    assertThat(cookBookParam, not(nullValue()));
    assertThat(((ComponentAst) (cookBookParam.getValue().getRight()))
        .getIdentifier().getName(), is("vegan-cook-book"));
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
