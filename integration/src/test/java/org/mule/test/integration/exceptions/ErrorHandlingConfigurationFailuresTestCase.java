/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.config.spring.SpringXmlConfigurationBuilder;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.context.MuleContextBuilder;
import org.mule.runtime.core.api.context.MuleContextFactory;
import org.mule.runtime.core.api.context.notification.MuleContextNotificationListener;
import org.mule.runtime.core.api.context.DefaultMuleContextBuilder;
import org.mule.runtime.core.api.context.DefaultMuleContextFactory;
import org.mule.runtime.core.api.context.notification.MuleContextNotification;
import org.mule.runtime.core.api.util.concurrent.Latch;
import org.mule.tck.config.TestServicesConfigurationBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.ForceXalanTransformerFactory;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features(ERROR_HANDLING)
@Stories("Validations")
@RunWith(Parameterized.class)
public class ErrorHandlingConfigurationFailuresTestCase extends AbstractMuleTestCase {

  @Rule
  public SystemProperty useXalan;

  /**
   * Verify that regardless of the XML library used, validation errors are handled correctly.
   * 
   * @return
   */
  @Parameterized.Parameters
  public static Collection<Object[]> parameters() {
    return Arrays.asList(new Object[][] {{false}, {true}});
  }

  public ErrorHandlingConfigurationFailuresTestCase(boolean isUseXalan) {
    if (isUseXalan) {
      useXalan = new ForceXalanTransformerFactory();
    } else {
      useXalan = null;
    }
  }

  @Test(expected = ConfigurationException.class)
  public void errorHandlerCantHaveMiddleExceptionStrategyWithoutExpression() throws Exception {
    loadConfiguration("org/mule/test/integration/exceptions/exception-strategy-in-choice-without-expression.xml");
  }

  // TODO MULE-10061 - Review once the MuleContext lifecycle is clearly defined
  @Test(expected = InitialisationException.class)
  public void defaultExceptionStrategyReferencesNonExistentExceptionStrategy() throws Exception {
    loadConfiguration("org/mule/test/integration/exceptions/default-error-handler-reference-non-existent-es.xml");
  }

  @Test(expected = InitialisationException.class)
  public void defaultErrorHandlerMustHaveCatchAll() throws Exception {
    loadConfiguration("org/mule/test/integration/exceptions/default-error-handler-catch-all.xml");
  }

  @Test(expected = InitialisationException.class)
  public void xaTransactionalBlockNotAllowed() throws Exception {
    loadConfiguration("org/mule/test/integration/transaction/xa-transactional-try-config.xml");
  }

  @Test(expected = InitialisationException.class)
  public void unknownErrorFilteringNotAllowed() throws Exception {
    loadConfiguration("org/mule/test/integration/exceptions/unknown-error-filtering-config.xml");
  }

  @Test(expected = InitialisationException.class)
  public void criticalErrorFilteringNotAllowed() throws Exception {
    loadConfiguration("org/mule/test/integration/exceptions/critical-error-filtering-config.xml");
  }

  @Test(expected = ConfigurationException.class)
  public void severalAnyMappingsNotAllowed() throws Exception {
    loadConfiguration("org/mule/test/integration/exceptions/several-any-mappings-config.xml");
  }

  @Test(expected = ConfigurationException.class)
  public void middleAnyMappingsNotAllowed() throws Exception {
    loadConfiguration("org/mule/test/integration/exceptions/middle-any-mapping-config.xml");
  }

  @Test(expected = ConfigurationException.class)
  public void repeatedMappingsNotAllowed() throws Exception {
    loadConfiguration("org/mule/test/integration/exceptions/repeated-mappings-config.xml");
  }

  private void loadConfiguration(String configuration) throws MuleException, InterruptedException {
    MuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
    List<ConfigurationBuilder> builders = new ArrayList<>();
    builders.add(new SpringXmlConfigurationBuilder(configuration));
    builders.add(new TestServicesConfigurationBuilder());
    MuleContextBuilder contextBuilder = new DefaultMuleContextBuilder();
    MuleContext muleContext = muleContextFactory.createMuleContext(builders, contextBuilder);
    final AtomicReference<Latch> contextStartedLatch = new AtomicReference<>();
    contextStartedLatch.set(new Latch());
    muleContext.registerListener(new MuleContextNotificationListener<MuleContextNotification>() {

      @Override
      public boolean isBlocking() {
        return false;
      }

      @Override
      public void onNotification(MuleContextNotification notification) {
        if (notification.getAction() == MuleContextNotification.CONTEXT_STARTED) {
          contextStartedLatch.get().countDown();
        }
      }
    });
    muleContext.start();
    contextStartedLatch.get().await(20, TimeUnit.SECONDS);
  }

}
