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

import static info.rmapproject.loader.camel.impl.file.EnhancedZipDataFormat.HEADER_ZIP_ENTRY;

import java.io.File;
import java.util.zip.ZipEntry;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

public class ZipHarvestServiceTest
        extends CamelTestSupport {

    @Produce
    protected ProducerTemplate template;

    @EndpointInject(uri = "mock:out")
    private MockEndpoint mock_out;

    @Test
    public void zipTest() throws Exception {

        final File dir = new File(getClass().getResource("/fileDriver/data.zip").getFile()).getParentFile();

        final ZipFileHarvestService toTest = new ZipFileHarvestService();
        toTest.setSrcUri("file:" + dir + "?noop=true&include=.*\\.zip$");
        toTest.setDestUri("direct:out");

        toTest.addRoutesToCamelContext(this.context);

        mock_out.setExpectedCount(3);
        assertMockEndpointsSatisfied();

        for (final Exchange e : mock_out.getExchanges()) {
            assertEquals(new File(e.getIn().getHeader(HEADER_ZIP_ENTRY, ZipEntry.class).getName()).getName(),
                    e.getIn().getBody());
        }

    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {

        return new RouteBuilder() {

            @Override
            public void configure() {
                from("direct:out").convertBodyTo(String.class).to("mock:out");
            }
        };
    }
}
