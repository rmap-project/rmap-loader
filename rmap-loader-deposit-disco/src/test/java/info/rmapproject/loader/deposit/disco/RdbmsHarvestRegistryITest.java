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

package info.rmapproject.loader.deposit.disco;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.Date;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.zapodot.junit.db.EmbeddedDatabaseRule;
import org.zapodot.junit.db.EmbeddedDatabaseRule.CompatibilityMode;

import info.rmapproject.loader.HarvestRecordStatus;
import info.rmapproject.loader.model.RecordInfo;

/**
 * @author apb@jhu.edu
 */
public class RdbmsHarvestRegistryITest {

    @Rule
    public EmbeddedDatabaseRule dbRule = EmbeddedDatabaseRule
            .builder()
            .withMode(CompatibilityMode.PostgreSQL)
            .build();

    @Rule
    public TestName name = new TestName();

    RdbmsHarvestRegistry toTest = new RdbmsHarvestRegistry();

    @Before
    public void setUp() {
        toTest.setDataSource(dbRule.getDataSource());
        toTest.init();
    }

    @Test
    public void noRecordTest() {
        final RecordInfo info = new RecordInfo();
        info.setId(URI.create("http://not-exist"));
        final HarvestRecordStatus status = toTest.getStatus(info);

        assertFalse(status.recordExists());
    }

    @Test
    public void registerTest() {
        final URI recordId = URI.create("http://record-id");
        final URI discoUri = URI.create("http://disco-uri/" + name.getMethodName());
        final Date date = new Date(1234);

        final RecordInfo info = new RecordInfo();
        info.setId(recordId);
        info.setDate(date);

        toTest.register(info, discoUri);

        final HarvestRecordStatus status = toTest.getStatus(info);
        assertTrue(status.recordExists());
        assertTrue(status.isLatest());
        assertEquals(discoUri, status.latest());
    }

    @Test
    public void latestRecordTest() {
        final URI recordId = URI.create("http://record-id");

        final URI oldDiscoUri = URI.create("http://disco-uri/" + name.getMethodName() + "/old");
        final URI newDiscoUri = URI.create("http://disco-uri/" + name.getMethodName() + "/new");

        final Date oldDate = new Date(1000000);
        final Date newDate = new Date(2000000);

        final RecordInfo oldRecordInfo = new RecordInfo();
        oldRecordInfo.setId(recordId);
        oldRecordInfo.setDate(oldDate);

        final RecordInfo newRecordInfo = new RecordInfo();
        newRecordInfo.setId(recordId);
        newRecordInfo.setDate(newDate);

        toTest.register(newRecordInfo, newDiscoUri);
        toTest.register(oldRecordInfo, oldDiscoUri);

        final HarvestRecordStatus oldStatus = toTest.getStatus(oldRecordInfo);
        assertTrue(oldStatus.recordExists());
        assertFalse(oldStatus.isLatest());
        assertEquals(newDiscoUri, oldStatus.latest());

        final HarvestRecordStatus newStatus = toTest.getStatus(newRecordInfo);
        assertTrue(newStatus.recordExists());
        assertTrue(newStatus.isLatest());
        assertEquals(newDiscoUri, newStatus.latest());
    }

    // This simply needs to not throw an exception to succeed;
    @Test
    public void DDLTest() {
        final RdbmsHarvestRegistry second = new RdbmsHarvestRegistry();
        second.setDataSource(dbRule.getDataSource());
        second.init();

        final RdbmsHarvestRegistry third = new RdbmsHarvestRegistry();
        third.setDataSource(dbRule.getDataSource());
        third.init();
    }

}
