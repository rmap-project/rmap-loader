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

import static info.rmapproject.loader.util.ActiveMQConfig.buildConnectionFactory;
import static info.rmapproject.loader.util.ConfigUtil.string;
import static info.rmapproject.loader.util.LogUtil.adjustLogLevels;

import java.net.URI;

import javax.jms.ConnectionFactory;

import com.zaxxer.hikari.HikariDataSource;

/**
 * @author apb@jhu.edu
 */
public class Main {

    public static void main(final String[] args) throws Exception {
        adjustLogLevels();
        final ConnectionFactory factory = buildConnectionFactory();

        final HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(string("jdbc.url", "jdbc:sqlite:"));
        ds.setUsername(string("jdbc.username", null));
        ds.setPassword(string("jdbc.password", null));

        final RdbmsHarvestRegistry harvestRegistry = new RdbmsHarvestRegistry();
        harvestRegistry.setDataSource(ds);
        harvestRegistry.init();

        final DiscoDepositConsumer depositor = new DiscoDepositConsumer();
        depositor.setAuthToken(string("rmap.api.auth.token", null));
        depositor.setRmapDiscoEndpoint(makeDiscoEndpointUri());
        depositor.setHarvestRegistry(harvestRegistry);

        final DiscoDepositService depositWiring = new DiscoDepositService();
        depositWiring.setConnectionFactory(factory);
        depositWiring.setDiscoConsumer(depositor);
        depositWiring.setQueueSpec(string("jms.queue.src", "rmap.harvest.disco.>"));

        depositWiring.start();
        Thread.currentThread().join();
        depositWiring.close();
    }

    private static URI makeDiscoEndpointUri() {
        return URI.create(string("rmap.api.baseuri",
                "https://test.rmap-project.org/apitest/").replaceFirst("/$", "") + "/discos/");
    }
}
