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

package info.rmapproject.loader.util;

import static info.rmapproject.loader.util.ConfigUtil.integer;
import static info.rmapproject.loader.util.ConfigUtil.string;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;

/**
 * @author apb@jhu.edu
 */
public abstract class ActiveMQConfig {

    public static ConnectionFactory buildConnectionFactory() {
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
