/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features(ERROR_HANDLING)
@Stories("On Error Nested Exceptions")
public class OnErrorNestedExceptionsTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/on-error-nested-exceptions-config.xml";
  }

  @Test
  public void propagatesToOuterWithoutExceptionAndSucceeds() throws Exception {
    Event event = flowRunner("propagatesToOuterWithoutExceptionAndSucceeds").run();
    assertThat(event.getMessage(), hasPayload(equalTo("propagated again")));
  }

  @Test
  public void propagatesToOuterWithoutExceptionAndFails() throws Exception {
    MessagingException exception = flowRunner("propagatesToOuterWithoutExceptionAndFails").runExpectingException();
    assertThat(exception.getEvent().getMessage(), hasPayload(equalTo("propagated again")));
  }

  @Test
  public void propagatesToOuterWithExceptionAndSucceeds() throws Exception {
    Event event = flowRunner("propagatesToOuterWithExceptionAndSucceeds").run();
    assertThat(event.getMessage(), hasPayload(equalTo("propagated again")));
  }

  @Test
  public void propagatesToOuterWithExceptionAndFails() throws Exception {
    MessagingException exception = flowRunner("propagatesToOuterWithExceptionAndFails").runExpectingException();
    assertThat(exception.getEvent().getMessage(), hasPayload(equalTo("propagated again")));
  }

}
