
package info.rmapproject.loader.experimental;

import java.io.StringReader;
import java.io.StringWriter;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

@Component(service = RoutesBuilder.class, configurationPolicy = ConfigurationPolicy.REQUIRE, property = "role=transform")
public class JenaModelTransform
        extends RouteBuilder {

    private static final ModelRDFTranslator translator =
            new ModelRDFTranslator();

    @Override
    public void configure() throws Exception {
        from("direct:in").process(translator).to("direct:out");
    }

    private static class ModelRDFTranslator
            implements Processor {

        @Override
        public void process(Exchange exchange) throws Exception {

            Model m = ModelFactory.createMemModelMaker().createDefaultModel();
            m.read(new StringReader(exchange.getIn().getBody(String.class)),
                   "",
                   "RDF/XML");

            StringWriter out = new StringWriter();
            m.write(out, "N-TRIPLE", "");

            if (exchange.getPattern().isOutCapable()) {
                exchange.getOut().setBody(out.toString(), String.class);
            } else {
                exchange.getIn().setBody(out.toString(), String.class);
            }
        }
    }
}
