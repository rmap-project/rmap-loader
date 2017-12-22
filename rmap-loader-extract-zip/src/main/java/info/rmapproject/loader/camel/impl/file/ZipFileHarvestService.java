/*
 * Copyright 2017 Johns Hopkins University
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

import static info.rmapproject.loader.jms.HarvestRecordConverter.writeRecordInfo;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.Date;
import java.util.zip.ZipEntry;

import javax.jms.Message;

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.ZipFileDataFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.rmapproject.loader.model.HarvestInfo;
import info.rmapproject.loader.model.RecordInfo;

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
        LOG.info("Generating records with content-type " + contentType);

        fmt.setUsingIterator(true);

        from(src).choice()
                .when(e -> e.getIn().getHeader(Exchange.FILE_NAME, String.class).endsWith(".zip"))
                .unmarshal(zipFormat)
                .split(enhancedZipSplitter).streaming().to("direct:recordInfo").end().endChoice()
                .otherwise().to("direct:recordInfo");

        from("direct:recordInfo")
                .process(e -> LOG.debug("Sending record from {} to {}",
                        e.getIn().getHeader(Exchange.FILE_NAME), dest))
                .process(WRITE_RECORD_INFO)
                .to(dest);
    }

    private final Processor WRITE_RECORD_INFO = e -> {

        final ZipEntry zip = e.getIn().getHeader("zip.entry", ZipEntry.class);

        final HarvestInfo hi = new HarvestInfo();
        hi.setId(URI.create("file:" + e.getIn().getHeader(Exchange.FILE_NAME_CONSUMED)));
        hi.setDate(new Date(e.getIn().getHeader(Exchange.FILE_LAST_MODIFIED, Long.class)));
        hi.setSrc(URI.create("file:" + e.getIn().getHeader(Exchange.FILE_NAME_CONSUMED)));

        final RecordInfo ri = new RecordInfo();
        ri.setId(URI.create("file:" + zip.getName()));
        ri.setContentType(contentType);
        ri.setDate(new Date(zip.getLastModifiedTime().toMillis()));
        ri.setSrc(URI.create("file:" + zip.getName()));
        ri.setHarvestInfo(hi);

        writeRecordInfo(ri, proxy((o, m, args) -> {
            e.getIn().setHeader((String) args[0], args[1]);
            return null;
        }));
    };

    private static final Message proxy(InvocationHandler h) {
        return (Message) Proxy.newProxyInstance(Message.class.getClassLoader(), new Class[] { Message.class }, h);
    }

    private static final Expression enhancedZipSplitter = new Expression() {

        public Object evaluate(Exchange exchange) {
            return new EnhancedZipIterator(exchange.getIn());
        }

        @Override
        public <T> T evaluate(Exchange exchange, Class<T> type) {
            final Object result = evaluate(exchange);
            return exchange.getContext().getTypeConverter().convertTo(type, exchange, result);
        }
    };
}
