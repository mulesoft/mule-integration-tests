/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.it.soap.connect;

import static org.mule.service.soap.SoapTestUtils.assertSimilarXml;

import org.mule.runtime.api.message.Message;

import java.io.ByteArrayInputStream;

import org.junit.Ignore;
import org.junit.Test;

public class InvokeOperationExecutionTestCase extends SoapFootballExtensionArtifactFunctionalTestCase {

  @Test
  public void simpleNoParamsOperation() throws Exception {
    Message message = flowRunner("getLeagues").withPayload(getBodyXml("getLeagues", "")).keepStreamsOpen().run().getMessage();
    String response = getBodyXml("getLeaguesResponse", "<league>Calcio</league><league>La Liga</league>");
    assertSimilarXml(response, (String) message.getPayload().getValue());
  }

  @Test
  public void operationWithHeaders() throws Exception {
    String requestBody = getBodyXml("getLeagueTeams", "<name>La Liga</name>");
    Message message = flowRunner("getLeagueTeams").withPayload(requestBody).keepStreamsOpen().run().getMessage();
    String response = getBodyXml("getLeagueTeamsResponse", "<team>Barcelona</team><team>Real Madrid</team><team>Atleti</team>");
    assertSimilarXml(response, (String) message.getPayload().getValue());
  }

  @Test
  public void operationWithImplicitBody() throws Exception {
    Message message =
        flowRunner("getBestLeague").withPayload(getBodyXml("getBestLeague", "<name>Best League</name>")).keepStreamsOpen().run()
            .getMessage();
    String response = getBodyXml("getBestLeagueResponse", "<league>La Liga</league>");
    assertSimilarXml(response, (String) message.getPayload().getValue());
  }

  @Test
  public void operationWithImplicitBodyButExplicitMessage() throws Exception {
    Message message =
        flowRunner("getBestLeagueWithExplicitMessage").withPayload(getBodyXml("getBestLeague", "<name>Best League</name>"))
            .keepStreamsOpen().run()
            .getMessage();
    String response = getBodyXml("getBestLeagueResponse", "<league>La Liga</league>");
    assertSimilarXml(response, (String) message.getPayload().getValue());
  }

  @Test
  public void operationWithImplicitBodyWithHeaders() throws Exception {
    String requestBody = getBodyXml("getLeagueTeams", "<name>La Liga</name>");
    Message message = flowRunner("getLeagueTeamsWithImplicitBody").withPayload(requestBody).keepStreamsOpen().run().getMessage();
    String response = getBodyXml("getLeagueTeamsResponse", "<team>Barcelona</team><team>Real Madrid</team><team>Atleti</team>");
    assertSimilarXml(response, (String) message.getPayload().getValue());
  }

  @Test
  @Ignore("W-14042815")
  public void uploadAttachment() throws Exception {
    Message message = flowRunner("uploadResult")
        .withPayload(getBodyXml("uploadResult", ""))
        .withVariable("att", new ByteArrayInputStream("Barcelona Won".getBytes()))
        .keepStreamsOpen()
        .run().getMessage();
    String response = getBodyXml("uploadResultResponse", "<message>Ok</message>");
    assertSimilarXml(response, (String) message.getPayload().getValue());
  }
}
