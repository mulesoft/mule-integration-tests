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

import org.mule.functional.api.exception.FunctionalTestException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.processor.Processor;
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
    SensingExceptionParentProcessor.caught = false;
    SensingExceptionChildProcessor.caught = true;
  }

  @Test
  public void testRequestResponseEndpointExceptionPropagation() throws Exception {
    expectedException.expectCause(instanceOf(FunctionalTestException.class));
    runFlow("flow");
  }

  @Test
  public void testFlowWithChildFlowExceptionPropagation() throws Exception {
    runFlow("flowWithChildFlow");

    assertThat(SensingExceptionParentProcessor.caught, is(false));
    assertThat(SensingExceptionChildProcessor.caught, is(true));
  }

  @Test
  public void testFlowWithSubFlowExceptionPropagation() throws Exception {
    runFlow("flowWithSubFlow");

    assertThat(SensingExceptionParentProcessor.caught, is(true));
  }

  @Test
  public void testFlowWithChildServiceExceptionPropagation() throws Exception {
    runFlow("flowWithChildService");

    assertThat(SensingExceptionParentProcessor.caught, is(false));
    assertThat(SensingExceptionChildProcessor.caught, is(true));
  }

  public static class SensingExceptionParentProcessor implements Processor {

    static boolean caught;

    @Override
    public Event process(Event event) throws MuleException {
      caught = true;
      return event;
    }
  }

  public static class SensingExceptionChildProcessor implements Processor {

    static boolean caught;

    @Override
    public Event process(Event event) throws MuleException {
      caught = true;
      return event;
    }
  }
}
