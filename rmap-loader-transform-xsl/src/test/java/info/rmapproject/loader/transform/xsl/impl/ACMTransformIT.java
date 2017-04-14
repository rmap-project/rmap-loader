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

package info.rmapproject.loader.transform.xsl.impl;

import static info.rmapproject.loader.transform.xsl.impl.Xslt2Splitter.HEADER_XSLT_FILE_NAME;
import static info.rmapproject.loader.validation.DiscoValidator.validate;
import static info.rmapproject.loader.validation.DiscoValidator.Format.RDF_XML;

import java.io.InputStream;
import java.nio.file.Paths;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

public class ACMTransformIT
        extends CamelTestSupport {

    private final String basedir;

    public ACMTransformIT() {
        try {
            basedir = Paths.get(getClass().getResource("/ACM").toURI()).toString();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Produce
    protected ProducerTemplate template;

    @EndpointInject(uri = "mock:out")
    private MockEndpoint mock_out;

    @Test
    public void basicTest() throws Exception {
        template.sendBody("direct:in", getClass().getResourceAsStream("/ACM/TRANS-TOMS-V1I4-355656.xml"));
        mock_out.setExpectedCount(9);
        mock_out.assertIsSatisfied();

        mock_out.getExchanges().stream().map(Exchange::getIn).map(m -> m.getBody(InputStream.class))
                .forEach(i -> validate(i, RDF_XML));

    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {

        return new RouteBuilder() {

            Xslt2Splitter xslt2 = new Xslt2Splitter();

            @Override
            public void configure() {

                try {
                    from("direct:in").setHeader(HEADER_XSLT_FILE_NAME, constant(basedir + "/acm_to_disco.xsl"))
                            .process(xslt2).split(body()).to("mock:out");
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }

            }
        };
    }

}
