/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.rmapproject.loader.deposit.disco;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;

import org.apache.activemq.junit.EmbeddedActiveMQBroker;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author apb@jhu.edu
 */
public class DiscoDepositServiceTest extends CamelBlueprintTestSupport {

    static final protected String serviceEndpoint = "http://127.0.0.1:" + System.getProperty(
            "rmap.dynamic.test.port") +
            "/discos";

    @Rule
    public EmbeddedActiveMQBroker broker = new EmbeddedActiveMQBroker();

    @Override
    protected String getBlueprintDescriptor() {
        return "OSGI-INF/blueprint/blueprint-test.xml";
    }

    // @Override
    // protected String setConfigAdminInitialConfiguration(final Properties props) {
    // return "info.rmapproject.deposit.disco.rdfxml";
    // }

    @Before
    public void fakeRmap() throws Exception {
        context().start();
        new RouteBuilder() {

            @Override
            public void configure() throws Exception {

                System.err.println("Configuring");
                from("jetty:" + serviceEndpoint +
                        "?matchOnUriPrefix=true")
                                .to("mock:rmap");

                from("timer:register?repeatCount=1").setBody(constant("FROM CAMEL")).process(e -> System.err.println("GO")).to(
                        "broker:queue:foo.format.disco_rdf");

            }
        }.addRoutesToCamelContext(context());

    }

    @Test
    public void consumptionTest() throws Exception {
        /*
        final Connection conn = broker.createConnectionFactory().createConnection();
        final Session session = conn.createSession(false,
                Session.AUTO_ACKNOWLEDGE);

        final Queue q = session.createQueue("foo.format.disco_rdf");
        final MessageProducer p = session.createProducer(q);
        p.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

        p.send(session.createTextMessage("test"));

        session.close();

        conn.close();
        */

        ((MockEndpoint) context().getEndpoint("mock:rmap")).setExpectedMessageCount(1);
        ((MockEndpoint) context().getEndpoint("mock:rmap")).assertIsSatisfied();
    }
}
