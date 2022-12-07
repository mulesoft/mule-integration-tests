/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test;

import static org.junit.rules.ExpectedException.none;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;
import static org.mule.test.allure.AllureConstants.MuleDsl.MULE_DSL;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.core.api.config.ConfigurationException;

@Feature(MULE_DSL)
@Story(DSL_VALIDATION_STORY)
public class OperationsFailuresTestCase extends MuleArtifactFunctionalTestCase {

    @Rule
    public ExpectedException expectedException = none();

    @Override
    protected String getConfigFile() {
        //We put the expectedException here because the error raises when trying to load the configuration
        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("Using an invalid function within an operation");
        return "mule-operations-using-lookup.xml";
    }

    @Test
    @Description("An operation cannot use lookup function (even without explicit binding)")
    public void returningTypeFromDependency() throws Exception {
        // We are actually expecting for the expectedException
        flowRunner("test").run();
    }

}
