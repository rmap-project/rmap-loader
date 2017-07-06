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

package info.rmapproject.loader;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.zapodot.junit.db.EmbeddedDatabaseRule;
import org.zapodot.junit.db.EmbeddedDatabaseRule.CompatibilityMode;

/**
 * Tests for RdbmsHarvestRunRegistry
 * @author karen.hanson@jhu.edu
 */
public class RdbmsHarvestRunRegistryITest {

    @Rule
    public EmbeddedDatabaseRule dbRule = EmbeddedDatabaseRule
            .builder()
            .withMode(CompatibilityMode.PostgreSQL)
            .build();

    @Rule
    public TestName name = new TestName();

    RdbmsHarvestRunRegistry toTest = new RdbmsHarvestRunRegistry();

    @Before
    public void setUp() {
        toTest.setDataSource(dbRule.getDataSource());
        toTest.init();
    }

    @Test
    public void noPreviousRunsLastRunDateNull() {
    	String runName ="test name";
        final Date lastRunDate = toTest.getLastRunDate(runName);
        assertTrue(lastRunDate==null);
    }

    @Test
    public void createRunDatesAndRetrieveLatest() {
    	try {
	    	String runName ="test name";
	    	Date date1 = new Date();
	    	toTest.addRunDate(runName, date1);
	    	Thread.sleep(1000);
	    	
	    	Date date2 = new Date();
	    	toTest.addRunDate(runName, date2);
	    	
	    	Date lastRunDate = toTest.getLastRunDate(runName);
	    	
	    	assertTrue(lastRunDate.equals(date2));
	    	
	    	
    	} catch (Exception ex) {
    		ex.printStackTrace();
    		fail("An error occured during Test: createRunDatesAndRetrieveLatest()");
    	}
    	
    	
    }


    // This simply needs to not throw an exception to succeed;
    @Test
    public void DDLTest() {
        final RdbmsHarvestRunRegistry second = new RdbmsHarvestRunRegistry();
        second.setDataSource(dbRule.getDataSource());
        second.init();

        final RdbmsHarvestRunRegistry third = new RdbmsHarvestRunRegistry();
        third.setDataSource(dbRule.getDataSource());
        third.init();
    }

}
