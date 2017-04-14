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

package info.rmapproject.loader.util;

import static info.rmapproject.loader.util.ActiveMQConfig.buildConnectionFactory;
import static info.rmapproject.loader.util.ConfigUtil.integer;

import javax.jms.ConnectionFactory;

import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.CamelContext;
import org.apache.camel.component.jms.JmsConfiguration;
import org.apache.camel.impl.DefaultCamelContext;

/**
 * @author apb@jhu.edu
 */
public abstract class CamelConfig {

    public static CamelContext buildCamelContext() {
        return buildCamelContext(buildConnectionFactory());
    }

    public static CamelContext buildCamelContext(ConnectionFactory conn) {
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
}
