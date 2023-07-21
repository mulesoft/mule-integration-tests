/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.Matchers.any;
import static org.hamcrest.core.Is.is;

import org.mule.functional.api.exception.ExpectedError;
import org.mule.functional.api.flow.FlowRunner;
import org.mule.test.AbstractIntegrationTestCase;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Rule;
import org.junit.Test;

public class ExceptionMappingTestCase extends AbstractIntegrationTestCase {

  @Rule
  public ExpectedError expected = ExpectedError.none();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/exception-mapping-config.xml";
  }

  @Test
  public void transformationError() throws Exception {
    expected.expectErrorType(any(String.class), is("EXPRESSION"));

    new FlowRunner(registry, "transformationErrorFlow").withPayload(new InputStream() {

      @Override
      public int read() throws IOException {
        throw new IOException();
      }
    }).run();
  }

  @Test
  public void expressionError() throws Exception {
    expected.expectErrorType(any(String.class), is("EXPRESSION"));
    new FlowRunner(registry, "expressionErrorFlow").run();
  }

}
