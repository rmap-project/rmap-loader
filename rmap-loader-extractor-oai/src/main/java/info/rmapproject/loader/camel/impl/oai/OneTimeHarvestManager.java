
package info.rmapproject.loader.camel.impl.oai;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.RoutesBuilder;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

/**
 * Because OAI harvest routes are one-off, this class disposes of them when
 * requested.
 */
public class OneTimeHarvestManager {

    private Map<RoutesBuilder, ComponentInstance> managedRoutesBuilders =
            new HashMap<>();

    @Reference(service = RoutesBuilder.class, cardinality = ReferenceCardinality.MULTIPLE, target = "(role=oai)")
    public void addRoutesBuilder(ComponentContext context) {
        managedRoutesBuilders.put((RoutesBuilder) context
                .getComponentInstance().getInstance(), context
                .getComponentInstance());
    }

    public void removeRoutesBuilder(ComponentContext context) {
        managedRoutesBuilders.remove(context.getComponentInstance());
    }

    public void dispose(RoutesBuilder routes) {
        managedRoutesBuilders.get(routes).dispose();
    }
}
