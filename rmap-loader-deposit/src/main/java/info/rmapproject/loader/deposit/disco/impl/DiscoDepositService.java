
package info.rmapproject.loader.deposit.disco.impl;

import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http4.HttpClientConfigurer;
import org.apache.camel.component.http4.HttpComponent;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

@Component(service = RoutesBuilder.class, configurationPolicy = ConfigurationPolicy.REQUIRE, property = {
        "loader.domain=DiSCO", "loader.role=loader"})
public class DiscoDepositService
        extends RouteBuilder {

    public static final String CONFIG_DISCO_API_URI = "rmap.disco.api.uri";

    public static final String CONFIG_DEPOSIT_THREADS = "deposit.threads";

    public static final String CONFIG_HTTP_SUCCESS = "deposit.success.httpcode";

    private int loaderThreads = 1;

    private int http_success = 201;

    private String discoApiURI;

    @Activate
    public void configure(Map<String, String> config) {
        discoApiURI = config.get(CONFIG_DISCO_API_URI);
        loaderThreads = Integer.valueOf(config.get(CONFIG_DEPOSIT_THREADS));

        if (config.containsKey(CONFIG_HTTP_SUCCESS)) {
            http_success = Integer.valueOf(config.get(CONFIG_HTTP_SUCCESS));
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

        from(String.format("seda:in?concurrentConsumers=%d", loaderThreads))
                // TODO:  When we figure out provenance, decide how to include extraction/transform data
                .setHeader(Exchange.HTTP_URI, constant(discoApiURI))
                .to("http4:disco-host?throwExceptionOnFailure=false")
                .choice()
                //
                .when(header(Exchange.HTTP_RESPONSE_CODE)
                        .isNotEqualTo(constant(http_success))).to("direct:err");
        // TODO:  Think about if we need to record the URI in the result.  Ideally, we could just
        // query the API, and not maintain a local map.  This is why we don't do anything here right now

    }
}
