
package info.rmapproject.loader.osgi.impl;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.core.osgi.OsgiCamelContextPublisher;
import org.apache.camel.core.osgi.OsgiDefaultCamelContext;
import org.apache.camel.core.osgi.utils.BundleDelegatingClassLoader;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.ExplicitCamelContextNameStrategy;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import info.rmapproject.loader.camel.ContextFactory;

/**
 * Creates CamelContexts OSGI SCR-friendly manner.
 * 
 * @author apb18
 */
@Component
public class OsgiContextFactory
        implements ContextFactory {

    OsgiCamelContextPublisher publisher;

    private BundleContext bundleContext;

    private Map<String, CamelContext> oneTimeContexts =
            new ConcurrentHashMap<>();

    private Map<String, RoutesBuilder> oneTimeRouteBuilders =
            new ConcurrentHashMap<>();

    private Map<RoutesBuilder, ComponentInstance> oneTimeRouteBuilderComponents =
            new ConcurrentHashMap<>();

    @Activate
    public void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        publisher = new OsgiCamelContextPublisher(bundleContext);
        try {
            publisher.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CamelContext newContext(RoutesBuilder routes, String id) {
        CamelContext context;

        if (bundleContext != null) {

            context = new OsgiDefaultCamelContext(bundleContext);
            context.setApplicationContextClassLoader(new BundleDelegatingClassLoader(bundleContext
                    .getBundle()));
            Thread.currentThread()
                    .setContextClassLoader(context.getApplicationContextClassLoader());

        } else {
            context = new DefaultCamelContext();
        }

        if (publisher != null) {
            context.getManagementStrategy().addEventNotifier(publisher);
        }

        if (id != null) {
            context.setNameStrategy(new ExplicitCamelContextNameStrategy(id));
        }

        context.setUseMDCLogging(true);
        context.setUseBreadcrumb(true);

        if (routes != null) {
            oneTimeRouteBuilders.put(context.getName(), routes);
            try {
                routes.addRoutesToCamelContext(context);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            oneTimeContexts.put(context.getName(), context);
        }

        return context;
    }

    @Override
    public void disposeContext(CamelContext context) {
        if (context == null) return;

        /*
         * Remove the managed RouteBuilder from the list of manged buildders,
         * and dispose it
         */
        RoutesBuilder managedBuilder =
                oneTimeRouteBuilders.remove(context.getName());

        if (managedBuilder != null) {
            Optional.of(oneTimeRouteBuilderComponents.remove(managedBuilder))
                    .ifPresent(ComponentInstance::dispose);
        }

        /* Now stop and remove any managed contexts that were derived from it */
        try {
            context.stop();
            oneTimeContexts.remove(context.getName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
