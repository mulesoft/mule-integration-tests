/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.extension.dsl.syntax;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_NAMESPACE;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.construct.ConstructModel;
import org.mule.runtime.core.api.extension.MuleExtensionModelProvider;
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

  private void assertComponentDsl(String modelName, String... attributes) {
    ConstructModel construct = extensionModel.getConstructModel(modelName).get();
    DslElementSyntax constructSyntax = dslSyntaxResolver.resolve(construct);

    assertThat(constructSyntax.getPrefix(), is(CORE_PREFIX));
    assertThat(constructSyntax.getNamespace(), is(CORE_NAMESPACE));

    Arrays.stream(attributes).forEach(attribute -> assertParameterAttribute(constructSyntax, attribute));

    Optional<DslElementSyntax> processorsSyntax = constructSyntax.getChild("processors");
    assertThat(processorsSyntax.isPresent(), is(false));
  }

  private void assertParameterAttribute(DslElementSyntax dslElementSyntax, String parameterName) {
    Optional<DslElementSyntax> parameterDslElementSyntax = dslElementSyntax.getAttribute(parameterName);
    assertThat(parameterDslElementSyntax.isPresent(), is(true));
    assertThat(parameterDslElementSyntax.get().getElementName(), is(""));
    assertThat(parameterDslElementSyntax.get().getAttributeName(), is(parameterName));
  }
}
