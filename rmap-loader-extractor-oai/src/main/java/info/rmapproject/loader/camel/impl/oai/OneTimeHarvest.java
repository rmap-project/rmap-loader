
package info.rmapproject.loader.camel.impl.oai;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.http.client.utils.URIBuilder;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import static info.rmapproject.loader.camel.impl.oai.OAIDriver.OAI_PARAM_FROM;
import static info.rmapproject.loader.camel.impl.oai.OAIDriver.OAI_PARAM_METADATA_PREFIX;
import static info.rmapproject.loader.camel.impl.oai.OAIDriver.OAI_PARAM_SET;
import static info.rmapproject.loader.camel.impl.oai.OAIDriver.OAI_PARAM_UNTIL;
import static info.rmapproject.loader.camel.impl.oai.OAIDriver.OAI_PARAM_VERB;
import static info.rmapproject.loader.camel.impl.oai.OAIDriver.OAI_VERB_LIST_RECORDS;

@ObjectClassDefinition(name = "info.rmapproject.loader.camel.impl.oai.OneTimeHarvest", description = "Performs a single OAI harvest")
@interface OneTimeHarvestConfig {

    @AttributeDefinition(description = "OAI baseURI")
    String oai_baseURI() default "http://example.org/oai";

    @AttributeDefinition(description = "metadata format")
    String oai_metadataPrefix() default "oai_dc";

    @AttributeDefinition(description = "set (optional)")
    String oai_set();

    @AttributeDefinition(description = "from (YYYY-MM-DD, optional)")
    String oai_from();

    @AttributeDefinition(description = "from (YYYY-MM-DD, optional)")
    String oai_until();
}

@Designate(ocd = OneTimeHarvestConfig.class, factory = true)
@Component(service = RoutesBuilder.class, configurationPolicy = ConfigurationPolicy.REQUIRE, property = {
        "extractor.role=oai", "loader.role=extractor"})
public class OneTimeHarvest
        extends RouteBuilder {

    static final String ROUTE_OAI_RESUME = "oai.resume";

    static final String ROUTE_OAI_REQUEST = "oai.request";

    private String baseURI;

    private String metadataPrefix = "oai_dc";

    private String set;

    private String from;

    private String until;

    private RoutesBuilder driver;

    @Activate
    @Modified
    public void init(OneTimeHarvestConfig config) {
        this.baseURI = config.oai_baseURI();
        this.metadataPrefix = config.oai_metadataPrefix();
        set = config.oai_set();
        from = config.oai_from();
        until = config.oai_until();
    }

    @Reference(target = "(oai.role=driver)")
    public void setOaiDriver(RoutesBuilder driver) {
        this.driver = driver;
    }

    public void addRoutesToCamelContext(CamelContext context) throws Exception {
        driver.addRoutesToCamelContext(context);
        super.addRoutesToCamelContext(context);
    }

    @Override
    public void configure() throws Exception {

        from("timer:oneTime?repeatCount=1").process(oaiURL).to("direct:oai.request");
    }

    private Processor oaiURL = (exchange -> {
        URIBuilder uri = new URIBuilder(baseURI).addParameter(OAI_PARAM_VERB, OAI_VERB_LIST_RECORDS)
                .addParameter(OAI_PARAM_METADATA_PREFIX, metadataPrefix);

        addIfPresent(set, OAI_PARAM_SET, uri);
        addIfPresent(from, OAI_PARAM_FROM, uri);
        addIfPresent(until, OAI_PARAM_UNTIL, uri);

        exchange.getIn().setHeader(Exchange.HTTP_URI, uri.toString());
    });

    private static void addIfPresent(String value, String oaiParam, URIBuilder uri) {

        if (value != null) {
            uri.addParameter(oaiParam, value);
        }
    }
}
