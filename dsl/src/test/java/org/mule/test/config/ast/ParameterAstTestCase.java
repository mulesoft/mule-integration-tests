/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.ast;

import static org.mule.metadata.api.utils.MetadataTypeUtils.getTypeId;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.FLOW_IDENTIFIER;
import static org.mule.runtime.core.api.construct.Flow.INITIAL_STATE_STARTED;
import static org.mule.runtime.core.api.construct.Flow.INITIAL_STATE_STOPPED;
import static org.mule.runtime.extension.api.ExtensionConstants.ERROR_MAPPINGS_PARAMETER_NAME;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isMap;
import static org.mule.test.allure.AllureConstants.ArtifactAst.ARTIFACT_AST;
import static org.mule.test.allure.AllureConstants.ArtifactAst.ParameterAst.PARAMETER_AST;
import static org.mule.test.allure.AllureConstants.SourcesFeature.SOURCES;

import static java.lang.Boolean.TRUE;
import static java.lang.System.lineSeparator;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.ArrayMatching.arrayContaining;
import static org.hamcrest.collection.ArrayMatching.hasItemInArray;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;

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
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
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

import org.hamcrest.Matchers;

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

    ComponentParameterAst proxyConfig = oAuthHttpRequestConnection.getParameter(DEFAULT_GROUP_NAME, "proxyConfig");
    assertThat(proxyConfig.getRawValue(), is(nullValue()));
    assertThat(proxyConfig.getValue(), is(Either.empty()));
    assertThat(getTypeId(proxyConfig.getModel().getType()), equalTo(of(HttpProxyConfig.class.getName())));

    ComponentAst grantType =
        (ComponentAst) oAuthHttpRequestConnection.getParameter(DEFAULT_GROUP_NAME, "authentication").getValue().getRight();

    ComponentParameterAst proxyConfigParameter = grantType.getParameter("ClientCredentialsGrantType", "proxyConfig");
    assertThat(proxyConfigParameter.getRawValue(), is(nullValue()));
    assertThat(proxyConfigParameter.getValue().getRight(), not(nullValue()));

    ComponentAst oauthHttpProxy = (ComponentAst) proxyConfigParameter.getValue().getRight();
    assertThat(oauthHttpProxy.getIdentifier().toString(), is("http:proxy"));
    ComponentParameterAst portParameter = oauthHttpProxy.getParameter("proxy", "port");
    assertThat(portParameter.getValue().getRight(), is(8083));
    ComponentParameterAst hostParameter = oauthHttpProxy.getParameter("proxy", "host");
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
    assertThat(defaultParametersFlow.getParameter(DEFAULT_GROUP_NAME, "initialState").isDefaultValue(), is(true));
    assertThat(defaultParametersFlow.getParameter(DEFAULT_GROUP_NAME, "initialState").getValue().getRight(), is("started"));
    assertThat(defaultParametersFlow.getParameter(DEFAULT_GROUP_NAME, "maxConcurrency").isDefaultValue(), is(true));
    assertThat(defaultParametersFlow.getParameter(DEFAULT_GROUP_NAME, "maxConcurrency").getValue(), is(Either.empty()));

    // Non default flow parameters
    ComponentAst flowParameters = findComponent(artifactAst.topLevelComponentsStream(), FLOW_IDENTIFIER, "flowParameters")
        .orElseThrow(() -> new AssertionError("Couldn't find 'flowParameters' flow"));
    assertThat(flowParameters.getParameter(DEFAULT_GROUP_NAME, "initialState").isDefaultValue(), is(false));
    assertThat(flowParameters.getParameter(DEFAULT_GROUP_NAME, "initialState").getValue().getRight(), is("stopped"));
    assertThat(flowParameters.getParameter(DEFAULT_GROUP_NAME, "maxConcurrency").isDefaultValue(), is(false));
    assertThat(flowParameters.getParameter(DEFAULT_GROUP_NAME, "maxConcurrency").getValue().getRight(), is(2));

    // HTTP listener parameters
    ComponentAst httpListener = findComponent(defaultParametersFlow.directChildrenStream(), "http:listener")
        .orElseThrow(() -> new AssertionError("Couldn't find 'http:listener'"));
    assertThat(httpListener.getParameter(DEFAULT_GROUP_NAME, "path").isDefaultValue(), is(false));
    assertThat(httpListener.getParameter(DEFAULT_GROUP_NAME, "path").getValue().getRight(), is("/run"));
    assertThat(httpListener.getParameter(DEFAULT_GROUP_NAME, "config-ref").isDefaultValue(), is(false));
    assertThat(httpListener.getParameter(DEFAULT_GROUP_NAME, "config-ref").getValue().getRight(),
               is("defaultHttpListenerConfig"));
    assertThat(httpListener.getParameter(DEFAULT_GROUP_NAME, "allowedMethods").isDefaultValue(), is(true));
    assertThat(httpListener.getParameter(DEFAULT_GROUP_NAME, "allowedMethods").getValue(), is(Either.empty()));

    // HTTP listener config parameters
    ComponentAst httpListenerConfig =
        findComponent(artifactAst.topLevelComponentsStream(), "http:listener-config", "defaultHttpListenerConfig")
            .orElseThrow(() -> new AssertionError("Couldn't find 'defaultHttpListenerConfig' http:listener-config"));
    ComponentAst httpConnectionConfig = httpListenerConfig.directChildrenStream().findFirst().get();
    assertThat(httpConnectionConfig.getParameter("Connection", "protocol").isDefaultValue(), is(true));
    assertThat(httpConnectionConfig.getParameter("Connection", "protocol").getValue().getRight(), is("HTTP"));
    assertThat(httpConnectionConfig.getParameter("Connection", "port").isDefaultValue(), is(false));
    assertThat(httpConnectionConfig.getParameter("Connection", "port").getValue().getRight(), is(8081));
    assertThat(httpConnectionConfig.getParameter("Connection", "host").isDefaultValue(), is(false));
    assertThat(httpConnectionConfig.getParameter("Connection", "host").getValue().getRight(), is("localhost"));
    assertThat(httpConnectionConfig.getParameter("Connection", "usePersistentConnections").isDefaultValue(), is(true));
    assertThat(httpConnectionConfig.getParameter("Connection", "usePersistentConnections").getValue().getRight(), is(true));
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

    assertThat(timeBasedAggregator.getParameter("Aggregator config", "period").isDefaultValue(), is(false));
    assertThat(timeBasedAggregator.getParameter("Aggregator config", "period").getValue().getRight(), is(1));
    assertThat(timeBasedAggregator.getParameter("Aggregator config", "periodUnit").isDefaultValue(), is(true));
    assertThat(timeBasedAggregator.getParameter("Aggregator config", "periodUnit").getValue().getRight(), is("SECONDS"));
    // Expression not defined should return default
    assertThat(timeBasedAggregator.getParameter("Aggregator config", "content").isDefaultValue(), is(true));
    assertThat(timeBasedAggregator.getParameter("Aggregator config", "content").getValue().getLeft(), is("payload"));

    // Aggregator default value expression parameter
    timeBasedAggregatorFlow =
        findComponent(artifactAst.topLevelComponentsStream(), FLOW_IDENTIFIER, "payloadContentAggregatorFlow")
            .orElseThrow(() -> new AssertionError("Couldn't find 'payloadContentAggregatorFlow' flow"));
    timeBasedAggregator = findComponent(timeBasedAggregatorFlow.directChildrenStream(), "aggregators:time-based-aggregator")
        .orElseThrow(() -> new AssertionError("Couldn't find 'aggregators:time-based-aggregator'"));

    assertThat(timeBasedAggregator.getParameter("Aggregator config", "period").isDefaultValue(), is(false));
    assertThat(timeBasedAggregator.getParameter("Aggregator config", "period").getValue().getRight(), is(10));
    assertThat(timeBasedAggregator.getParameter("Aggregator config", "periodUnit").isDefaultValue(), is(true));
    assertThat(timeBasedAggregator.getParameter("Aggregator config", "periodUnit").getValue().getRight(), is("SECONDS"));
    // Expression same as default value
    assertThat(timeBasedAggregator.getParameter("Aggregator config", "content").isDefaultValue(), is(true));
    assertThat(timeBasedAggregator.getParameter("Aggregator config", "content").getValue().getLeft(), is("payload"));

    // Aggregator non default value expression parameter
    timeBasedAggregatorFlow =
        findComponent(artifactAst.topLevelComponentsStream(), FLOW_IDENTIFIER, "customContentAggregatorFlow")
            .orElseThrow(() -> new AssertionError("Couldn't find 'customContentAggregatorFlow' flow"));
    timeBasedAggregator = findComponent(timeBasedAggregatorFlow.directChildrenStream(), "aggregators:time-based-aggregator")
        .orElseThrow(() -> new AssertionError("Couldn't find 'aggregators:time-based-aggregator'"));

    assertThat(timeBasedAggregator.getParameter("Aggregator config", "period").isDefaultValue(), is(false));
    assertThat(timeBasedAggregator.getParameter("Aggregator config", "period").getValue().getRight(), is(20));
    assertThat(timeBasedAggregator.getParameter("Aggregator config", "periodUnit").isDefaultValue(), is(false));
    assertThat(timeBasedAggregator.getParameter("Aggregator config", "periodUnit").getValue().getRight(), is("MINUTES"));
    // Non default value expression
    assertThat(timeBasedAggregator.getParameter("Aggregator config", "content").isDefaultValue(), is(false));
    assertThat(timeBasedAggregator.getParameter("Aggregator config", "content").getValue().getLeft(), is("message"));
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

    final ComponentAst tlsContext =
        (ComponentAst) clientGlobalConfigConnection.getParameter(DEFAULT_GROUP_NAME, "tlsContext").getValue().getRight();

    final ComponentAst trustStore =
        (ComponentAst) tlsContext.getParameter("Tls", "trust-store").getValue().getRight();
    assertThat(trustStore.getParameter("TrustStore", "path").getValue().getRight(), is("tls/ssltest-cacerts.jks"));
    assertThat(trustStore.getParameter("TrustStore", "password").getValue().getRight(), is("changeit"));
    final ComponentAst keyStore = (ComponentAst) tlsContext.getParameter("Tls", "key-store").getValue().getRight();
    assertThat(keyStore.getParameter("KeyStore", "path").getValue().getRight(), is("tls/ssltest-keystore.jks"));
    assertThat(keyStore.getParameter("KeyStore", "keyPassword").getValue().getRight(), is("changeit"));
    assertThat(keyStore.getParameter("KeyStore", "password").getValue().getRight(), is("changeit"));

    Optional<ComponentAst> withInfrastructureParametersFlow =
        findComponent(artifactAst.topLevelComponentsStream(), FLOW_IDENTIFIER, "withInfrastructureParametersFlow");
    assertThat(withInfrastructureParametersFlow, not(empty()));

    final List<ComponentAst> flowChildren = withInfrastructureParametersFlow.get().directChildrenStream().collect(toList());

    final ComponentAst source = flowChildren.get(0);

    final ComponentParameterAst primaryNodeOnly = source.getParameter(DEFAULT_GROUP_NAME, "primaryNodeOnly");
    assertThat(primaryNodeOnly.getValue().getRight(), is(true));

    final ComponentAst redeliveryPolicy =
        (ComponentAst) (source.getParameter(DEFAULT_GROUP_NAME, "redeliveryPolicy").getValue().getRight());
    assertThat(redeliveryPolicy.getModel(NamedObject.class).get().getName(),
               is("RedeliveryPolicy"));
    assertThat(redeliveryPolicy.getIdentifier().getName(),
               is("redelivery-policy"));
    assertThat(redeliveryPolicy.getParameter("RedeliveryPolicy", "maxRedeliveryCount").getValue().getRight(),
               is(4));
    assertThat(redeliveryPolicy.getParameter("RedeliveryPolicy", "idExpression").getValue().getLeft(),
               is("payload.id"));

    final ComponentParameterAst streamingStrategyParameter = source.getParameter(DEFAULT_GROUP_NAME, "streamingStrategy");
    assertThat(getTypeId(streamingStrategyParameter.getModel().getType()).get(),
               is("ByteStreamingStrategy"));
    final ComponentAst streamingStrategy = (ComponentAst) (streamingStrategyParameter.getValue().getRight());
    assertThat(streamingStrategy.getModel(NamedObject.class).get().getName(),
               is("non-repeatable-stream"));
    assertThat(streamingStrategy.getIdentifier().getName(),
               is("non-repeatable-stream"));

    final ComponentParameterAst reconnectionStrategyParam = source.getParameter("Connection", "reconnectionStrategy");
    assertThat(getTypeId(reconnectionStrategyParam.getModel().getType()).get(),
               is("ReconnectionStrategy"));
    final ComponentAst reconnectionStrategy = (ComponentAst) (reconnectionStrategyParam.getValue().getRight());
    assertThat(reconnectionStrategy.getModel(NamedObject.class).get().getName(),
               is("reconnect"));
    assertThat(reconnectionStrategy.getIdentifier().getName(),
               is("reconnect"));

    final ComponentAst operation = flowChildren.get(1);

    final ComponentParameterAst target = operation.getParameter("Output", "target");
    assertThat(target.getValue().getRight(), is("response"));
    final ComponentParameterAst targetValue = operation.getParameter("Output", "targetValue");
    assertThat(targetValue.getValue().getLeft(), is("payload.body"));

    final List<ErrorMapping> errorMappings =
        (List<ErrorMapping>) (operation.getParameter("Error Mappings", ERROR_MAPPINGS_PARAMETER_NAME).getValue().getRight());
    assertThat(errorMappings, hasSize(1));
    assertThat(errorMappings.get(0).getSource(), is("HTTP:SECURITY"));
    assertThat(errorMappings.get(0).getTarget(), is("APP:GET_OUT"));

    final ComponentParameterAst streamingStrategyOpParam = operation.getParameter(DEFAULT_GROUP_NAME, "streamingStrategy");
    assertThat(getTypeId(streamingStrategyOpParam.getModel().getType()).get(),
               is("ByteStreamingStrategy"));
    final ComponentAst streamingStrategyOp = (ComponentAst) (streamingStrategyOpParam.getValue().getRight());
    assertThat(streamingStrategyOp.getModel(NamedObject.class).get().getName(),
               is("non-repeatable-stream"));
    assertThat(streamingStrategyOp.getIdentifier().getName(),
               is("non-repeatable-stream"));

    final ComponentParameterAst operationReconnectionParam = operation.getParameter("Connection", "reconnectionStrategy");
    assertThat(getTypeId(operationReconnectionParam.getModel().getType()).get(),
               is("ReconnectionStrategy"));
    final ComponentAst operationReconnection = (ComponentAst) (operationReconnectionParam.getValue().getRight());
    assertThat(operationReconnection.getModel(NamedObject.class).get().getName(),
               is("reconnect"));
    assertThat(operationReconnection.getIdentifier().getName(),
               is("reconnect"));
    assertThat(operationReconnection.getParameter("reconnect", "frequency").getValue().getRight(),
               is(3000L));
    assertThat(operationReconnection.getParameter("reconnect", "count").getValue().getRight(),
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

    final ComponentParameterAst configTls = petStoreInlineTls.getParameter(DEFAULT_GROUP_NAME, "tlsContext");
    assertThat(configTls, not(nullValue()));
    assertThat(configTls.getValue().getRight(), not(nullValue()));

    final ComponentParameterAst cageParam = petStoreInlineTls.getParameter(DEFAULT_GROUP_NAME, "cage");
    final ComponentAst cagePojo = (ComponentAst) cageParam.getValue().getRight();

    // this is the param name as defined in the pojo field, no additional handling is done for pojos
    final ComponentParameterAst pojoTls = cagePojo.getParameter("PetCage", "tls");
    assertThat(pojoTls, not(nullValue()));
    assertThat(pojoTls.getValue().getRight(), not(nullValue()));
  }

  @Test
  @Issue("MULE-19976")
  public void tlsContextWithRevocationCheckParameter() {
    ArtifactAst artifactAst =
        buildArtifactAst("parameters-test-tls-global-with-revocation-check-config.xml", PetStoreConnector.class);

    final ComponentAst pojoTls = artifactAst.topLevelComponentsStream()
        .filter(componentAst -> componentAst.getComponentId().map(id -> id.equals("globalTlsContext")).orElse(false))
        .findFirst()
        .get();

    ComponentParameterAst revocationCheck = pojoTls.getParameter("Tls", "revocation-check");
    assertThat(revocationCheck, not(nullValue()));
    assertThat(revocationCheck.getValue().getRight(), not(nullValue()));
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

    ComponentParameterAst investmentParameter = heisenbergApprove.getParameter(DEFAULT_GROUP_NAME, "investment");
    ComponentAst investmentAst = (ComponentAst) investmentParameter.getValue().getRight();
    assertThat(investmentAst, not(nullValue()));

    ComponentParameterAst commercialName = investmentAst.getParameter("CarWash", "commercialName");
    ComponentParameterAst carsPerMinute = investmentAst.getParameter("CarWash", "carsPerMinute");

    assertThat(investmentAst.getParameters(), is(not(empty())));

    assertThat(commercialName.getValue().getRight(), is("A1"));
    assertThat(carsPerMinute.getValue().getRight(), is(5));

    List investmentSpinOffs = (List) investmentAst.getParameter("CarWash", "investmentSpinOffs").getValue().getRight();
    ComponentAst firstSpinOff = (ComponentAst) investmentSpinOffs.get(0);
    ComponentAst carWash = (ComponentAst) firstSpinOff.getParameter(DEFAULT_GROUP_NAME, "value").getValue().getRight();
    List discardedInvestments = (List) carWash.getParameter("CarWash", "discardedInvestments").getValue().getRight();
    ComponentAst firstDiscardedInvestment = (ComponentAst) discardedInvestments.get(0);

    assertThat(firstDiscardedInvestment.directChildren(), is(Matchers.empty()));

    ComponentParameterAst investmentPlanB = firstDiscardedInvestment.getParameter("CarDealer", "investmentPlanB");
    assertThat(investmentPlanB.getValue().getValue().isPresent(), is(true));
    ComponentAst carDealer = (ComponentAst) investmentPlanB.getValue().getRight();

    List<String> presentParameterNames = carDealer.getParameters().stream()
        .filter(param -> param.getValue().getValue().isPresent()).map(param -> param.getModel().getName()).collect(toList());
    assertThat(presentParameterNames, containsInAnyOrder("carStock", "commercialName", "valuation"));
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

    ComponentParameterAst recursivePojoParameter = heisenbergApprove.getParameter(DEFAULT_GROUP_NAME, "recursivePojo");
    ComponentAst recursivePojo = (ComponentAst) recursivePojoParameter.getValue().getRight();
    assertThat(recursivePojo, not(nullValue()));

    ComponentParameterAst recursivePojoNextParameter = recursivePojo.getParameter("RecursivePojo", "next");
    assertThat(getTypeId(recursivePojoNextParameter.getModel().getType()), equalTo(of(RecursivePojo.class.getName())));
    assertThat(recursivePojoNextParameter.getValue().getRight(), is(nullValue()));

    ComponentParameterAst recursivePojoChildsParameter = recursivePojo.getParameter("RecursivePojo", "childs");
    assertThat(recursivePojoChildsParameter.getModel().getType(), instanceOf(ArrayType.class));
    assertThat(getTypeId(((ArrayType) recursivePojoChildsParameter.getModel().getType()).getType()),
               equalTo(of(RecursivePojo.class.getName())));
    assertThat(recursivePojoChildsParameter.getValue().getRight(), not(nullValue()));

    ComponentAst childRecursivePojo = ((List<ComponentAst>) recursivePojoChildsParameter.getValue().getRight()).stream()
        .findFirst().orElseThrow(() -> new AssertionError("Couldn't find child declaration"));
    ComponentParameterAst childRecursivePojoNextParameter = childRecursivePojo.getParameter("RecursivePojo", "next");
    assertThat(getTypeId(childRecursivePojoNextParameter.getModel().getType()), equalTo(of(RecursivePojo.class.getName())));
    ComponentAst childRecursivePojoNext = (ComponentAst) childRecursivePojoNextParameter.getValue().getRight();
    assertThat(childRecursivePojoNext, not(nullValue()));

    ComponentParameterAst childRecursivePojoNextMappedChildsParameter =
        childRecursivePojoNext.getParameter("RecursivePojo", "mappedChilds");
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
        childRecursivePojoNextMappedChildsParameterComponent.get(0).getParameter(DEFAULT_GROUP_NAME, "key");
    assertThat(childRecursivePojoNextMappedChildsParameterComponentKeyParam.getValue().getRight(), equalTo("someKey"));
    assertThat(getTypeId(childRecursivePojoNextMappedChildsParameterComponentKeyParam.getModel().getType()),
               equalTo(of(String.class.getName())));
    ComponentParameterAst childRecursivePojoNextMappedChildsParameterComponentValueParam =
        childRecursivePojoNextMappedChildsParameterComponent.get(0).getParameter(DEFAULT_GROUP_NAME, "value");
    assertThat(childRecursivePojoNextMappedChildsParameterComponentValueParam.getValue().getLeft(),
               equalTo("{} as Object {class: 'new org.mule.test.heisenberg.extension.model.RecursivePojo'}"));
    assertThat(getTypeId(childRecursivePojoNextMappedChildsParameterComponentValueParam.getModel().getType()),
               equalTo(of(RecursivePojo.class.getName())));

    ComponentParameterAst recursivePojoMappedChildsParameter = recursivePojo.getParameter("RecursivePojo", "mappedChilds");
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

    ComponentParameterAst deathsBySeasonsParam = heisenbergConfig.getParameter(DEFAULT_GROUP_NAME, "deathsBySeasons");
    assertThat(isMap(deathsBySeasonsParam.getModel().getType()), is(true));
    Optional<MetadataType> optionalOpenRestriction =
        ((ObjectType) deathsBySeasonsParam.getModel().getType()).getOpenRestriction();
    assertThat(optionalOpenRestriction, not(empty()));
    assertThat(optionalOpenRestriction.get(), instanceOf(ArrayType.class));
    assertThat(getTypeId(((ArrayType) optionalOpenRestriction.get()).getType()), equalTo(of(String.class.getName())));

    List<ComponentAst> deathsBySeasons = (List<ComponentAst>) deathsBySeasonsParam.getValue().getRight();
    assertThat(deathsBySeasons, hasSize(1));

    ComponentAst deathBySeason = deathsBySeasons.stream().findFirst().get();
    ComponentParameterAst keyParameter = deathBySeason.getParameter(DEFAULT_GROUP_NAME, "key");
    assertThat(keyParameter.getValue().getRight(), is("s01"));
    ComponentParameterAst valueParameter = deathBySeason.getParameter(DEFAULT_GROUP_NAME, "value");
    List<ComponentAst> values = (List<ComponentAst>) valueParameter.getValue().getRight();
    assertThat(values, hasSize(2));

    assertThat(values.get(0).getParameter(DEFAULT_GROUP_NAME, "value").getValue().getRight(), is("emilio"));
    assertThat(values.get(1).getParameter(DEFAULT_GROUP_NAME, "value").getValue().getRight(), is("domingo"));
  }

  @Test
  public void mapListOfComplexValueType() {
    ArtifactAst artifactAst = buildArtifactAst("parameters-test-pojo-config.xml",
                                               HeisenbergExtension.class, SubTypesMappingConnector.class, VeganExtension.class);

    ComponentAst heisenbergConfig = getHeisenbergConfiguration(artifactAst);

    ComponentParameterAst weaponValueMapsParam = heisenbergConfig.getParameter(DEFAULT_GROUP_NAME, "weaponValueMap");
    assertThat(isMap(weaponValueMapsParam.getModel().getType()), is(true));
    Optional<MetadataType> optionalOpenRestriction =
        ((ObjectType) weaponValueMapsParam.getModel().getType()).getOpenRestriction();
    assertThat(optionalOpenRestriction, not(empty()));
    assertThat(getTypeId(optionalOpenRestriction.get()), equalTo(of(Weapon.class.getName())));

    List<ComponentAst> weaponValueMaps = (List<ComponentAst>) weaponValueMapsParam.getValue().getRight();
    assertThat(weaponValueMaps, hasSize(2));

    ComponentAst weaponValueMap = weaponValueMaps.stream().findFirst().get();
    ComponentParameterAst keyParameter = weaponValueMap.getParameter(DEFAULT_GROUP_NAME, "key");
    assertThat(keyParameter.getValue().getRight(), is("first"));
    ComponentParameterAst valueParameter = weaponValueMap.getParameter(DEFAULT_GROUP_NAME, "value");
    ComponentAst ricinValue = (ComponentAst) valueParameter.getValue().getRight();
    assertThat(ricinValue.getParameter("Ricin", "microgramsPerKilo").getValue().getRight(), is(Long.valueOf(22)));
    ComponentAst destination = (ComponentAst) ricinValue.getParameter("Ricin", "destination").getValue().getRight();
    assertThat(destination, not(nullValue()));
    assertThat(destination.getParameter("door", "victim").getValue().getRight(), equalTo("Lidia"));
    assertThat(destination.getParameter("door", "address").getValue().getRight(), equalTo("Stevia coffe shop"));

    weaponValueMap = weaponValueMaps.stream().skip(1).findFirst().get();
    keyParameter = weaponValueMap.getParameter(DEFAULT_GROUP_NAME, "key");
    assertThat(keyParameter.getValue().getRight(), is("second"));
    valueParameter = weaponValueMap.getParameter(DEFAULT_GROUP_NAME, "value");
    ComponentAst revolver = (ComponentAst) valueParameter.getValue().getRight();
    assertThat(revolver.getParameter("Revolver", "name").getValue().getRight(), is("sledgeHammer's"));
    assertThat(revolver.getParameter("Revolver", "bullets").getValue().getRight(), is(1));
  }

  @Test
  public void mapSimpleValueType() {
    ArtifactAst artifactAst = buildArtifactAst("parameters-test-pojo-config.xml",
                                               HeisenbergExtension.class, SubTypesMappingConnector.class, VeganExtension.class);

    ComponentAst heisenbergConfig = getHeisenbergConfiguration(artifactAst);

    ComponentParameterAst recipesParam = heisenbergConfig.getParameter(DEFAULT_GROUP_NAME, "recipe");
    assertThat(isMap(recipesParam.getModel().getType()), is(true));
    Optional<MetadataType> optionalOpenRestriction = ((ObjectType) recipesParam.getModel().getType()).getOpenRestriction();
    assertThat(optionalOpenRestriction, not(empty()));
    assertThat(optionalOpenRestriction.get(), instanceOf(NumberType.class));

    List<ComponentAst> recipes = (List<ComponentAst>) recipesParam.getValue().getRight();
    assertThat(recipes, hasSize(3));

    ComponentAst recipe = recipes.stream().findFirst().get();
    ComponentParameterAst keyParameter = recipe.getParameter(DEFAULT_GROUP_NAME, "key");
    assertThat(keyParameter.getValue().getRight(), is("methylamine"));
    ComponentParameterAst valueParameter = recipe.getParameter(DEFAULT_GROUP_NAME, "value");
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

    ComponentParameterAst enemiesParam = heisenbergConfig.getParameter(DEFAULT_GROUP_NAME, "enemies");
    List<ComponentAst> enemies = (List<ComponentAst>) enemiesParam.getValue().getRight();
    assertThat(enemies, not(nullValue()));
    assertThat(enemies, hasSize(2));

    assertThat(enemies.get(0).getParameter(DEFAULT_GROUP_NAME, "value").getValue().getRight(), equalTo("Gustavo Fring"));
    assertThat(enemies.get(1).getParameter(DEFAULT_GROUP_NAME, "value").getValue().getRight(), equalTo("Hank"));
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

    ComponentParameterAst componentParameterAst = componentAst.getParameter(DEFAULT_GROUP_NAME, "initialState");
    assertThat(componentParameterAst.getRawValue(), equalTo("stopped"));
    assertThat(componentParameterAst.isDefaultValue(), is((false)));
    assertThat(findParameterModel(constructModel, componentParameterAst), not(empty()));
    String[] values = (String[]) componentParameterAst.getModel().getType().getAnnotation(EnumAnnotation.class).get().getValues();
    assertThat(values, allOf(hasItemInArray(INITIAL_STATE_STARTED), hasItemInArray(INITIAL_STATE_STOPPED)));

    componentParameterAst = componentAst.getParameter(DEFAULT_GROUP_NAME, "maxConcurrency");
    assertThat(findParameterModel(constructModel, componentParameterAst), not(empty()));
    assertThat(componentParameterAst.isDefaultValue(), is((false)));
    assertThat(componentParameterAst.getModel().getType().getAnnotation(IntAnnotation.class), not(empty()));

    assertThat(componentAst.getComponentId(), not(empty()));
    componentParameterAst = componentAst.getParameter(DEFAULT_GROUP_NAME, NAME);
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
    ComponentParameterAst poolingProfileParameterAst = connectionProvider.getParameter(DEFAULT_GROUP_NAME, "poolingProfile");
    assertThat(poolingProfileParameterAst, not(empty()));
    Optional<ComponentAst> poolingProfileParameter = poolingProfileParameterAst.getValue().getValue();
    assertThat(poolingProfileParameter, not(empty()));
    ComponentParameterAst maxWaitUnit = poolingProfileParameter.get().getParameter("pooling-profile", "maxWaitUnit");
    assertThat(maxWaitUnit.getValue().isRight(), is(true));
    assertThat(maxWaitUnit.getValue().getRight(), is("")); // MULE-19183

    Optional<ComponentParameterAst> additionalPropertiesParameterAst = poolingProfileParameter.map(
                                                                                                   ppp -> ppp
                                                                                                       .getParameter("pooling-profile",
                                                                                                                     "additionalProperties"));
    assertThat(additionalPropertiesParameterAst, not(empty()));
    Optional<List<ComponentParameterAst>> additionalProperties = additionalPropertiesParameterAst.get().getValue().getValue();
    assertThat(additionalProperties, not(empty()));
    assertThat(additionalProperties.get().isEmpty(), is(true));
    ComponentParameterAst connectionPropertiesParameterAst =
        connectionProvider.getParameter("Connection", "connectionProperties");
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

    assertThat(connectionProperty.getParameter(DEFAULT_GROUP_NAME, "key").getValue().getRight(), equalTo("first"));
    assertThat(connectionProperty.getParameter(DEFAULT_GROUP_NAME, "value").getValue().getRight(), equalTo("propertyOne"));

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
    assertThat(connectionProperty.getParameter(DEFAULT_GROUP_NAME, "key").getValue().getRight(), equalTo("second"));
    assertThat(connectionProperty.getParameter(DEFAULT_GROUP_NAME, "value").getValue().getRight(), equalTo("propertyTwo"));
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
    ComponentParameterAst nameComponentParameter = httpListenerConfig.getParameter(DEFAULT_GROUP_NAME, NAME);
    assertThat(nameComponentParameter.getValue().getRight(), equalTo(httpListenerConfig.getComponentId().get()));

    Optional<ComponentAst> optionalConnectionProvider =
        httpListenerConfig.recursiveStream().filter(inner -> inner.getModel(ConnectionProviderModel.class).isPresent())
            .findFirst();
    assertThat(optionalConnectionProvider, not(empty()));

    ComponentAst connectionProvider = optionalConnectionProvider.get();
    ComponentParameterAst tlsContextParameter = connectionProvider.getParameter(DEFAULT_GROUP_NAME, "tlsContext");
    assertThat(tlsContextParameter.getValue().getLeft(), nullValue());
    assertThat(tlsContextParameter.getValue().getRight(), equalTo("listenerTlsContext"));

    ComponentParameterAst basePathParameter = httpListenerConfig.getParameter(DEFAULT_GROUP_NAME, "basePath");
    assertThat(basePathParameter.getValue().getLeft(), nullValue());
    assertThat(basePathParameter.getValue().getRight(), equalTo("/api"));

    ComponentParameterAst listenerInterceptorsParameter =
        httpListenerConfig.getParameter(DEFAULT_GROUP_NAME, "listenerInterceptors");
    assertThat(listenerInterceptorsParameter.getValue().getLeft(), nullValue());
    ComponentAst listenerInterceptors = (ComponentAst) listenerInterceptorsParameter.getValue().getRight();

    ComponentParameterAst corsInterceptorParameter =
        listenerInterceptors.getParameter("CorsInterceptorWrapper", "corsInterceptor");
    assertThat(corsInterceptorParameter.getValue().getLeft(), nullValue());
    ComponentAst corsInterceptor = (ComponentAst) corsInterceptorParameter.getValue().getRight();

    ComponentParameterAst allowCredentialsParameter = corsInterceptor.getParameter("CorsListenerInterceptor", "allowCredentials");
    assertThat(allowCredentialsParameter.getValue().getLeft(), is(nullValue()));
    assertThat(allowCredentialsParameter.getValue().getRight(), is(TRUE));

    ComponentParameterAst originsParameter = corsInterceptor.getParameter("CorsListenerInterceptor", "origins");
    assertThat(originsParameter.getValue().getLeft(), is(nullValue()));
    assertThat(originsParameter.getValue().getRight(), not(nullValue()));

    List<ComponentAst> origins = (List<ComponentAst>) originsParameter.getValue().getRight();
    ComponentAst origin = origins.stream().findFirst().get();
    ComponentParameterAst originUrlParameter = origin.getParameter("origin", "url");
    assertThat(originUrlParameter.getValue().getLeft(), nullValue());
    assertThat(originUrlParameter.getValue().getRight(), is("http://www.the-origin-of-time.com"));
    ComponentParameterAst originAccessControlMaxAgeParameter = origin.getParameter("origin", "accessControlMaxAge");
    assertThat(originAccessControlMaxAgeParameter.getValue().getLeft(), nullValue());
    assertThat(originAccessControlMaxAgeParameter.getValue().getRight(), is(30l));

    assertParameters(origin, "origin", "allowedMethods", "Method", "methodName", "POST", "PUT", "GET");
    assertParameters(origin, "origin", "allowedHeaders", "Header", "headerName", "x-allow-origin",
                     "x-yet-another-valid-header");
    assertParameters(origin, "origin", "exposeHeaders", "Header", "headerName", "x-forwarded-for");

    origin = origins.stream().skip(1).findFirst().get();
    originUrlParameter = origin.getParameter("origin", "url");
    assertThat(originUrlParameter.getValue().getLeft(), nullValue());
    assertThat(originUrlParameter.getValue().getRight(), is("http://www.the-origin-of-life.com"));
    originAccessControlMaxAgeParameter = origin.getParameter("origin", "accessControlMaxAge");
    assertThat(originAccessControlMaxAgeParameter.getValue().getLeft(), nullValue());
    assertThat(originAccessControlMaxAgeParameter.getValue().getRight(), is(60l));

    assertParameters(origin, "origin", "allowedMethods", "Method", "methodName", "POST", "GET");
    assertParameters(origin, "origin", "allowedHeaders", "Header", "headerName", "x-allow-origin");
    assertParameters(origin, "origin", "exposeHeaders", "Header", "headerName", "x-forwarded-for");
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

    final Either<String, Object> ricinsValue = killWithRicinsOperation.getParameter(DEFAULT_GROUP_NAME, "ricins").getValue();
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

    final Either<String, Object> ricinsValue = killWithRicinsOperation.getParameter(DEFAULT_GROUP_NAME, "ricins").getValue();
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

    final Either<String, Object> ricinsValue = killWithRicinsOperation.getParameter(DEFAULT_GROUP_NAME, "ricins").getValue();
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
        petstoreConfigComponentAst.getParameter("Advanced Leash Configuration", "brand");
    assertThat(brandParameter, not(nullValue()));
    assertThat(brandParameter.getValue().getLeft(), is(nullValue()));
    assertThat(brandParameter.getValue().getRight(), not(nullValue()));

    // The "material" parameter belongs to the "Advanced Leash Configuration" parameter group which shows in the DSL
    ComponentParameterAst materialParameter = petstoreConfigComponentAst.getParameter("Advanced Leash Configuration", "material");
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

    final ComponentParameterAst schedulerFlowFixedSourceSchStrategy =
        schedulerFlowFixedSource.getParameter(DEFAULT_GROUP_NAME, "schedulingStrategy");
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

    final ComponentParameterAst dbSchedulerFlowFixedSchStrategy =
        dbSchedulerFlowFixed.getParameter(DEFAULT_GROUP_NAME, "schedulingStrategy");
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

    final ComponentParameterAst dbSchedulerFlowCronSchStrategy =
        dbSchedulerFlowCronSource.getParameter(DEFAULT_GROUP_NAME, "schedulingStrategy");
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

    final ComponentParameterAst cookBookParam = appleConfig.getParameter(DEFAULT_GROUP_NAME, "cookBook");
    assertThat(cookBookParam, not(nullValue()));
    assertThat(((ComponentAst) (cookBookParam.getValue().getRight()))
        .getIdentifier().getName(), is("vegan-cook-book"));
  }

  @Test
  @Issue("MULE-19676")
  public void configPojoParameterWithWrappedParamsHasNotTheWrapperAsChild() {
    ArtifactAst artifactAst = buildArtifactAst("parameters-test-pojo-config.xml",
                                               HeisenbergExtension.class, SubTypesMappingConnector.class, VeganExtension.class);

    final ComponentAst appleConfig = artifactAst.topLevelComponentsStream()
        .filter(componentAst -> componentAst.getComponentId().map(id -> id.equals("apple")).orElse(false))
        .findFirst()
        .get();

    assertThat(appleConfig.directChildren(), is(Matchers.empty()));
  }

  @Test
  @Issue("MULE-19824")
  public void objectTagPropertiesHasNotTheWrapperAsChild() {
    ArtifactAst artifactAst = buildArtifactAst("parameters-test-pojo-config.xml",
                                               HeisenbergExtension.class, SubTypesMappingConnector.class, VeganExtension.class);

    final ComponentAst object = artifactAst.topLevelComponentsStream()
        .filter(componentAst -> componentAst.getComponentId().map(id -> id.equals("anObject")).orElse(false))
        .findFirst()
        .get();

    assertThat(object.directChildren(), is(Matchers.empty()));
  }

  @Test
  @Issue("MULE-19770")
  public void cdataParameterNotTrimmed() {
    ArtifactAst artifactAst = buildArtifactAst("parameters-test-http-through-sockets-config.xml",
                                               SocketsExtension.class);

    final ComponentAst httpRequestThroughSocketsFlow = artifactAst.topLevelComponentsStream()
        .filter(componentAst -> componentAst.getComponentId().map(id -> id.equals("httpRequestThroughSockets")).orElse(false))
        .findFirst()
        .get();

    ComponentAst sendAndReceiveOp = httpRequestThroughSocketsFlow.directChildren().get(0);

    ComponentParameterAst contentParam = sendAndReceiveOp.getParameter(DEFAULT_GROUP_NAME, "content");

    assertThat(contentParam.getValue().getRight(), is("POST /test HTTP/1.1\n" +
        "Host: localhost:8081" + lineSeparator() +
        "Transfer-Encoding: chunked, deflate" + lineSeparator() +
        "2" + lineSeparator() +
        "OK" + lineSeparator() +
        // This trailing double line separator MUST be kept!
        "0" + lineSeparator() + lineSeparator()));
  }

  @Test
  @Issue("MULE-19809")
  public void generationInformationSyntaxForNotAllowInlineDefinitionNestedParam() {
    ArtifactAst artifactAst = buildArtifactAst("parameters-test-pojo-config.xml",
                                               HeisenbergExtension.class, SubTypesMappingConnector.class, VeganExtension.class);

    ComponentAst heisenbergConfig = artifactAst.topLevelComponentsStream()
        .filter(componentAst -> componentAst.getComponentId().map(id -> id.equals("heisenberg")).orElse(false))
        .findFirst()
        .get();

    Optional<DslElementSyntax> wildCardsSyntax =
        heisenbergConfig.getParameter("General", "wildCards").getGenerationInformation().getSyntax();
    assertThat(wildCardsSyntax.isPresent(), is(true));
    assertThat(wildCardsSyntax.get().getElementName(), is("wild-cards"));
  }

  @Test
  @Issue("MULE-20047")
  public void pojoParamIsSubtypeFromAnotherExtension() {
    ArtifactAst artifactAst = buildArtifactAst("parameters-test-pojo-config.xml",
                                               HeisenbergExtension.class, SubTypesMappingConnector.class, VeganExtension.class);

    ComponentAst killWithRevolver = artifactAst.topLevelComponentsStream()
        .filter(componentAst -> componentAst.getComponentId().map(id -> id.equals("killWithRevolver")).orElse(false))
        .findFirst()
        .get();

    ComponentAst killOperation = killWithRevolver.directChildren().get(0);
    ComponentAst deadlyValue = (ComponentAst) killOperation.getParameter(DEFAULT_GROUP_NAME, "deadly")
        .getValue()
        .getRight();
    ComponentAst deadlyWeapon = (ComponentAst) deadlyValue.getParameter("Deadly", "weapon")
        .getValue()
        .getRight();

    assertThat(killOperation.directChildren(), hasSize(0));
    assertThat(deadlyValue.directChildren(), hasSize(0));
    assertThat(deadlyWeapon.directChildren(), hasSize(0));

    assertThat(deadlyWeapon.getIdentifier().getNamespace(), is("subtypes"));
    assertThat(deadlyWeapon.getIdentifier().getName(), is("revolver"));

    ComponentAst killWithRicin = artifactAst.topLevelComponentsStream()
        .filter(componentAst -> componentAst.getComponentId().map(id -> id.equals("killWithRicin")).orElse(false))
        .findFirst()
        .get();

    killOperation = killWithRicin.directChildren().get(0);
    deadlyValue = (ComponentAst) killOperation.getParameter(DEFAULT_GROUP_NAME, "deadly")
        .getValue()
        .getRight();
    deadlyWeapon = (ComponentAst) deadlyValue.getParameter("Deadly", "weapon")
        .getValue()
        .getRight();

    assertThat(killOperation.directChildren(), hasSize(0));
    assertThat(deadlyValue.directChildren(), hasSize(0));
    assertThat(deadlyWeapon.directChildren(), hasSize(0));

    assertThat(deadlyWeapon.getIdentifier().getNamespace(), is("heisenberg"));
    assertThat(deadlyWeapon.getIdentifier().getName(), is("ricin"));

    ComponentAst destination = (ComponentAst) deadlyWeapon.getParameter("Ricin", "destination")
        .getValue()
        .getRight();

    assertThat(destination.getParameter("door", "victim").getValue().getRight(), is("Lidia"));
    assertThat(destination.directChildren(), hasSize(0));
  }

  private void assertParameters(ComponentAst container, String containerParameterGroupName, String containerParameterName,
                                String elementParameterGroupName, String elementParameterName, String... rightValues) {
    ComponentParameterAst containerParameter = container.getParameter(containerParameterGroupName, containerParameterName);
    assertThat(containerParameter.getValue().getLeft(), nullValue());
    assertThat(containerParameter.getValue().getRight(), not(nullValue()));

    List<ComponentAst> elementComponents = (List<ComponentAst>) containerParameter.getValue().getRight();
    String[] actual = elementComponents.stream().map(componentAst -> {
      ComponentParameterAst elementParameter = componentAst.getParameter(elementParameterGroupName, elementParameterName);
      assertThat(elementParameter.getValue().getLeft(), nullValue());
      return (String) elementParameter.getValue().getRight();
    }).toArray(String[]::new);
    assertThat(actual, arrayContaining(rightValues));
  }

}
