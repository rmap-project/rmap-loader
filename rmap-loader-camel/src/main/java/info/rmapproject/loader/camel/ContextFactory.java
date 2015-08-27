
package info.rmapproject.loader.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;

/**
 * Produces new camel contexts, and manages their lifecycle.
 * <p>
 * The underlying implementation is responsible for assuring that the identities
 * of all created contexts are available from each other's registry.
 * </p>
 */
public interface ContextFactory {

    /**
     * Create a new, named CamelContext whose lifecycle is bound to the given
     * RoutesBuilder.
     * 
     * @param routes
     *        The CamelContext will be loaded with the given routes. If the
     *        RoutesBuilder is disposed (e.g. in an OSGI lifeCycle, the
     *        CamelContext will be shut down and disposed as well.
     * @param id
     *        Unique identifier of the CamelContext.
     * @return
     */
    public CamelContext newContext(RoutesBuilder routes, String id);

    /**
     * Create a new CamelContext whose lifecycle is bound to the given
     * RoutesBuilder.
     * 
     * @param routes
     *        The CamelContext will be loaded with the given routes. If the
     *        RoutesBuilder is disposed (e.g. in an OSGI lifeCycle, the
     *        CamelContext will be shut down and disposed as well.
     * @param id
     *        Unique identifier of the CamelContext.
     * @return
     */
    public default CamelContext newContext(RoutesBuilder routes) {
        return newContext(routes, null);
    }

    /**
     * Creates a new, empty CamelContext.
     * 
     * @return A new CamelContext.
     */
    public default CamelContext newContext() {
        return newContext(null, null);
    }

    /**
     * Dispose a given CamelContext, and any RoutesBuilders that were involved
     * in its creation.
     * 
     * @param context
     *        A CamelContext created by this factory. If the supplied context
     *        was not created by this factory, then this just stops it.
     */
    public void disposeContext(CamelContext context);

}
