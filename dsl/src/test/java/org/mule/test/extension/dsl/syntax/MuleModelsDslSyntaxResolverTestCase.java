/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.extension.dsl.syntax;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_NAMESPACE;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.construct.ConstructModel;
import org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.extension.api.dsl.syntax.resolver.SingleExtensionImportTypesStrategy;

import java.util.Arrays;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

public class MuleModelsDslSyntaxResolverTestCase {

  private static final String ASYNC_COMPONENT = "async";
  private static final String UNTIL_SUCCESSFUL_COMPONENT = "untilSuccessful";
  private static final String PARALLEL_FOREACH_COMPONENT = "parallelForeach";
  private static final String TRY_COMPONENT = "try";
  private static final String ERROR_HANDLER_COMPONENT = "errorHandler";
  private static final String CHOICE_COMPONENT = "choice";
  private static final String FOREACH_COMPONENT = "foreach";
  private static final String FIRST_SUCCESSFUL_COMPONENT = "firstSuccessful";
  private static final String SCATTER_GATHER_COMPONENT = "scatterGather";
  private static final String ROUND_ROBIN_COMPONENT = "roundRobin";
  private static final String FLOW_COMPONENT = "flow";
  private static final String SUBFLOW_COMPONENT = "subFlow";


  private DslSyntaxResolver dslSyntaxResolver;
  private ExtensionModel extensionModel = MuleExtensionModelProvider.getExtensionModel();

  @Before
  public void createDslSyntaxResolver() {
    dslSyntaxResolver = DslSyntaxResolver.getDefault(extensionModel, new SingleExtensionImportTypesStrategy());
  }

  @Test
  public void asyncComponentDslSyntax() {
    assertComponentDsl(ASYNC_COMPONENT, "name", "maxConcurrency");
  }

  @Test
  public void untilSuccessfulComponentDslSyntax() {
    assertComponentDsl(UNTIL_SUCCESSFUL_COMPONENT, "maxRetries", "millisBetweenRetries");
  }

  @Test
  public void parallelForeachComponentDslSyntax() {
    assertComponentDsl(PARALLEL_FOREACH_COMPONENT, "timeout", "target", "maxConcurrency", "targetValue");
  }

  @Test
  public void tryComponentDslSyntax() {
    assertComponentDsl(TRY_COMPONENT, "transactionalAction", "transactionType");
  }

  @Test
  public void errorHandlerComponentDslSyntax() {
    ConstructModel construct = extensionModel.getConstructModel(ERROR_HANDLER_COMPONENT).get();
    DslElementSyntax constructSyntax = dslSyntaxResolver.resolve(construct);

    assertParameterAttribute(constructSyntax, "name");
    assertParameterAttribute(constructSyntax, "ref");

    Optional<DslElementSyntax> onErrorSyntax = constructSyntax.getChild("onError");
    Optional<DslElementSyntax> onErrorContinueSyntax = constructSyntax.getChild("onErrorContinue");
    Optional<DslElementSyntax> onErrorPropagateSyntax = constructSyntax.getChild("onErrorPropagate");

    assertThat(onErrorSyntax.isPresent(), is(true));
    assertThat(onErrorContinueSyntax.isPresent(), is(true));
    assertThat(onErrorPropagateSyntax.isPresent(), is(true));
    assertThat(onErrorContinueSyntax.get().getChild("processors").isPresent(), is(false));
    assertThat(onErrorPropagateSyntax.get().getChild("processors").isPresent(), is(false));
  }

  @Test
  public void choiceComponentDslSyntax() {
    ConstructModel construct = extensionModel.getConstructModel(CHOICE_COMPONENT).get();
    DslElementSyntax constructSyntax = dslSyntaxResolver.resolve(construct);

    Optional<DslElementSyntax> whenSyntax = constructSyntax.getChild("when");
    Optional<DslElementSyntax> otherwiseSyntax = constructSyntax.getChild("otherwise");
    assertThat(whenSyntax.isPresent(), is(true));
    assertThat(otherwiseSyntax.isPresent(), is(true));

    assertThat(whenSyntax.get().getChild("processors").isPresent(), is(false));
    assertThat(otherwiseSyntax.get().getChild("processors").isPresent(), is(false));
  }

  @Test
  public void foreachComponentDslSyntax() {
    assertComponentDsl(FOREACH_COMPONENT);
  }

  @Test
  public void flowComponentDslSyntax() {
    assertComponentDsl(FLOW_COMPONENT, "name");
  }

  @Test
  public void subFlowComponentDslSyntax() {
    assertComponentDsl(SUBFLOW_COMPONENT, "name");
  }

  @Test
  public void firstSuccessfulComponentDslSyntax() {
    assertComponentWithRouteDsl(FIRST_SUCCESSFUL_COMPONENT);
  }

  @Test
  public void scatterGatherComponentDslSyntax() {
    assertComponentWithRouteDsl(SCATTER_GATHER_COMPONENT);
  }

  @Test
  public void roundRobinComponentDslSyntax() {
    assertComponentWithRouteDsl(ROUND_ROBIN_COMPONENT);
  }

  private void assertComponentDsl(String modelName, String... attributes) {
    DslElementSyntax constructSyntax = getAndAssertConstruct(modelName, attributes);
    Optional<DslElementSyntax> processorsSyntax = constructSyntax.getChild("processors");
    assertThat(processorsSyntax.isPresent(), is(false));
  }

  private void assertComponentWithRouteDsl(String modelName, String... attributes) {
    DslElementSyntax constructSyntax = getAndAssertConstruct(modelName, attributes);
    Optional<DslElementSyntax> routeSyntax = constructSyntax.getChild("route");
    assertThat(routeSyntax.isPresent(), is(true));
    Optional<DslElementSyntax> processorsSyntax = routeSyntax.get().getChild("processors");
    assertThat(processorsSyntax.isPresent(), is(false));
  }

  private DslElementSyntax getAndAssertConstruct(String modelName, String[] attributes) {
    ConstructModel construct = extensionModel.getConstructModel(modelName).get();
    DslElementSyntax constructSyntax = dslSyntaxResolver.resolve(construct);

    assertThat(constructSyntax.getPrefix(), is(CORE_PREFIX));
    assertThat(constructSyntax.getNamespace(), is(CORE_NAMESPACE));

    Arrays.stream(attributes).forEach(attribute -> assertParameterAttribute(constructSyntax, attribute));
    return constructSyntax;
  }

  private void assertParameterAttribute(DslElementSyntax dslElementSyntax, String parameterName) {
    Optional<DslElementSyntax> parameterDslElementSyntax = dslElementSyntax.getAttribute(parameterName);
    assertThat(parameterDslElementSyntax.isPresent(), is(true));
    assertThat(parameterDslElementSyntax.get().getElementName(), is(""));
    assertThat(parameterDslElementSyntax.get().getAttributeName(), is(parameterName));
  }
}
