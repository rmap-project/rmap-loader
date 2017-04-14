/*
 * Copyright 2017 Johns Hopkins University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.rmapproject.loader.camel.impl.oai;

import static info.rmapproject.loader.camel.impl.oai.OAIDriver.OAI_PARAM_METADATA_PREFIX;
import static info.rmapproject.loader.camel.impl.oai.OAIDriver.OAI_PARAM_VERB;
import static info.rmapproject.loader.camel.impl.oai.OAIDriver.OAI_VERB_LIST_RECORDS;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.http.client.utils.URIBuilder;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class OAIDriverIT
        extends CamelTestSupport {

    @Produce
    protected ProducerTemplate template;

    @EndpointInject(uri = "mock:err")
    private MockEndpoint mock_err;

    @EndpointInject(uri = "mock:stop")
    private MockEndpoint mock_stop;

    @EndpointInject(uri = "mock:out")
    private MockEndpoint mock_out;

    private static Server server;

    private static URL url;

    private static OaiServer oai = new OaiServer();

    private static final String METADATA_PREFIX = "prefix";

    private static final String RESUMPTION_TOKEN = "resumptionToken";

    @Path("oai")
    public static class OaiServer {

        Map<String, String> headers = new HashMap<>();

        int status = 200;

        RequestListener listener;

        InputStream content;

        @GET
        @Produces(MediaType.APPLICATION_XML)
        public Response get(@Context UriInfo uri) {

            if (listener != null) {
                listener.onRequest(uri);
            }

            final ResponseBuilder response = Response.status(status);
            headers.forEach((k, v) -> response.header(k, v));
            response.entity(content);
            return response.build();
        }
    }

    /* Simple test for single-resource harvest with no resumption */
    @Test
    public void noResumptionTest() throws Exception {
        final String content = IOUtils.readStringFromStream(OAIDriverTest.class
                .getResourceAsStream("/oai/pubmed_50_oai_dc_noResumption.xml"));
        oai.content = new ByteArrayInputStream(content.getBytes());

        mock_out.setExpectedCount(50);
        mock_stop.setExpectedCount(1);
        mock_err.setExpectedCount(0);
        mock_err.setAssertPeriod(500);

        template.sendBodyAndHeader("direct:start",
                "",
                Exchange.HTTP_URI,
                new URIBuilder(url.toString()).addParameter(OAI_PARAM_VERB, OAI_VERB_LIST_RECORDS)
                        .addParameter(OAI_PARAM_METADATA_PREFIX, METADATA_PREFIX).build()
                        .toString());

        mock_out.assertIsSatisfied();
    }

    /*
     * Harvester gets two records: The first one ends in a resumption token, the second one terminates the harvest
     * with an empty token.
     */
    @Test
    public void resumptionSequenceTest() throws Exception {
        final AtomicInteger requestNumber = new AtomicInteger(0);

        oai.listener = new RequestListener() {

            @Override
            public void onRequest(UriInfo uri) {
                final int request = requestNumber.getAndIncrement();

                System.out.println("Request: " + request);
                switch (request) {
                case 0:
                    /* First request, not from resumption token */
                    assertNull(param(RESUMPTION_TOKEN, uri));
                    oai.content = OAIDriverIT.class.getResourceAsStream("/oai/pubmed_50_oai_dc_resumption.xml");
                    break;
                case 1:
                    /* Second request, from resumption token */
                    assertNotNull(param(RESUMPTION_TOKEN, uri));
                    oai.content =
                            OAIDriverIT.class.getResourceAsStream("/oai/pubmed_50_oai_dc_emptyResumption.xml");
                    break;
                default:
                    fail("only should have been two requests!");
                }

                if (request == 0) {
                    assertNull(param(RESUMPTION_TOKEN, uri));
                }
            }
        };

        mock_out.setExpectedCount(50 * 2);
        mock_stop.setExpectedCount(1);
        mock_err.setExpectedCount(0);
        mock_err.setAssertPeriod(500);

        /* Empty message */
        template.sendBodyAndHeader("direct:start",
                "",
                Exchange.HTTP_URI,
                new URIBuilder(url.toString()).addParameter(OAI_PARAM_VERB, OAI_VERB_LIST_RECORDS)
                        .addParameter(OAI_PARAM_METADATA_PREFIX, METADATA_PREFIX).build()
                        .toString());

        mock_out.assertIsSatisfied();

    }

    /* Make sure retry-after is respected */
    @Test
    public void retryAfterTest() throws Exception {
        final AtomicInteger requestNumber = new AtomicInteger(0);

        final Integer retry_delay = 1;
        final AtomicLong retryAt = new AtomicLong();

        oai.listener = new RequestListener() {

            @Override
            public void onRequest(UriInfo uri) {
                final int request = requestNumber.getAndIncrement();

                System.out.println("Request: " + request + ", " + uri.getRequestUri().toString());
                switch (request) {
                case 0:
                    /* First request, not from resumption token */
                    assertNull(param(RESUMPTION_TOKEN, uri));
                    oai.content = OAIDriverIT.class.getResourceAsStream("/oai/pubmed_50_oai_dc_resumption.xml");
                    break;
                case 1:
                    oai.status = Status.SERVICE_UNAVAILABLE.getStatusCode();
                    oai.headers.put(HttpHeaders.RETRY_AFTER, retry_delay.toString());
                    oai.content = new ByteArrayInputStream(new byte[0]);
                    retryAt.set(new Date().getTime());
                    break;
                case 2:
                    /*
                     * Third request; after delay, and from resumption token
                     */

                    oai.status = Status.OK.getStatusCode();
                    oai.headers.clear();

                    assertNotNull(param(RESUMPTION_TOKEN, uri));

                    oai.content =
                            OAIDriverIT.class.getResourceAsStream("/oai/pubmed_50_oai_dc_emptyResumption.xml");
                    break;
                default:
                    fail("only should have been two requests!");
                }

                if (request == 0) {
                    assertNull(param(RESUMPTION_TOKEN, uri));
                }
            }
        };

        mock_out.setExpectedCount(50 * 2);
        mock_stop.setExpectedCount(1);
        mock_err.setExpectedCount(0);
        mock_err.setAssertPeriod(1500);

        /* Empty message */
        template.sendBodyAndHeader("direct:start",
                "",
                Exchange.HTTP_URI,
                new URIBuilder(url.toString()).addParameter(OAI_PARAM_VERB, OAI_VERB_LIST_RECORDS)
                        .addParameter(OAI_PARAM_METADATA_PREFIX, METADATA_PREFIX).build()
                        .toString());

        mock_out.assertIsSatisfied();
    }

    @BeforeClass
    public static void startServer() throws Exception {
        url = new URL("http://localhost:8787/test/oai");
        final String ENDPOINT_ADDRESS = "http://localhost:8787/test";

        final JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setResourceClasses(OaiServer.class);

        sf.setResourceProvider(OaiServer.class, new SingletonResourceProvider(oai, true));
        sf.setAddress(ENDPOINT_ADDRESS);

        server = sf.create();
    }

    @Before
    public void resetResponse() throws Exception {
        oai.status = 200;
        oai.headers.clear();
        oai.content = new ByteArrayInputStream("<content/ >".getBytes());
        oai.listener = null;
        startCamelContext();
    }

    @AfterClass
    public static void destroy() throws Exception {
        server.stop();
        server.destroy();
    }

    @Override
    protected JndiRegistry createRegistry() throws Exception {
        final JndiRegistry registry = super.createRegistry();

        final DefaultCamelContext testShim = new DefaultCamelContext(registry);
        testShim.setName("testShim");

        final OAIDriver oai = new OAIDriver();
        testShim.addRoutes(oai);

        testShim.start();

        registry.bind("toTest", testShim);
        return registry;

    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {

        return new RouteBuilder() {

            @Override
            public void configure() {

                /* In */
                from("direct:start").to("toTest:oai.request");

                /* Outs */
                from("toTest:out").to("mock:out");
                from("toTest:err").to("mock:err");

            }
        };
    }

    private String param(String name, UriInfo uriInfo) {
        final List<String> values = uriInfo.getQueryParameters(true).get(name);

        if (values == null) {
            return null;
        }

        assertTrue(values.size() < 2);

        if (values.isEmpty()) {
            return null;
        } else {
            return values.get(0);
        }
    }

    private interface RequestListener {

        public void onRequest(UriInfo uri);
    }

}
