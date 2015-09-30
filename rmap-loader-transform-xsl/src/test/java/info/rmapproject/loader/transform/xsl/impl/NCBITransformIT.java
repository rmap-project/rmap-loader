
package info.rmapproject.loader.transform.xsl.impl;

import java.nio.file.Paths;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.XsltUriResolver;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.spi.ClassResolver;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

import static info.rmapproject.loader.camel.Lambdas.processor;

public class NCBITransformIT
        extends CamelTestSupport {

    private final String basedir;

    public NCBITransformIT() {
        try {
            basedir =
                    Paths.get(getClass().getResource("/NCBI").toURI())
                            .toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JndiRegistry registry;

    @Produce
    protected ProducerTemplate template;

    @EndpointInject(uri = "mock:out")
    private MockEndpoint mock_out;

    @Test
    public void XTest() throws Exception {
        template.sendBody("direct:in",
                          getClass()
                                  .getResourceAsStream("/NCBI/input/pubmedids.xml"));
        mock_out.setExpectedCount(1);
        mock_out.assertIsSatisfied();
        mock_out.setResultWaitTime(100000);
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {

        return new RouteBuilder() {

            public void configure() {

                try {
                    from("direct:in")
                            .to("xslt:file:" + basedir
                                    + "/pubmed_article_to_disco.xsl?saxon=true#uriResolver=myResolver")
                            .process(processor(e -> {
                                System.out.println("hello"
                                        + e.getIn().getBody(String.class));
                            })).to("mock:out");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            }
        };
    }

    protected JndiRegistry createRegistry() throws Exception {
        if (registry == null) {
            registry = super.createRegistry();
        }
        return registry;
    }

    @Override
    protected CamelContext createCamelContext() throws Exception {

        createRegistry();

        CamelContext testCxt = super.createCamelContext();

        registry.bind("myResolver",
                      new CustomResolver(testCxt.getClassResolver(), "file:" + basedir + "/pubmed_article_to_disco.xsl"));

        return testCxt;
    }

    private class CustomResolver
            extends XsltUriResolver {

        public CustomResolver(ClassResolver resolver, String location) {

            super(resolver, "file:" + location);
        }

        public Source resolve(String href, String base)
                throws TransformerException {
            System.out.println(String
                    .format("Resolving URI with href: %s and base: %s",
                            href,
                            base));
            return super.resolve(href, base);
        }
    }
}
