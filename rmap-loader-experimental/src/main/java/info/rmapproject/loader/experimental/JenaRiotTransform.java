
package info.rmapproject.loader.experimental;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFWriter;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;


@Component(service = RoutesBuilder.class, configurationPolicy = ConfigurationPolicy.REQUIRE, property = "role=transform")
public class JenaRiotTransform
        extends RouteBuilder {

    private static final StreamRDFTranslator translator =
            new StreamRDFTranslator();

    @Override
    public void configure() throws Exception {
        from("direct:in").process(translator).to("direct:out");
    }

    private static class StreamRDFTranslator
            implements Processor {

        @Override
        public void process(Exchange exchange) throws Exception {

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            StreamRDF sink =
                    StreamRDFWriter.getWriterStream(out, RDFFormat.NTRIPLES);
            RDFDataMgr.parse(sink,
                             exchange.getIn().getBody(InputStream.class),
                             "",
                             Lang.RDFXML);

            if (exchange.getPattern().isOutCapable()) {
                exchange.getOut().setBody(out.toByteArray());
            } else {
                exchange.getIn().setBody(out.toByteArray());
            }
        }
    }

}
