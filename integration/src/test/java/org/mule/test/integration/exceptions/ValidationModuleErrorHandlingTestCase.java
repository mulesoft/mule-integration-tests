package org.mule.test.integration.exceptions;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;
import org.mule.runtime.api.message.Message;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.tests.api.TestQueueManager;

import javax.inject.Inject;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ErrorHandlingStory.ERROR_HANDLER;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ErrorHandlingStory.ERROR_MAPPINGS;

@Feature(ERROR_HANDLING)
@Story(ERROR_HANDLER)
@Story(ERROR_MAPPINGS)
public class ValidationModuleErrorHandlingTestCase extends AbstractIntegrationTestCase {

    @Inject
    private TestQueueManager queueManager;

    @Override
    protected String getConfigFile() {
        return "org/mule/test/integration/exceptions/validation-module-error-handling.xml";
    }

    @Test
    public void validationAllWithErrorMapping() throws Exception {
        flowRunner("validationAllWithErrorMapping").run();
        Message response = queueManager.read("dlq", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();
        assertThat(response, notNullValue());
    }

}
