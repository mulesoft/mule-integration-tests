/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.mule.runtime.core.api.error.Errors.Identifiers.CRITICAL_IDENTIFIER;
import static org.mule.runtime.core.api.error.Errors.Identifiers.SOURCE_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.error.Errors.Identifiers.SOURCE_ERROR_RESPONSE_GENERATE_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.error.Errors.Identifiers.SOURCE_ERROR_RESPONSE_SEND_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.error.Errors.Identifiers.SOURCE_RESPONSE_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.error.Errors.Identifiers.SOURCE_RESPONSE_GENERATE_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.error.Errors.Identifiers.SOURCE_RESPONSE_SEND_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.error.Errors.Identifiers.UNKNOWN_ERROR_IDENTIFIER;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;

import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

import org.mule.extension.http.internal.temporary.HttpConnector;
import org.mule.extension.socket.api.SocketsExtension;
import org.mule.functional.junit4.AbstractConfigurationFailuresTestCase;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(ERROR_HANDLING)
@Story(DSL_VALIDATION_STORY)
public class ErrorHandlingConfigurationFailuresTestCase extends AbstractConfigurationFailuresTestCase {

  // TODO MULE-10061 - Review once the MuleContext lifecycle is clearly defined
  @Test
  public void defaultErrorHandlerReferencesNonExistentErrorHandler() throws Exception {
    var thrown =
        assertThrows(ConfigurationException.class,
                     () -> loadConfiguration("org/mule/test/integration/exceptions/default-error-handler-reference-non-existent-es.xml"));
    assertThat(thrown.getMessage(), containsString("No global error handler defined with name 'nonExistentEh'."));
  }

  @Test
  public void xaTransactionalTryNotAllowed() throws Exception {
    var thrown = assertThrows(InitialisationException.class,
                              () -> loadConfiguration("org/mule/test/integration/transaction/xa-transactional-try-config.xml"));
    assertThat(thrown.getMessage(), containsString("Unable to create Try Scope with a Transaction Type: [XA]"));
  }

  @Test
  public void xaTransactionalTryNotAllowedWithGlobalErrorHandler() throws Exception {
    var thrown =
        assertThrows(InitialisationException.class,
                     () -> loadConfiguration("org/mule/test/integration/transaction/xa-transactional-try-config-global-err.xml"));
    assertThat(thrown.getMessage(), containsString("Unable to create Try Scope with a Transaction Type: [XA]"));
  }

  @Test
  public void unknownErrorFilteringNotAllowed() throws Exception {
    var thrown = assertThrows(ConfigurationException.class,
                              () -> loadConfiguration("org/mule/test/integration/exceptions/unknown-error-filtering-config.xml"));
    assertThat(thrown.getMessage(), containsString(notFound(UNKNOWN_ERROR_IDENTIFIER)));
  }

  @Test
  public void sourceErrorResponseFilteringNotAllowed() throws Exception {
    var thrown =
        assertThrows(ConfigurationException.class,
                     () -> loadConfiguration("org/mule/test/integration/exceptions/source-error-response-filtering-config.xml"));
    assertThat(thrown.getMessage(), containsString(notFound(SOURCE_ERROR_IDENTIFIER)));
  }

  @Test
  public void raisesErrorEmptyErrorTypeNotAllowed() throws Exception {
    var thrown = assertThrows(ConfigurationException.class,
                              () -> loadConfiguration("org/mule/test/integration/exceptions/raise-error-empty-type-config.xml"));
    assertThat(thrown.getMessage(),
               containsString("The value '' of attribute 'type' on element 'raise-error' is not valid with respect to its type, 'nonBlankString'"));
  }

  @Test
  @Issue("W-11802232")
  public void raisesErrorPropertyErrorTypeNotAllowed() throws Exception {
    var thrown =
        assertThrows(ConfigurationException.class,
                     () -> loadConfiguration("org/mule/test/integration/exceptions/raise-error-property-type-config.xml"));
    assertThat(thrown.getMessage(), containsString("Couldn't find configuration property value for key ${error.type}"));
  }

  @Test
  public void sourceErrorResponseSendFilteringNotAllowed() throws Exception {
    var thrown =
        assertThrows(ConfigurationException.class,
                     () -> loadConfiguration("org/mule/test/integration/exceptions/source-error-response-send-filtering-config.xml"));
    assertThat(thrown.getMessage(), containsString(notFound(SOURCE_ERROR_RESPONSE_SEND_ERROR_IDENTIFIER)));
  }

  @Test
  public void sourceErrorResponseGenerateFilteringNotAllowed() throws Exception {
    var thrown =
        assertThrows(ConfigurationException.class,
                     () -> loadConfiguration("org/mule/test/integration/exceptions/source-error-response-generate-filtering-config.xml"));
    assertThat(thrown.getMessage(), containsString(notFound(SOURCE_ERROR_RESPONSE_GENERATE_ERROR_IDENTIFIER)));
  }

  @Test
  public void criticalErrorFilteringNotAllowed() throws Exception {
    var thrown =
        assertThrows(ConfigurationException.class,
                     () -> loadConfiguration("org/mule/test/integration/exceptions/critical-error-filtering-config.xml"));
    assertThat(thrown.getMessage(), containsString(notFound(CRITICAL_IDENTIFIER)));
  }

  @Test
  public void nonExistingSourceMappingNotAllowed() throws Exception {
    var thrown =
        assertThrows(ConfigurationException.class,
                     () -> loadConfiguration("org/mule/test/integration/exceptions/non-existing-source-mapping-config.xml"));
    assertThat(thrown.getMessage(), containsString("Could not find error 'NON_EXISTING'"));
  }

  @Test
  public void nonExistingCoreMappingsNotAllowed() throws Exception {
    var thrown =
        assertThrows(ConfigurationException.class,
                     () -> loadConfiguration("org/mule/test/integration/exceptions/non-existent-core-mapping-config.xml"));
    assertThat(thrown.getMessage(), containsString("There's no MULE error named 'NON_EXISTING'"));
  }

  @Test
  @Issue("W-11802232")
  public void propertyErrorMappingsSourceNotAllowed() throws Exception {
    var thrown =
        assertThrows(ConfigurationException.class,
                     () -> loadConfiguration("org/mule/test/integration/exceptions/property-error-mapping-source-config.xml"));
    assertThat(thrown.getMessage(), containsString("Couldn't find configuration property value for key ${error.type}"));
  }

  @Test
  @Issue("W-11802232")
  public void propertyErrorMappingsTargetNotAllowed() throws Exception {
    var thrown =
        assertThrows(ConfigurationException.class,
                     () -> loadConfiguration("org/mule/test/integration/exceptions/property-error-mapping-target-config.xml"));
    assertThat(thrown.getMessage(), containsString("Couldn't find configuration property value for key ${error.type}"));
  }

  @Test
  public void usedNamespaceMappingsNotAllowed() throws Exception {
    var thrown = assertThrows(ConfigurationException.class,
                              () -> loadConfiguration("org/mule/test/integration/exceptions/used-namespace-mappings-config.xml"));
    assertThat(thrown.getMessage(), containsString("Cannot use error type 'HTTP:NOT_FOUND': namespace already exists"));
  }

  @Test
  public void usedNamespaceNonExistentTypeMappingsNotAllowed() throws Exception {
    var thrown =
        assertThrows(ConfigurationException.class,
                     () -> loadConfiguration("org/mule/test/integration/exceptions/used-namespace-nonexistent-type-mappings-config.xml"));
    assertThat(thrown.getMessage(), containsString("Cannot use error type 'HTTP:NONEXISTENT': namespace already exists"));
  }

  @Test
  public void nonExistingCoreErrorCannotBeRaised() throws Exception {
    var thrown =
        assertThrows(ConfigurationException.class,
                     () -> loadConfiguration("org/mule/test/integration/exceptions/non-existent-core-raise-error-config.xml"));
    assertThat(thrown.getMessage(), containsString("There's no MULE error named 'NONEXISTENT'"));
  }

  @Test
  public void usedNamespaceErrorCannotBeRaised() throws Exception {
    var thrown =
        assertThrows(ConfigurationException.class,
                     () -> loadConfiguration("org/mule/test/integration/exceptions/used-namespace-raise-error-config.xml"));
    assertThat(thrown.getMessage(), containsString("Cannot use error type 'HTTP:TIMEOUT': namespace already exists"));
  }

  @Test
  public void usedNamespaceNonExistentTypeErrorCannotBeRaised() throws Exception {
    var thrown =
        assertThrows(ConfigurationException.class,
                     () -> loadConfiguration("org/mule/test/integration/exceptions/used-namespace-nonexistent-type-raise-error-config.xml"));
    assertThat(thrown.getMessage(), containsString("Cannot use error type 'HTTP:NOT_FOUND': namespace already exists."));
  }

  @Test
  public void sourceResponseGenerateOnErrorContinue() throws Exception {
    var thrown =
        assertThrows(InitialisationException.class,
                     () -> loadConfiguration("org/mule/test/integration/exceptions/on-error-continue-response-generate.xml"));
    assertThat(thrown.getMessage(), equalTo(notAllowed(SOURCE_RESPONSE_GENERATE_ERROR_IDENTIFIER)));
  }

  @Test
  public void sourceResponseSendOnErrorContinue() throws Exception {
    var thrown =
        assertThrows(InitialisationException.class,
                     () -> loadConfiguration("org/mule/test/integration/exceptions/on-error-continue-response-send.xml"));
    assertThat(thrown.getMessage(), equalTo(notAllowed(SOURCE_RESPONSE_SEND_ERROR_IDENTIFIER)));
  }

  @Test
  public void sourceResponseErrorOnErrorContinue() throws Exception {
    var thrown =
        assertThrows(InitialisationException.class,
                     () -> loadConfiguration("org/mule/test/integration/exceptions/on-error-continue-response-error.xml"));
    assertThat(thrown.getMessage(), equalTo(notAllowed(SOURCE_RESPONSE_ERROR_IDENTIFIER)));
  }

  @Test
  public void sourceErrorInListOnErrorContinue() throws Exception {
    var thrown =
        assertThrows(InitialisationException.class,
                     () -> loadConfiguration("org/mule/test/integration/exceptions/on-error-continue-source-error-list.xml"));
    assertThat(thrown.getMessage(), equalTo(notAllowed(SOURCE_RESPONSE_ERROR_IDENTIFIER)));
  }

  private String notFound(String type) {
    return format("Could not find error '%s'", type);
  }

  private String notAllowed(String type) {
    return format("Source errors are not allowed in 'on-error-continue' handlers. Offending type is '%s'.", type);
  }

  @Override
  protected List<ExtensionModel> getRequiredExtensions() {
    ExtensionModel sockets = loadExtension(SocketsExtension.class, emptySet());
    ExtensionModel http = loadExtension(HttpConnector.class, singleton(sockets));

    final List<ExtensionModel> extensions = new ArrayList<>();
    extensions.addAll(super.getRequiredExtensions());
    extensions.add(http);
    extensions.add(sockets);

    return extensions;
  }

  @Override
  protected void applyConfiguration(DefaultMuleConfiguration muleConfiguration) {
    super.applyConfiguration(muleConfiguration);

    muleConfiguration.setMinMuleVersion(new MuleVersion("4.5.0"));
  }

}
