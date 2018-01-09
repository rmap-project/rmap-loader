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

import static info.rmapproject.loader.util.ConfigUtil.integer;
import static info.rmapproject.loader.util.ConfigUtil.string;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;

/**
 * @author apb@jhu.edu
 */
public abstract class ActiveMQConfig {

    public static CloseableConnectionFactory buildConnectionFactory() {
        final ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory();
        factory.setBrokerURL(string("jms.brokerUrl", "tcp://localhost:61616"));
        factory.setUserName(string("jms.username", null));
        factory.setPassword(string("jms.password", null));

        final PooledConnectionFactory pool = new PooledConnectionFactory(factory);
        pool.setMaxConnections(integer("jms.maxConnections", 10));
        pool.start();

        return new CloseableConnectionFactoryImpl(pool);
    }

    private static class CloseableConnectionFactoryImpl implements CloseableConnectionFactory {

        final ConnectionFactory pool;

        CloseableConnectionFactoryImpl(PooledConnectionFactory pool) {
            this.pool = pool;
        }

        @Override
        public void close() throws Exception {
            ((PooledConnectionFactory) pool).stop();
        }

        @Override
        public JMSContext createContext(String userName, String password, int sessionMode) {
            return pool.createContext(userName, password, sessionMode);
        }

        @Override
        public JMSContext createContext(String userName, String password) {
            return pool.createContext(userName, password);
        }

        @Override
        public JMSContext createContext(int sessionMode) {
            return pool.createContext(sessionMode);
        }

        @Override
        public JMSContext createContext() {
            return pool.createContext();
        }

        @Override
        public Connection createConnection(String userName, String password) throws JMSException {
            return pool.createConnection(userName, password);
        }

        @Override
        public Connection createConnection() throws JMSException {
            return pool.createConnection();
        }
    }
}
