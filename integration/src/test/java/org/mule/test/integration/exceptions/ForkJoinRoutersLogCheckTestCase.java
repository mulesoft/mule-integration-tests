package org.mule.test.integration.exceptions;

import static org.mule.runtime.api.util.MuleSystemProperties.MULE_PRINT_LEGACY_COMPOSITE_EXCEPTION_LOG;
import static org.mule.test.allure.AllureConstants.Logging.LOGGING;
import static org.mule.test.allure.AllureConstants.Logging.LoggingStory.ERROR_REPORTING;

import static java.lang.System.setProperty;
import static java.util.Arrays.asList;

import static org.junit.runners.Parameterized.Parameters;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.List;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
@Feature(LOGGING)
@Story(ERROR_REPORTING)
public class ForkJoinRoutersLogCheckTestCase extends AbstractIntegrationTestCase {

    @Rule
    public SystemProperty legacyCompositeRoutingExceptionLog;

    @Override
    protected String getConfigFile() {
        return "org/mule/test/integration/exceptions/fork-join-routers-log-config.xml";
    }

    @Parameters(name = "Legacy log: {0}")
    public static List<Object[]> parameters() {
        return asList(
                new Object[] {true},
                new Object[] {false});
    }

    public ForkJoinRoutersLogCheckTestCase(boolean legacyCompositeRoutingExceptionLog) {
        this.legacyCompositeRoutingExceptionLog = new SystemProperty(MULE_PRINT_LEGACY_COMPOSITE_EXCEPTION_LOG, Boolean.toString(legacyCompositeRoutingExceptionLog));
    }

    @Test
    @Issue("W-10965130")
    public void compositeRoutingExceptionForParallelForEach() throws Exception {
//        boolean originalProperty = Boolean.parseBoolean(System.getProperty(MULE_PRINT_LEGACY_COMPOSITE_EXCEPTION_LOG));
//        setProperty(MULE_PRINT_LEGACY_COMPOSITE_EXCEPTION_LOG, Boolean.toString(false));
        runSuccesses("parallelForEachFlow");
//        setProperty(MULE_PRINT_LEGACY_COMPOSITE_EXCEPTION_LOG, Boolean.toString(originalProperty));
    }

    @Test
    @Issue("W-10965130")
    public void compositeRoutingExceptionForScatterGather() throws Exception {
//        boolean originalProperty = Boolean.parseBoolean(System.getProperty(MULE_PRINT_LEGACY_COMPOSITE_EXCEPTION_LOG));
//        setProperty(MULE_PRINT_LEGACY_COMPOSITE_EXCEPTION_LOG, Boolean.toString(false));
        runSuccesses("scatterGatherFlow");
//        setProperty(MULE_PRINT_LEGACY_COMPOSITE_EXCEPTION_LOG, Boolean.toString(originalProperty));

    }

    @Test
    @Issue("W-10965130")
    public void compositeRoutingExceptionForParallelForEachPreviousVersionLog() throws Exception {
//        boolean originalProperty = Boolean.parseBoolean(System.getProperty(MULE_PRINT_LEGACY_COMPOSITE_EXCEPTION_LOG));
//        setProperty(MULE_PRINT_LEGACY_COMPOSITE_EXCEPTION_LOG, Boolean.toString(true));
        runSuccesses("previousParallelForEachFlow");
//        setProperty(MULE_PRINT_LEGACY_COMPOSITE_EXCEPTION_LOG, Boolean.toString(originalProperty));
    }

    @Test
    @Issue("W-10965130")
    public void compositeRoutingExceptionForScatterGatherPreviousVersionLog() throws Exception {
//        boolean originalProperty = Boolean.parseBoolean(System.getProperty(MULE_PRINT_LEGACY_COMPOSITE_EXCEPTION_LOG));
//        setProperty(MULE_PRINT_LEGACY_COMPOSITE_EXCEPTION_LOG, Boolean.toString(true));
        runSuccesses("previousScatterGatherFlow");
//        setProperty(MULE_PRINT_LEGACY_COMPOSITE_EXCEPTION_LOG, Boolean.toString(originalProperty));
    }


    private void runSuccesses(String flowName) throws Exception {
        flowRunner(flowName).run();
    }

//    public static class CustomException extends MuleException {
//
//        private static final long serialVersionUID = -5911115770998812278L;
//        private static final String MESSAGE = "Error";
//
//        public CustomException() {
//            super(I18nMessageFactory.createStaticMessage(MESSAGE));
//        }
//
//        @Override
//        public String getDetailedMessage() {
//            return MESSAGE;
//        }
//
//        @Override
//        public String getVerboseMessage() {
//            return MESSAGE;
//        }
//
//        @Override
//        public String getSummaryMessage() {
//            return MESSAGE;
//        }
//    }
//
//    public static final class ThrowNpeProcessor extends AbstractComponent implements Processor {
//
//        @Override
//        public CoreEvent process(CoreEvent event) throws MuleException {
//            throw new NullPointerException("expected");
//        }
//    }
}
