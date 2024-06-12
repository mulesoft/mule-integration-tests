/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.it.soap.connect;

import static org.hamcrest.Matchers.containsString;
import static org.mule.runtime.soap.api.exception.error.SoapErrors.BAD_REQUEST;
import static org.mule.runtime.soap.api.exception.error.SoapErrors.SOAP_FAULT;

import org.mule.functional.api.exception.ExpectedError;

import org.junit.Rule;
import org.junit.Test;

public class InvokeOperationErrorsTestCase extends SoapFootballExtensionArtifactFunctionalTestCase {

  @Rule
  public ExpectedError expected = ExpectedError.none();

  @Test
  public void badRequest() throws Exception {
    expected.expectErrorType("SOAP", BAD_REQUEST.toString());
    expected.expectMessage(containsString("the request body is not a valid XML"));

    flowRunner("getLeagues").withPayload("not a valid XML").keepStreamsOpen().run();
  }

  @Test
  public void commonSoapFault() throws Exception {
    expected.expectErrorType("SOAP", SOAP_FAULT.toString());
    expected.expectMessage(containsString("Unexpected wrapper element {http://services.connect.soap.it.mule.org/}noOp found"));

    flowRunner("getLeagues").withPayload(getBodyXml("noOp", "")).keepStreamsOpen().run();
  }
}
