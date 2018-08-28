/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.classloading;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;
import org.junit.Test;


public class ResourceAccessTestCase extends MuleArtifactFunctionalTestCase {

  private static final String COMMONS_LANG_MANIFEST = "resource::org.apache.commons:commons-lang3:3.8:META-INF/MANIFEST.MF";
  private static final String COMMONS_TEXT_MANIFEST = "resource::org.apache.commons:commons-text:1.4:META-INF/MANIFEST.MF";
  private static final String COMMONS_COMPRESS_MANIFEST =
      "resource::org.apache.commons:commons-compress:1.18:META-INF/MANIFEST.MF";
  private static final String EMPTY_STRING = "";

  @Rule
  public SystemProperty serviceDependency = new SystemProperty("serviceDependency", COMMONS_LANG_MANIFEST);
  @Rule
  public SystemProperty pluginDependency = new SystemProperty("pluginDependency", COMMONS_TEXT_MANIFEST);
  @Rule
  public SystemProperty appDependency = new SystemProperty("appDependency", COMMONS_COMPRESS_MANIFEST);


  @Override
  protected String getConfigFile() {
    return "resource-access-config.xml";
  }

  @Test
  public void serviceCanAccessDependencyResources() throws Exception {
    assertThat(flowRunner("service").run().getMessage(), hasPayload(containsString("Apache Commons Lang")));
  }

  @Test
  public void serviceCannotAccessPluginDependencyResources() throws Exception {
    assertThat(flowRunner("serviceToPluginDep").run().getMessage(), hasPayload(equalTo(EMPTY_STRING)));
  }

  @Test
  public void serviceCannotAccessAppDependencyResources() throws Exception {
    assertThat(flowRunner("serviceToAppDep").run().getMessage(), hasPayload(equalTo(EMPTY_STRING)));
  }

  @Test
  public void pluginCanAccessDependencyResources() throws Exception {
    assertThat(flowRunner("plugin").run().getMessage(), hasPayload(containsString("Apache Commons Text")));
  }

  @Test
  public void pluginCannotAccessServiceDependencyResources() throws Exception {
    assertThat(flowRunner("pluginToServiceDep").run().getMessage(), hasPayload(equalTo(EMPTY_STRING)));
  }

  @Test
  public void pluginCannotAccessAppDependencyResources() throws Exception {
    assertThat(flowRunner("pluginToAppDep").run().getMessage(), hasPayload(equalTo(EMPTY_STRING)));
  }

  @Test
  public void appCanAccessDependencyResources() throws Exception {
    assertThat(flowRunner("app").run().getMessage(), hasPayload(containsString("Apache Commons Compress")));
  }

  @Test
  public void appCannotAccessPluginDependencyResources() throws Exception {
    assertThat(flowRunner("appToPluginDep").run().getMessage(), hasPayload(equalTo(EMPTY_STRING)));
  }

  @Test
  public void appCannotAccessServiceDependencyResources() throws Exception {
    assertThat(flowRunner("appToServiceDep").run().getMessage(), hasPayload(equalTo(EMPTY_STRING)));
  }

}
