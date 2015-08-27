
package info.rmapproject.loader.experimental;

import org.apache.camel.RoutesBuilder;

public class JenaModelTransformTest
        extends TransformTest {

    @Override
    protected RoutesBuilder getRoutes() {
        return new JenaModelTransform();
    }
    
}
