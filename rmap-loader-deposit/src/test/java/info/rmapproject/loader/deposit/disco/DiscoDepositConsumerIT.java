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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import info.rmapproject.loader.HarvestRecord;
import info.rmapproject.loader.HarvestRecordStatus;
import info.rmapproject.loader.HarvestRegistry;
import info.rmapproject.loader.model.RecordInfo;

/**
 * @author apb@jhu.edu
 */
@RunWith(MockitoJUnitRunner.class)
public class DiscoDepositConsumerIT extends RmapITBase {

    @Mock
    public HarvestRegistry harvestRegistry;

    private static final String AUTH_TOKEN = "myToken";

    DiscoDepositConsumer toTest = new DiscoDepositConsumer();

    @Before
    public void setUp() {
        toTest.setRmapDiscoEndpoint(getRmapURI());
        toTest.setHarvestRegistry(harvestRegistry);
        toTest.setAuthToken(AUTH_TOKEN);
    }

    @Test
    public void newDiscoTest() throws Exception {
        final HarvestRecordStatus status = new HarvestRecordStatus();
        status.setRecordExists(false);

        when(harvestRegistry.getStatus(any(RecordInfo.class))).thenReturn(status);

        final HarvestRecord record = new HarvestRecord();
        final String CONTENT = "myContent";
        final String LOCATION = "http://example.org/location";
        final String CONTENT_TYPE = "text/test";
        final RecordInfo recordInfo = new RecordInfo();

        record.setRecordInfo(recordInfo);
        recordInfo.setContentType(CONTENT_TYPE);

        record.setBody(CONTENT.getBytes());

        final StringBuilder requestBody = new StringBuilder();
        final StringBuilder requestContentType = new StringBuilder();

        setHandler((req, resp) -> {
            try {
                requestBody.append(IOUtils.toString(req.getInputStream(), UTF_8));
                requestContentType.append(req.getContentType());
                resp.setStatus(HttpStatus.SC_CREATED);
                resp.setHeader("Location", LOCATION);

            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        });

        toTest.accept(record);
        assertEquals(CONTENT, requestBody.toString());
        assertEquals(CONTENT_TYPE, requestContentType.toString());

        verify(harvestRegistry).register(eq(recordInfo), eq(URI.create(LOCATION)));
    }

    @Test
    public void updateDiscoTest() throws Exception {
        final URI OLD_DISCO_URI = URI.create("http://example.org/old");
        final HarvestRecordStatus status = new HarvestRecordStatus();
        status.setRecordExists(true);
        status.setLatest(OLD_DISCO_URI);
        status.setIsLatest(false);

        when(harvestRegistry.getStatus(any(RecordInfo.class))).thenReturn(status);

        final HarvestRecord record = new HarvestRecord();
        final String CONTENT = "myContent";
        final String LOCATION = "http://example.org/location";
        final String CONTENT_TYPE = "text/test";
        final RecordInfo recordInfo = new RecordInfo();

        record.setRecordInfo(recordInfo);
        recordInfo.setContentType(CONTENT_TYPE);

        record.setBody(CONTENT.getBytes());

        final StringBuilder requestBody = new StringBuilder();
        final StringBuilder requestContentType = new StringBuilder();
        final StringBuilder requestPath = new StringBuilder();

        setHandler((req, resp) -> {
            try {
                requestBody.append(IOUtils.toString(req.getInputStream(), UTF_8));
                requestContentType.append(req.getContentType());
                requestPath.append(req.getPathInfo().replaceFirst("/", ""));
                resp.setStatus(HttpStatus.SC_CREATED);
                resp.setHeader("Location", LOCATION);

            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        });

        toTest.accept(record);
        assertEquals(CONTENT, requestBody.toString());
        assertEquals(OLD_DISCO_URI.toString(), requestPath.toString());
        assertEquals(CONTENT_TYPE, requestContentType.toString());
        verify(harvestRegistry).register(eq(recordInfo), eq(URI.create(LOCATION)));
    }

    @Test
    public void skipUpToDateTest() {
        final HarvestRecordStatus status = new HarvestRecordStatus();
        status.setRecordExists(true);
        status.setIsLatest(true);

        when(harvestRegistry.getStatus(any(RecordInfo.class))).thenReturn(status);

        final HarvestRecord record = new HarvestRecord();

        final AtomicBoolean handledRequest = new AtomicBoolean(false);

        setHandler((req, resp) -> {
            handledRequest.set(true);
        });

        toTest.accept(record);

        assertFalse(handledRequest.get());
        verify(harvestRegistry, times(0)).register(any(RecordInfo.class), any(URI.class));
    }

    @Test
    public void badHttpResponseTest() {
        final HarvestRecordStatus status = new HarvestRecordStatus();
        status.setRecordExists(false);

        when(harvestRegistry.getStatus(any(RecordInfo.class))).thenReturn(status);

        final HarvestRecord record = new HarvestRecord();

        setHandler((req, resp) -> {
            try {
                resp.sendError(HttpStatus.SC_BAD_REQUEST);
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        });

        try {
            toTest.accept(record);
            fail("Consumer should have thrown an exception");
        } catch (final Exception e) {
            // expected
        }

        verify(harvestRegistry, times(0)).register(any(RecordInfo.class), any(URI.class));
    }
}
