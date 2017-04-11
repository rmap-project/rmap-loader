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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.jms.ConnectionFactory;

import org.apache.activemq.junit.EmbeddedActiveMQBroker;
import org.eclipse.jetty.server.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import info.rmapproject.loader.HarvestRecord;

/**
 * @author apb@jhu.edu
 */
@RunWith(MockitoJUnitRunner.class)
public class DiscoDepositServiceIT {

    @Rule
    public EmbeddedActiveMQBroker broker = new EmbeddedActiveMQBroker();

    @Rule
    public TestName name = new TestName();

    @Mock
    Consumer<HarvestRecord> consumer;

    static final String LISTEN_QUEUE = "rmap.harvest.disco.>";

    String SEND_QUEUE;

    String ERROR_QUEUE;

    private ConnectionFactory connectionFactory;

    Server server;

    DiscoDepositService toTest = new DiscoDepositService();

    HarvestRecordWriter writer = new HarvestRecordWriter();

    JmsClient jms;

    @Before
    public void setUp() {
        connectionFactory = broker.createConnectionFactory();

        jms = new JmsClient(connectionFactory);

        writer = new HarvestRecordWriter(jms);

        toTest.setQueueSpec(LISTEN_QUEUE);
        toTest.setConnectionFactory(broker.createConnectionFactory());
        toTest.setDiscoConsumer(consumer);

        SEND_QUEUE = "rmap.harvest.disco." + name.getMethodName();

        ERROR_QUEUE = "rmap.harvest.error.disco." + name.getMethodName();
    }

    @Test
    public void simpleDepositTest() throws Exception {
        toTest.start();

        final CountDownLatch depositRecievedByRmap = new CountDownLatch(1);

        final String CONTENT = "content";

        doAnswer(i -> {
            final HarvestRecord received = i.getArgument(0);
            assertEquals(CONTENT, new String(received.getBody()));
            depositRecievedByRmap.countDown();

            return null;
        }).when(consumer).accept(any(HarvestRecord.class));

        final HarvestRecord record = new HarvestRecord();
        record.setBody(CONTENT.getBytes(UTF_8));
        writer.write(SEND_QUEUE, record);

        assertTrue(depositRecievedByRmap.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void multipleQueueTest() throws Exception {
        toTest.start();

        final CountDownLatch depositRecievedByRmap = new CountDownLatch(2);

        final String CONTENT = "content";
        final String SECOND_QUEUE = SEND_QUEUE + ".second";

        doAnswer(i -> {
            System.out.println("\nQUEUE\n");
            final HarvestRecord received = i.getArgument(0);
            assertEquals(CONTENT, new String(received.getBody()));
            depositRecievedByRmap.countDown();

            return null;
        }).when(consumer).accept(any(HarvestRecord.class));

        final HarvestRecord record = new HarvestRecord();
        record.setBody(CONTENT.getBytes(UTF_8));
        writer.write(SEND_QUEUE, record);
        writer.write(SECOND_QUEUE, record);

        assertTrue(depositRecievedByRmap.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void errorTest() throws Exception {
        toTest.start();

        try (JmsClient client = new JmsClient(connectionFactory)) {

            final CountDownLatch errors = new CountDownLatch(1);

            final CountDownLatch depositRecievedByRmap = new CountDownLatch(1);

            final String CONTENT = "content";

            doAnswer(i -> {
                depositRecievedByRmap.countDown();
                throw new RuntimeException("Got an exception!");
            }).when(consumer).accept(any(HarvestRecord.class));

            final HarvestRecord record = new HarvestRecord();
            record.setBody(CONTENT.getBytes(UTF_8));
            writer.write(SEND_QUEUE, record);

            assertTrue(depositRecievedByRmap.await(10, TimeUnit.SECONDS));

            client.listen(ERROR_QUEUE, m -> {
                errors.countDown();
            });
            assertTrue(errors.await(10, TimeUnit.SECONDS));
        }
    }

    @After
    public void disconnect() throws Exception {
        jms.close();
        toTest.close();
    }

}
