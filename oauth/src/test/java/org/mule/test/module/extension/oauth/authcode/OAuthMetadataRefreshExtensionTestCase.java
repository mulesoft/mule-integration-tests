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
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.metadata.descriptor.InputMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.OutputMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.TypeMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.test.module.extension.oauth.BaseOAuthExtensionTestCase;

import java.util.function.Supplier;

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
    assertThatMetadataResolutionIsSuccessfulWithRefresh(this::getOutputMetadata);
  }

  @Test
  public void tokenExpiredOnOutputMetadataResolution() throws Exception {
    assertTokenExpiration(this::getOutputMetadata);
  }

  @Test
  public void tokenRefreshOnInputMetadataResolution() throws Exception {
    assertThatMetadataResolutionIsSuccessfulWithRefresh(this::getInputMetadata);
  }

  @Test
  public void tokenExpiredOnInputMetadataResolution() throws Exception {
    assertTokenExpiration(this::getInputMetadata);
  }

  @Test
  public void tokenRefreshOnAttributeMetadataResolution() throws Exception {
    assertThatMetadataResolutionIsSuccessfulWithRefresh(this::getAttributesOutputMetadata);
  }

  @Test
  public void tokenExpiredOnAttributeMetadataResolution() throws Exception {
    assertTokenExpiration(this::getAttributesOutputMetadata);
  }

  @Test
  public void tokenRefreshOnMetadataKeysResolution() throws Exception {
    assertThatMetadataResolutionIsSuccessfulWithRefresh(this::getMetadataKeys);
  }

  @Test
  public void tokenExpiredOnMetadataKeysResolution() throws Exception {
    assertTokenExpiration(this::getMetadataKeys);
  }

  private MetadataResult<MetadataKeysContainer> getMetadataKeys() {
    return metadataService.getMetadataKeys(Location.builder()
        .globalName("metadata").addProcessorsPart().addIndexPart(0).build());
  }

  @Test
  public void tokenRefreshOnEntitiesResolution() throws Exception {
    assertThatMetadataResolutionIsSuccessfulWithRefresh(this::getEntitiesMetadata);
  }

  @Test
  public void tokenExpiredOnEntitiesResolution() throws Exception {
    assertTokenExpiration(this::getEntitiesMetadata);
  }

  @Test
  public void tokenRefreshOnEntityTypeResolution() throws Exception {
    assertThatMetadataResolutionIsSuccessfulWithRefresh(this::getEntityMetadata);
  }

  @Test
  public void tokenExpiredOnEntityTypeResolution() throws Exception {
    assertTokenExpiration(this::getEntityMetadata);
  }

  private void assertThatMetadataResolutionIsSuccessfulWithRefresh(Supplier<MetadataResult> metadataResultSupplier) {
    simulateCallback();

    stubRefreshedTokenAlreadyExpired();
    assertThat(metadataResultSupplier.get().isSuccess(), is(true));
  }

  private void assertTokenExpiration(Supplier<MetadataResult> metadataResultSupplier) {
    simulateCallback();

    stubRefreshedTokenAlreadyExpiredTwice();
    assertThat(metadataResultSupplier.get().isSuccess(), is(false));
  }

  private MetadataResult<OutputMetadataDescriptor> getOutputMetadata() {
    return metadataService.getOutputMetadata(Location.builder()
        .globalName("metadata").addProcessorsPart().addIndexPart(0).build(),
                                             newKey("anyKey").build());
  }

  private MetadataResult<InputMetadataDescriptor> getInputMetadata() {
    return metadataService.getInputMetadata(Location.builder()
        .globalName("metadata")
        .addProcessorsPart()
        .addIndexPart(0)
        .build(),
                                            newKey("anyKey").build());
  }

  private MetadataResult<OutputMetadataDescriptor> getAttributesOutputMetadata() {
    return metadataService.getOutputMetadata(Location.builder()
        .globalName("anotherMetadata")
        .addProcessorsPart()
        .addIndexPart(0)
        .build(),
                                             newKey("anyKey").build());
  }

  private MetadataResult<MetadataKeysContainer> getEntitiesMetadata() {
    return metadataService.getEntityKeys(Location.builder()
        .globalName("entitiesMetadata")
        .addProcessorsPart()
        .addIndexPart(0)
        .build());
  }

  private MetadataResult<TypeMetadataDescriptor> getEntityMetadata() {
    return metadataService.getEntityMetadata(Location.builder()
        .globalName("entitiesMetadata").addProcessorsPart().addIndexPart(0)
        .build(),
                                             newKey("anyKey").build());
  }
}
