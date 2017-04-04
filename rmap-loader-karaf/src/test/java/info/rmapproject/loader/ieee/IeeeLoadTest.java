/*
 * Copyright 2013 Johns Hopkins University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.rmapproject.loader.ieee;

import static info.rmapproject.loader.camel.ContextHelper.fix;
import static info.rmapproject.loader.camel.Lambdas.expression;
import static info.rmapproject.loader.camel.impl.file.FileDriver.ENDPOINT_PROCESS_FILE;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.ExplicitCamelContextNameStrategy;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHeaders;
import org.junit.Ignore;
import org.junit.Test;

import info.rmapproject.loader.camel.impl.file.FileDriver;

public class IeeeLoadTest extends CamelTestSupport {

    @Produce
    protected ProducerTemplate template;

    @EndpointInject(uri = "mock:out")
    private MockEndpoint mock_out;

    private final SimpleRegistry registry = new SimpleRegistry();

    private CamelContext parentContext;

    private static String HEADER_SEQ_NUMBER = "sequence.number";

    private static String HEADER_THROTTLE_RATE_MIN = "throttle.per.min";

    private static int PAUSE_EVERY_N_RECORDS = 15000;

    private static int DEFAULT_THROTTLE_RATE_MIN = 50 * 60;

    @Test
    @Ignore
    public void zipTest() throws Exception {

        final AtomicInteger count = new AtomicInteger();

        parentContext.addRoutes(new RouteBuilder() {

            File dir = new File("/data/t/discos-10-15");

            @Override
            public void configure() throws Exception {

                from("file:" + dir + "?delete=true&include=.*\\.zip$").to("direct:" + ENDPOINT_PROCESS_FILE);

                from("direct:out").process(e -> {
                    e.getIn().setBody(e.getIn().getBody(String.class).replace(
                            "http://rmap-project.org/rmap/terms/1.0/Disco\"/>",
                            "http://rmap-project.org/rmap/terms/DiSCO\"/><dcterms:creator rdf:resource=\"http://rmap-project.org/rmap/agent/IEEELoader\"/>"));
                }).setHeader(Exchange.CONTENT_TYPE, constant("application/vnd.rmap-project.disco+rdf+xml"))
                        .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                        .setHeader(HttpHeaders.AUTHORIZATION,
                                constant("Basic " + Base64.encodeBase64String(
                                        "QXI9XA2QzjuyIHySG1PReut2pSnHW0a5:cir8eg8I6rXuCOqwmARmbYhs929Q2gIrEhs9NP9erm1J0SOj8pXyt0AprqL4Jtey"
                                                .getBytes())))
                        .process(e -> e.getIn().setHeader(HEADER_SEQ_NUMBER, count.incrementAndGet()))
                        .to("direct:count");

                from("direct:count").process(e -> {
                    if (e.getIn().getHeader(HEADER_SEQ_NUMBER, Integer.class) % 100 == 0) {
                        System.out.println(e.getIn().getHeader(HEADER_SEQ_NUMBER));
                    }
                });

                from("direct:skip").filter(e -> e.getIn().getHeader(HEADER_SEQ_NUMBER, Integer.class) > -1)
                        .setHeader(HEADER_THROTTLE_RATE_MIN,
                                expression(e -> e.getIn().getHeader(HEADER_SEQ_NUMBER, Integer.class) %
                                        PAUSE_EVERY_N_RECORDS == 0 ? 1 : DEFAULT_THROTTLE_RATE_MIN))
                        .throttle(header(HEADER_THROTTLE_RATE_MIN)).timePeriodMillis(30 * 1000).to("direct:queue");

                from("direct:queue").enrich("direct:_do_http_op", ((orig, http) -> {
                    http.getIn().getHeaders().entrySet()
                            .forEach(e -> orig.getIn().getHeaders().put(e.getKey(), e.getValue()));
                    if (http.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class) > 299) {
                        System.err.println(http.getIn().getBody(String.class));
                    }
                    return orig;
                })).choice().when(e -> (e.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class) > 299))
                        .process(e -> e.getIn().setHeader(Exchange.FILE_NAME,
                                e.getIn().getHeader("CamelFileNameConsumed", String.class) + e.getIn().getHeader(
                                        Exchange.FILE_NAME)))
                        .to("file:/data/fail").end().process(e -> {

                            if (e.getIn().getHeader(HEADER_SEQ_NUMBER, Integer.class) % 100 == 0) {
                                System.out.println(e.getIn().getHeader(HEADER_SEQ_NUMBER));
                            }
                        });

                /* Sanitize headers and perform an HTTP operation */
                from("direct:_do_http_op").to("direct:_sanitize_headers")
                        .to("jetty:https://test.rmap-project.org/apitest/discos/?throwExceptionOnFailure=false");

                /*
                 * Strip all headers except for certain HTTP headers used in requests
                 */
                from("direct:_sanitize_headers").id("lsp-sanitize-headers").removeHeaders("*", Exchange.HTTP_URI,
                        Exchange.CONTENT_TYPE, Exchange.HTTP_METHOD, HttpHeaders.ACCEPT, HttpHeaders.AUTHORIZATION,
                        HttpHeaders.CONTENT_ENCODING, HttpHeaders.IF_MATCH, "Content-Disposition", "Slug");

            }

        });

        synchronized (this) {
            wait();
        }

    }

    @Override
    protected CamelContext createCamelContext() throws Exception {

        parentContext = newBlackBoxContext(new FileDriver(), "fileDriver");

        return parentContext;
    }

    private CamelContext newBlackBoxContext(RoutesBuilder routes, String id) throws Exception {
        final CamelContext cxt = fix(new DefaultCamelContext(registry));
        cxt.setNameStrategy(new ExplicitCamelContextNameStrategy(id));
        cxt.addRoutes(routes);
        cxt.start();

        registry.put(id, cxt);
        return cxt;

    }

}
