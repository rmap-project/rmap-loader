
package info.rmapproject.loader.camel;

import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Processor;

public abstract class Lambdas {

    public static Processor processor(final Consumer<Exchange> consumer) {
        return new Processor() {

            @Override
            public void process(Exchange exchange) throws Exception {
                consumer.accept(exchange);
            }
        };
    }

    public static Expression expression(final Function<Exchange, ?> f) {
        return new Expression() {

            @SuppressWarnings("unchecked")
            @Override
            public <T> T evaluate(Exchange exchange, Class<T> type) {
                return (T) f.apply(exchange);
            }
        };
    }
}
