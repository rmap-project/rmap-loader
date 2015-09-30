
package info.rmapproject.loader.camel.impl.oai;

import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import info.rmapproject.loader.HarvestService;
import info.rmapproject.loader.camel.ContextConsumer;
import info.rmapproject.loader.camel.ContextFactory;

@Component(service = {HarvestService.class, ContextConsumer.class})
public class OaiHarvestService
        implements HarvestService, ContextConsumer {

    private ContextFactory cxtFactory;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    public void setContextFactory(ContextFactory factory) {
        cxtFactory = factory;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, target = "(&(role=oai)(!(oneTime=true)))")
    public void addHarvest(RoutesBuilder routes) throws Exception {
        CamelContext context = cxtFactory.newContext(routes);
        addCamelContext(context);
    }

    public void removeHarvest(RoutesBuilder routes) {
        
    }

    @Reference()
    public void addCamelContext(CamelContext context) {

    }
}
