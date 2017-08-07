/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.it.soap.connect;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.mule.metadata.api.utils.MetadataTypeUtils.getTypeId;
import static org.mule.runtime.module.extension.soap.internal.loader.SoapInvokeOperationDeclarer.ATTACHMENTS_PARAM;
import static org.mule.runtime.module.extension.soap.internal.loader.SoapInvokeOperationDeclarer.BODY_PARAM;
import static org.mule.runtime.module.extension.soap.internal.loader.SoapInvokeOperationDeclarer.HEADERS_PARAM;

import org.mule.metadata.api.model.BinaryType;
import org.mule.metadata.api.model.BooleanType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.junit.Test;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class InvokeMetadataTestCase extends SoapFootballExtensionArtifactFunctionalTestCase {

  private static final String INVALID_KEY_ERROR = "The binding operation name [invalidKey] was not found in the current wsdl";
  private MetadataService metadataService;

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    metadataService = muleContext.getRegistry().lookupObject(MetadataService.class);
  }

  @Test
  public void metadataKeys() {
    Location location = Location.builder().globalName("getLeagues").addProcessorsPart().addIndexPart(1).build();
    final MetadataResult<MetadataKeysContainer> result = metadataService.getMetadataKeys(location);
    assertThat(result.isSuccess(), is(true));
    Set<MetadataKey> keys = result.get().getKeysByCategory().values().iterator().next();
    assertThat(keys, hasSize(2));
    MetadataKey leaguesService = keys.iterator().next();
    assertThat(leaguesService.getId(), is("leagues"));
    assertThat(leaguesService.getChilds(), hasSize(3));
    List<String> operationKeysNames = leaguesService.getChilds().stream().map(MetadataKey::getId).collect(toList());
    assertThat(operationKeysNames, containsInAnyOrder("getLeagues", "getLeagueTeams", "getPresidentInfo"));
  }

  @Test
  public void outputMetadata() {
    OperationModel model = getMetadata("getTeams");
    ObjectType output = toObjectType(model.getOutput().getType());
    ObjectType body = toObjectType(output.getFields().iterator().next().getValue());
    assertThat(getTypeId(body).get(), containsString("getTeamsResponse"));
    assertThat(body.getFields(), hasSize(1));
    ObjectType responseType = toObjectType(body.getFields().iterator().next().getValue());
    assertThat(responseType.getFields(), hasSize(1));
    assertThat(responseType.getFields().iterator().next().getValue(), is(instanceOf(StringType.class)));
  }

  @Test
  public void inputHeadersMetadata() {
    OperationModel model = getMetadata("getLeagueTeams");
    ObjectType headers = toObjectType(getParameter(model, HEADERS_PARAM).getType());
    assertThat(headers.getFields(), hasSize(1));
    ObjectType auth = toObjectType(headers.getFields().iterator().next().getValue());
    assertThat(auth.getFields(), hasSize(1));
    assertThat(getTypeId(auth).get(), containsString("auth"));
    assertThat(auth.getFields(), hasSize(1));
    assertThat(auth.getFields().iterator().next().getValue(), is(instanceOf(StringType.class)));
  }

  @Test
  public void requestMetadata() {
    OperationModel model = getMetadata("getPresidentInfo");
    ObjectType request = toObjectType(getParameter(model, BODY_PARAM).getType());
    assertThat(request.getFields(), hasSize(1));
    ObjectType getPresidentInfoType = toObjectType(request.getFields().iterator().next().getValue());
    assertThat(getPresidentInfoType.getFields(), hasSize(1));
    assertThat(getPresidentInfoType.getFields().iterator().next().getValue(), is(instanceOf(BooleanType.class)));
  }

  @Test
  public void inputAttachmentsMetadata() {
    OperationModel model = getMetadata("uploadResult");
    ObjectType attachments = toObjectType(getParameter(model, ATTACHMENTS_PARAM).getType());
    assertThat(attachments.getFields(), hasSize(1));
    ObjectFieldType attachment = attachments.getFields().iterator().next();
    assertThat(attachment.getKey().getName().getLocalPart(), is("result"));
    assertThat(attachment.getValue(), is(instanceOf(BinaryType.class)));
  }

  @Test
  public void headersMetadata() {
    OperationModel model = getMetadata("getPresidentInfo");
    List<ObjectFieldType> outputFields = new ArrayList<>(toObjectType(model.getOutput().getType()).getFields());
    assertThat(outputFields, hasSize(2));
    ObjectFieldType headers = outputFields.stream().filter(f -> f.getKey().toString().contains("headers")).findAny().get();
    Collection<ObjectFieldType> soapHeaders = toObjectType(headers.getValue()).getFields();
    assertThat(soapHeaders, hasSize(1));
    ObjectFieldType identity = soapHeaders.iterator().next();
    assertThat(identity.getKey().getName().getLocalPart(), is("identity"));
    assertThat(toObjectType(identity.getValue()).getFields(), hasSize(1));
    assertThat(toObjectType(identity.getValue()).getFields().iterator().next().getValue(), is(instanceOf(StringType.class)));
  }

  @Test
  public void invalidKey() {
    Location location = Location.builder().globalName("invalidKey").addProcessorsPart().addIndexPart(0).build();
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> result = metadataService.getOperationMetadata(location);
    assertThat(result.isSuccess(), is(false));
    assertThat(result.getFailures(), hasSize(4));
    result.getFailures().forEach(failure -> assertThat(failure.getReason(), containsString(INVALID_KEY_ERROR)));
  }

  private ParameterModel getParameter(OperationModel model, String name) {
    return model.getAllParameterModels().stream().filter(p -> p.getName().equals(name)).findAny().get();
  }

  private ObjectType toObjectType(MetadataType headers) {
    assertThat(headers, is(instanceOf(ObjectType.class)));
    return ((ObjectType) headers);
  }

  private OperationModel getMetadata(String flow) {
    Location location = Location.builder().globalName(flow).addProcessorsPart().addIndexPart(0).build();
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> result = metadataService.getOperationMetadata(location);
    assertThat(result.getFailures().stream().map(f -> f.getMessage()).collect(joining(", \n")), result.isSuccess(), is(true));
    return result.get().getModel();
  }
}
