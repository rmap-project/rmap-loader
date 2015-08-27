
package info.rmapproject.loader.framework.impl;

import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import info.rmapproject.loader.camel.ContextFactory;
import info.rmapproject.loader.model.RecordInfo;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * Main loader framework.
 * <p>
 * Provides a CamelContext and routes for plugging in extractors, transformers,
 * and loaders as well as managing queues of records between them.
 * </p>
 * 
 * @author apb18
 */
@Component(configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true)
public class LoaderFramework {

    public static final String HEADER_EXTRACT_INFO = "record.extracted";

    public static final String HEADER_TRANSFORM_INFO = "record.transformed";

    public static final String HEADER_FORMAT = "format";

    /** Service property which identifies the queue for extracted records */
    public static final String CONFIG_EXTRACTED_QUEUE_URI = "queue.extracted";

    /** Service property which identifies the queue for transformed records */
    public static final String CONFIG_TRANSFORMED_QUEUE_URI =
            "queue.transformed";

    /**
     * Service property which identifies a domain model.
     * <p>
     * This names the property that contains the name of a domain model.
     * Transformers produce records within some domain model, and loaders load
     * records from a domain model. This property names the relevant model (e.g.
     * RMap-DiSCO).
     * </p>
     */
    public static final String PROPERTY_DOMAIN_MODEL = "loader.domain";

    public static final String PROPERTY_EXTRACTED_FORMAT = "loader.format";

    private ContextFactory factory;

    private CamelContext cxt;

    private Map<RoutesBuilder, CamelContext> blackBoxContexts =
            new ConcurrentHashMap<>();

    private Queue<RoutesBuilder> wiringQueue = new ConcurrentLinkedQueue<>();

    private String extractedQueueURI;

    private String transformedQueueURI;

    @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC)
    public void setContextFactory(ContextFactory factory) {
        this.factory = factory;
    }

    /** Create a CamelContext for the framework, and start it */
    @Activate
    public void start(Map<String, String> config) {

        cxt = factory.newContext();

        extractedQueueURI = config.get(CONFIG_TRANSFORMED_QUEUE_URI);
        transformedQueueURI = config.get(CONFIG_TRANSFORMED_QUEUE_URI);

        try {
            cxt.start();
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    /** Shut down framework CamelContext */
    @Deactivate
    public void stop() {
        try {
            for (RoutesBuilder r : blackBoxContexts.keySet()) {
                removeRoutes(r);
            }
            cxt.stop();
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    /**
     * Add and run the given routes which fulfill the role of an extractor.
     * <p>
     * At least one of the given routes must terminate in a local endpoint named
     * 'out' (e.g. direct:out). Messages sent to the 'out' endpoint MUST contain
     * a body containing an extracted record. It SHOULD contain a header with
     * field {@link #HEADER_EXTRACT_INFO} of type {@link RecordInfo}. It MAY
     * also contain a header {@link #HEADER_FORMAT} to indicate the specific
     * format of an extracted record. If not present, the value of the service
     * property {@link #PROPERTY_EXTRACTED_FORMAT} will be used. If neither are
     * present, the format will be assumed to have value
     * <code>unspecified</code>
     * </p>
     * 
     * @param routes
     *        One or more camel routes.
     * @param properties
     *        If it contains a {@link #PROPERTY_EXTRACTED_FORMAT}, result
     *        records will be assumed to be in that format, unless overridden by
     *        the value in the header {@link #HEADER_FORMAT} present in
     *        individual messages emitted by the extractor routes.
     */
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, target = "(loader.role=extractor)", unbind = "removeRoutes")
    public void addExtractorRoutes(RoutesBuilder routes,
                                   Map<String, String> properties) {

        /* First create a black box camel context */
        CamelContext extractorCxt = factory.newContext(routes);
        blackBoxContexts.put(routes, extractorCxt);

        try {
            extractorCxt.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        addExtractor(extractorCxt, properties);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, target = "(loader.role=extractor)", unbind = "removeRoutes")
    public void addExtractor(CamelContext extractorCxt,
                             Map<String, String> properties) {

        /* Add a route from the context to the queue */
        System.out.println("Wiring in extractor context "
                + extractorCxt.getName());
        System.out.println(extractorCxt.getEndpointMap().keySet());
        wire(extractorCxt,
             new QueueSpec().to(extractedQueueURI).withFormat(properties
                     .get(PROPERTY_EXTRACTED_FORMAT)));
    }

    /**
     * Add and run the given routes which fulfill the role of a transformer.
     * <p>
     * At least one of the given routes must terminate in a local endpoint named
     * 'out' (e.g. direct:out). Messages sent to this output endpoint MUST
     * contain a body containing an instance of a domain model
     */
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, target = "(&(loader.role=transformer)(loader.format=*))", unbind = "removeRoutes")
    public void addTransformerRoutes(RoutesBuilder routes,
                                     Map<String, String> properties) {

        /* First create a black box camel context */
        CamelContext transformerCxt = factory.newContext(routes);
        blackBoxContexts.put(routes, transformerCxt);

        try {
            transformerCxt.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        addTransformer(transformerCxt, properties);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, target = "(&(loader.role=transformer)(loader.format=*))", unbind = "removeRoutes")
    public void addTransformer(CamelContext transformerCxt,
                               Map<String, String> properties) {

        /* Now add a route from the context to the queue */
        System.out.println("Wiring in transformer context "
                + transformerCxt.getName());
        System.out.println(transformerCxt.getEndpointMap().keySet());
        wire(transformerCxt,
             new QueueSpec().to(transformedQueueURI).from(extractedQueueURI)
                     .withFormat(properties.get(PROPERTY_EXTRACTED_FORMAT))
                     .withDomain(properties.get(PROPERTY_DOMAIN_MODEL)));
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, target = "(&(loader.role=loader)(loader.domain=*))", unbind = "removeRoutes")
    public void addLoaderRoutes(RoutesBuilder routes,
                                Map<String, String> properties) {

        /* First create a black box camel context */
        CamelContext loaderCxt = factory.newContext(routes);
        blackBoxContexts.put(routes, loaderCxt);

        try {
            loaderCxt.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        addLoader(loaderCxt, properties);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, target = "(&(loader.role=loader)(loader.domain=*))", unbind = "removeRoutes")
    public void addLoader(CamelContext loaderCxt, Map<String, String> properties) {

        /* Now add a route from the context to the queue */
        wire(loaderCxt,
             new QueueSpec().from(transformedQueueURI).withDomain(properties
                     .get(PROPERTY_DOMAIN_MODEL)));
    }

    /*
     * Add the routes that wire a black box context into the framework
     * CamelContext
     */
    private void wire(CamelContext blackBoxContext, QueueSpec routing) {
        final String blackBoxOut =
                String.format("%s:out", blackBoxContext.getName());

        final String blackBoxIn =
                String.format("%s:in", blackBoxContext.getName());

        /* Now route it */
        RouteBuilder wiring = new RouteBuilder() {

            @Override
            public void configure() throws Exception {

                if (routing.from != null) {
                    System.out
                            .println(String
                                    .format("Wiring in route (%s) from %s to %s",
                                            getWiringRouteId(Direction.TO,
                                                             blackBoxContext),
                                            routingURI(routing.from,
                                                       Optional.ofNullable(routing.format)
                                                               .orElse(Optional
                                                                       .ofNullable(routing.domain)
                                                                       .orElse("unspecified"))),
                                            blackBoxIn));
                    from(routingURI(routing.from,
                                    Optional.ofNullable(routing.format)
                                            .orElse(Optional
                                                    .ofNullable(routing.domain)
                                                    .orElse("unspecified"))))
                            .id(getWiringRouteId(Direction.TO, blackBoxContext))
                            .to(blackBoxIn);
                }

                System.out.println(String
                        .format("Wiring in route (%s) from %s to %s",
                                getWiringRouteId(Direction.FROM,
                                                 blackBoxContext),
                                blackBoxOut,
                                "header('dest')"));

                if (routing.to != null) {
                    from(blackBoxOut)
                            .id(getWiringRouteId(Direction.FROM,
                                                 blackBoxContext))
                            .process(setDest(routing))
                            .recipientList(header("dest"));
                }

            }
        };

        try {
            /* Add routes, or queue them if the context isn't up yet */
            if (cxt != null) {
                cxt.addRoutes(wiring);
            } else {
                wiringQueue.add(wiring);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void removeRoutes(RoutesBuilder routes) {
        CamelContext cxtToStop = blackBoxContexts.remove(routes);
        try {
            if (cxt.getRoute(getWiringRouteId(Direction.FROM, cxtToStop)) != null) {
                cxt.removeRoute(getWiringRouteId(Direction.FROM, cxtToStop));
            }

            if (cxt.getRoute(getWiringRouteId(Direction.TO, cxtToStop)) != null) {
                cxt.removeRoute(getWiringRouteId(Direction.TO, cxtToStop));
            }
        } catch (Exception e) {
            /* log, or something */
        } finally {
            try {
                cxtToStop.stop();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static String getWiringRouteId(Direction direction,
                                           CamelContext blackBox) {
        return String.format("wiring-%s-%s",
                             direction.toString(),
                             blackBox.getName());
    }

    private static String routingURI(String uri, String format) {
        return uri.replace("$format", format);
    }

    private static Processor setDest(QueueSpec routing) {
        return new Processor() {

            @Override
            public void process(Exchange exchange) throws Exception {

                String modifier = "unspecified";

                if (routing.to != null && routing.from != null) {
                    /* We're sending to a domain destination */
                    modifier =
                            exchange.getIn()
                                    .getHeader("domain",
                                               Optional.ofNullable(routing.domain)
                                                       .orElse(modifier),
                                               String.class);
                } else {

                    modifier =
                            exchange.getIn()
                                    .getHeader("format",
                                               Optional.ofNullable(routing.format)
                                                       .orElse(modifier),
                                               String.class);
                }

                System.err.println("Setting dest to "
                        + routingURI(routing.to, modifier));
                exchange.getIn().setHeader("dest",
                                           routingURI(routing.to, modifier));
            }
        };

    }

    private static class QueueSpec {

        private String from;

        private String to;

        private String format;

        private String domain;

        private QueueSpec from(String uri) {
            from = uri;
            return this;
        }

        private QueueSpec to(String uri) {
            to = uri;
            return this;
        }

        private QueueSpec withFormat(String format) {
            this.format = format;
            return this;
        }

        private QueueSpec withDomain(String domain) {
            this.domain = domain;
            return this;
        }
    }

    private enum Direction {
        TO, FROM
    }

}
