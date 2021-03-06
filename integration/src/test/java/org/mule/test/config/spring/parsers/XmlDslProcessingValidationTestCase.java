/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring.parsers;

import static org.junit.rules.ExpectedException.none;

import org.mule.functional.junit4.ApplicationContextBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.test.IntegrationTestCaseRunnerConfig;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

// TODO MULE-18446 Migrate this test to a unit test where the implementation of the validation will be
@Ignore("MULE-18446")
public class XmlDslProcessingValidationTestCase extends AbstractMuleTestCase implements IntegrationTestCaseRunnerConfig {

  @Rule
  public ExpectedException expectedException = none();

  @Test
  @Ignore("MULE-17711")
  public void parameterAndChildAtOnce() throws Exception {
    expectedException
        .expectMessage("Component parsers-test:element-with-attribute-and-child has a child element parsers-test:my-pojo which is used for the same purpose of the configuration parameter myPojo. Only one must be used.");
    new ApplicationContextBuilder().setApplicationResources(new String[] {
        "org/mule/config/spring/parsers/dsl-validation-duplicate-pojo-or-list-parameter-config.xml"}).build();
  }

  @Test
  public void emptyChildSimpleParameter() throws Exception {
    expectedException
        .expectMessage("Parameter at org/mule/config/spring/parsers/dsl-validation-empty-simple-child-parameter.xml:10 must provide a non-empty value");
    new ApplicationContextBuilder().setApplicationResources(new String[] {
        "org/mule/config/spring/parsers/dsl-validation-empty-simple-child-parameter.xml"}).build();
  }

}
