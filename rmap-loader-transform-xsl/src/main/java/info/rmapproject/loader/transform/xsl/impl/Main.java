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

import static info.rmapproject.loader.util.ConfigUtil.integer;
import static info.rmapproject.loader.util.ConfigUtil.string;
import static info.rmapproject.loader.util.LogUtil.adjustLogLevels;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.component.jms.JmsConfiguration;
import org.apache.camel.impl.DefaultCamelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author apb@jhu.edu
 */
public class Main {

    static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(final String[] args) throws Exception {

        adjustLogLevels();

        final ConnectionFactory conn = buildConnectionFactory();

        final CamelContext cxt = buildCamelContext(conn);

        final XSLTransformService xsl = new XSLTransformService();
        xsl.setSrcUri("msg:queue:" + string("jms.queue.src", "rmap.harvest.xml.>"));
        xsl.setDestUri("msg:queue:" + string("jms.queue.dest", "rmap.harvest.disco.transformed"));
        xsl.setOutputContentType(string("content.type", "application/vnd.rmap-project.disco+rdf+xml"));
        xsl.setXsltFile(string("xslt.file", null));

        xsl.addRoutesToCamelContext(cxt);

        Thread.currentThread().join();
    }

    private static CamelContext buildCamelContext(ConnectionFactory conn) {
        final JmsConfiguration camelConfig = new JmsConfiguration();
        camelConfig.setMaxConcurrentConsumers(integer("jms.camel.consumers", 1));

        final ActiveMQComponent camelActiveMq = new ActiveMQComponent();
        camelActiveMq.setConfiguration(camelConfig);
        camelActiveMq.setConnectionFactory(conn);

        final CamelContext cxt = new DefaultCamelContext();
        cxt.addComponent("msg", camelActiveMq);

        try {
            cxt.start();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        return cxt;
    }

    private static ConnectionFactory buildConnectionFactory() {
        final ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory();
        factory.setBrokerURL(string("jms.brokerUrl", "tcp://localhost:61616"));
        factory.setUserName(string("jms.username", null));
        factory.setPassword(string("jms.password", null));

        final PooledConnectionFactory pool = new PooledConnectionFactory(factory);
        pool.setMaxConnections(integer("jms.maxConnections", 10));
        pool.start();

        return pool;
    }
}
