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

import static info.rmapproject.loader.camel.ContextHelper.fix;
import static info.rmapproject.loader.camel.impl.file.EnhancedZipDataFormat.HEADER_ZIP_ENTRY;
import static info.rmapproject.loader.camel.impl.file.FileDriver.ENDPOINT_PROCESS_FILE;

import java.io.File;
import java.util.zip.ZipEntry;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.ExplicitCamelContextNameStrategy;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Ignore;
import org.junit.Test;

public class FileDriverTest
        extends CamelTestSupport {

    @Produce
    protected ProducerTemplate template;

    @EndpointInject(uri = "mock:out")
    private MockEndpoint mock_out;

    private final SimpleRegistry registry = new SimpleRegistry();

    private CamelContext parentContext;

    @Test
    @Ignore // fails on Windows
    public void zipTest() throws Exception {
        final String testContextId = "zipTest";

        newBlackBoxContext(new RouteBuilder() {

            File dir = new File(getClass().getResource("/fileDriver/data.zip").getFile()).getParentFile();

            @Override
            public void configure() throws Exception {
                from("file:" + dir + "?noop=true&include=.*\\.zip$").to("fileDriver:" + ENDPOINT_PROCESS_FILE);
            }

        }, testContextId);

        mock_out.setExpectedCount(3);
        assertMockEndpointsSatisfied();

        for (final Exchange e : mock_out.getExchanges()) {
            assertEquals(new File(e.getIn().getHeader(HEADER_ZIP_ENTRY, ZipEntry.class).getName()).getName(),
                    e.getIn().getBody(String.class));
        }

    }

    @Override
    protected CamelContext createCamelContext() throws Exception {

        parentContext = newBlackBoxContext(new FileDriver(), "fileDriver");

        parentContext.addRoutes(new RouteBuilder() {

            @Override
            public void configure() throws Exception {
                from("direct:out").to("mock:out");
            }
        });

        return parentContext;
    }

    private CamelContext newBlackBoxContext(RoutesBuilder routes, String id) throws Exception {
        final CamelContext cxt = fix(new DefaultCamelContext(registry));
        cxt.setNameStrategy(new ExplicitCamelContextNameStrategy(id));
        cxt.addRoutes(routes);
        cxt.start();

        registry.put(id, cxt);
        return cxt;

    }

}