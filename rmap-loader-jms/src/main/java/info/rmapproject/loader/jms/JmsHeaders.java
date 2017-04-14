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

/**
 * RMap harvest headers for JMS records.
 *
 * @author apb@jhu.edu
 */
public interface JmsHeaders {

    /**
     * This is set if a message is routed to an error queue.
     */
    public static final String PROP_HARVEST_EXCEPTION = "rmap.harvest.exception";

    /** Identifies a harvest action (required) */
    public static final String PROP_HARVEST_ID = "rmap.harvest.id";

    /** The date of the harvest action (optional) */
    public static final String PROP_HARVEST_DATE = "rmap.harvest.date";

    /** Identifies the source of the harvest (optional) */
    public static final String PROP_HARVEST_SRC = "rmap.harvest.src";

    /** Individual record ID (required) */
    public static final String PROP_HARVEST_RECORD_ID = "rmap.harvest.record.id";

    /** Logical record date; the last modified date given by the source (required). */
    public static final String PROP_HARVEST_RECORD_DATE = "rmap.harvest.record.date";

    /** Source of the record, e.g a document URI if it has one (optional) */
    public static final String PROP_HARVEST_RECORD_SRC = "rmap.harvest.record.src";

    public static final String PROP_HARVEST_RECORD_CONTENT_TYPE = "Content-Type";
}
