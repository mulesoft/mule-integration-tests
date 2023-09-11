/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.processors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mule.runtime.core.api.error.Errors.CORE_NAMESPACE_NAME;
import static org.mule.runtime.core.api.error.Errors.Identifiers.DUPLICATE_MESSAGE_ERROR_IDENTIFIER;
import static org.mule.tck.junit4.matcher.ErrorTypeMatcher.errorType;

import org.mule.functional.api.exception.ExpectedError;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Rule;
import org.junit.Test;

public class IdempotentMessageValidatorTestCase extends AbstractIntegrationTestCase {

  @Rule
  public ExpectedError expectedError = ExpectedError.none();

  @Override
  public String getConfigFile() {
    return "org/mule/processors/idempotent-message-validator-config.xml";
  }

  @Test
  public void validateWithGlobalObjectStore() throws Exception {
    String payload = "payload";
    CoreEvent response = flowRunner("validate-global").withPayload(payload).run();
    assertThat(response.getMessage().getPayload().getValue(), is(equalTo(payload)));
    expectedError.expectErrorType(CORE_NAMESPACE_NAME, DUPLICATE_MESSAGE_ERROR_IDENTIFIER);
    flowRunner("validate-global").withPayload(payload).run();
  }

  @Test
  public void validateWithGlobalObjectStoreFromDifferentValidators() throws Exception {
    String payload = "payload";
    CoreEvent response = flowRunner("validate-global").withPayload(payload).run();
    assertThat(response.getMessage().getPayload().getValue(), is(equalTo(payload)));
    expectedError.expectErrorType(CORE_NAMESPACE_NAME, DUPLICATE_MESSAGE_ERROR_IDENTIFIER);
    flowRunner("validate-global2").withPayload(payload).run();
  }

  @Test
  public void validateWithPrivateObjectStoreUsesADifferentObjectStore() throws Exception {
    String globalPayload = "global-payload";
    String privatePayload = "private-payload";
    CoreEvent globalResponse = flowRunner("validate-global").withPayload(globalPayload).run();
    CoreEvent privateResponse = flowRunner("validate-private").withPayload(privatePayload).run();
    assertThat(globalResponse.getMessage().getPayload().getValue(), is(equalTo(globalPayload)));
    assertThat(privateResponse.getMessage().getPayload().getValue(), is(equalTo(privatePayload)));

    flowRunner("validate-global").withPayload(globalPayload)
        .runExpectingException(errorType(CORE_NAMESPACE_NAME, DUPLICATE_MESSAGE_ERROR_IDENTIFIER));
    flowRunner("validate-private").withPayload(privatePayload)
        .runExpectingException(errorType(CORE_NAMESPACE_NAME, DUPLICATE_MESSAGE_ERROR_IDENTIFIER));
  }

  @Test
  public void validateWithImplicitObjectStoreUsesADifferentObjectStore() throws Exception {
    String globalPayload = "global-payload";
    String implicitPayload = "implicit-payload";
    CoreEvent globalResponse = flowRunner("validate-global").withPayload(globalPayload).run();
    CoreEvent implicitResponse = flowRunner("validate-implicit").withPayload(implicitPayload).run();
    assertThat(globalResponse.getMessage().getPayload().getValue(), is(equalTo(globalPayload)));
    assertThat(implicitResponse.getMessage().getPayload().getValue(), is(equalTo(implicitPayload)));

    flowRunner("validate-global").withPayload(globalPayload)
        .runExpectingException(errorType(CORE_NAMESPACE_NAME, DUPLICATE_MESSAGE_ERROR_IDENTIFIER));
    flowRunner("validate-implicit").withPayload(implicitPayload)
        .runExpectingException(errorType(CORE_NAMESPACE_NAME, DUPLICATE_MESSAGE_ERROR_IDENTIFIER));
  }

  @Test
  public void validateImplicitValidatorCreatesOneObjectStorePerFlow() throws Exception {
    final String implicitPayload = "implicit-payload";
    CoreEvent implicitResponse = flowRunner("validate-implicit").withPayload(implicitPayload).run();
    CoreEvent otherImplicitResponse = flowRunner("other-validate-implicit").withPayload(implicitPayload).run();
    assertThat(implicitResponse.getMessage().getPayload().getValue(), is(equalTo(implicitPayload)));
    assertThat(otherImplicitResponse.getMessage().getPayload().getValue(), is(equalTo(implicitPayload)));

    flowRunner("other-validate-implicit").withPayload(implicitPayload)
        .runExpectingException(errorType(CORE_NAMESPACE_NAME, DUPLICATE_MESSAGE_ERROR_IDENTIFIER));
    flowRunner("validate-implicit").withPayload(implicitPayload)
        .runExpectingException(errorType(CORE_NAMESPACE_NAME, DUPLICATE_MESSAGE_ERROR_IDENTIFIER));
  }

}
