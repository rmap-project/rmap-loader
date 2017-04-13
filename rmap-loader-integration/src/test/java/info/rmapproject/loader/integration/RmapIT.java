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

package info.rmapproject.loader.integration;

import static info.rmapproject.loader.integration.JarRunner.jar;

import java.io.File;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.broker.TransportConnector;
import org.apache.activemq.junit.EmbeddedActiveMQBroker;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.LoggerFactory;

/**
 * @author apb@jhu.edu
 */
public class RmapIT extends FakeRmap {

    @Rule
    public EmbeddedActiveMQBroker customizedBroker = new EmbeddedActiveMQBroker() {

        @Override
        protected void configure() {
            try {

                final TransportConnector tcp = new TransportConnector();
                tcp.setUri(URI.create("tcp://localhost:61616"));

                this.getBrokerService().addConnector(tcp);
                // this.getBrokerService().setPlugins(new BrokerPlugin[] { auth });
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }
    };

    static Process load;

    static Process xslt;

    static Process unzip;

    @BeforeClass
    public static void start() throws Exception {
        unzip = jar(new File(System.getProperty("zip.extract.jar")))
                .logOutput(LoggerFactory.getLogger("extract-zip"))
                .start();

        xslt = jar(new File(System.getProperty("xsl.transform.jar").toString()))
                .logOutput(LoggerFactory.getLogger("transform"))
                .start();

        load = jar(new File(System.getProperty("disco.loader.jar").toString()))
                .logOutput(LoggerFactory.getLogger("load"))
                .start();
    }

    @Test
    public void smokeTest() throws Exception {
        final CountDownLatch x = new CountDownLatch(1);
        x.await(10, TimeUnit.SECONDS);
    }

    @AfterClass
    public static void stop() {
        load.destroyForcibly();
        xslt.destroyForcibly();
        unzip.destroyForcibly();
    }

}
