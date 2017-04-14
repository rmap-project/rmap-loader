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

import static info.rmapproject.loader.camel.impl.oai.OAIDriver.OAI_PARAM_RESUMPTION_TOKEN;
import static info.rmapproject.loader.camel.impl.oai.OAIDriver.OAI_PARAM_VERB;
import static info.rmapproject.loader.camel.impl.oai.OAIDriver.OAI_VERB_LIST_RECORDS;

import java.io.InputStream;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Before;
import org.junit.Test;

public class OAIDriverTest
        extends CamelTestSupport {

    private static final String MOCK_OAI_ENDPOINT_ID = "mock.oai.request";

    private static final String MOCK_OAI_STOP_ENDPOINT_ID = "mock.oai.stop";

    @Produce
    protected ProducerTemplate template;

    @EndpointInject(uri = "mock:out")
    private MockEndpoint mock_out;

    @EndpointInject(uri = "mock:stop")
    private MockEndpoint mock_stop;

    @EndpointInject(uri = "mock:oai")
    private MockEndpoint mock_oai;

    @Test
    public void splitTest() throws Exception {
        final InputStream toSplit = OAIDriverTest.class.getResourceAsStream("/oai/pubmed_50_oai_dc_resumption.xml");

        mock_out.setExpectedCount(50);

        template.sendBody("direct:in", toSplit);

        mock_out.assertIsSatisfied();
    }

    @Test
    public void stopWhenNoResumptionTest() throws Exception {
        final InputStream oaiListRecords = OAIDriverTest.class.getResourceAsStream(
                "/oai/pubmed_50_oai_dc_noResumption.xml");

        mock_stop.setExpectedCount(1);
        mock_oai.setExpectedCount(0);
        mock_oai.setAssertPeriod(100);

        template.sendBody("direct:resumption", oaiListRecords);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void stopWhenEmptyResumptionTest() throws Exception {
        final InputStream oaiListRecords =
                OAIDriverTest.class.getResourceAsStream("/oai/pubmed_50_oai_dc_emptyResumption.xml");

        mock_stop.setExpectedCount(1);
        mock_oai.setExpectedCount(0);
        mock_oai.setAssertPeriod(100);

        template.sendBody("direct:resumption", oaiListRecords);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void resumeWhenResumptionTokenTest() throws Exception {
        final InputStream oaiListRecords = OAIDriverTest.class.getResourceAsStream(
                "/oai/pubmed_50_oai_dc_resumption.xml");

        mock_oai.setExpectedCount(1);
        mock_stop.setExpectedCount(0);
        mock_stop.setAssertPeriod(100);

        final URIBuilder uri = new URIBuilder("http://example.org");

        final String RESUMPTION_TOKEN = "oai%3Apubmedcentral.nih.gov%3A139967!!!oai_dc!bmcbioc";

        template.sendBodyAndHeader("direct:resumption", oaiListRecords, Exchange.HTTP_URI, uri.build().toString());

        assertMockEndpointsSatisfied();

        final String token = mock_oai.getExchanges().get(0).getIn().getHeader("oai.resumptionToken", String.class);

        assertEquals(RESUMPTION_TOKEN, token);
        assertEquals(uri.setParameter(OAI_PARAM_VERB, OAI_VERB_LIST_RECORDS)
                .setParameter(OAI_PARAM_RESUMPTION_TOKEN, RESUMPTION_TOKEN).build().toString(),
                mock_oai.getExchanges().get(0).getIn().getHeader(Exchange.HTTP_URI, String.class));
    }

    @Before
    public void startRoutes() throws Exception {
        startCamelContext();
    }

    @Override
    protected JndiRegistry createRegistry() throws Exception {
        final JndiRegistry registry = super.createRegistry();

        final DefaultCamelContext testShim = new DefaultCamelContext(registry);
        testShim.setName("testShim");
        testShim.addRoutes(new OAIDriver());

        testShim.start();

        testShim.getRouteDefinition(OAIDriver.ROUTE_OAI_RESUME).adviceWith(testShim, new AdviceWithRouteBuilder() {

            @Override
            public void configure() throws Exception {
                weaveById("doResume").replace().to("direct:" + MOCK_OAI_ENDPOINT_ID);
                weaveById("doStop").replace().to("direct:" + MOCK_OAI_STOP_ENDPOINT_ID);
            }
        });

        registry.bind("toTest", testShim);
        return registry;

    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {

        return new RouteBuilder() {

            @Override
            public void configure() {

                /* For oai splitting */
                from("direct:in").to("toTest:in");
                from("toTest:out").to("mock:out");

                /* ResumptionToken */
                from("direct:resumption").to("toTest:oai.resume");
                from("toTest:" + MOCK_OAI_ENDPOINT_ID).to("mock:oai");
                from("toTest:" + MOCK_OAI_STOP_ENDPOINT_ID).to("mock:stop");
            }
        };
    }
}
