
package info.rmapproject.loader.experimental;

import org.apache.camel.RoutesBuilder;

public class JenaStreamTransformTest
        extends TransformTest {

    @Override
    protected RoutesBuilder getRoutes() {
        return new JenaStreamTransform();
    }

}
