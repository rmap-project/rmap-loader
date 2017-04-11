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

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.ZipFileDataFormat;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.rmapproject.loader.camel.ContextFactory;

@ObjectClassDefinition
@interface ZipFileHarvestConfig {

    String srcUri() default "file:.?delete=true&include=.*\\.zip$";

    String destUri() default "activemq:queue:rmap.dest_fmt.out";

    String contentType() default "application/xml";
}

@Component(configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true)
@Designate(ocd = ZipFileHarvestConfig.class, factory = true)
public class ZipFileHarvestService
        extends RouteBuilder {

    static Logger LOG = LoggerFactory.getLogger(ZipFileHarvestService.class);

    EnhancedZipDataFormat zipFormat = new EnhancedZipDataFormat();

    ZipFileDataFormat fmt = new ZipFileDataFormat();

    private String src;

    private String dest;

    private String contentType = "application/xml";

    private CamelContext cxt;

    private ContextFactory contextFactory;

    public void setSrcUri(String src) {
        this.src = src;
    }

    public void setDestUri(String dest) {
        this.dest = dest;
    }

    @Reference
    public void setContextFactory(ContextFactory factory) {
        this.contextFactory = factory;
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

    @Activate
    public void start(ZipFileHarvestConfig config) {
        setSrcUri(config.srcUri());
        setDestUri(config.destUri());
        setOutputContentType(config.contentType());
        start();
    }

    public void start() {
        try {
            this.cxt = contextFactory.newContext();
            addRoutesToCamelContext(this.cxt);
            this.cxt.start();
        } catch (final Exception e) {
            throw new RuntimeException("Could not create camel context", e);
        }
    }

    @Deactivate
    public void stop() throws Exception {
        if (cxt != null) {
            cxt.stop();
        }
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
