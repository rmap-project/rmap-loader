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

import static info.rmapproject.loader.deposit.disco.HarvestRecordConverter.toMessage;

import java.util.function.Supplier;

import javax.jms.JMSException;
import javax.jms.Session;

import info.rmapproject.loader.HarvestRecord;

/**
 * @author apb@jhu.edu
 */
public class HarvestRecordWriter implements AutoCloseable {

    private JmsClient jms;

    private Supplier<Session> sessions;

    public void setJmsClient(JmsClient client) {
        this.jms = client;
    }

    public HarvestRecordWriter(JmsClient client) {
        this.jms = client;
        init();
    }

    public HarvestRecordWriter() {

    }

    public void write(String queue, HarvestRecord record) {
        try {
            jms.write(queue, toMessage(record, sessions.get()));
        } catch (final JMSException e) {
            throw new RuntimeException("Could not write harvest record", e);
        }
    }

    public void init() {
        this.sessions = jms.getSessionSupplier();
    }

    @Override
    public void close() {
        jms.close();
    }
}
