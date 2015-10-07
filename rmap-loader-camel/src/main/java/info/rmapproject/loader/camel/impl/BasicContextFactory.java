
package info.rmapproject.loader.camel.impl;

import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.ExplicitCamelContextNameStrategy;
import org.apache.camel.impl.JndiRegistry;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import info.rmapproject.loader.camel.ContextFactory;

import static info.rmapproject.loader.camel.ContextHelper.fix;

@Component(configurationPolicy = ConfigurationPolicy.REQUIRE)
public class BasicContextFactory
        implements ContextFactory {

    private final JndiRegistry registry;

    public BasicContextFactory() {
        registry = new JndiRegistry();
    }

    public BasicContextFactory(JndiRegistry registry) {
        this.registry = registry;
    }

    @Override
    public CamelContext newContext(RoutesBuilder routes, String id) {

        CamelContext cxt = fix(new DefaultCamelContext(registry));

        if (id != null) {
            cxt.setNameStrategy(new ExplicitCamelContextNameStrategy(id));
        }

        try {
            if (routes != null) {
                cxt.addRoutes(routes);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        registry.bind(cxt.getName(), cxt);

        return cxt;
    }

    @Override
    public void disposeContext(CamelContext context) {
        /* Can't really unbind... */
    }

}
