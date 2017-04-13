
package info.rmapproject.loader.transform.xsl.impl;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

public class Xslt2SplitterTest
        extends CamelTestSupport {

    private final String basedir;

    public Xslt2SplitterTest() {
        try {
            basedir =
                    Paths.get(getClass().getResource("/xslt2").toURI())
                            .toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Produce
    protected ProducerTemplate template;

    @EndpointInject(uri = "mock:out")
    private MockEndpoint mock_out;

    /* Verifies that result-document are captured */
    @Test
    public void outputFileCountTest() throws Exception {

        Map<String, Object> headers = new HashMap<>();
        headers.put(Xslt2Splitter.HEADER_XSLT_FILE_NAME, basedir
                + "/transform.xsl");

        template.sendBodyAndHeaders("direct:in", getClass()
                .getResourceAsStream("/xslt2/input.xml"), headers);

        mock_out.setExpectedCount(5);

        mock_out.assertIsSatisfied();
    }

    /* Verifies that xsl:include files in the same basedir as the transform work */
    @Test
    public void includeFileTest() throws Exception {

        Map<String, Object> headers = new HashMap<>();
        headers.put(Xslt2Splitter.HEADER_XSLT_FILE_NAME, basedir
                + "/transform-include.xsl");

        template.sendBodyAndHeaders("direct:in", getClass()
                .getResourceAsStream("/xslt2/input.xml"), headers);

        mock_out.setExpectedCount(5);

        mock_out.assertIsSatisfied();
    }

    /* Verifies that plain old XSLT 1.0 transforms work too */
    @Test
    public void defaultResultOutputTest() throws Exception {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Xslt2Splitter.HEADER_XSLT_FILE_NAME, basedir
                + "/transform-simple.xsl");

        template.sendBodyAndHeaders("direct:in", getClass()
                .getResourceAsStream("/xslt2/input.xml"), headers);

        mock_out.setExpectedCount(1);

        mock_out.assertIsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {

        Xslt2Splitter xslt = new Xslt2Splitter();

        return new RouteBuilder() {

            public void configure() {

                try {

                    from("direct:in").process(xslt).split(body())
                            .to("mock:out");

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            }
        };
    }
}
