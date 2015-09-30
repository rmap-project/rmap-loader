
package info.rmapproject.loader.transform.xsl.impl;

import static info.rmapproject.loader.camel.Lambdas.processor;

import java.nio.file.Paths;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

public class ACMTransformIT
        extends CamelTestSupport {

    private final String basedir;

    public ACMTransformIT() {
        try {
            basedir =
                    Paths.get(getClass().getResource("/ACM").toURI())
                            .toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Produce
    protected ProducerTemplate template;

    @EndpointInject(uri = "mock:out")
    private MockEndpoint mock_out;

    @Test
    public void XTest() throws Exception {
        template.sendBody("direct:in",
                          getClass()
                                  .getResourceAsStream("/ACM/TRANS-TOMS-V1I4-355656.xml"));
        mock_out.setExpectedCount(2);
        mock_out.assertIsSatisfied();
        mock_out.setResultWaitTime(10000);
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {

        return new RouteBuilder() {

            public void configure() {

                try {
                    from("direct:in")
                            .to("xslt:file:" + basedir + "/acm_to_disco.xsl?saxon=true")
                            .process(processor(e -> {
                                System.out.println(
                                        e.getIn().getBody(String.class) + "\n");
                            })).to("mock:out");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            }
        };
    }

}
