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
import org.apache.camel.model.dataformat.ZipFileDataFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZipFileHarvestService
        extends RouteBuilder {

    static Logger LOG = LoggerFactory.getLogger(ZipFileHarvestService.class);

    EnhancedZipDataFormat zipFormat = new EnhancedZipDataFormat();

    ZipFileDataFormat fmt = new ZipFileDataFormat();

    private String src;

    private String dest;

    private String contentType = "application/xml";

    public void setSrcUri(String src) {
        this.src = src;
    }

    public void setDestUri(String dest) {
        this.dest = dest;
    }

    public void setOutputContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public void configure() throws Exception {
        fmt.setUsingIterator(true);

        from(src).choice()
                .when(e -> e.getIn().getHeader(Exchange.FILE_NAME, String.class).endsWith(".zip"))
                .unmarshal(zipFormat)
                .split(enhancedZipSplitter).streaming().to("direct:recordInfo").end().endChoice()
                .otherwise().to("direct:recordInfo");

        from("direct:recordInfo")
                .setHeader(Exchange.CONTENT_TYPE,
                        constant(contentType))
                .to(dest);
    }

    private static final Expression enhancedZipSplitter = new Expression() {

        public Object evaluate(Exchange exchange) {
            final Message inputMessage = exchange.getIn();
            return new EnhancedZipIterator(inputMessage);
        }

        @Override
        public <T> T evaluate(Exchange exchange, Class<T> type) {
            final Object result = evaluate(exchange);
            return exchange.getContext().getTypeConverter().convertTo(type, exchange, result);
        }
    };
}
