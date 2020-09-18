/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.oauth.authcode;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.runtime.api.metadata.MetadataKeyBuilder.newKey;
import static org.mule.runtime.api.metadata.MetadataService.METADATA_SERVICE_KEY;

import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.test.module.extension.oauth.BaseOAuthExtensionTestCase;

import java.util.function.Supplier;

import com.github.tomakehurst.wiremock.client.WireMock;
import javax.inject.Inject;
import javax.inject.Named;
import org.junit.Before;
import org.junit.Test;

public class OAuthMetadataRefreshExtensionTestCase extends BaseOAuthExtensionTestCase {

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"auth-code-oauth-extension-static-config.xml", "oauth-extension-flows.xml"};
  }

  @Inject
  @Named(METADATA_SERVICE_KEY)
  protected MetadataService metadataService;

  @Before
  public void setOwnerId() throws Exception {
    ownerId = getCustomOwnerId();
    storedOwnerId = getCustomOwnerId() + "-oauth";
  }

  @Test
  public void refreshTokenOnOutputMetadataResolution() throws Exception {
    assertThatMetadataResolutionIsSuccessfulWithRefresh(() -> metadataService
        .getOutputMetadata(Location.builder().globalName("metadata").addProcessorsPart().addIndexPart(0).build(),
                           newKey("anyKey").build()));
  }


  @Test
  public void tokenRefreshOnInputMetadataResolution() throws Exception {
    assertThatMetadataResolutionIsSuccessfulWithRefresh(() -> metadataService
        .getInputMetadata(Location.builder().globalName("metadata").addProcessorsPart().addIndexPart(0).build(),
                          newKey("anyKey").build()));
  }

  @Test
  public void tokenRefreshOnAttributeMetadataResolution() throws Exception {
    assertThatMetadataResolutionIsSuccessfulWithRefresh(() -> metadataService
        .getOutputMetadata(Location.builder().globalName("anotherMetadata").addProcessorsPart().addIndexPart(0).build(),
                           newKey("anyKey").build()));
  }

  @Test
  public void tokenRefreshOnMetadataKeysResolution() throws Exception {
    assertThatMetadataResolutionIsSuccessfulWithRefresh(() -> metadataService
        .getMetadataKeys(Location.builder().globalName("metadata").addProcessorsPart().addIndexPart(0).build()));
  }

  @Test
  public void tokenRefreshOnEntitiesResolution() throws Exception {
    assertThatMetadataResolutionIsSuccessfulWithRefresh(() -> metadataService
        .getEntityKeys(Location.builder().globalName("entitiesMetadata").addProcessorsPart().addIndexPart(0).build()));
  }

  @Test
  public void tokenRefreshOnEntityTypeResolution() throws Exception {
    assertThatMetadataResolutionIsSuccessfulWithRefresh(() -> metadataService
        .getEntityMetadata(Location.builder().globalName("entitiesMetadata").addProcessorsPart().addIndexPart(0).build(),
                           newKey("anyKey").build()));
  }

  private void assertThatMetadataResolutionIsSuccessfulWithRefresh(Supplier<MetadataResult> metadataResultSupplier) {
    simulateCallback();

    WireMock.reset();
    stubTokenUrl(accessTokenContent(ACCESS_TOKEN + "-refreshed"));

    assertThat(metadataResultSupplier.get().isSuccess(), is(true));
  }

}
