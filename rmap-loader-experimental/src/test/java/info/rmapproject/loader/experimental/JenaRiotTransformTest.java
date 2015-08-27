
package info.rmapproject.loader.experimental;

import org.apache.camel.RoutesBuilder;

public class JenaRiotTransformTest
        extends TransformTest {

    @Override
    protected RoutesBuilder getRoutes() {
        return new JenaRiotTransform();
    }

}
