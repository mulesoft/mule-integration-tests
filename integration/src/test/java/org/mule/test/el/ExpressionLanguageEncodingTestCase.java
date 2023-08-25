/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.el;

import static com.google.common.base.Charsets.UTF_16;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mule.tck.junit4.matcher.IsEmptyOptional.empty;
import static org.mule.test.allure.AllureConstants.ExpressionLanguageFeature.EXPRESSION_LANGUAGE;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;

import java.nio.charset.Charset;
import java.util.Optional;

import io.qameta.allure.Feature;
import org.junit.Rule;
import org.junit.Test;

@Feature(EXPRESSION_LANGUAGE)
public class ExpressionLanguageEncodingTestCase extends AbstractIntegrationTestCase {

  @Rule
  public SystemProperty encoding = new SystemProperty("mule.encoding", UTF_16.displayName());

  @Override
  protected String getConfigFile() {
    return "org/mule/test/el/expression-language-encoding-config.xml";
  }

  @Test
  public void usesMuleEncoding() throws Exception {
    Message result = flowRunner("text").keepStreamsOpen().run().getMessage();
    Optional<Charset> charset = result.getPayload().getDataType().getMediaType().getCharset();
    assertThat(charset, not(empty()));
    assertThat(charset.get(), is(UTF_16));
    assertThat(IOUtils.toString(((CursorStreamProvider) result.getPayload().getValue()).openCursor(), UTF_16),
               is("This is evolution."));
  }
}
