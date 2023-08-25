/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.it.soap.connect.services;

import static java.util.Arrays.asList;
import static javax.jws.WebParam.Mode.OUT;
import static org.mule.test.soap.extension.CalcioServiceProvider.CALCIO_FRIENDLY_NAME;
import static org.mule.test.soap.extension.FootballSoapExtension.LEAGUES_PORT;
import static org.mule.test.soap.extension.FootballSoapExtension.LEAGUES_SERVICE;
import static org.mule.test.soap.extension.LaLigaServiceProvider.LA_LIGA;

import org.mule.service.soap.service.EchoException;

import java.util.List;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.ws.Holder;

@WebService(portName = LEAGUES_PORT, serviceName = LEAGUES_SERVICE)
public class FootballService {

  private LaLigaService delegate = new LaLigaService();

  public static final String AUTH = "Authorized";
  public static final String CORRUPTO = "Corrupto";
  public final static String JULITO_MIN_DESC = "Julio Humberto Grondona";
  public final static String JULITO_FULL_DESC = JULITO_MIN_DESC + "(September 18, 1931 - July 30, 2014), Corrupt Argentinian";

  @WebResult(name = "text")
  @WebMethod(action = "getPresidentInfo")
  public String getPresidentInfo(@WebParam(name = "fullInfo") boolean fullInfo,
                                 @WebParam(name = "identity", header = true, mode = OUT) Holder<String> headerOut) {
    if (fullInfo) {
      return JULITO_FULL_DESC;
    }

    headerOut.value = CORRUPTO;
    return JULITO_MIN_DESC;
  }

  @WebResult(name = "team")
  @WebMethod(action = "getLeagueTeams")
  public List<String> getLeagueTeams(@WebParam(name = "auth", header = true) String headerIn,
                                     @WebParam(name = "name") String leagueName)
      throws EchoException {

    if (!headerIn.equals(AUTH)) {
      throw new EchoException("Missing Required Authorization Headers");
    }

    if (!leagueName.equals(LA_LIGA)) {
      throw new EchoException("No teams for league: " + leagueName);
    }

    return delegate.getTeams();
  }

  @WebResult(name = "league")
  @WebMethod(action = "getBestLeague")
  public List<String> getBestLeague(@WebParam(name = "name") String name) {
    return asList(LA_LIGA);
  }

  @WebResult(name = "league")
  @WebMethod(action = "getLeagues")
  public List<String> getLeagues() {
    return asList(CALCIO_FRIENDLY_NAME, LA_LIGA);
  }
}
