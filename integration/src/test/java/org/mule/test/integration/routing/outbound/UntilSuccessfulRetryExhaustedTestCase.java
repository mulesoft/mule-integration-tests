/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing.outbound;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.fail;
import static org.mule.test.allure.AllureConstants.RoutersFeature.ROUTERS;
import static org.mule.test.allure.AllureConstants.RoutersFeature.UntilSuccessfulStory.UNTIL_SUCCESSFUL;

import io.qameta.allure.Issue;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.api.notification.ExceptionNotificationListener;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.extension.api.annotation.Ignore;
import org.mule.runtime.extension.api.error.MuleErrors;
import org.mule.tck.junit4.matcher.ErrorTypeMatcher;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(ROUTERS)
@Story(UNTIL_SUCCESSFUL)
public class UntilSuccessfulRetryExhaustedTestCase extends AbstractIntegrationTestCase {

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
  public void onRetryExhaustedCausedByConnectionExceptionErrorTypeMustBeRetryExhausted() throws Exception {
    flowRunner("retryExhaustedCausedByConnectivityError").withPayload("message")
        .runExpectingException(ErrorTypeMatcher.errorType(MuleErrors.RETRY_EXHAUSTED));
  }

  @Test
  @Issue("MULE-18041")
  @Issue("MULE-18562")
  @Ignore
  public void retryExhaustedCausedByConnectionExceptionLogCheck() throws Exception {
    flowRunner("retryExhaustedCausedByConnectivityErrorLogCheck").withPayload("message")
        .run();
  }

  @Test
  @Issue("MULE-18041")
  @Issue("MULE-18562")
  @Ignore
  public void retryExhaustedCausedByNonConnectionExceptionLogCheck() throws Exception {
    flowRunner("retryExhaustedCausedByNonConnectivityErrorLogCheck").withPayload("message")
        .run();
  }

  @Test
  @Issue("MULE-18041")
  @Issue("MULE-18562")
  @Ignore
  public void retryExhaustedCausedByMuleRuntimeExceptionLogCheck() throws Exception {
    flowRunner("retryExhaustedCausedByMuleRuntimeErrorLogCheck").withPayload("message")
        .run();
  }

  public static class MuleRuntimeError extends MuleRuntimeException {

    public MuleRuntimeError() {
      super(I18nMessageFactory.createStaticMessage("Mule runtime error"));
    }
  }

}
