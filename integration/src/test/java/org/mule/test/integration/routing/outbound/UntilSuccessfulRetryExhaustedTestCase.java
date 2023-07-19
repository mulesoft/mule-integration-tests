/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration.routing.outbound;

import static org.mule.runtime.api.util.MuleSystemProperties.SUPPRESS_ERRORS_PROPERTY;
import static org.mule.test.allure.AllureConstants.RoutersFeature.ROUTERS;
import static org.mule.test.allure.AllureConstants.RoutersFeature.UntilSuccessfulStory.UNTIL_SUCCESSFUL;

import static java.lang.Boolean.parseBoolean;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.notification.ExceptionNotificationListener;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.retry.policy.RetryPolicyExhaustedException;
import org.mule.runtime.extension.api.error.MuleErrors;
import org.mule.tests.api.TestQueueManager;
import org.mule.tck.junit4.matcher.ErrorTypeMatcher;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Collection;

import javax.inject.Inject;

import io.qameta.allure.Issue;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.runners.Parameterized;
import org.junit.Rule;
import org.junit.Test;

@Feature(ROUTERS)
@Story(UNTIL_SUCCESSFUL)
@RunnerDelegateTo(Parameterized.class)
public class UntilSuccessfulRetryExhaustedTestCase extends AbstractIntegrationTestCase {

  @Rule
  public SystemProperty suppressErrors;

  @Inject
  private TestQueueManager queueManager;

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
    if (isSupressErrors()) {
      flowRunner("retryExhaustedCausedByConnectivityError").withPayload("message")
          .runExpectingException(ErrorTypeMatcher.errorType(MuleErrors.RETRY_EXHAUSTED));
    } else {
      flowRunner("retryExhaustedCausedByConnectivityError").withPayload("message")
          .runExpectingException(ErrorTypeMatcher.errorType(MuleErrors.CONNECTIVITY));
    }
  }

  private boolean isSupressErrors() {
    return parseBoolean(suppressErrors.getValue());
  }

  @Test
  @Issue("MULE-18041")
  public void retryExhaustedCausedByConnectionExceptionLogCheck() throws Exception {
    if (isSupressErrors()) {
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
    if (isSupressErrors()) {
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
    if (isSupressErrors()) {
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
    if (isSupressErrors()) {
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
    if (isSupressErrors()) {
      assertThat(error.getErrorType().toString(), equalTo("MULE:RETRY_EXHAUSTED"));
    } else {
      assertThat(error.getErrorType().toString(), equalTo("MULE:SECURITY"));
    }
  }

  public static class MuleRuntimeError extends MuleRuntimeException {

    public MuleRuntimeError() {
      super(I18nMessageFactory.createStaticMessage("Mule runtime error"));
    }
  }

}
