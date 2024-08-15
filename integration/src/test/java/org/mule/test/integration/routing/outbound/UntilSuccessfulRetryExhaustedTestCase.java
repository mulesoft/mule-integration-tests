/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing.outbound;

import static org.mule.runtime.api.util.MuleSystemProperties.SUPPRESS_ERRORS_PROPERTY;
import static org.mule.test.allure.AllureConstants.ScopeFeature.SCOPE;
import static org.mule.test.allure.AllureConstants.ScopeFeature.UntilSuccessfulStory.UNTIL_SUCCESSFUL;

import static java.lang.Boolean.parseBoolean;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.lang.String.format;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.notification.ExceptionNotificationListener;
import org.mule.runtime.api.serialization.ObjectSerializer;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.retry.policy.RetryPolicyExhaustedException;
import org.mule.runtime.extension.api.error.MuleErrors;
import org.mule.tests.api.TestQueueManager;
import org.mule.tck.junit4.matcher.ErrorTypeMatcher;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.io.InputStream;
import java.util.Collection;

import javax.inject.Inject;

import io.qameta.allure.Issue;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.runners.Parameterized;
import org.junit.Rule;
import org.junit.Test;

@Feature(SCOPE)
@Story(UNTIL_SUCCESSFUL)
@RunnerDelegateTo(Parameterized.class)
public class UntilSuccessfulRetryExhaustedTestCase extends AbstractIntegrationTestCase {

  @Rule
  public SystemProperty suppressErrors;

  @Inject
  private TestQueueManager queueManager;

  @Inject
  private ObjectSerializer defaultSerializer;

  private final boolean isSuppressErrors;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/routing/outbound/until-successful-retry-exhausted.xml";
  }

  @Parameterized.Parameters(name = "Suppress errors: {0}")
  public static Collection<String> parameters() {
    return asList("true", "false");
  }

  public UntilSuccessfulRetryExhaustedTestCase(String suppressErrors) {
    this.suppressErrors = new SystemProperty(SUPPRESS_ERRORS_PROPERTY, suppressErrors);
    this.isSuppressErrors = parseBoolean(suppressErrors);
  }

  @Test
  public void onRetryExhaustedCallExceptionStrategy() throws Exception {
    final Latch exceptionStrategyCalledLatch = new Latch();
    notificationListenerRegistry
        .registerListener((ExceptionNotificationListener) notification -> exceptionStrategyCalledLatch.release());
    flowRunner("retryExhaustedCausedByUntypedError").withPayload("message").run();
    if (!exceptionStrategyCalledLatch.await(10000, MILLISECONDS)) {
      fail("exception strategy was not executed");
    }
  }

  @Test
  public void onNestedRetryExhaustedCallExceptionStrategy() throws Exception {
    final Latch exceptionStrategyCalledLatch = new Latch();
    notificationListenerRegistry
        .registerListener((ExceptionNotificationListener) notification -> exceptionStrategyCalledLatch.release());
    flowRunner("retryExhaustedCausedByUntypedError").withPayload("message").run();
    if (!exceptionStrategyCalledLatch.await(10000, MILLISECONDS)) {
      fail("exception strategy was not executed");
    }
  }

  @Test
  public void onRetryExhaustedCausedByConnectionExceptionErrorTypeMustBeRetryExhausted() throws Exception {
    if (isSuppressErrors()) {
      flowRunner("retryExhaustedCausedByConnectivityError").withPayload("message")
          .runExpectingException(ErrorTypeMatcher.errorType(MuleErrors.RETRY_EXHAUSTED));
    } else {
      flowRunner("retryExhaustedCausedByConnectivityError").withPayload("message")
          .runExpectingException(ErrorTypeMatcher.errorType(MuleErrors.CONNECTIVITY));
    }
  }

  private boolean isSuppressErrors() {
    return parseBoolean(suppressErrors.getValue());
  }

  @Test
  @Issue("MULE-18041")
  public void retryExhaustedCausedByConnectionExceptionLogCheck() throws Exception {
    if (isSuppressErrors()) {
      flowRunner("retryExhaustedCausedByConnectivityErrorWithSuppressionLogCheck").withPayload("message")
          .run();
    } else {
      flowRunner("retryExhaustedCausedByConnectivityErrorWithoutSuppressionLogCheck").withPayload("message")
          .run();
    }
  }

  @Test
  @Issue("MULE-18041")
  public void retryExhaustedCausedByNonConnectionExceptionLogCheck() throws Exception {
    if (isSuppressErrors()) {
      flowRunner("retryExhaustedCausedByNonConnectivityErrorWithSuppressionLogCheck").withPayload("message")
          .run();
    } else {
      flowRunner("retryExhaustedCausedByNonConnectivityErrorWithoutSuppressionLogCheck").withPayload("message")
          .run();
    }
  }

  @Test
  @Issue("MULE-18041")
  public void retryExhaustedCausedByMuleRuntimeExceptionLogCheck() throws Exception {
    if (isSuppressErrors()) {
      flowRunner("retryExhaustedCausedByMuleRuntimeErrorWithSuppressionLogCheck").withPayload("message")
          .run();
    } else {
      flowRunner("retryExhaustedCausedByMuleRuntimeErrorWithoutSuppressionLogCheck").withPayload("message")
          .run();
    }
  }

  @Test
  @Issue("MULE-18562")
  public void retryExhaustedUnsuppressedErrorTypeHandling() throws Exception {
    CoreEvent event = flowRunner("retryExhaustedUnsuppressedErrorTypeHandling").withPayload("message").run();
    if (isSuppressErrors()) {
      assertThat(event.getMessage().getPayload().getValue(), is("retry-exhausted-handled"));
    } else {
      assertThat(event.getMessage().getPayload().getValue(), is("security-handled"));
    }
  }

  @Test
  @Issue("MULE-18562")
  public void retryExhaustedSuppressedErrorTypeHandling() throws Exception {
    CoreEvent event = flowRunner("retryExhaustedSuppressedErrorTypeHandling").withPayload("message").run();
    assertThat(event.getMessage().getPayload().getValue(), is("handled"));
  }

  @Test
  @Issue("MULE-18562")
  public void retryExhaustedErrorWithSuppressionsCheck() throws Exception {
    flowRunner("retryExhaustedErrorWithSuppressionsCheck").withPayload("message").run();
    // Returned error assertions
    Error error = (Error) queueManager.read("dlq", RECEIVE_TIMEOUT, MILLISECONDS).getMessage().getPayload().getValue();
    assertThat(error.getCause(), instanceOf(RetryPolicyExhaustedException.class));
    assertThat(error.getDescription(), equalTo("Mule runtime error"));
    assertThat(error.getDetailedDescription(), equalTo("'until-successful' retries exhausted"));
    assertThat(error.getFailingComponent(), containsString("retryExhaustedErrorWithSuppressionsCheck/processors/0"));
    assertThat(error.getErrorMessage(), nullValue());
    assertThat(error.getChildErrors(), empty());
    if (isSuppressErrors()) {
      assertThat(error.getErrorType().toString(), equalTo("MULE:RETRY_EXHAUSTED"));
    } else {
      assertThat(error.getErrorType().toString(), equalTo("MULE:SECURITY"));
    }
  }

  @Test
  @Issue("W-15643200")
  public void retryExhaustedErrorSerializationCheck() throws Exception {
    flowRunner("retryExhaustedErrorSerializationCheck").withPayload("message").run();
    Error error = (Error) queueManager.read("dlq", RECEIVE_TIMEOUT, MILLISECONDS).getMessage().getPayload().getValue();
    // Serialize the error
    byte[] errorSerialization = defaultSerializer.getInternalProtocol().serialize(error);
    // Deserialize the error
    Error errorDeserialization = defaultSerializer.getInternalProtocol().deserialize(errorSerialization);
    assertThat(errorDeserialization.getCause(), instanceOf(RetryPolicyExhaustedException.class));
    assertThat(errorDeserialization.getDescription(), equalTo("Mule runtime error"));
  }

  @Test
  @Issue("W-15643200")
  public void retryExhaustedLegacyErrorSerializationCheck() throws Exception {
    // Deserialize the legacy error.
    try (InputStream errorSerializationInputStream =
        getClass()
            .getResourceAsStream(format("UntilSuccessfulRetryExhaustedTestCase_retryExhaustedErrorSerializationCheck%s.bin",
                                        isSuppressErrors ? "_suppressedTrue" : "_suppressedFalse"))) {
      Error deserializedError = defaultSerializer.getInternalProtocol().deserialize(errorSerializationInputStream);
      assertThat(deserializedError.getCause(), instanceOf(RetryPolicyExhaustedException.class));
      assertThat(deserializedError.getDescription(), equalTo("Mule runtime error"));
    }
  }

  public static class MuleRuntimeError extends MuleRuntimeException {

    public MuleRuntimeError() {
      super(I18nMessageFactory.createStaticMessage("Mule runtime error"));
    }
  }

}
