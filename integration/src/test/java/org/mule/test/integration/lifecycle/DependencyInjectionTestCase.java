/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.lifecycle;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.ArtifactObjectsDependencyInjectionStory.ARTIFACT_OBJECTS_DEPENDENCY_INJECTION_STORY;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.LIFECYCLE_AND_DEPENDENCY_INJECTION;
import org.mule.runtime.api.exception.MuleException;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features(LIFECYCLE_AND_DEPENDENCY_INJECTION)
@Stories(ARTIFACT_OBJECTS_DEPENDENCY_INJECTION_STORY)
public class DependencyInjectionTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/lifecycle/artifact-object-dependency-injection-config.xml";
  }

  @Test
  public void validateInjectedObjectsDefinedInXmlConfig() {
    DependencyInjectionBean dependencyInjectionBean = muleContext.getRegistry().get("dependencyInjectionBean");
    validateInjectedObjects(dependencyInjectionBean);
  }

  @Test
  public void validateInjectedObjectsUsingInjector() throws MuleException {
    DependencyInjectionBean dependencyInjectionBean = new DependencyInjectionBean();
    muleContext.getInjector().inject(dependencyInjectionBean);
    validateInjectedObjects(dependencyInjectionBean);
  }

  private void validateInjectedObjects(DependencyInjectionBean dependencyInjectionBean) {
    assertThat(dependencyInjectionBean.getConfigurationComponentLocator(), notNullValue());
    assertThat(dependencyInjectionBean.getExtensionsClient(), notNullValue());
    assertThat(dependencyInjectionBean.getExpressionLanguage(), notNullValue());
    assertThat(dependencyInjectionBean.getMuleExpressionLanguage(), notNullValue());
    assertThat(dependencyInjectionBean.getTransformationService(), notNullValue());
    assertThat(dependencyInjectionBean.getObjectSerializer(), notNullValue());
    assertThat(dependencyInjectionBean.getServerNotificationHandler(), notNullValue());
    assertThat(dependencyInjectionBean.getTransformationService(), notNullValue());

    assertThat(dependencyInjectionBean.getLocalObjectStoreManager(), notNullValue());
    assertThat(dependencyInjectionBean.getObjectStoreManager(), notNullValue());
    assertThat(dependencyInjectionBean.getObjectStoreManager().equals(dependencyInjectionBean.getLocalObjectStoreManager()),
               is(true));

    assertThat(dependencyInjectionBean.getLocalLockFactory(), notNullValue());
    assertThat(dependencyInjectionBean.getLockFactory(), notNullValue());
    assertThat(dependencyInjectionBean.getLocalLockFactory().equals(dependencyInjectionBean.getLockFactory()),
               is(true));
  }

}
