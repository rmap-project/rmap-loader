/*
 * Copyright 2013 Johns Hopkins University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.rmapproject.loader.camel.impl.file;


import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileDriver
        extends RouteBuilder {

    static Logger LOG = LoggerFactory.getLogger(FileDriver.class);

    EnhancedZipDataFormat zipFormat = new EnhancedZipDataFormat();

    public static final String ENDPOINT_PROCESS_FILE = "file-process";

    @Override
    public void configure() throws Exception {

        from("direct:" + ENDPOINT_PROCESS_FILE).streamCaching().choice()
                .when(e -> e.getIn().getHeader(Exchange.FILE_NAME, String.class).endsWith(".zip")).unmarshal(zipFormat)
                .split(enhancedZipSplitter).streaming().to("direct:out").end().endChoice()
                .otherwise().to("direct:out");

    }

    private static final Expression enhancedZipSplitter = new Expression() {

        public Object evaluate(Exchange exchange) {
            Message inputMessage = exchange.getIn();
            return new EnhancedZipIterator(inputMessage);
        }

        @Override
        public <T> T evaluate(Exchange exchange, Class<T> type) {
            Object result = evaluate(exchange);
            return exchange.getContext().getTypeConverter().convertTo(type, exchange, result);
        }
    };
}
