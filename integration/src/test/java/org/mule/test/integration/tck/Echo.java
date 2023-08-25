/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration.tck;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;

/**
 * Simple web service to use for test cases.
 */
@WebService
public interface Echo {

  @WebResult(name = "text")
  public String echo(@WebParam(name = "text") String string);
}
