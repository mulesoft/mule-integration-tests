/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.runtime.api.metadata.MetadataService.METADATA_SERVICE_KEY;
import static org.mule.test.allure.AllureConstants.RoutersFeature.ROUTERS;
import static org.mule.test.allure.AllureConstants.RoutersFeature.RoundRobinStory.ROUND_ROBIN;

import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.test.AbstractIntegrationTestCase;

import javax.inject.Inject;
import javax.inject.Named;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(ROUTERS)
@Story(ROUND_ROBIN)
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

  @Test
  @Description("Resolves metadata from a connector inside of a incomplete Scatter-Gather. In runtime this case fails, " +
      "but in lazy mode ignores any validation.")
  public void metadataFromElementInsideScatterGather() {
    Location build = Location
        .builderFromStringRepresentation("select-inside-scatter-gather/processors/0/route/0/processors/0").build();
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> operationMetadata = metadataService.getOperationMetadata(build);

    assertThat(operationMetadata.isSuccess(), is(true));
  }

}
