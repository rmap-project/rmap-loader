
package info.rmapproject.loader.experimental;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import javax.naming.Context;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.dataset.DataSet;
import org.apache.camel.component.dataset.DataSetEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

public abstract class TransformTest
        extends CamelTestSupport {

    private static final long DATASET_SIZE = 10;

    private int length = 0;

    private static final AtomicLong elapsed = new AtomicLong(0);

    protected abstract RoutesBuilder getRoutes();

    private DataSet data = new DataSet() {

        private final String content = new DiscoRandomizer().getRandomDisco();

        @Override
        public void populateMessage(Exchange exchange, long messageIndex)
                throws Exception {
            exchange.getIn().setBody(content);
        }

        @Override
        public long getSize() {
            return DATASET_SIZE;
        }

        @Override
        public long getReportCount() {
            return 1000;
        }

        @Override
        public void assertMessageExpected(DataSetEndpoint endpoint,
                                          Exchange expected,
                                          Exchange actual,
                                          long messageIndex) throws Exception {
            if (length == 0) {
                length = actual.getIn().getBody(String.class).length();
                assertNotEquals(length, 0);
            } else {
                assertEquals(length, actual.getIn().getBody(String.class)
                        .length());
            }
        }
    };

    @Test
    public void doTest() throws Exception {

        assertMockEndpointsSatisfied(120, TimeUnit.SECONDS);
        System.out.println(elapsed.get() / DATASET_SIZE);

    }

    @Override
    protected JndiRegistry createRegistry() throws Exception {
        JndiRegistry registry = super.createRegistry();

        DefaultCamelContext testShim = new DefaultCamelContext(registry);
        testShim.setName("testShim");
        testShim.addRoutes(getRoutes());
        testShim.start();

        registry.bind("toTest", testShim);
        return registry;

    }

    @Override
    protected Context createJndiContext() throws Exception {
        Context context = super.createJndiContext();
        context.bind("foo", data);
        return context;
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {

        return new RouteBuilder() {

            public void configure() {

                from("toTest:out").process(apply(e -> {
                    elapsed.addAndGet(System.nanoTime()
                            - e.getIn().getHeader("start", Long.class));
                })).to("dataset:foo");

                from("dataset:foo").process(apply(e -> {
                    e.getIn().setHeader("start", System.nanoTime());
                })).to("toTest:in");
            }
        };
    }

    private Processor apply(final Consumer<Exchange> consumer) {
        return new Processor() {

            @Override
            public void process(Exchange exchange) throws Exception {
                consumer.accept(exchange);
            }
        };
    }
}
