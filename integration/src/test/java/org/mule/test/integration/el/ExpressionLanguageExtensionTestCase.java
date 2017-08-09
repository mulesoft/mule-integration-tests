/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.el;

import static org.junit.Assert.assertSame;

import org.mule.functional.api.component.EventCallback;
import org.mule.runtime.api.el.MuleExpressionLanguage;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.el.mvel.MVELExpressionLanguage;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class ExpressionLanguageExtensionTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/el/expression-language-extension-config.xml";
  }

  @Test
  public void doesNotOverrideExpressionLanguageInExpressionManagerOnCreation() throws Exception {
    MuleExpressionLanguage originalExpressionManager = muleContext.getExpressionManager();

    flowRunner("createsExpressionLanguage").withPayload(TEST_MESSAGE).run();

    MuleExpressionLanguage newExpressionManager = muleContext.getExpressionManager();

    assertSame(originalExpressionManager, newExpressionManager);
  }

  public static class ExpressionLanguageFactory implements EventCallback {

    @Override
    public void eventReceived(InternalEvent event, Object component, MuleContext muleContext) throws Exception {
      new TestExpressionLanguage(muleContext);
    }
  }

  public static class TestExpressionLanguage extends MVELExpressionLanguage {

    public TestExpressionLanguage(MuleContext muleContext) {
      super(muleContext);
    }
  }
}
