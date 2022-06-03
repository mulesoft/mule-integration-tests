package org.mule.test.integration.exceptions;

import static org.mule.runtime.api.util.MuleSystemProperties.MULE_PRINT_LEGACY_COMPOSITE_EXCEPTION_LOG;
import static org.mule.tck.junit4.rule.VerboseExceptions.setVerboseExceptions;
import static org.mule.test.allure.AllureConstants.Logging.LOGGING;
import static org.mule.test.allure.AllureConstants.Logging.LoggingStory.ERROR_REPORTING;

import static java.lang.System.setProperty;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.tck.junit4.rule.VerboseExceptions;
import org.mule.test.AbstractIntegrationTestCase;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Feature(LOGGING)
@Story(ERROR_REPORTING)
public class ForkJoinRoutersLogCheckTestCase extends AbstractIntegrationTestCase {

    // Just to ensure the previous value is set after the test
    @ClassRule
    public static VerboseExceptions verboseExceptions = new VerboseExceptions(false);

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Override
    protected String getConfigFile() {
        return "org/mule/test/integration/exceptions/fork-join-routers-log-config.xml";
    }

    @Test
    @Issue("W-10965130")
    public void compositeRoutingExceptionForParallelForEach() throws Exception {
        boolean originalProperty = Boolean.parseBoolean(System.getProperty(MULE_PRINT_LEGACY_COMPOSITE_EXCEPTION_LOG));
        setProperty(MULE_PRINT_LEGACY_COMPOSITE_EXCEPTION_LOG, Boolean.toString(false));
        runSuccesses(false, "parallelForEachFlow");
        setProperty(MULE_PRINT_LEGACY_COMPOSITE_EXCEPTION_LOG, Boolean.toString(originalProperty));
    }

    @Test
    @Issue("W-10965130")
    public void compositeRoutingExceptionForScatterGather() throws Exception {
        boolean originalProperty = Boolean.parseBoolean(System.getProperty(MULE_PRINT_LEGACY_COMPOSITE_EXCEPTION_LOG));
        setProperty(MULE_PRINT_LEGACY_COMPOSITE_EXCEPTION_LOG, Boolean.toString(false));
        runSuccesses(false, "scatterGatherFlow");
        setProperty(MULE_PRINT_LEGACY_COMPOSITE_EXCEPTION_LOG, Boolean.toString(originalProperty));

    }

    @Test
    @Issue("W-10965130")
    public void compositeRoutingExceptionForParallelForEachPreviousVersionLog() throws Exception {
        boolean originalProperty = Boolean.parseBoolean(System.getProperty(MULE_PRINT_LEGACY_COMPOSITE_EXCEPTION_LOG));
        setProperty(MULE_PRINT_LEGACY_COMPOSITE_EXCEPTION_LOG, Boolean.toString(true));
        runSuccesses(false, "previousParallelForEachFlow");
        setProperty(MULE_PRINT_LEGACY_COMPOSITE_EXCEPTION_LOG, Boolean.toString(originalProperty));
    }

    @Test
    @Issue("W-10965130")
    public void compositeRoutingExceptionForScatterGatherPreviousVersionLog() throws Exception {
        boolean originalProperty = Boolean.parseBoolean(System.getProperty(MULE_PRINT_LEGACY_COMPOSITE_EXCEPTION_LOG));
        setProperty(MULE_PRINT_LEGACY_COMPOSITE_EXCEPTION_LOG, Boolean.toString(true));
        runSuccesses(false, "previousScatterGatherFlow");
        setProperty(MULE_PRINT_LEGACY_COMPOSITE_EXCEPTION_LOG, Boolean.toString(originalProperty));
    }


    private void runSuccesses(boolean verboseExceptions, String flowName) throws Exception {
        setVerboseExceptions(verboseExceptions);
        flowRunner(flowName).run();
    }

    public static class CustomException extends MuleException {

        private static final long serialVersionUID = -5911115770998812278L;
        private static final String MESSAGE = "Error";

        public CustomException() {
            super(I18nMessageFactory.createStaticMessage(MESSAGE));
        }

        @Override
        public String getDetailedMessage() {
            return MESSAGE;
        }

        @Override
        public String getVerboseMessage() {
            return MESSAGE;
        }

        @Override
        public String getSummaryMessage() {
            return MESSAGE;
        }
    }

    public static final class ThrowNpeProcessor extends AbstractComponent implements Processor {

        @Override
        public CoreEvent process(CoreEvent event) throws MuleException {
            throw new NullPointerException("expected");
        }
    }
}
