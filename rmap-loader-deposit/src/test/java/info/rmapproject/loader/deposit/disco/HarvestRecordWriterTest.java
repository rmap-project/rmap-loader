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

import static info.rmapproject.loader.deposit.disco.HarvestRecordListener.onHarvestRecord;
import static org.junit.Assert.assertTrue;
import static org.unitils.reflectionassert.ReflectionAssert.assertReflectionEquals;

import java.net.URI;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.activemq.junit.EmbeddedActiveMQBroker;
import org.junit.Rule;
import org.junit.Test;
import org.unitils.reflectionassert.ReflectionComparatorMode;

import info.rmapproject.loader.HarvestRecord;
import info.rmapproject.loader.model.HarvestInfo;
import info.rmapproject.loader.model.RecordInfo;

/**
 * @author apb@jhu.edu
 */
public class HarvestRecordWriterTest {

    @Rule
    public EmbeddedActiveMQBroker broker = new EmbeddedActiveMQBroker();

    @Test
    public void successfulSendTest() throws Exception {
        try (JmsClient jms = new JmsClient(broker.createConnectionFactory())) {
            final String queue = "test.queue";

            final RecordInfo recordInfo = new RecordInfo();
            recordInfo.setContentType("test/stuff");
            recordInfo.setDate(new Date(1234));
            recordInfo.setId(URI.create("http://example.org/recordId"));
            recordInfo.setSrc(URI.create("http://example.org/source"));

            final HarvestInfo harvestInfo = new HarvestInfo();
            recordInfo.setHarvestInfo(harvestInfo);
            harvestInfo.setDate(new Date(5678));
            harvestInfo.setId(URI.create("http://example.org/HarvestId"));
            harvestInfo.setSrc(URI.create("http://example.org/src"));

            final HarvestRecord record = new HarvestRecord();
            record.setRecordInfo(recordInfo);
            record.setBody("HELLO".getBytes());

            final HarvestRecordWriter writer = new HarvestRecordWriter(jms);
            writer.write(queue, record);

            final AtomicReference<HarvestRecord> receivedRecord = new AtomicReference<>();
            final CountDownLatch messageReceived = new CountDownLatch(1);

            jms.listen(queue, onHarvestRecord(received -> {
                messageReceived.countDown();
                receivedRecord.set(received);
            }));

            assertTrue(messageReceived.await(10, TimeUnit.SECONDS));
            assertReflectionEquals(record, receivedRecord.get(), ReflectionComparatorMode.LENIENT_ORDER);

        }
    }

    @Test
    public void errorTest() throws Exception {
        try (JmsClient jms = new JmsClient(broker.createConnectionFactory())) {
            final String queue = "errorTest.test.queue";

            final HarvestRecord record = new HarvestRecord();
            record.setRecordInfo(new RecordInfo());

            final HarvestRecordWriter writer = new HarvestRecordWriter(jms);

            final CountDownLatch errorReceived = new CountDownLatch(1);
            writer.write(queue, record);

            // Throw an error upon listen. Register an error handler that directs to the error queue.
            jms.listen(queue, onHarvestRecord(received -> {
                throw new RuntimeException("processing failed");
            }).withExceptionHandler((msg, x) -> {
                errorReceived.countDown();
            }));

            assertTrue(errorReceived.await(10, TimeUnit.SECONDS));
        }
    }
}
