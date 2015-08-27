
package info.rmapproject.loader.camel.impl.oai;

import info.rmapproject.loader.camel.Lambdas;

import java.util.Date;
import java.util.Map;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.component.http4.HttpClientConfigurer;
import org.apache.camel.component.http4.HttpComponent;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

@Component(service = RoutesBuilder.class, configurationPolicy = ConfigurationPolicy.REQUIRE, property = {
        "extractor.role=oai", "loader.role=extractor"})
public class OAIHarvest
        extends RouteBuilder {

    static final String ROUTE_OAI_RESUME = "oai.resume";

    static final String ROUTE_OAI_REQUEST = "oai.request";

    /*
     * OAI constants, used in OAI requests, or formulating an OAI message to
     * initiate a harvest.
     */

    public static final String OAI_NS = "http://www.openarchives.org/OAI/2.0/";

    public static final String OAI_PARAM_METADATA_PREFIX = "metadataPrefix";

    public static final String OAI_PARAM_VERB = "verb";

    public static final String OAI_PARAM_SET = "set";

    public static final String OAI_PARAM_FROM = "from";

    public static final String OAI_PARAM_UNTIL = "until";

    public static final String OAI_PARAM_RESUMPTION_TOKEN = "resumptionToken";

    public static final String OAI_VERB_LIST_RECORDS = "ListRecords";

    /* Configuration parameters */
    public static final String CONFIG_PARAM_BASEURI = "oai.baseURI";

    public static final String CONFIG_PARAM_MEADATA_PREFIX =
            "oai.metadataPrefix";

    public static final String CONFIG_PARAM_OAI_SET = "oai.set";

    public static final String CONFIG_PARAM_OAI_FROMDATE = "oai.from";

    public static final String CONFIG_PARAM_OAI_UNTILDATE = "oai.until";

    public static final String CONFIG_PARAM_DEFAULT_REQUEST_DELAY =
            "defaultDelay";

    public static final String CONFIG_PARAM_DEFAULT_RETRY_DELAY =
            "retryDefaultDelay";

    private String baseURI;

    private String metadataPrefix = "oai_dc";

    private String set;

    private String from;

    private String until;

    private long delay = 0;

    private long retryDelay = 60000l;

    private OaiURLProcessor oaiURL = new OaiURLProcessor();

    private DelayExpression retryAfter = new DelayExpression();

    private ResumptionTokenProcessor resumptionToken =
            new ResumptionTokenProcessor();

    Namespaces ns = new Namespaces("oai", OAI_NS);

    @Activate
    public void configure(Map<String, String> config) {
        this.baseURI = config.get(CONFIG_PARAM_BASEURI);
        this.metadataPrefix = config.get(CONFIG_PARAM_MEADATA_PREFIX);
        set = config.get(CONFIG_PARAM_OAI_SET);
        from = config.get(CONFIG_PARAM_OAI_FROMDATE);
        until = config.get(CONFIG_PARAM_OAI_UNTILDATE);

        if (config.containsKey(CONFIG_PARAM_DEFAULT_REQUEST_DELAY)) {
            delay =
                    Long.parseLong(config
                            .get(CONFIG_PARAM_DEFAULT_REQUEST_DELAY));
        }

        if (config.containsKey(CONFIG_PARAM_DEFAULT_REQUEST_DELAY)) {
            retryDelay =
                    Long.parseLong(config.get(CONFIG_PARAM_DEFAULT_RETRY_DELAY));
        }
    }

    @Override
    public void configure() throws Exception {

        HttpComponent httpComponent =
                getContext().getComponent("http4", HttpComponent.class);
        httpComponent.setHttpClientConfigurer(new HttpClientConfigurer() {

            @Override
            public void configureHttpClient(HttpClientBuilder clientBuilder) {
                clientBuilder
                        .setRedirectStrategy(new DefaultRedirectStrategy());

            }
        });

        /* Start initial OAI ListRecords */
        from("direct:start").routeId("setup").process(oaiURL)
                .to("direct:oai.request");

        /*
         * Perform an OAI http request. TODO: Do this entirely with streaming.
         * Instead of multicast, do an xpath split with an OR:
         * //oai:metadata/*[1]|//oai:resumptionToken. Route based on element.
         */
        from("direct:oai.request").routeId(ROUTE_OAI_REQUEST)
                .process(resumptionToken)
                .to("http4:oai-host?throwExceptionOnFailure=false")
                .streamCaching()
                //
                .choice()
                //
                .when(header(Exchange.HTTP_RESPONSE_CODE).isNotEqualTo(200))
                .to("direct:checkError")//
                .otherwise().multicast().to("direct:in", "direct:oai.resume");

        /* Check a bad response */
        from("direct:checkError").routeId("checkError").choice()
                .when(header(Exchange.HTTP_RESPONSE_CODE).isNotEqualTo(503))
                .to("direct:err").otherwise().delay(retryAfter)
                .to("direct:oai.request");

        /* Split into oai Records */
        from("direct:in").routeId("split")
                .split(ns.xpath("//oai:metadata/*[1]")).streaming()
                .convertBodyTo(String.class).to("direct:out");

        /* See if there is a resumption token. If so, request anew. If not, stop */
        from("direct:oai.resume")
                .routeId(ROUTE_OAI_RESUME)
                .process(Lambdas.processor(e -> {
                    System.out.println("Checking resumption");
                }))
                .setHeader("oai.resumptionToken",
                           ns.xpath("//oai:resumptionToken/text()",
                                    String.class))
                //
                .process(Lambdas.processor(e -> {
                    System.out.println("Got resumption token '"
                            + e.getIn().getHeader("oai.resumptionToken") + "'");
                })).choice().when(header("oai.resumptionToken").isEqualTo(""))
                .to("direct:stop")
                //Stop
                .otherwise().delay(delay).to("direct:oai.request")
                .id("doResume"); // Delay and resume
    }

    private class OaiURLProcessor
            implements Processor {

        @Override
        public void process(Exchange exchange) throws Exception {
            URIBuilder uri =
                    new URIBuilder(Optional.ofNullable(exchange.getIn()
                            .getHeader("oai.baseURL", String.class))
                            .orElse(baseURI));

            uri.addParameter(OAI_PARAM_VERB,
                             Optional.ofNullable(exchange.getIn()
                                     .getHeader("oai.verb", String.class))
                                     .orElse(OAI_VERB_LIST_RECORDS));

            uri.addParameter(OAI_PARAM_METADATA_PREFIX,
                             Optional.ofNullable(exchange.getIn()
                                     .getHeader("oai.metadataPrefix",
                                                String.class))
                                     .orElse(metadataPrefix));

            addIfPresent(set, OAI_PARAM_SET, "oai.set", exchange.getIn(), uri);
            addIfPresent(from,
                         OAI_PARAM_FROM,
                         "oai.from",
                         exchange.getIn(),
                         uri);
            addIfPresent(until,
                         OAI_PARAM_UNTIL,
                         "oai.until",
                         exchange.getIn(),
                         uri);

            exchange.getIn().setHeader(Exchange.HTTP_URI, uri.toString());
        }
    }

    private class ResumptionTokenProcessor
            implements Processor {

        @Override
        public void process(Exchange exchange) throws Exception {
            String resumptionToken =
                    exchange.getIn().getHeader("oai.resumptionToken",
                                               String.class);
            if (resumptionToken != null) {
                URIBuilder newURI =
                        new URIBuilder(exchange.getIn()
                                .getHeader(Exchange.HTTP_URI, String.class))
                                .setParameter(OAI_PARAM_RESUMPTION_TOKEN,
                                              resumptionToken);
                exchange.getIn().setHeader(Exchange.HTTP_URI,
                                           newURI.build().toString());
            }

        }
    }

    private class DelayExpression
            implements Expression {

        @Override
        public <T> T evaluate(Exchange exchange, Class<T> type) {
            Long delay = retryDelay;
            String retry =
                    exchange.getIn().getHeader("Retry-After", String.class);

            if (retry.matches("^\\d+$")) {
                /* Number of seconds. From rfc2616 */
                delay = Long.parseLong(retry) * 1000;
            } else {
                /* Http Date. From rfc2615 */
                delay =
                        DateUtils.parseDate(retry).getTime()
                                - new Date().getTime();
            }

            return constant(delay).evaluate(exchange, type);
        }

    }

    private static void addIfPresent(String defaultValue,
                                     String oaiParam,
                                     String messageHeader,
                                     Message message,
                                     URIBuilder uri) {
        String oaiParamValue =
                Optional.ofNullable(message.getHeader(messageHeader,
                                                      String.class))
                        .orElse(defaultValue);

        if (oaiParamValue != null) {
            uri.addParameter(oaiParam, oaiParamValue);
        }
    }
}
