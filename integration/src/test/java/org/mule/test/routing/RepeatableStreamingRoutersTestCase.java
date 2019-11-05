package org.mule.test.routing;

import static java.lang.String.format;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.http.api.HttpConstants.HttpStatus.OK;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Rule;
import org.junit.Test;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

public class RepeatableStreamingRoutersTestCase extends AbstractIntegrationTestCase {
    private static final String PAYLOAD = "{\"name\": \"tato\", \"id\": \"42\"}";
    private static final String EXPECTED_OUTPUT = "\"tato - 42\"";

    @Rule
    public DynamicPort listenPort = new DynamicPort("port");

    @Override
    protected String getConfigFile() {
        return "routers-with-streams-config.xml";
    }

    private void doTest(HttpEntity entity, String endpoint, String expected) throws Exception {
        HttpPost httpPost = new HttpPost(format("http://localhost:%s/%s", listenPort.getNumber(), endpoint));
        httpPost.setEntity(entity);

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            try (CloseableHttpResponse response = client.execute(httpPost)) {
                assertThat(response.getStatusLine().getStatusCode(), is(OK.getStatusCode()));
                assertThat(IOUtils.toString(response.getEntity().getContent()), is(expected));
            }
        }
    }

    @Test
    public void scatterGather() throws Exception {
        doTest(new StringEntity(PAYLOAD, APPLICATION_JSON), "scatter-gather", EXPECTED_OUTPUT);
    }

    @Test
    public void scatterGatherByteArray() throws Exception {
        doTest(new ByteArrayEntity(PAYLOAD.getBytes(), APPLICATION_JSON), "scatter-gather", EXPECTED_OUTPUT);
    }

    @Test
    public void async() throws Exception {
        doTest(new StringEntity(PAYLOAD, APPLICATION_JSON), "async", EXPECTED_OUTPUT);
    }

    @Test
    public void asyncByteArray() throws Exception {
        doTest(new ByteArrayEntity(PAYLOAD.getBytes(), APPLICATION_JSON), "async", EXPECTED_OUTPUT);
    }
}
