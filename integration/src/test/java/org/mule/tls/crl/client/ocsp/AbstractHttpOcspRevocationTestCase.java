/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tls.crl.client.ocsp;

import static java.lang.String.format;

import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;
import static org.junit.Assume.assumeFalse;

import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tls.crl.AbstractHttpTlsRevocationTestCase;

import java.io.IOException;
import java.io.InputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;

public abstract class AbstractHttpOcspRevocationTestCase extends AbstractHttpTlsRevocationTestCase {

  @Rule
  public DynamicPort dynamicOcspPort;

  @Rule
  public SystemProperty certAliasSystemProperty;

  private static final String RUN_OCSP_SERVER_COMMAND = "openssl ocsp " +
      "-index test-classes/tls/ocsp/%s " +
      "-port %d " +
      "-CA test-classes/tls/ocsp/server.crt " +
      "-rkey test-classes/tls/ocsp/%s.pem " +
      "-rsigner test-classes/tls/ocsp/%s.crt";

  static final String REVOKED_OCSP_LIST = "revoked-ocsp.txt";

  static final String VALID_OCSP_LIST = "valid-ocsp.txt";

  /**
   * This certified entity was generated to test revocation with OCSP mechanism.
   */
  protected static final String ENTITY_CERTIFIED_REVOCATION_OCSP_SUB_PATH = "entityOcsp1";

  /**
   * The entity4 certificate has a hardcoded ocsp url: http://localhost:1111
   */
  static final int FIXED_OCSP_PORT = 1111;


  static final String DEFAULT_OCSP_RESPONDER = "server";

  private Process process;

  private final String ocspList;

  private int ocspPort;

  private String ocspResponder;


  AbstractHttpOcspRevocationTestCase(String configFile, String ocspList, String certAlias) {
    super(configFile, ENTITY_CERTIFIED_REVOCATION_OCSP_SUB_PATH);
    this.ocspList = ocspList;
    initialiseOcspStatus(certAlias);
  }

  private void initialiseOcspStatus(String certAlias) {
    if (certAlias != null) {
      certAliasSystemProperty = new SystemProperty("certAlias", certAlias);
      dynamicOcspPort = new DynamicPort("dynamicOcspPort");
      ocspPort = dynamicOcspPort.getNumber();
      ocspResponder = certAliasSystemProperty.getValue();
    } else {
      ocspPort = FIXED_OCSP_PORT;
      ocspResponder = DEFAULT_OCSP_RESPONDER;
    }
  }


  @Before
  public void setUp() throws Exception {
    assumeFalse(IS_OS_WINDOWS);

    String openSslCmd = format(RUN_OCSP_SERVER_COMMAND, ocspList, ocspPort, ocspResponder, ocspResponder);
    LOGGER.info("Openssl Command: {}", openSslCmd);

    process = Runtime.getRuntime().exec(openSslCmd);
    String cmdOutput = getOcspServerCommandOutput(process.getErrorStream());
    LOGGER.info("Command output: {}", cmdOutput);

    assumeFalse("Since openssl ocsp command has a flaky behavior the test will be ignored if an error occurs in server initialisation.",
                cmdOutput.contains("Error"));
  }

  @After
  public void tearDown() {
    if (process != null) {
      process.destroy();
    }
  }

  /**
   * @param commandOutput the error stream of the ocsp server process.
   * @return the first line of the ocsp server command output.
   * @throws IOException
   */
  private String getOcspServerCommandOutput(InputStream commandOutput) throws IOException {
    char aux;
    StringBuilder stringBuilder = new StringBuilder();
    while ((aux = (char) commandOutput.read()) != '\n') {
      stringBuilder.append(aux);
    }
    return stringBuilder.toString();
  }

}
