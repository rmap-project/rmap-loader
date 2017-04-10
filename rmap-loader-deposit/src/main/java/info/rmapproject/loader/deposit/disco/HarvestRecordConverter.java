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

import java.net.URI;
import java.util.Date;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import info.rmapproject.loader.HarvestRecord;
import info.rmapproject.loader.model.HarvestInfo;
import info.rmapproject.loader.model.RecordInfo;

/**
 * Converts JMS {@link Message} to and from {@link HarvestRecord}
 * <p>
 * This is a low-level class typically not used by clients.
 * </p>
 *
 * @author apb@jhu.edu
 */
public class HarvestRecordConverter {

    private static final String PROP_HARVEST_ID = "rmap.harvest.id";

    private static final String PROP_HARVEST_DATE = "rmap.harvest.date";

    private static final String PROP_HARVEST_SRC = "rmap.harvest.src";

    private static final String PROP_HARVEST_RECORD_ID = "rmap.harvest.record.id";

    private static final String PROP_HARVEST_RECORD_DATE = "rmap.harvest.record.date";

    private static final String PROP_HARVEST_RECORD_SRC = "rmap.harvest.record.src";

    /**
     * Create a HarvestRecord from a JMS message.
     *
     * @param m The message
     * @return populated {@link HarvestRecord}.
     * @throws JMSException
     */
    public static HarvestRecord fromMessage(Message m) throws JMSException {
        final HarvestRecord record = new HarvestRecord();
        record.setBody(body(m));
        record.setRecordInfo(getRecordInfo(m));

        return record;
    }

    public static Message toMessage(HarvestRecord record, Session session) throws JMSException {
        final TextMessage message = session.createTextMessage();

        final RecordInfo recordInfo = record.getRecordInfo();
        if (recordInfo != null) {
            writeRecordInfo(recordInfo, message);
        }

        message.setText(new String(record.getBody()));
        return message;
    }

    private static byte[] body(Message m) throws JMSException {
        if (m instanceof TextMessage) {
            return ((TextMessage) m).getText().getBytes(UTF_8);
        } else {
            throw new JMSException("Unknown message type " + m.getClass());
        }
    }

    private static RecordInfo getRecordInfo(Message m) throws JMSException {

        final RecordInfo recordInfo = new RecordInfo();
        final HarvestInfo harvestInfo = new HarvestInfo();
        recordInfo.setHarvestInfo(harvestInfo);

        if (m.propertyExists(PROP_HARVEST_ID)) {
            harvestInfo.setId(URI.create(m.getStringProperty(PROP_HARVEST_ID)));
        }

        if (m.propertyExists(PROP_HARVEST_DATE)) {
            harvestInfo.setDate(new Date(m.getLongProperty(PROP_HARVEST_DATE)));
        }

        if (m.propertyExists(PROP_HARVEST_SRC)) {
            harvestInfo.setSrc(URI.create(m.getStringProperty(PROP_HARVEST_SRC)));
        }

        if (m.propertyExists(PROP_HARVEST_RECORD_ID)) {
            recordInfo.setId(URI.create(m.getStringProperty(PROP_HARVEST_RECORD_ID)));
        }

        if (m.propertyExists(PROP_HARVEST_RECORD_DATE)) {
            recordInfo.setDate(new Date(m.getLongProperty(PROP_HARVEST_RECORD_DATE)));
        }

        if (m.propertyExists(PROP_HARVEST_RECORD_SRC)) {
            recordInfo.setSrc(URI.create(m.getStringProperty(PROP_HARVEST_RECORD_SRC)));
        }

        return recordInfo;

    }

    private static void writeRecordInfo(RecordInfo recordInfo, Message message) throws JMSException {
        if (recordInfo.getId() != null) {
            message.setStringProperty(PROP_HARVEST_RECORD_ID, recordInfo.getId().toString());
        }

        if (recordInfo.getDate() != null) {
            message.setLongProperty(PROP_HARVEST_RECORD_DATE, recordInfo.getDate().getTime());
        }

        if (recordInfo.getSrc() != null) {
            message.setStringProperty(PROP_HARVEST_RECORD_SRC, recordInfo.getSrc().toString());
        }

        if (recordInfo.getHarvestInfo() != null) {
            writeHarvestInfo(recordInfo.getHarvestInfo(), message);
        }
    }

    private static void writeHarvestInfo(HarvestInfo harvestInfo, Message message) throws JMSException {
        if (harvestInfo.getId() != null) {
            message.setStringProperty(PROP_HARVEST_ID, harvestInfo.getId().toString());
        }

        if (harvestInfo.getDate() != null) {
            message.setLongProperty(PROP_HARVEST_DATE, harvestInfo.getDate().getTime());
        }

        if (harvestInfo.getSrc() != null) {
            message.setStringProperty(PROP_HARVEST_SRC, harvestInfo.getSrc().toString());
        }
    }

}
