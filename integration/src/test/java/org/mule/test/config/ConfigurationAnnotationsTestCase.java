/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
import static org.mule.runtime.api.component.Component.NS_MULE_DOCUMENTATION;
import static org.mule.runtime.api.component.Component.Annotations.NAME_ANNOTATION_KEY;
import static org.mule.runtime.api.util.ComponentLocationProvider.getSourceCode;
import static org.mule.test.allure.AllureConstants.MuleDsl.MULE_DSL;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslAnnotationsStory.DSL_ANNOTATIONS_STORY;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.test.AbstractIntegrationTestCase;

import javax.xml.namespace.QName;

import org.junit.Test;
import org.springframework.context.annotation.Description;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Description("Test that configuration-based annotations are propagated to the appropriate runtime objects")
@Feature(MULE_DSL)
@Story(DSL_ANNOTATIONS_STORY)
public class ConfigurationAnnotationsTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/config/spring/annotations.xml";
  }

  @Test
  public void testFlowAnnotations() {
    FlowConstruct flow = registry.<FlowConstruct>lookupByName("Bridge").get();
    assertThat(flow, not(nullValue()));
    assertThat(getDocName(flow), is("Bridge flow"));
    assertThat(getDocDescription(flow), is("Main flow"));
    assertThat(getSourceCode(flow),
               is("<flow name=\"Bridge\" doc:name=\"Bridge flow\">" + lineSeparator() + "<annotations>"
                   + lineSeparator() + "<doc:description>"
                   + "<![CDATA[" + lineSeparator() + "Main flow" + lineSeparator() + "]]>"
                   + "</doc:description>" + lineSeparator()
                   + "</annotations>" + lineSeparator() + "<logger doc:name=\"echo\">" + "</logger>"
                   + lineSeparator() + "</flow>"));
  }

  @Test
  public void testFlowWithExceptionStrategyAnnotations() {
    FlowConstruct flow = registry.<FlowConstruct>lookupByName("WithRefExceptionStrategy").get();
    assertThat(flow, not(nullValue()));
    assertThat(getDocName(flow), is("With Referenced Exception Strategy"));
    assertThat(getDocDescription(flow), is(nullValue()));
    assertThat(getSourceCode(flow),
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
    FlowConstruct flow = registry.<FlowConstruct>lookupByName("NotAnnotatedBridge").get();
    assertThat(flow, not(nullValue()));
    assertThat(getDocName(flow), is(nullValue()));
    assertThat(getDocDescription(flow), is(nullValue()));
    assertThat(getSourceCode(flow), is("<flow name=\"NotAnnotatedBridge\">" + lineSeparator()
        + "<logger></logger>" + lineSeparator() + "</flow>"));
  }

  @Test
  public void testJavaComponentAnnotations() {
    Flow flow = (Flow) registry.<FlowConstruct>lookupByName("Bridge").get();
    Processor logger = flow.getProcessors().get(0);
    assertThat(getSourceCode((Component) logger), is("<logger doc:name=\"echo\">" + "</logger>"));
  }

  @Test
  @Issue("MULE-19631")
  public void annotationAvailableInComponent() {
    Flow flow = (Flow) registry.<FlowConstruct>lookupByName("withCustomAnnotation").get();

    assertThat(flow.getAnnotation(new QName("http://www.my-org.org/schema/custom", "anything")), is("This is something custom"));
  }

  protected String getDocName(Object obj) {
    return (String) ((Component) obj).getAnnotation(NAME_ANNOTATION_KEY);
  }

  protected String getDocDescription(Object obj) {
    return (String) ((Component) obj)
        .getAnnotation(new QName(NS_MULE_DOCUMENTATION, "description"));
  }

}
