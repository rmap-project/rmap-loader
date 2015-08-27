
package info.rmapproject.loader.experimental;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

/**
 * Really fast transformer.
 * <p>
 * About 10x faster than parsing into memory + serializing, using RIOT or jena
 * classic models.
 * </p>
 */
@Component(service = RoutesBuilder.class, configurationPolicy = ConfigurationPolicy.REQUIRE, property = "role=transform")
public class JenaStreamTransform
        extends RouteBuilder {

    private static final StreamRDFTranslator translator =
            new StreamRDFTranslator();

    private static final ExecutorService executor = Executors
            .newCachedThreadPool();

    @Override
    public void configure() throws Exception {
        from("direct:in").process(translator).to("direct:out");
    }

    private static class StreamRDFTranslator
            implements Processor {

        @Override
        public void process(Exchange exchange) throws Exception {

            PipedInputStream in = new PipedInputStream();
            final PipedOutputStream out = new PipedOutputStream();
            in.connect(out);

            StreamRDF sink =
                    StreamRDFWriter.getWriterStream(out, RDFFormat.NTRIPLES);
            InputStream orig = exchange.getIn().getBody(InputStream.class);

            executor.execute(new Runnable() {

                public void run() {
                    try {
                        RDFDataMgr.parse(sink, orig, "", Lang.RDFXML);
                    } finally {
                        try {
                            out.close();
                        } catch (IOException e) {
                            throw new RuntimeException();
                        }
                    }
                }
            });

            if (exchange.getPattern().isOutCapable()) {
                exchange.getOut().setBody(in);
            } else {
                exchange.getIn().setBody(in);
            }
        }
    }

}
