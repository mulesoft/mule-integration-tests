/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.runtime;

import static org.mule.extension.ws.WscTestUtils.SIMPLE_ATTACHMENT;
import static org.mule.extension.ws.WscTestUtils.resourceAsString;
import static org.mule.test.allure.AllureConstants.WscFeature.WSC_EXTENSION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.stream.XMLStreamException;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features(WSC_EXTENSION)
@Stories("Attachments")
public class SoapWithAttachmentsTestCase extends AttachmentsTestCase {

  public SoapWithAttachmentsTestCase() {
    super(false);
  }

  @Override
  protected void assertDownloadedAttachment(Message attachmentPart) throws XMLStreamException, IOException {
    String expectedAttachmentContent = resourceAsString(SIMPLE_ATTACHMENT);
    assertThat(IOUtils.toString((InputStream) attachmentPart.getPayload().getValue()), is(expectedAttachmentContent));
  }
}
