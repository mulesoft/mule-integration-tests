/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.runtime.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.runtime.api.metadata.MetadataService.METADATA_SERVICE_KEY;
import static org.mule.tck.junit4.matcher.metadata.MetadataKeyResultSuccessMatcher.isSuccess;
import static org.mule.test.allure.AllureConstants.LazyInitializationFeature.LAZY_INITIALIZATION;
import static org.mule.test.allure.AllureConstants.RoutersFeature.ROUTERS;
import static org.mule.test.allure.AllureConstants.RoutersFeature.ScatterGatherStory.SCATTER_GATHER;

import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.config.api.LazyComponentInitializer;
import org.mule.test.AbstractIntegrationTestCase;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Story;

@Features({@Feature(LAZY_INITIALIZATION), @Feature(ROUTERS)})
@Story(SCATTER_GATHER)
public class RoutesLazyInitTestCase extends AbstractIntegrationTestCase {

  @Override
  public boolean enableLazyInit() {
    return true;
  }

  @Override
  public boolean disableXmlValidations() {
    return true;
  }

  @Override
  protected String getConfigFile() {
    return "lazy-routes.xml";
  }

  @Inject
  @Named(METADATA_SERVICE_KEY)
  private MetadataService metadataService;

  @Inject
  private LazyComponentInitializer lazyComponentInitializer;

  @Test
  @Description("Resolves metadata from a connector inside of a incomplete Scatter-Gather. In runtime this case fails, " +
      "but in lazy mode ignores any validation.")
  public void metadataFromElementInsideScatterGather() {
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> operationMetadata = metadataService
        .getOperationMetadata(builderFromStringRepresentation("select-inside-scatter-gather/processors/0/route/0/processors/0")
            .build());

    assertThat(operationMetadata, isSuccess());
  }

  @Test
  public void singleRouteValidation() {
    lazyComponentInitializer.initializeComponent(builderFromStringRepresentation("select-inside-scatter-gather").build());
  }

}
