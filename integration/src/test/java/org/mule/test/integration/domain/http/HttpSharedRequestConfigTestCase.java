/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.domain.http;

import io.qameta.allure.Issue;
import org.mule.functional.junit4.DomainFunctionalTestCase;

import org.junit.Ignore;
import org.junit.Test;

@Ignore("MULE-10633")
@Issue("MULE-10633")
public class HttpSharedRequestConfigTestCase extends DomainFunctionalTestCase {

  private static final String FIRST_APP_NAME = "app-1";
  private static final String SECOND_APP_NAME = "app-2";

  @Override
  protected String getDomainConfig() {
    return "domain/http/http-shared-request-config.xml";
  }

  @Override
  public ApplicationConfig[] getConfigResources() {
    return new ApplicationConfig[] {new ApplicationConfig(FIRST_APP_NAME, new String[] {"domain/http/http-request-app.xml"}),
        new ApplicationConfig(SECOND_APP_NAME, new String[] {"domain/http/http-request-app.xml"})};
  }

  @Test
  public void useSameRequestConfig() throws Exception {
    // TODO MULE-10633 assert that getting the config from each application return the same instance (the one from the domain)
  }

}
