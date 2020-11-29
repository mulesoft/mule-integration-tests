/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing.outbound;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.test.allure.AllureConstants.RoutersFeature.ROUTERS;
import static org.mule.test.allure.AllureConstants.RoutersFeature.UntilSuccessfulStory.UNTIL_SUCCESSFUL;

import io.qameta.allure.Issue;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.notification.ExceptionNotificationListener;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.retry.policy.RetryPolicyExhaustedException;
import org.mule.runtime.extension.api.error.MuleErrors;
import org.mule.tck.junit4.matcher.ErrorTypeMatcher;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.mule.tests.api.TestQueueManager;

import javax.inject.Inject;

@Feature(ROUTERS)
@Story(UNTIL_SUCCESSFUL)
public class UntilSuccessfulRetryExhaustedTestCase extends AbstractIntegrationTestCase {

  @Inject
  private TestQueueManager queueManager;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/routing/outbound/until-successful-retry-exhausted.xml";
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
    flowRunner("retryExhaustedCausedByConnectivityError").withPayload("message")
        .runExpectingException(ErrorTypeMatcher.errorType(MuleErrors.RETRY_EXHAUSTED));
  }

  @Test
  @Issue("MULE-18041")
  public void retryExhaustedCausedByConnectionExceptionLogCheck() throws Exception {
    flowRunner("retryExhaustedCausedByConnectivityErrorLogCheck").withPayload("message")
        .run();
  }

  @Test
  @Issue("MULE-18041")
  public void retryExhaustedCausedByNonConnectionExceptionLogCheck() throws Exception {
    flowRunner("retryExhaustedCausedByNonConnectivityErrorLogCheck").withPayload("message")
        .run();
  }

  @Test
  @Issue("MULE-18041")
  public void retryExhaustedCausedByMuleRuntimeExceptionLogCheck() throws Exception {
    flowRunner("retryExhaustedCausedByMuleRuntimeErrorLogCheck").withPayload("message")
        .run();
  }

  @Test
  @Issue("MULE-18562")
  public void retryExhaustedUnsuppressedErrorTypeHandling() throws Exception {
    CoreEvent event = flowRunner("retryExhaustedUnsuppressedErrorTypeHandling").withPayload("message").run();
    assertThat(event.getMessage().getPayload().getValue(), is("handled"));
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
    assertThat(error.getErrorType().toString(), equalTo("MULE:RETRY_EXHAUSTED"));
    assertThat(error.getCause(), instanceOf(RetryPolicyExhaustedException.class));
    assertThat(error.getDescription(), equalTo("Mule runtime error"));
    assertThat(error.getDetailedDescription(), equalTo("'until-successful' retries exhausted"));
    assertThat(error.getFailingComponent(), containsString("retryExhaustedErrorWithSuppressionsCheck/processors/0"));
    assertThat(error.getErrorMessage(), nullValue());
    assertThat(error.getChildErrors(), empty());
  }

  public static class MuleRuntimeError extends MuleRuntimeException {

    public MuleRuntimeError() {
      super(I18nMessageFactory.createStaticMessage("Mule runtime error"));
    }
  }

}
