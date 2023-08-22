/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.lifecycle;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.LIFECYCLE_AND_DEPENDENCY_INJECTION;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.ArtifactObjectsDependencyInjectionStory.ARTIFACT_OBJECTS_DEPENDENCY_INJECTION_STORY;

import org.mule.runtime.api.exception.MuleException;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.tests.api.pojos.DependencyInjectionObject;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(LIFECYCLE_AND_DEPENDENCY_INJECTION)
@Story(ARTIFACT_OBJECTS_DEPENDENCY_INJECTION_STORY)
public class DependencyInjectionTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/lifecycle/artifact-object-dependency-injection-config.xml";
  }

  @Test
  public void validateInjectedObjectsDefinedInXmlConfig() {
    DependencyInjectionObject dependencyInjectionObject =
        registry.<DependencyInjectionObject>lookupByName("dependencyInjectionBean").get();
    validateInjectedObjects(dependencyInjectionObject);
  }

  @Test
  public void validateInjectedObjectsUsingInjector() throws MuleException {
    DependencyInjectionObject dependencyInjectionObject = new DependencyInjectionObject();
    muleContext.getInjector().inject(dependencyInjectionObject);
    validateInjectedObjects(dependencyInjectionObject);
  }

  private void validateInjectedObjects(DependencyInjectionObject dependencyInjectionObject) {
    assertThat(dependencyInjectionObject.getConfigurationComponentLocator(), notNullValue());
    assertThat(dependencyInjectionObject.getExtensionsClient(), notNullValue());
    assertThat(dependencyInjectionObject.getExpressionLanguage(), notNullValue());
    assertThat(dependencyInjectionObject.getMuleExpressionLanguage(), notNullValue());
    assertThat(dependencyInjectionObject.getTransformationService(), notNullValue());
    assertThat(dependencyInjectionObject.getObjectSerializer(), notNullValue());
    assertThat(dependencyInjectionObject.getServerNotificationHandler(), notNullValue());

    assertThat(dependencyInjectionObject.getLocalObjectStoreManager(), notNullValue());
    assertThat(dependencyInjectionObject.getObjectStoreManager(), notNullValue());
    assertThat(dependencyInjectionObject.getObjectStoreManager().equals(dependencyInjectionObject.getLocalObjectStoreManager()),
               is(true));

    assertThat(dependencyInjectionObject.getLocalLockFactory(), notNullValue());
    assertThat(dependencyInjectionObject.getLockFactory(), notNullValue());
    assertThat(dependencyInjectionObject.getLocalLockFactory().equals(dependencyInjectionObject.getLockFactory()),
               is(true));
  }

}
