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

package info.rmapproject.loader.osgi.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.camel.component.jms.JmsConfiguration;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author apb@jhu.edu
 */
@ObjectClassDefinition
@interface ActiveMqPublisherConfig {

    String jms_brokerUrl() default "tcp://localhost:61616";

    String jms_username();

    String jms_password();

    String jms_osgi_jndi_service_name() default "rmap/jms";

    int jms_connetions() default 10;

    String jms_camel_component_name() default "msg";

    int jms_camel_consumers() default 1;
}

@Component(configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true)
@Designate(ocd = ActiveMqPublisherConfig.class, factory = true)
public class ActiveMQPublisher {

    Logger LOG = LoggerFactory.getLogger(ActiveMQPublisher.class);

    ServiceRegistration<ConnectionFactory> connectionFactoryService;

    ServiceRegistration<org.apache.camel.Component> camelComponentService;

    ActiveMQComponent component = new ActiveMQComponent();

    PooledConnectionFactory pool;

    @Activate
    public void start(BundleContext cxt, ActiveMqPublisherConfig config) {
        final ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory();
        factory.setBrokerURL(config.jms_brokerUrl());
        factory.setUserName(config.jms_username());
        factory.setPassword(config.jms_password());

        pool.setMaxConnections(config.jms_connetions());
        pool.setConnectionFactory(factory);

        LOG.info("Publishing {} as {}", ConnectionFactory.class, config.jms_osgi_jndi_service_name());
        final Dictionary<String, String> connectionFactoryProps = new Hashtable<>();
        connectionFactoryProps.put("osgi.jndi.service.name", config.jms_osgi_jndi_service_name());
        connectionFactoryService = cxt.registerService(ConnectionFactory.class, pool, connectionFactoryProps);

        final JmsConfiguration camelConfig = new JmsConfiguration();
        camelConfig.setMaxConcurrentConsumers(config.jms_camel_consumers());

        component.setConfiguration(camelConfig);

        LOG.info("Publishing {} as {}", ActiveMQComponent.class, config.jms_osgi_jndi_service_name());
        final Dictionary<String, String> activeMqComponentProps = new Hashtable<>();
        activeMqComponentProps.put("name", config.jms_camel_component_name());

        camelComponentService =
                cxt.registerService(org.apache.camel.Component.class, component,
                        activeMqComponentProps);
    }

    @Deactivate
    public void stop() {
        connectionFactoryService.unregister();
        camelComponentService.unregister();

        try {
            component.stop();
        } catch (final Exception e) {
            LOG.warn("Error shutting down camel componet: ", e);
        } finally {
            pool.stop();
        }
    }
}
