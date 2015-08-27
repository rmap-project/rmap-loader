
package info.rmapproject.loader.experimental;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.impl.ExplicitCamelContextNameStrategy;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.camel.util.ObjectHelper;
import org.junit.Test;

public class ContextComponentTest
        extends CamelTestSupport {

    @Produce
    protected ProducerTemplate template;

    //@EndpointInject(uri = "mock:end")
    private MockEndpoint mock_end;

    private JndiRegistry registry;

    private CamelContext parentContext;

    @Test
    public void testBlackBoxRoutes() throws Exception {

        /* CamelContext names for the black box contexts */
        final String BLACK_BOX_1 = "bb1";
        final String BLACK_BOX_2 = "bb2";

        /* These are the context component URIs (e.g. bb1:in, bb2:out, etc) */
        final String BLACK_BOX_1_IN = BLACK_BOX_1 + ":in";
        final String BLACK_BOX_1_OUT = BLACK_BOX_1 + ":out";
        final String BLACK_BOX_2_IN = BLACK_BOX_2 + ":in";
        final String BLACK_BOX_2_OUT = BLACK_BOX_2 + ":out";

        /* Create first black box context */
        newBlackBoxContext(new RouteBuilder() {

            @Override
            public void configure() throws Exception {
                from("direct:in").setBody(constant("one")).to("direct:out");

            }
        }, BLACK_BOX_1);

        /* Create second black box context */
        newBlackBoxContext(new RouteBuilder() {

            @Override
            public void configure() throws Exception {
                from("direct:in").setBody(constant("two")).to("direct:out");

            }
        }, BLACK_BOX_2);

        /* Now wire the two black boxes together within our parent context */
        parentContext.addRoutes(new RouteBuilder() {

            @Override
            public void configure() throws Exception {
                from("direct:start").to(BLACK_BOX_1_IN);
                from(BLACK_BOX_1_OUT).to(BLACK_BOX_2_IN);
                from(BLACK_BOX_2_OUT).to("direct:end");
            }
        });

        /*
         * Send a message to 'direct:start'. It should be routed through the
         * black boxes, and end up in mock:end
         */
        template.sendBody("testing", "direct:start");

        //mock_end.expectedMessageCount(1);

    }

    private CamelContext newBlackBoxContext(RoutesBuilder routes, String id)
            throws Exception {
        CamelContext cxt = fix(new DefaultCamelContext(registry));
        cxt.setNameStrategy(new ExplicitCamelContextNameStrategy(id));
        cxt.addRoutes(routes);
        cxt.start();

        registry.bind(id, cxt);
        return cxt;

    }

    protected JndiRegistry createRegistry() throws Exception {

        return registry = super.createRegistry();
    }

    @Override
    protected CamelContext createCamelContext() throws Exception {

        return parentContext = fix(super.createCamelContext());
    }

    private CamelContext fix(final CamelContext cxt) {

        return (CamelContext) Proxy
                .newProxyInstance(CamelContext.class.getClassLoader(),
                                  new Class[] {CamelContext.class, ModelCamelContext.class},
                                  new InvocationHandler() {

                                      @Override
                                      public Object invoke(Object proxy,
                                                           Method method,
                                                           Object[] args)
                                              throws Throwable {
                                          
                                          System.out.println(cxt.getName() + ": " + method.getName());
                                          if (method.getName()
                                                  .equals("getEndpoint")) {
                                              final Endpoint delegate =
                                                      (Endpoint) method
                                                              .invoke(cxt, args);

                                              return (Endpoint) Proxy
                                                      .newProxyInstance(Endpoint.class
                                                                                .getClassLoader(),
                                                                        new Class[] {Endpoint.class},
                                                                        new InvocationHandler() {

                                                                            @Override
                                                                            public Object invoke(Object proxy,
                                                                                                 Method method,
                                                                                                 Object[] args)
                                                                                    throws Throwable {

                                                                                if (method
                                                                                        .getName()
                                                                                        .equals("equals")
                                                                                        && args.length == 1) {
                                                                                    
                                                                                    System.out.println("Equals? ");

                                                                                    if (args[0] instanceof DefaultEndpoint) {
                                                                                        DefaultEndpoint that =
                                                                                                (DefaultEndpoint) args[0];
                                                                                        return ObjectHelper
                                                                                                .equal(delegate.getEndpointUri(),
                                                                                                       that.getEndpointUri())
                                                                                                && ObjectHelper
                                                                                                        .equal(delegate.getCamelContext()
                                                                                                                       .getName(),
                                                                                                               that.getCamelContext()
                                                                                                                       .getName());
                                                                                    }
                                                                                    return false;

                                                                                }
                                                                                return method
                                                                                        .invoke(delegate,
                                                                                                args);
                                                                            }

                                                                        });
                                          }
                                          return method.invoke(cxt, args);
                                      }
                                  });
    }
}
