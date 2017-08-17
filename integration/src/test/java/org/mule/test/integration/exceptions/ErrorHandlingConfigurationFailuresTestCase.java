/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mule.functional.junit4.matchers.ThrowableMessageMatcher.hasMessage;
import static org.mule.runtime.config.spring.api.SpringXmlConfigurationBuilderFactory.createConfigurationBuilder;
import static org.mule.runtime.core.api.context.notification.MuleContextNotification.CONTEXT_STARTED;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.CRITICAL_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.SOURCE_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.SOURCE_ERROR_RESPONSE_GENERATE_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.SOURCE_ERROR_RESPONSE_SEND_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.SOURCE_RESPONSE_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.SOURCE_RESPONSE_GENERATE_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.SOURCE_RESPONSE_SEND_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.UNKNOWN_ERROR_IDENTIFIER;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.context.DefaultMuleContextBuilder;
import org.mule.runtime.core.api.context.DefaultMuleContextFactory;
import org.mule.runtime.core.api.context.MuleContextBuilder;
import org.mule.runtime.core.api.context.MuleContextFactory;
import org.mule.runtime.core.api.context.notification.IntegerAction;
import org.mule.runtime.core.api.context.notification.MuleContextNotification;
import org.mule.runtime.core.api.context.notification.MuleContextNotificationListener;
import org.mule.runtime.core.api.context.notification.NotificationListenerRegistry;
import org.mule.runtime.core.api.util.concurrent.Latch;
import org.mule.tck.config.TestServicesConfigurationBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(ERROR_HANDLING)
@Story("Validations")
public class ErrorHandlingConfigurationFailuresTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void errorHandlerCantHaveMiddleExceptionStrategyWithoutExpression() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException
        .expectMessage(containsString("Every handler (except for the last one) within an 'error-handler' must specify a 'when' or 'type' attribute."));
    loadConfiguration("org/mule/test/integration/exceptions/exception-strategy-in-choice-without-expression.xml");
  }

  // TODO MULE-10061 - Review once the MuleContext lifecycle is clearly defined
  @Test
  public void defaultExceptionStrategyReferencesNonExistentExceptionStrategy() throws Exception {
    expectedException.expect(InitialisationException.class);
    expectedException.expectMessage(containsString("No global error handler defined with name 'nonExistentEh'."));
    loadConfiguration("org/mule/test/integration/exceptions/default-error-handler-reference-non-existent-es.xml");
  }

  @Test
  public void xaTransactionalBlockNotAllowed() throws Exception {
    expectedException.expect(InitialisationException.class);
    expectedException.expectMessage(containsString("No factory available for transaction type XA"));
    loadConfiguration("org/mule/test/integration/transaction/xa-transactional-try-config.xml");
  }

  @Test
  public void unknownErrorFilteringNotAllowed() throws Exception {
    expectedException.expect(InitialisationException.class);
    expectedException.expectCause(hasMessage(equalTo(notFound(UNKNOWN_ERROR_IDENTIFIER))));
    loadConfiguration("org/mule/test/integration/exceptions/unknown-error-filtering-config.xml");
  }

  @Test
  public void sourceErrorResponseFilteringNotAllowed() throws Exception {
    expectedException.expect(InitialisationException.class);
    expectedException.expectCause(hasMessage(equalTo(notFound(SOURCE_ERROR_IDENTIFIER))));
    loadConfiguration("org/mule/test/integration/exceptions/source-error-response-filtering-config.xml");
  }

  @Test
  public void sourceErrorResponseSendFilteringNotAllowed() throws Exception {
    expectedException.expect(InitialisationException.class);
    expectedException.expectCause(hasMessage(equalTo(notFound(SOURCE_ERROR_RESPONSE_SEND_ERROR_IDENTIFIER))));
    loadConfiguration("org/mule/test/integration/exceptions/source-error-response-send-filtering-config.xml");
  }

  @Test
  public void sourceErrorResponseGenerateFilteringNotAllowed() throws Exception {
    expectedException.expect(InitialisationException.class);
    expectedException.expectCause(hasMessage(equalTo(notFound(SOURCE_ERROR_RESPONSE_GENERATE_ERROR_IDENTIFIER))));
    loadConfiguration("org/mule/test/integration/exceptions/source-error-response-generate-filtering-config.xml");
  }

  @Test
  public void criticalErrorFilteringNotAllowed() throws Exception {
    expectedException.expect(InitialisationException.class);
    expectedException.expectCause(hasMessage(equalTo(notFound(CRITICAL_IDENTIFIER))));
    loadConfiguration("org/mule/test/integration/exceptions/critical-error-filtering-config.xml");
  }

  @Test
  public void severalAnyMappingsNotAllowed() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException.expectMessage(containsString("Only one mapping for 'ANY' or an empty source type is allowed."));
    loadConfiguration("org/mule/test/integration/exceptions/several-any-mappings-config.xml");
  }

  @Test
  public void middleAnyMappingsNotAllowed() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException.expectMessage(containsString("Only the last error mapping can have 'ANY' or an empty source type."));
    loadConfiguration("org/mule/test/integration/exceptions/middle-any-mapping-config.xml");
  }

  @Test
  public void repeatedMappingsNotAllowed() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException
        .expectMessage(containsString("Repeated source types are not allowed. Offending types are 'ROUTING', 'EXPRESSION'."));
    loadConfiguration("org/mule/test/integration/exceptions/repeated-mappings-config.xml");
  }

  @Test
  public void sourceResponseGenerateOnErrorContinue() throws Exception {
    expectedException.expect(InitialisationException.class);
    expectedException.expectMessage(equalTo(notAllowed(SOURCE_RESPONSE_GENERATE_ERROR_IDENTIFIER)));
    loadConfiguration("org/mule/test/integration/exceptions/on-error-continue-response-generate.xml");
  }

  @Test
  public void sourceResponseSendOnErrorContinue() throws Exception {
    expectedException.expect(InitialisationException.class);
    expectedException.expectMessage(equalTo(notAllowed(SOURCE_RESPONSE_SEND_ERROR_IDENTIFIER)));
    loadConfiguration("org/mule/test/integration/exceptions/on-error-continue-response-send.xml");
  }

  @Test
  public void sourceResponseErrorOnErrorContinue() throws Exception {
    expectedException.expect(InitialisationException.class);
    expectedException.expectMessage(equalTo(notAllowed(SOURCE_RESPONSE_ERROR_IDENTIFIER)));
    loadConfiguration("org/mule/test/integration/exceptions/on-error-continue-response-error.xml");
  }

  @Test
  public void sourceErrorInListOnErrorContinue() throws Exception {
    expectedException.expect(InitialisationException.class);
    expectedException.expectMessage(equalTo(notAllowed(SOURCE_RESPONSE_ERROR_IDENTIFIER)));
    loadConfiguration("org/mule/test/integration/exceptions/on-error-continue-source-error-list.xml");
  }

  private String notFound(String type) {
    return format("Could not find ErrorType for the given identifier: '%s'", type);
  }

  private String notAllowed(String type) {
    return format("Source errors are not allowed in 'on-error-continue' handlers. Offending type is '%s'.", type);
  }

  private void loadConfiguration(String configuration) throws MuleException, InterruptedException {
    MuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
    List<ConfigurationBuilder> builders = new ArrayList<>();
    builders.add(createConfigurationBuilder(configuration));
    builders.add(new TestServicesConfigurationBuilder());
    MuleContextBuilder contextBuilder = new DefaultMuleContextBuilder();
    MuleContext muleContext = muleContextFactory.createMuleContext(builders, contextBuilder);
    final AtomicReference<Latch> contextStartedLatch = new AtomicReference<>();
    contextStartedLatch.set(new Latch());
    muleContext.getRegistry().lookupObject(NotificationListenerRegistry.class)
        .registerListener(new MuleContextNotificationListener<MuleContextNotification>() {

          @Override
          public boolean isBlocking() {
            return false;
          }

          @Override
          public void onNotification(MuleContextNotification notification) {
            if (new IntegerAction(CONTEXT_STARTED).equals(notification.getAction())) {
              contextStartedLatch.get().countDown();
            }
          }
        });
    muleContext.start();
    contextStartedLatch.get().await(20, SECONDS);
  }

}
