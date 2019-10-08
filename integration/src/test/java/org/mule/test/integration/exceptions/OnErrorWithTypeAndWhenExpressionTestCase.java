/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Arrays;
import java.util.Collection;

@Feature(ERROR_HANDLING)
@Story("When expression for error handler")
@RunnerDelegateTo(Parameterized.class)
public class OnErrorWithTypeAndWhenExpressionTestCase extends AbstractIntegrationTestCase {

    private String flowName;

    @Override
    protected String getConfigFile() {
        return "org/mule/test/integration/exceptions/on-error-when-expression.xml";
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {"withType"},
                {"withIncorrectType"},
                {"withWhen"},
                {"withIncorrectWhen"},
                {"withWhenAndType"},
                {"withCorrectWhenIncorrectType"},
                {"withIncorrectWhenCorrectType"},
                {"withIncorrectWhenIncorrectType"}
        });
    }

    public OnErrorWithTypeAndWhenExpressionTestCase(String flowName) {
        this.flowName = flowName;
    }

    @Test
    public void runTest() throws Exception {
        CoreEvent event = flowRunner(flowName).run();
        assertThat(event.getMessage().getPayload().getValue(), is("Correct"));
    }


}
