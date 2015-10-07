
package info.rmapproject.loader.osgi.impl;

import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.core.osgi.OsgiDefaultCamelContext;
import org.apache.camel.core.osgi.utils.BundleDelegatingClassLoader;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.ExplicitCamelContextNameStrategy;
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

            context = new OsgiDefaultCamelContext(bundleContext);
            context.setApplicationContextClassLoader(new BundleDelegatingClassLoader(bundleContext.getBundle()));
            Thread.currentThread().setContextClassLoader(context.getApplicationContextClassLoader());

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
            try {
                routes.addRoutesToCamelContext(context);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

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
