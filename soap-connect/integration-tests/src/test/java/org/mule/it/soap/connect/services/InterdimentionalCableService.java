/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.it.soap.connect.services;

import static java.util.Arrays.asList;

import java.util.List;

import javax.jws.WebMethod;
import javax.jws.WebResult;
import javax.jws.WebService;

@WebService(portName = "CablePort", serviceName = "CableService")
public class InterdimentionalCableService {

  @WebResult(name = "channel")
  @WebMethod(action = "getChannels")
  public List<String> getChannels() {
    return asList("Two Brothers", "Fake Doors", "The Adventures of Stealy");
  }
}
