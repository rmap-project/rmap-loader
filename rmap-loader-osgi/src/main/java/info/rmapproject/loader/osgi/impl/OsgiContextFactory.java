
package info.rmapproject.loader.osgi.impl;

import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.core.osgi.OsgiDefaultCamelContext;
import org.apache.camel.core.osgi.utils.BundleDelegatingClassLoader;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultShutdownStrategy;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.impl.SimpleRegistry;

import org.osgi.framework.BundleContext;
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

    ContextFixerPublisher publisher;

    SimpleRegistry registry = new SimpleRegistry();

    private BundleContext bundleContext;

    @Activate
    public void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        publisher = new ContextFixerPublisher(bundleContext);
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

            context = new OsgiDefaultCamelContext(bundleContext, registry);
            context.setApplicationContextClassLoader(new BundleDelegatingClassLoader(bundleContext.getBundle()));
            Thread.currentThread().setContextClassLoader(context.getApplicationContextClassLoader());
            
            DefaultShutdownStrategy shutdown = new DefaultShutdownStrategy();
            shutdown.setTimeout(10);
            context.setShutdownStrategy(shutdown);

        } else {
            context = new DefaultCamelContext();
        }

        if (publisher != null) {
            context.getManagementStrategy().addEventNotifier(publisher);
        }

        if (id != null && id != "") {
            context.setNameStrategy(new ExplicitOsgiCamelContextNameStrategy(bundleContext, id));
        }

        context.setUseMDCLogging(true);
        context.setUseBreadcrumb(true);

        if (routes != null) {
            try {
                routes.addRoutesToCamelContext(context);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        registry.put(context.getName(), context);

        return context;
    }

    @Override
    public void disposeContext(CamelContext context) {
        if (context == null) return;

        /* Stop the context */
        try {
            context.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
