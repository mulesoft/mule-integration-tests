/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.el;

import static org.junit.Assert.assertSame;
import org.mule.functional.api.component.EventCallback;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.MuleExpressionLanguage;
import org.mule.runtime.api.el.ValidationResult;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExtendedExpressionLanguageAdaptor;
import org.mule.runtime.core.api.event.BaseEvent;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.Iterator;

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
    public void eventReceived(BaseEvent event, Object component, MuleContext muleContext) throws Exception {
      new TestExpressionLanguage();
    }
  }

  public static class TestExpressionLanguage implements ExtendedExpressionLanguageAdaptor {

    @Override
    public void addGlobalBindings(BindingContext bindingContext) {

    }

    @Override
    public TypedValue evaluate(String expression, BaseEvent event, ComponentLocation componentLocation,
                               BindingContext bindingContext)
        throws ExpressionRuntimeException {
      return null;
    }

    @Override
    public TypedValue evaluate(String expression, BaseEvent event, BindingContext context)
        throws ExpressionRuntimeException {
      return null;
    }

    @Override
    public TypedValue evaluate(String expression, DataType expectedOutputType, BaseEvent event, BindingContext context)
        throws ExpressionRuntimeException {
      return null;
    }

    @Override
    public TypedValue evaluate(String expression, DataType expectedOutputType, BaseEvent event,
                               ComponentLocation componentLocation, BindingContext context, boolean failOnNull)
        throws ExpressionRuntimeException {
      return null;
    }

    @Override
    public ValidationResult validate(String expression) {
      return null;
    }

    @Override
    public Iterator<TypedValue<?>> split(String expression, BaseEvent event, ComponentLocation componentLocation,
                                         BindingContext bindingContext)
        throws ExpressionRuntimeException {
      return null;
    }

    @Override
    public Iterator<TypedValue<?>> split(String expression, BaseEvent event, BindingContext bindingContext)
        throws ExpressionRuntimeException {
      return null;
    }

    @Override
    public TypedValue evaluate(String expression, BaseEvent event, BaseEvent.Builder eventBuilder,
                               ComponentLocation componentLocation, BindingContext bindingContext)
        throws ExpressionRuntimeException {
      return null;
    }

    @Override
    public void enrich(String expression, BaseEvent event, BaseEvent.Builder eventBuilder,
                       ComponentLocation componentLocation, Object object) {

    }

    @Override
    public void enrich(String expression, BaseEvent event, BaseEvent.Builder eventBuilder,
                       ComponentLocation componentLocation, TypedValue value) {

    }
  }
}
