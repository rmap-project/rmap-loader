
package info.rmapproject.loader.deposit.disco;

import static info.rmapproject.loader.camel.Lambdas.expression;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;

public class DiscoDepositService
        extends RouteBuilder {

    public static final String CONFIG_DISCO_API_URI = "rmap.disco.api.uri";

    public static final String CONFIG_DEPOSIT_THREADS = "deposit.threads";

    public static final String CONFIG_HTTP_SUCCESS = "deposit.success.httpcode";

    private static int PAUSE_EVERY_N_RECORDS = 15000;

    private static int DEFAULT_THROTTLE_RATE_MIN = 50 * 60;

    private static String HEADER_SEQ_NUMBER = "sequence.number";

    private static String HEADER_THROTTLE_RATE_MIN = "throttle.per.min";

    private static final String FAIL_QUEUE = "queue.fail";

    private final AtomicLong count = new AtomicLong(0);

    @Override
    public void configure() throws Exception {

        System.err.println("CONFIGURING");

        from("{{disco.src.uri}}?concurrentconsumers={{disco.deposit.threads}}").id("disco-load")
                .setHeader(Exchange.CONTENT_TYPE).simple(
                        "{{disco.content.type}}")
                .process(e -> System.err.println("MESSAGE"))
                .to("direct:throttle")
                .to("{{disco.rmap.uri}}&authUsername={{auth.user}}&authPassword={{auth.passwd}}");

        from("direct:throttle").id("disco-load-throttle")
                .process(LABEL_WITH_SEQUENCE_NUMBER)
                .setHeader(HEADER_THROTTLE_RATE_MIN,
                        expression(e -> e.getIn().getHeader(HEADER_SEQ_NUMBER, Integer.class) %
                                PAUSE_EVERY_N_RECORDS == 0 ? 1 : DEFAULT_THROTTLE_RATE_MIN))
                .throttle(header(HEADER_THROTTLE_RATE_MIN)).timePeriodMillis(60 * 1000);

        from("direct:error").id("disco-load-error").routingSlip(header(FAIL_QUEUE));

    }

    final Processor LABEL_WITH_SEQUENCE_NUMBER = e -> e.getIn().setHeader(HEADER_SEQ_NUMBER, count.incrementAndGet());
}
