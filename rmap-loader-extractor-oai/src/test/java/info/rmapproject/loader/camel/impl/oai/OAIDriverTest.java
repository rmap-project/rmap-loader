
package info.rmapproject.loader.camel.impl.oai;

import java.io.InputStream;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.test.junit4.CamelTestSupport;
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
        InputStream toSplit = OAIDriverTest.class.getResourceAsStream("/oai/pubmed_50_oai_dc_resumption.xml");

        mock_out.setExpectedCount(50);

        template.sendBody("direct:in", toSplit);

        mock_out.assertIsSatisfied();
    }

    @Test
    public void stopWhenNoResumptionTest() throws Exception {
        InputStream oaiListRecords = OAIDriverTest.class.getResourceAsStream("/oai/pubmed_50_oai_dc_noResumption.xml");

        mock_stop.setExpectedCount(1);
        mock_oai.setExpectedCount(0);
        mock_oai.setAssertPeriod(100);

        template.sendBody("direct:resumption", oaiListRecords);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void stopWhenEmptyResumptionTest() throws Exception {
        InputStream oaiListRecords =
                OAIDriverTest.class.getResourceAsStream("/oai/pubmed_50_oai_dc_emptyResumption.xml");

        mock_stop.setExpectedCount(1);
        mock_oai.setExpectedCount(0);
        mock_oai.setAssertPeriod(100);

        template.sendBody("direct:resumption", oaiListRecords);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void resumeWhenResumptionTokenTest() throws Exception {
        InputStream oaiListRecords = OAIDriverTest.class.getResourceAsStream("/oai/pubmed_50_oai_dc_resumption.xml");

        mock_oai.setExpectedCount(1);
        mock_stop.setExpectedCount(0);
        mock_stop.setAssertPeriod(100);

        template.sendBody("direct:resumption", oaiListRecords);

        assertMockEndpointsSatisfied();

        String token = mock_oai.getExchanges().get(0).getIn().getHeader("oai.resumptionToken", String.class);

        assertEquals("oai%3Apubmedcentral.nih.gov%3A139967!!!oai_dc!bmcbioc", token);
    }

    @Before
    public void startRoutes() throws Exception {
        startCamelContext();
    }

    @Override
    protected JndiRegistry createRegistry() throws Exception {
        JndiRegistry registry = super.createRegistry();

        DefaultCamelContext testShim = new DefaultCamelContext(registry);
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
