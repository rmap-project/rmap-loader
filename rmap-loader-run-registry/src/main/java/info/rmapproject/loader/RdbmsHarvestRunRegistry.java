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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple optional RDBMS-based registry to record run dates for a harvester.
 *
 * @author karen.hanson@jhu.edu
 */
public class RdbmsHarvestRunRegistry implements HarvestRunRegistry {

    Logger LOG = LoggerFactory.getLogger(RdbmsHarvestRunRegistry.class);

    DataSource source;

    public void setDataSource(DataSource source) {
        this.source = source;
    }

    public void init() {
        try (Connection conn = source.getConnection()) {
            try (Statement statement = conn.createStatement()) {
            	boolean createTable = true;
            	
            	statement.execute("SHOW TABLES");
            	ResultSet rs = statement.getResultSet();
            	while (rs.next()){
            		if (rs.getString(1).toUpperCase().equals("LOADERRUN")) {
            			createTable=false;
            			break;
            		}
            	}

                if (createTable) {
                	statement.execute("CREATE TABLE IF NOT EXISTS loaderRun (name VARCHAR(255) NOT NULL, runDate BIGINT NOT NULL)");
                    statement.execute("CREATE INDEX name_idx ON loaderRun (name)");
                }

            }

        } catch (final SQLException e) {
            throw new RuntimeException("Could not create tables", e);
        }

    }

    @Override
    public void addRunDate(String name, Date runDate) {

        if (name == null || name.length()==0 ||runDate == null) {
            LOG.debug("Empty name or runDate parameter. Cannot add run date.");
            throw new IllegalArgumentException("Name and runDate parameter required to add a run date");
        }
        try (Connection conn = source.getConnection()) {
            final PreparedStatement insertRecord = conn.prepareStatement(
                    "INSERT INTO loaderRun (name, runDate) VALUES (?, ?)");

            insertRecord.setString(1, name.toString());
            insertRecord.setLong(2, runDate.getTime());

            if (insertRecord.executeUpdate() < 1) {
                throw new RuntimeException("Record did not insert into registry");
            }

        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public Date getLastRunDate(String name) {

    	Date lastRunDate = null;
    	
        if (name == null || name.length()==0 ) {
            LOG.debug("Empty name parameter. Cannot retrieve last run date.");
            throw new IllegalArgumentException("Name parameter required to retrieve last run date");
        }

        try (Connection conn = source.getConnection()) {
            final PreparedStatement findRecord = conn.prepareStatement(
                    "SELECT runDate FROM loaderRun WHERE name = ? ORDER BY runDate DESC LIMIT 1");
            findRecord.setString(1, name);
            
            try (ResultSet results = findRecord.executeQuery()) {
	                if (results.next()) {
	                	lastRunDate = new Date(results.getLong(1));
	                }
	            }
	     } catch (final SQLException e) {
	         throw new RuntimeException(e);
	     }

        return lastRunDate;
    }

}
