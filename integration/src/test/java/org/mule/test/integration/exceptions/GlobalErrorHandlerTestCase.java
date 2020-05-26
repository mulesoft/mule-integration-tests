package org.mule.test.integration.exceptions;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.test.AbstractIntegrationTestCase;

public class GlobalErrorHandlerTestCase extends AbstractIntegrationTestCase {

    @Override
    protected String getConfigFile() {
        return "org/mule/test/integration/exceptions/global-error-handler.xml";
    }

    @Test
    public void errorHandlerWithSelfReference() throws Exception {
        // This should fail, but in 4.2 it didn't so we must accept it in 4.3, since it does work
        CoreEvent event = flowRunner("flowWithErrorHandlerSelfReferencing").run();
        assertThat(event.getMessage().getPayload().getValue(), is("Chocotorta"));
    }
}
