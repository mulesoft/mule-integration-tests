/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.mule.functional.api.component.EventCallback;
import org.mule.functional.api.exception.FunctionalTestException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.BaseEvent;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Assert that flows do not propagate exceptions via runFlow or use of flow-ref. Also assert that a sub-flow/processor-chain does
 * not handle it's own exception but they are rather handled by calling flow.
 */
public class ExceptionPropagationMule5737TestCase extends AbstractIntegrationTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/exception-propagation-mule-5737-config.xml";
  }

  @Before
  public void before() {
    SensingExceptionParentCallback.caught = false;
    SensingExceptionChildCallback.caught = true;
  }

  @Test
  public void testRequestResponseEndpointExceptionPropagation() throws Exception {
    expectedException.expectCause(instanceOf(FunctionalTestException.class));
    runFlow("flow");
  }

  @Test
  public void testFlowWithChildFlowExceptionPropagation() throws Exception {
    runFlow("flowWithChildFlow");

    assertThat(SensingExceptionParentCallback.caught, is(false));
    assertThat(SensingExceptionChildCallback.caught, is(true));
  }

  @Test
  public void testFlowWithSubFlowExceptionPropagation() throws Exception {
    runFlow("flowWithSubFlow");

    assertThat(SensingExceptionParentCallback.caught, is(true));
  }

  @Test
  public void testFlowWithChildServiceExceptionPropagation() throws Exception {
    runFlow("flowWithChildService");

    assertThat(SensingExceptionParentCallback.caught, is(false));
    assertThat(SensingExceptionChildCallback.caught, is(true));
  }

  public static class SensingExceptionParentCallback implements EventCallback {

    static boolean caught;

    @Override
    public void eventReceived(BaseEvent event, Object component, MuleContext muleContext) throws Exception {
      caught = true;
    }
  }

  public static class SensingExceptionChildCallback implements EventCallback {

    static boolean caught;

    @Override
    public void eventReceived(BaseEvent event, Object component, MuleContext muleContext) throws Exception {
      caught = true;
    }
  }
}
