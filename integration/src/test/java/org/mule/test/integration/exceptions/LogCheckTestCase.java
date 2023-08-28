/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.mule.tck.junit4.rule.VerboseExceptions.setVerboseExceptions;
import static org.mule.test.allure.AllureConstants.Logging.LOGGING;
import static org.mule.test.allure.AllureConstants.Logging.LoggingStory.ERROR_REPORTING;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.tck.junit4.rule.VerboseExceptions;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(LOGGING)
@Story(ERROR_REPORTING)
public class LogCheckTestCase extends AbstractIntegrationTestCase {

  // Just to ensure the previous value is set after the test
  @ClassRule
  public static VerboseExceptions verboseExceptions = new VerboseExceptions(false);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/log-check-config.xml";
  }

  @Test
  public void runVerboseCheckEqualsVerbose() throws Exception {
    runSuccesses(true, "checkEqualsVerbose");
  }

  @Test
  public void runVerboseCheckStacktrace() throws Exception {
    runSuccesses(true, "checkStacktrace");
  }

  @Test
  public void runVerboseCheckSummary() throws Exception {
    runSuccesses(true, "checkSummary");
  }

  @Test
  public void runVerboseAllChecksTogetherNoneFailing() throws Exception {
    runSuccesses(true, "allChecksTogetherNoneFailing");
  }

  @Test
  public void checkNonVerboseEquals() throws Exception {
    runSuccesses(false, "checkSummary");
  }

  @Test
  public void checkNonVerboseSummary() throws Exception {
    runSuccesses(false, "checkSummary");
  }

  @Test
  public void unknownSummaryShowsFilteredStack() throws Exception {
    runSuccesses(false, "unknownFiltered");
  }

  @Test
  public void unknownVerboseShowsFullStack() throws Exception {
    runSuccesses(true, "unknownFull");
  }

  @Test
  public void exceptionInTryIsLogged() throws Exception {
    runSuccesses(false, "exceptionInTry");
  }

  @Test
  public void sameExceptionIsNotLoggedMoreThanOnceInTryScope() throws Exception {
    runSuccesses(false, "sameExceptionInTry");
  }

  @Test
  public void differentExceptionsAreLoggedInTryScope() throws Exception {
    runSuccesses(false, "differentExceptionsInTry");
  }

  @Test
  public void noLoggingFailsIfFlagIsNotSet() throws Exception {
    expectedException.expect(AssertionError.class);
    expectedException.expectMessage("Could not check exception because it was never logged");
    runSuccesses(false, "noLogFlowFlagNotSet");
  }

  @Test
  public void noLoggingSucceedsIfFlagIsSet() throws Exception {
    runSuccesses(false, "noLogFlowFlagSet");
  }

  @Test
  public void assertFailsIfNoException() throws Exception {
    expectedException.expect(AssertionError.class);
    expectedException.expectMessage("Handler could not check any exception log because no exception was raise");
    runSuccesses(false, "noExceptionFlow");
  }

  @Test
  @Issue("MULE-18041")
  public void suppressedMuleExceptionGetsLoggedAsSuppressedCause() throws Exception {
    runSuccesses(false, "suppressedMuleException");
  }

  @Test
  @Issue("MULE-18041")
  public void suppressedMuleExceptionsGetsLoggedAsSuppressedCauses() throws Exception {
    runSuccesses(true, "suppressedMuleExceptions");
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
