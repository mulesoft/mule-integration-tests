/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.json;

import static org.junit.Assert.assertEquals;

import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.util.IOUtils;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import java.io.ByteArrayInputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.qameta.allure.Description;
import org.junit.Rule;
import org.junit.Test;

public class JsonSchemaValidatorTestCase extends AbstractIntegrationTestCase {

  public static final String FLOW_CONFIG_FILE = "org/mule/test/integration/json/json-schema-validator-flow.xml";
  public static final String RESOURCE_FILE = "org/mule/test/integration/json/json-schema-validator";
  public static final String JSON_MESSAGE = "{\"number\": 1234}";
  public static final String EXPECTED_PAYLOAD = "SUCCESS";

  @Rule
  public DynamicPort port = new DynamicPort("port");

  @Override
  protected String getConfigFile() {
    return FLOW_CONFIG_FILE;
  }

  @Test
  @Description("W-11577522: Validates one use case where the version of com.github.java-json-tools:json-schema-validator " +
      "and com.fasterxml.jackson.core:jackson-databind must be compatible.")
  public void invokeValidateJsonSchemaToProveJsonSchemaValidatorAndJacksonDatabindAreCompatible() throws Exception {
    Message response = flowRunner("jsonSchemaValidatorFlow").withPayload(JSON_MESSAGE).run().getMessage();
    assertEquals(response.getPayload().getValue(), EXPECTED_PAYLOAD);
  }

  public static class JsonSchemaValidator {

    public static String validateJsonSchema() throws Exception {
      String json = JSON_MESSAGE;
      JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
      String schemaFilePath = IOUtils.getResourceAsUrl(RESOURCE_FILE, JsonSchemaValidatorTestCase.class).toURI().toString();
      JsonSchema jsonSchema = factory.getJsonSchema(schemaFilePath);
      ByteArrayInputStream inputStream = new ByteArrayInputStream(json.getBytes());
      JsonNode data = new ObjectMapper().readTree(inputStream);
      jsonSchema.validate(data, true);
      return EXPECTED_PAYLOAD;
    }
  }
}
