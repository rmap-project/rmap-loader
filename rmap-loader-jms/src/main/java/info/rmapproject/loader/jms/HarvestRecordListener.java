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

package info.rmapproject.loader.jms;

import static info.rmapproject.loader.jms.HarvestRecordConverter.fromMessage;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.jms.Message;
import javax.jms.MessageListener;

import info.rmapproject.loader.HarvestRecord;

/**
 * Translates between MessageListeners and Consumer<HarvestRecord>.
 * <p>
 * Listens for JMS messages. On receipt, it converts to a {@link HarvestRecord} and feeds the given consumer for
 * further processing. On failure, it passes the original message along an error handler, if provided.
 * </p>
 *
 * @author apb@jhu.edu
 */
public class HarvestRecordListener implements MessageListener {

    private final Consumer<HarvestRecord> consumer;

    private BiConsumer<Message, Exception> errorHandler;

    public HarvestRecordListener(Consumer<HarvestRecord> consumer) {
        this.consumer = consumer;
    }

    public static HarvestRecordListener onHarvestRecord(Consumer<HarvestRecord> consumer) {
        return new HarvestRecordListener(consumer);
    }

    @Override
    public void onMessage(Message m) {

        try {
            consumer.accept(fromMessage(m));
        } catch (final Exception e) {
            if (errorHandler != null) {
                errorHandler.accept(m, e);
            }
        }
    }

    public HarvestRecordListener withExceptionHandler(BiConsumer<Message, Exception> errorHandler) {
        this.errorHandler = errorHandler;
        return this;
    }
}
