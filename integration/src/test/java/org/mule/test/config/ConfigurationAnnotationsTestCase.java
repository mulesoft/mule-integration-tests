/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config;


import static java.lang.System.lineSeparator;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.meta.AnnotatedObject;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.LoggerMessageProcessor;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.test.AbstractIntegrationTestCase;

import javax.xml.namespace.QName;

import org.junit.Test;

/**
 * Test that configuration-based annotations are propagated to the appropriate runtime objects
 */
public class ConfigurationAnnotationsTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"org/mule/config/spring/annotations.xml", "org/mule/config/spring/annotations-config.xml"};
  }

  @Test
  public void testTransformerAnnotations() {
    Transformer stb = muleContext.getRegistry().lookupTransformer("StringtoByteArray");
    assertThat(stb, not(nullValue()));
    assertThat(getDocName(stb), is("stb-transformer"));
    assertThat(getDocDescription(stb), is("Convert a String to a Byte Array"));
    assertThat(getSourceFileLine(stb), is(8));
    assertThat(getSourceElement(stb),
               is("<string-to-byte-array-transformer name=\"StringtoByteArray\" doc:name=\"stb-transformer\">"
                   + lineSeparator() + "<annotations>" + lineSeparator()
                   + "<doc:description>Convert a String to a Byte Array</doc:description>" + lineSeparator()
                   + "</annotations>" + lineSeparator() + "</string-to-byte-array-transformer>"));
  }

  @Test
  public void testFlowAnnotations() {
    FlowConstruct flow = muleContext.getRegistry().lookupFlowConstruct("Bridge");
    assertThat(flow, not(nullValue()));
    assertThat(getDocName(flow), is("Bridge flow"));
    assertThat(getDocDescription(flow), is("Main flow"));
    assertThat(getSourceFileLine(flow), is(7));
    assertThat(getSourceElement(flow),
               is("<flow name=\"Bridge\" doc:name=\"Bridge flow\">" + lineSeparator() + "<annotations>"
                   + lineSeparator() + "<doc:description>Main flow</doc:description>" + lineSeparator()
                   + "</annotations>" + lineSeparator() + "<logger doc:name=\"echo\">" + "</logger>"
                   + lineSeparator() + "</flow>"));
  }

  @Test
  public void testFlowWithExceptionStrategyAnnotations() {
    FlowConstruct flow = muleContext.getRegistry().lookupFlowConstruct("WithRefExceptionStrategy");
    assertThat(flow, not(nullValue()));
    assertThat(getDocName(flow), is("With Referenced Exception Strategy"));
    assertThat(getDocDescription(flow), is(nullValue()));
    assertThat(getSourceFileLine(flow), is(18));
    assertThat(getSourceElement(flow),
               is("<flow name=\"WithRefExceptionStrategy\" doc:name=\"With Referenced Exception Strategy\">"
                   + lineSeparator() + "<logger doc:name=\"echo_ex\">" + "</logger>"
                   + lineSeparator()
                   + "<error-handler doc:name=\"error handler doc name\">" + lineSeparator()
                   + "<on-error-continue doc:name=\"On Error Continue\">" + lineSeparator()
                   + "<logger message=\"Exception! \" level=\"ERROR\" doc:name=\"Logger\"></logger>" + lineSeparator()
                   + "</on-error-continue>" + lineSeparator()
                   + "</error-handler>" + lineSeparator() + "</flow>"));
  }

  @Test
  public void testDefaultAnnotationsInNotAnnotatedObject() {
    FlowConstruct flow = muleContext.getRegistry().lookupFlowConstruct("NotAnnotatedBridge");
    assertThat(flow, not(nullValue()));
    assertThat(getDocName(flow), is(nullValue()));
    assertThat(getDocDescription(flow), is(nullValue()));
    assertThat(getSourceFileLine(flow), is(14));
    assertThat(getSourceElement(flow), is("<flow name=\"NotAnnotatedBridge\">" + lineSeparator()
        + "<logger></logger>" + lineSeparator() + "</flow>"));
  }

  @Test
  public void testJavaComponentAnnotations() {
    Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("Bridge");
    LoggerMessageProcessor logger = (LoggerMessageProcessor) flow.getProcessors().get(0);
    assertThat(getSourceFileLine(logger), is(11));
    assertThat(getSourceElement(logger), is("<logger doc:name=\"echo\">" + "</logger>"));
  }

  @Test
  public void testInsideSpringBeansAnnotations() {
    Transformer stb = muleContext.getRegistry().lookupTransformer("ManziTransformer");
    assertThat(stb, not(nullValue()));
    assertThat(getDocName(stb), is("manzi-transformer"));
    assertThat(getSourceFileLine(stb), is(14));
    assertThat(getSourceElement(stb),
               is("<append-string-transformer message=\"Manzi\" name=\"ManziTransformer\" doc:name=\"manzi-transformer\"></append-string-transformer>"));
  }

  protected String getDocName(Object obj) {
    return (String) ((AnnotatedObject) obj).getAnnotation(new QName("http://www.mulesoft.org/schema/mule/documentation", "name"));
  }

  protected String getDocDescription(Object obj) {
    return (String) ((AnnotatedObject) obj)
        .getAnnotation(new QName("http://www.mulesoft.org/schema/mule/documentation", "description"));
  }

  protected String getSourceFile(Object obj) {
    return (String) ((AnnotatedObject) obj)
        .getAnnotation(new QName("http://www.mulesoft.org/schema/mule/documentation", "sourceFileName"));
  }

  protected Integer getSourceFileLine(Object obj) {
    return (Integer) ((AnnotatedObject) obj)
        .getAnnotation(new QName("http://www.mulesoft.org/schema/mule/documentation", "sourceFileLine"));
  }

  protected String getSourceElement(Object obj) {
    return (String) ((AnnotatedObject) obj)
        .getAnnotation(new QName("http://www.mulesoft.org/schema/mule/documentation", "sourceElement"));
  }
}
