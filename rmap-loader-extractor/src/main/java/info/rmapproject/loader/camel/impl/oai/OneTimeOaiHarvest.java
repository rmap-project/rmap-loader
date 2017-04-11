
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
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.rmapproject.loader.camel.ContextFactory;

@ObjectClassDefinition(name = "info.rmapproject.loader.camel.impl.oai.OneTimeHarvest",
        description = "Performs a single OAI harvest")
@interface OneTimeHarvestConfig {

    @AttributeDefinition(description = "OAI baseURI")
    String oai_baseURI() default "http://oai.datacite.org/oai";

    @AttributeDefinition(description = "metadata format")
    String oai_metadataPrefix() default "datacite";

    @AttributeDefinition(description = "set (optional)")
    String oai_set();

    @AttributeDefinition(description = "from (YYYY-MM-DD, optional)")
    String oai_from();

    @AttributeDefinition(description = "from (YYYY-MM-DD, optional)")
    String oai_until();

    @AttributeDefinition(
            description = "Format ID (correlates extracted records to transformers that can process them")
    String loader_format() default "datacite";
}

@Designate(ocd = OneTimeHarvestConfig.class, factory = true)
@Component(configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true)
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

    private CamelContext cxt;

    private ContextFactory contextFactory;

    public void setDestUri(String dest) {
        this.dest = dest;
    }

    @Reference
    public void setContextFactory(ContextFactory factory) {
        this.contextFactory = factory;
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

    @Activate
    public void cfgStart(OneTimeHarvestConfig config) {
        LOG.info("Updating config");
        this.baseURI = config.oai_baseURI();
        this.metadataPrefix = config.oai_metadataPrefix();
        set = config.oai_set();
        from = config.oai_from();
        until = config.oai_until();

        start();
    }

    public void start() {
        try {
            this.cxt = contextFactory.newContext();

            final OAIDriver oai = new OAIDriver();
            oai.addRoutesToCamelContext(this.cxt);
            addRoutesToCamelContext(this.cxt);
            this.cxt.start();
        } catch (final Exception e) {
            throw new RuntimeException("Could not create camel context", e);
        }
    }

    @Deactivate
    public void stop() throws Exception {
        if (cxt != null) {
            cxt.stop();
        }
    }

    @Reference(target = "(oai.role=driver)")
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
