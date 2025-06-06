/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import static java.util.Base64.getDecoder;

import static jakarta.servlet.http.HttpServletResponse.SC_OK;
import static jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.IOException;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public abstract class AbstractModuleWithHttpTestCase extends AbstractCeXmlExtensionMuleArtifactFunctionalTestCase {

  protected static final String MODULE_GLOBAL_ELEMENT_XML = "modules/module-global-element.xml";
  static final String MODULE_GLOBAL_ELEMENT_PROXY_XML = "modules/nested/module-global-element-proxy.xml";
  static final String MODULE_GLOBAL_ELEMENT_ANOTHER_PROXY_XML = "modules/nested/module-global-element-another-proxy.xml";
  static final String USER_AND_PASS_VALIDATED_RESPONSE = "\"User and pass validated\"";
  static final String SUCCESS_RESPONSE = "{ \"response\":" + USER_AND_PASS_VALIDATED_RESPONSE + " }";
  private static final String FAILURE_RESPONSE = "{ \"response\":\"User and pass wrong\" }";

  private Server server;

  @Rule
  public DynamicPort httpPort = new DynamicPort("httpPort");

  @Before
  public void startServer() throws Exception {
    server = new Server(httpPort.getNumber());
    server.setHandler(new SimpleBasicAuthentication());
    server.start();
  }

  @After
  public void stopServer() throws Exception {
    if (server != null) {
      server.stop();
    }
  }

  /**
   * Asserts that a given flow can successfully be executed and it also checks that the authorization against the
   * {@link SimpleBasicAuthentication} handler does return a success response for the parametrized username
   *
   * @param flowName to execute
   * @param username to validate after hitting the HTTP endpoint
   */
  protected void assertFlowForUsername(String flowName, String username) throws Exception {
    CoreEvent muleEvent = flowRunner(flowName).run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), is("success with basic-authentication for user: " + username));
  }

  /**
   * Really simple handler for basic authentication where the user and pass, once decoded, must match the path of the request. For
   * example: "/basic-auth/userLP/passLP" request must have an "Authorization" header with "userLP:passLP" encoded in Base64 to
   * return 200, otherwise it will be 401 (unauthorized)
   */
  private class SimpleBasicAuthentication extends AbstractHandler {

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {
      int scUnauthorized;
      String message;
      String userAndPass = new String(getDecoder().decode(request.getHeader("Authorization").substring("Basic ".length())))
          .replace(':', '/');
      if (target.endsWith(userAndPass)) {
        scUnauthorized = SC_OK;
        message = SUCCESS_RESPONSE;
      } else {
        scUnauthorized = SC_UNAUTHORIZED;
        message = FAILURE_RESPONSE;
      }
      response.setStatus(scUnauthorized);
      response.getWriter().print(message);
      response.setContentType("application/json");
      baseRequest.setHandled(true);
    }
  }

}
