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

package info.rmapproject.loader.jms;

import static info.rmapproject.loader.jms.HarvestRecordConverter.fromMessage;
import static info.rmapproject.loader.jms.HarvestRecordConverter.toMessage;
import static org.unitils.reflectionassert.ReflectionAssert.assertReflectionEquals;

import java.net.URI;
import java.util.Date;

import javax.jms.Connection;
import javax.jms.Message;
import javax.jms.Session;

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
public class HarvestRecordConverterTest {

    @Rule
    public EmbeddedActiveMQBroker broker = new EmbeddedActiveMQBroker();

    @Test
    public void roundTripTest() throws Exception {
        try (Connection conn = broker.createConnectionFactory().createConnection()) {
            try (Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
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

                final Message msg = toMessage(record, session);
                final HarvestRecord roundTripped = fromMessage(msg);

                assertReflectionEquals(record, roundTripped, ReflectionComparatorMode.LENIENT_ORDER);

            }
        }
    }
}
