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
public class RdbmsHarvestRecordRegistryITest {

    @Rule
    public EmbeddedDatabaseRule dbRule = EmbeddedDatabaseRule
            .builder()
            .withMode(CompatibilityMode.PostgreSQL)
            .build();

    @Rule
    public TestName name = new TestName();

    RdbmsHarvestRecordRegistry toTest = new RdbmsHarvestRecordRegistry();

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
        assertTrue(status.isUpToDate());
        assertEquals(discoUri, status.latest());
    }

    @Test
    public void isUpToDateRecordTest() {
        final URI recordId = URI.create("http://record-id");

        final URI v1DiscoUri = URI.create("http://disco-uri/" + name.getMethodName() + "/v1");
        final URI v2DiscoUri = URI.create("http://disco-uri/" + name.getMethodName() + "/v2");

        final Date v1Date = new Date(1000000);
        final Date v2Date = new Date(2000000);
        final Date v3Date = new Date(3000000);

        final RecordInfo v1RecordInfo = new RecordInfo();
        v1RecordInfo.setId(recordId);
        v1RecordInfo.setDate(v1Date);

        final RecordInfo v2RecordInfo = new RecordInfo();
        v2RecordInfo.setId(recordId);
        v2RecordInfo.setDate(v2Date);

        final RecordInfo v3RecordInfo = new RecordInfo();
        v3RecordInfo.setId(recordId);
        v3RecordInfo.setDate(v3Date);
        
        toTest.register(v2RecordInfo, v2DiscoUri);
        toTest.register(v1RecordInfo, v1DiscoUri);

        final HarvestRecordStatus v1Status = toTest.getStatus(v1RecordInfo);
        assertTrue(v1Status.recordExists());
        assertTrue(v1Status.isUpToDate()); // comparing to old data should show it's already up to date
        assertEquals(v2DiscoUri, v1Status.latest());

        final HarvestRecordStatus v2Status = toTest.getStatus(v2RecordInfo);
        assertTrue(v2Status.recordExists());
        assertTrue(v2Status.isUpToDate()); // comparing to new data also shows it's already up to date
        assertEquals(v2DiscoUri, v2Status.latest());

        //new info not yet recorded, so it should know the registry is not up to date compared to this new record info
        final HarvestRecordStatus v3Status = toTest.getStatus(v3RecordInfo);
        assertTrue(v3Status.recordExists());
        assertFalse(v3Status.isUpToDate()); // comparing to new data also shows it's already up to date
        assertEquals(v2DiscoUri, v3Status.latest());
        
    }

    // This simply needs to not throw an exception to succeed;
    @Test
    public void DDLTest() {
        final RdbmsHarvestRecordRegistry second = new RdbmsHarvestRecordRegistry();
        second.setDataSource(dbRule.getDataSource());
        second.init();

        final RdbmsHarvestRecordRegistry third = new RdbmsHarvestRecordRegistry();
        third.setDataSource(dbRule.getDataSource());
        third.init();
    }

}
