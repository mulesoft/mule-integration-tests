/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import io.qameta.allure.Description;
import org.junit.Test;
import org.mule.runtime.core.api.event.CoreEvent;

public class ModuleUsingObjectStoreTestCase extends AbstractXmlExtensionMuleArtifactFunctionalTestCase {

  private static final String FIRST_CONFIGURATION = "firstConfiguration";
  private static final String SECOND_CONFIGURATION = "secondConfiguration";

  @Override
  protected String getModulePath() {
    return "modules/module-using-os.xml";
  }

  @Override
  protected String getConfigFile() {
    return "flows/flows-with-module-using-os.xml";
  }

  @Test
  @Description("Ensures that references to global elements are be properly initialized, such as the object store under the name"
    + "of 'os-config'.")
  public void testContains() throws Exception {
    assertContains(FIRST_CONFIGURATION, false);
    assertContains(SECOND_CONFIGURATION, false);

    assertStore(FIRST_CONFIGURATION);
    assertContains(FIRST_CONFIGURATION, true);
    assertContains(SECOND_CONFIGURATION, false);

    assertRemove(FIRST_CONFIGURATION);
    assertContains(FIRST_CONFIGURATION, false);
    assertContains(SECOND_CONFIGURATION, false);
  }

  private void assertStore(final String configuration) throws Exception {
    final CoreEvent event = flowRunner("store-" + configuration + "-flow").run();
    assertThat(event.getMessage().getPayload().getValue(), is(nullValue()));
  }

  private void assertRemove(final String configuration) throws Exception {
    final CoreEvent event = flowRunner("remove-" + configuration + "-flow").run();
    assertThat(event.getMessage().getPayload().getValue(), is(nullValue()));
  }

  private void assertContains(final String configuration, final boolean expected) throws Exception {
    final CoreEvent event = flowRunner("contains-" + configuration + "-flow").run();
    assertThat(event.getMessage().getPayload().getValue(), is(expected));
  }

}
