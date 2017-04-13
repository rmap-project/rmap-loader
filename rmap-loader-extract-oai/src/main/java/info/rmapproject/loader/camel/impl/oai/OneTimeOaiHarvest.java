
package info.rmapproject.loader.camel.impl.oai;

import static info.rmapproject.loader.camel.impl.oai.OAIDriver.ENDPOINT_OAI_REQUEST;
import static info.rmapproject.loader.camel.impl.oai.OAIDriver.OAI_PARAM_FROM;
import static info.rmapproject.loader.camel.impl.oai.OAIDriver.OAI_PARAM_METADATA_PREFIX;
import static info.rmapproject.loader.camel.impl.oai.OAIDriver.OAI_PARAM_SET;
import static info.rmapproject.loader.camel.impl.oai.OAIDriver.OAI_PARAM_UNTIL;
import static info.rmapproject.loader.camel.impl.oai.OAIDriver.OAI_PARAM_VERB;
import static info.rmapproject.loader.camel.impl.oai.OAIDriver.OAI_VERB_LIST_RECORDS;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OneTimeOaiHarvest
        extends RouteBuilder {

    static Logger LOG = LoggerFactory.getLogger(OneTimeOaiHarvest.class);

    static final String ROUTE_OAI_RESUME = "oai.resume";

    static final String ROUTE_OAI_REQUEST = "oai.request";

    private String baseURI;

    private String metadataPrefix = "oai_dc";

    private String set;

    private String from;

    private String until;

    private RoutesBuilder driver;

    private String dest;

    private String contentType = "application/xml";

    public void setDestUri(String dest) {
        this.dest = dest;
    }

    public void setOutputContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setOaiBaseUri(String uri) {
        this.baseURI = uri;
    }

    public void setMetadataPrefix(String prefix) {
        this.metadataPrefix = prefix;
    }

    public void setOaiSet(String set) {
        this.set = set;
    }

    public void setOaiFromDate(String date) {
        this.from = date;
    }

    public void setOaiUntilDate(String date) {
        this.until = date;
    }

    public void setOaiDriver(RoutesBuilder driver) {
        this.driver = driver;
    }

    @Override
    public void addRoutesToCamelContext(CamelContext context) throws Exception {
        driver.addRoutesToCamelContext(context);
        super.addRoutesToCamelContext(context);
    }

    @Override
    public void configure() throws Exception {

        from("direct:start").process(oaiURL)
                .process(e -> LOG.info("Launching one time harvest to {}", e.getIn().getHeader(Exchange.HTTP_URI)))
                .to(ENDPOINT_OAI_REQUEST);

        from("direct:out").to(dest);
    }

    private final Processor oaiURL = (exchange -> {
        final URIBuilder uri = new URIBuilder(baseURI).addParameter(OAI_PARAM_VERB, OAI_VERB_LIST_RECORDS)
                .addParameter(OAI_PARAM_METADATA_PREFIX, metadataPrefix);

        addIfPresent(set, OAI_PARAM_SET, uri);
        addIfPresent(from, OAI_PARAM_FROM, uri);
        addIfPresent(until, OAI_PARAM_UNTIL, uri);

        exchange.getIn().setHeader(Exchange.HTTP_URI, uri.toString());
    });

    private static void addIfPresent(String value, String oaiParam, URIBuilder uri) {

        if (value != null && !value.equals("")) {
            LOG.info("Adding optional param {} = '{}'", oaiParam, value);
            uri.addParameter(oaiParam, value);
        }
    }
}
