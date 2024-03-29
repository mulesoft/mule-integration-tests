/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.domain.http;

import static org.hamcrest.core.IsInstanceOf.instanceOf;

import org.mule.functional.junit4.ApplicationContextBuilder;
import org.mule.functional.junit4.DomainContextBuilder;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.qameta.allure.Issue;

@Ignore("MULE-10633")
@Issue("MULE-10633")
public class HttpSharePortSamePathTestCase extends AbstractMuleTestCase {

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");
  @Rule
  public SystemProperty endpointScheme = getEndpointSchemeSystemProperty();

  @Rule
  public ExpectedException expected = ExpectedException.none();

  private MuleContext domainContext;
  private MuleContext firstAppContext;

  @After
  public void after() {
    if (firstAppContext != null) {
      firstAppContext.dispose();
    }
    if (domainContext != null) {
      domainContext.dispose();
    }
  }

  @Test
  public void samePathDefinedInTwoAppsWithinSameDomain() throws Exception {
    final ArtifactContext domainArtifactContext = new DomainContextBuilder()
        .setDomainConfig("domain/http/http-shared-listener-config.xml")
        .build();
    domainContext = domainArtifactContext
        .getMuleContext();
    firstAppContext = new ApplicationContextBuilder()
        .setApplicationResources("domain/http/http-hello-mule-app.xml")
        .setDomainArtifactContext(domainArtifactContext)
        .build();
    ApplicationContextBuilder secondApp = new ApplicationContextBuilder();

    expected.expect(instanceOf(InitialisationException.class));
    secondApp.setApplicationResources("domain/http/http-hello-mule-app.xml")
        .setDomainArtifactContext(domainArtifactContext)
        .build();
  }

  public SystemProperty getEndpointSchemeSystemProperty() {
    return new SystemProperty("scheme", "http");
  }
}
