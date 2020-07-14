/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.ast;

import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.metadata.api.utils.MetadataTypeUtils.getTypeId;

import io.qameta.allure.Issue;
import org.junit.Test;
import org.mule.extension.http.api.request.proxy.HttpProxyConfig;
import org.mule.extension.oauth2.OAuthExtension;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;

public class ParameterAstTestCaseRuntimeMode extends AbstractParameterAstTestCase {

  @Override
  protected boolean runtimeMode() {
    return true;
  }

  @Override
  protected String getConfig() {
    return "org/mule/test/config/ast/parameters-test-runtime-mode-config.xml";
  }

  @Override
  protected Class[] getExtensions() {
    return new Class[] {OAuthExtension.class};
  }

  @Issue("MULE-18564")
  @Test
  public void oauthCredentialThroughProxyInlineDefinition() {
    ComponentAst httpRequestConfigWithOAuthProxyInline = findComponentByComponentId(artifactAst.topLevelComponentsStream(), "httpRequestConfigWithOAuthProxyInline")
            .orElseThrow(() -> new AssertionError("Couldn't find 'httpRequestConfigWithOAuthProxyInline'"));

    ComponentAst oAuthHttpRequestConnection = findComponent(httpRequestConfigWithOAuthProxyInline.directChildrenStream(), "http:request-connection")
            .orElseThrow(() -> new AssertionError("Couldn't find 'http:request-connection'"));

    // Nested parameters with child element should be added as complex ComponentParameterAst
    ComponentParameterAst proxyConfig = oAuthHttpRequestConnection.getParameter("proxyConfig");
    assertThat(proxyConfig.getRawValue(), is(nullValue()));
    assertThat(proxyConfig.getValue().getRight(), not(nullValue()));
    assertThat(getTypeId(proxyConfig.getModel().getType()), equalTo(of(HttpProxyConfig.class.getName())));

    ComponentAst oauthHttpProxy = (ComponentAst) proxyConfig.getValue().getRight();
    ComponentAst httpProxy = findComponent(oauthHttpProxy.directChildrenStream(), "http:proxy")
            .orElseThrow(() -> new AssertionError("Couldn't find 'http:proxy'"));
    ComponentParameterAst portParameter = httpProxy.getParameter("port");
    assertThat(portParameter.getValue().getRight(), is(8083));
    ComponentParameterAst hostParameter = httpProxy.getParameter("host");
    assertThat(hostParameter.getValue().getRight(), is("localhost"));
  }
}
