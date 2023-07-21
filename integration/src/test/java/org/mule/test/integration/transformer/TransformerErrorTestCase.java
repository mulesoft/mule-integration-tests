/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration.transformer;

import static org.hamcrest.Matchers.containsString;
import org.mule.functional.api.exception.ExpectedError;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Rule;
import org.junit.Test;

public class TransformerErrorTestCase extends AbstractIntegrationTestCase {

  @Rule
  public ExpectedError expectedError = ExpectedError.none();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/transformers/transformation-error-config.xml";
  }

  @Test
  public void errorFormattingDoesNotAffectData() throws Exception {
    expectedError.expectErrorType("MULE", "TRANSFORMATION");
    expectedError.expectMessage(containsString("Could not find a transformer"));
    flowRunner("transformFail").run();
  }

}
