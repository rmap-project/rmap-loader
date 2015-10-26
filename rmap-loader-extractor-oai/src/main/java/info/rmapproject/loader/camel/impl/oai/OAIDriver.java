
package info.rmapproject.loader.camel.impl.oai;

import java.util.Date;

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
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
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ObjectClassDefinition(name = "info.rmapproject.loader.camel.impl.oai.OAIDriver", description = "Basic OAI driver for performing OAI-PMH harvests")
@interface OAIDriverConfig {

    @AttributeDefinition(description = "Delay after fetching each record")
    int fetch_delay() default 0;

    @AttributeDefinition(description = "Default delay after 503 response, if no 'Retry-After' given")
    long retry_delay() default 60000;
}

@Designate(ocd = OAIDriverConfig.class)
@Component(service = RoutesBuilder.class, configurationPolicy = ConfigurationPolicy.OPTIONAL, property = {
        "oai.role=driver"})
public class OAIDriver
        extends RouteBuilder {

    private static Logger LOG = LoggerFactory.getLogger(OAIDriver.class);

    public static final String ROUTE_OAI_RESUME = "oai-resume";

    public static final String ROUTE_OAI_REQUEST = "oai-request";

    public static final String ROUTE_OAI_STOP = "oai-stop";

    public static final String ROUTE_OAI_REQUEST_PROCESS = "oai-request-process";

    public static final String OAI_NS = "http://www.openarchives.org/OAI/2.0/";

    public static final String OAI_PARAM_METADATA_PREFIX = "metadataPrefix";

    public static final String OAI_PARAM_VERB = "verb";

    public static final String OAI_PARAM_SET = "set";

    public static final String OAI_PARAM_FROM = "from";

    public static final String OAI_PARAM_UNTIL = "until";

    public static final String OAI_PARAM_RESUMPTION_TOKEN = "resumptionToken";

    public static final String OAI_VERB_LIST_RECORDS = "ListRecords";

    public static final String ENDPOINT_OAI_REQUEST = "direct:oai.request";

    private long retryDelay = 60000l;

    private long delay = 0;

    private DelayExpression retryAfter = new DelayExpression();

    Namespaces ns = new Namespaces("oai", OAI_NS);

    @Activate
    @Modified
    public void configure(OAIDriverConfig config) {
        LOG.info("Config updated");
        retryDelay = config.retry_delay();
        delay = config.fetch_delay();
    }

    @Deactivate
    public void deactivate() {
        LOG.info("Deactivating");
    }

    @Override
    public void configure() throws Exception {
        HttpComponent httpComponent = getContext().getComponent("http4", HttpComponent.class);
        httpComponent.setHttpClientConfigurer(new HttpClientConfigurer() {

            @Override
            public void configureHttpClient(HttpClientBuilder clientBuilder) {
                clientBuilder.setRedirectStrategy(new DefaultRedirectStrategy());

            }
        });

        /*
         * Perform an OAI http request. TODO: Do this entirely with streaming.
         * Instead of multicast, do an xpath split with an OR:
         * //oai:metadata/*[1]|//oai:resumptionToken. Route based on element.
         */
        from(ENDPOINT_OAI_REQUEST).routeId(ROUTE_OAI_REQUEST).to("seda:_do_request");

        from("seda:_do_request").routeId(ROUTE_OAI_REQUEST_PROCESS)
                .process(e -> LOG.info("Fetching from {}", e.getIn().getHeader(Exchange.HTTP_URI)))
                .to("http4:oai-host?throwExceptionOnFailure=false").streamCaching()
                //
                .choice()
                //
                .when(header(Exchange.HTTP_RESPONSE_CODE).isNotEqualTo(200)).to("direct:checkError")//
                .otherwise().multicast().to("direct:in", "direct:oai.resume");

        /* Check a bad response */
        from("direct:checkError").routeId("checkError")
                .process(e -> LOG.info("Got non-200 response: {}", e.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE)))
                .choice().when(header(Exchange.HTTP_RESPONSE_CODE).isNotEqualTo(503)).to("direct:err").otherwise()
                .delay(retryAfter).to(ENDPOINT_OAI_REQUEST);

        /* Split into oai Records */
        from("direct:in").routeId("oai-split-response").split(ns.xpath("//oai:metadata/*[1]")).streaming()
                .convertBodyTo(String.class).to("direct:out").end();

        /*
         * See if there is a resumption token. If so, request anew. If not, stop
         */
        from("direct:oai.resume").routeId(ROUTE_OAI_RESUME)
                .setHeader("oai.resumptionToken", ns.xpath("//oai:resumptionToken/text()", String.class))
                .process(e -> LOG.info("Got resumption token '{}'", e.getIn().getHeader("oai.resumptionToken")))
                //
                .choice().when(header("oai.resumptionToken").isEqualTo("")).to("direct:stop").id("doStop")
                //Stop
                .otherwise().delay(delay).process(resumptionToken).to(ENDPOINT_OAI_REQUEST).id("doResume"); // Delay and resume

        /* Stop. Tests can override this to verify stoppage */
        from("direct:stop").routeId(ROUTE_OAI_STOP).process(e -> LOG.info("Terminating harvest")).stop();
    }

    static final Processor resumptionToken = (e -> {
        String resumptionToken = e.getIn().getHeader("oai.resumptionToken", String.class);
        if (resumptionToken != null) {
            URIBuilder newURI = new URIBuilder(baseURI(e.getIn().getHeader(Exchange.HTTP_URI, String.class)))
                    .setParameter(OAI_PARAM_VERB, OAI_VERB_LIST_RECORDS)
                    .setParameter(OAI_PARAM_RESUMPTION_TOKEN, resumptionToken);
            e.getIn().setHeader(Exchange.HTTP_URI, newURI.build().toString());
        }
    });

    private static String baseURI(String uri) {

        return uri.contains("?") ? uri.substring(0, uri.indexOf("?")) : uri;
    }

    private class DelayExpression
            implements Expression {

        @Override
        public <T> T evaluate(Exchange exchange, Class<T> type) {
            Long delay = retryDelay;
            String retry = exchange.getIn().getHeader("Retry-After", String.class);

            if (retry.matches("^\\d+$")) {
                /* Number of seconds. From rfc2616 */
                delay = Long.parseLong(retry) * 1000;
            } else {
                /* Http Date. From rfc2615 */
                delay = DateUtils.parseDate(retry).getTime() - new Date().getTime();
            }

            return constant(delay).evaluate(exchange, type);
        }

    }

}
