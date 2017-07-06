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

import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.rmapproject.loader.HarvestRecordStatus;
import info.rmapproject.loader.HarvestRecordRegistry;
import info.rmapproject.loader.model.RecordInfo;

/**
 * Simple RDBMS-based registry for harvest records.
 * <p>
 * To keep things small, only the bare minimum data to satisfy the API is persisted.
 * </p>
 *
 * @author apb@jhu.edu
 */
public class RdbmsHarvestRecordRegistry implements HarvestRecordRegistry {

    Logger LOG = LoggerFactory.getLogger(RdbmsHarvestRecordRegistry.class);

    DataSource source;

    public void setDataSource(DataSource source) {
        this.source = source;
    }

    public void init() {
        try (Connection conn = source.getConnection()) {
            try (Statement statement = conn.createStatement()) {

                boolean newTable = false;
                if (statement.execute(
                        "CREATE TABLE IF NOT EXISTS recordInfo ( uri VARCHAR(255) NOT NULL, recordDate BIGINT NOT NULL, disco VARCHAR(255) NOT NULL)")) {
                    newTable = statement.getResultSet().next();
                } else {
                    newTable = statement.getUpdateCount() > 0;
                }

                if (newTable) {
                    statement.execute("CREATE INDEX uri_idx ON recordInfo (uri)");
                }
            }

        } catch (final SQLException e) {
            throw new RuntimeException("Could not create tables", e);
        }

    }

    @Override
    public void register(RecordInfo info, URI discoURI) {

        if (info.getId() == null) {
            LOG.debug("Empty record ID, not registering {}", discoURI);
            return;
        }
        try (Connection conn = source.getConnection()) {
            final PreparedStatement insertRecord = conn.prepareStatement(
                    "INSERT INTO recordInfo (uri, recordDate, disco) VALUES (?, ?, ?)");
            insertRecord.setString(1, info.getId().toString());
            insertRecord.setLong(2, info.getDate().getTime());
            insertRecord.setString(3, discoURI.toString());

            if (insertRecord.executeUpdate() < 1) {
                throw new RuntimeException("Record did not insert into registry");
            }

        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public HarvestRecordStatus getStatus(RecordInfo info) {

        final HarvestRecordStatus status = new HarvestRecordStatus();
        if (info.getId() == null) {
            LOG.debug("Empty record ID, returning empty record status");
            return status;
        }

        try (Connection conn = source.getConnection()) {
            final PreparedStatement findRecord = conn.prepareStatement(
                    "SELECT disco, recordDate FROM recordInfo WHERE uri = ? ORDER BY recordDate DESC LIMIT 1");
            findRecord.setString(1, info.getId().toString());
            try (ResultSet results = findRecord.executeQuery()) {
                if (results.next()) {
                    status.setRecordExists(true);
                    status.setLatest(URI.create(results.getString(1)));
                    status.setIsUpToDate(date(info) <= results.getLong(2));
                } else {
                    status.setRecordExists(false);
                    status.setIsUpToDate(false);
                }
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }

        return status;
    }

    private long date(RecordInfo info) {
        if (info.getDate() != null) {
            return info.getDate().getTime();
        } else {
            return 0;
        }
    }
}
