/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.it.soap.connect;

import static extension.org.mule.soap.it.TestServiceProvider.ERROR_MSG;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.util.TestConnectivityUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class InvalidProviderConfigTestCase extends AbstractSimpleServiceFunctionalTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public SystemProperty property = TestConnectivityUtils.disableAutomaticTestConnectivity();

  @Override
  protected String getConfigFile() {
    return "invalid-provider.xml";
  }

  @Test
  public void invalid() throws Exception {
    expectedException.expectMessage(ERROR_MSG);
    expectedException.expectCause(instanceOf(ConnectionException.class));
    throw flowRunner("invalid").runExpectingException();
  }
}
