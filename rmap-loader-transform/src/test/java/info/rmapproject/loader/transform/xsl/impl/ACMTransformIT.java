
package info.rmapproject.loader.transform.xsl.impl;

import java.io.InputStream;

import java.nio.file.Paths;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

import static info.rmapproject.loader.validation.DiscoValidator.validate;
import static info.rmapproject.loader.validation.DiscoValidator.Format.RDF_XML;

import static info.rmapproject.loader.transform.xsl.impl.Xslt2Splitter.HEADER_XSLT_FILE_NAME;

public class ACMTransformIT
        extends CamelTestSupport {

    private final String basedir;

    public ACMTransformIT() {
        try {
            basedir = Paths.get(getClass().getResource("/ACM").toURI()).toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Produce
    protected ProducerTemplate template;

    @EndpointInject(uri = "mock:out")
    private MockEndpoint mock_out;

    @Test
    public void basicTest() throws Exception {
        template.sendBody("direct:in", getClass().getResourceAsStream("/ACM/TRANS-TOMS-V1I4-355656.xml"));
        mock_out.setExpectedCount(9);
        mock_out.assertIsSatisfied();

        mock_out.getExchanges().stream().map(Exchange::getIn).map(m -> m.getBody(InputStream.class))
                .forEach(i -> validate(i, RDF_XML));

    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {

        return new RouteBuilder() {

            Xslt2Splitter xslt2 = new Xslt2Splitter();

            public void configure() {

                try {
                    from("direct:in").setHeader(HEADER_XSLT_FILE_NAME, constant(basedir + "/acm_to_disco.xsl"))
                            .process(xslt2).split(body()).to("mock:out");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            }
        };
    }

}
