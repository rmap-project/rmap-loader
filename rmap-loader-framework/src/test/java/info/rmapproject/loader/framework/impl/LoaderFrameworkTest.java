
package info.rmapproject.loader.framework.impl;

import java.util.HashMap;
import java.util.Map;

import info.rmapproject.loader.camel.ContextFactory;
import info.rmapproject.loader.camel.impl.BasicContextFactory;
import info.rmapproject.loader.framework.impl.LoaderFramework;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.ExplicitCamelContextNameStrategy;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

@SuppressWarnings("serial")
public class LoaderFrameworkTest
        extends CamelTestSupport {

    @Produce
    protected ProducerTemplate template;

    @EndpointInject(uri = "mock:end")
    private MockEndpoint mock_end;

    private static final String EXTRACTED_FORMAT = "exf";

    private static final String DOMAIN_MODEL = "dom";

    private static final String TEST_CONTEXT_ID = "testCxt";

    private JndiRegistry registry;

    /*
     * Verifies that extractor routes are run, and sent to the correct queue
     * based on format
     */
    @Test
    public void fromExtractorToQueueTest() {

        ContextFactory cxtFactory = new BasicContextFactory(registry);

        /* Initialize the framework */
        LoaderFramework fwk = new LoaderFramework();
        fwk.setContextFactory(cxtFactory);

        Map<String, String> config = new HashMap<>();
        config.put(LoaderFramework.CONFIG_EXTRACTED_QUEUE_URI,
                   "file:extracted/$format");
        config.put(LoaderFramework.CONFIG_TRANSFORMED_QUEUE_URI,
                   "file:translated/$format");

        fwk.start(config);

        fwk.addExtractorRoutes(new RouteBuilder() {

            @Override
            public void configure() throws Exception {
                from(String.format("%s:input", TEST_CONTEXT_ID)).loop(2)
                        .setBody(constant("extractor")).to("direct:out");

            }
        }, new HashMap<String, String>() {

            {
                put(LoaderFramework.PROPERTY_EXTRACTED_FORMAT, EXTRACTED_FORMAT);
            }
        });

        fwk.addTransformerRoutes(new RouteBuilder() {

            @Override
            public void configure() throws Exception {
                from("direct:in").setBody(constant("transformed"))
                        .to("direct:out");

            }
        },
                                 new HashMap<String, String>() {

                                     {
                                         put(LoaderFramework.PROPERTY_EXTRACTED_FORMAT,
                                             EXTRACTED_FORMAT);
                                         put(LoaderFramework.PROPERTY_DOMAIN_MODEL,
                                             DOMAIN_MODEL);
                                     }

                                 });

        /*
         * Add this as a context, as we want to inspect the output message by
         * routing it to a mock
         */
        fwk.addLoaderRoutes(new RouteBuilder() {

            @Override
            public void configure() throws Exception {
                from("direct:in").to(String
                        .format("%s:output", TEST_CONTEXT_ID));

            }
        },
                            new HashMap<String, String>() {

                                {
                                    put(LoaderFramework.PROPERTY_EXTRACTED_FORMAT,
                                        EXTRACTED_FORMAT);
                                }
                            });

        template.sendBody("direct:start", "start");

    }

    protected JndiRegistry createRegistry() throws Exception {
        if (registry == null) {
            registry = super.createRegistry();
        }
        return registry;
    }

    @Override
    protected CamelContext createCamelContext() throws Exception {

        /*
         * For some reason, CamelTestSupport creates the camel context before
         * the registry! So we need to make sure it's created.
         */
        createRegistry();

        System.out.println("Creatig test context");
        CamelContext testCxt = super.createCamelContext();
        testCxt.setNameStrategy(new ExplicitCamelContextNameStrategy(TEST_CONTEXT_ID));

        registry.bind(testCxt.getName(), testCxt);

        return testCxt;
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {

        return new RouteBuilder() {

            public void configure() {
                from("direct:start").to("direct:input");
                from("direct:output").to("mock:end");
            }
        };
    }
}
