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

package info.rmapproject.loader.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import info.rmapproject.loader.impl.file.ArchiveRecordExtractor;

/**
 * @author apb@jhu.edu
 */
public class ArchiveRecordExtractorTest {

    final File zipfile = new File(getClass().getResource("/fileDriver/data.zip").getFile());

    @Test
    public void recordBodyTest() throws Exception {

        final List<String> expectedBodies = Arrays.asList("1.txt", "2.txt", "3.txt");

        final Set<String> zippedBodies = new ArchiveRecordExtractor()
                .recordsFrom(zipfile.toPath())
                .map(r -> new String(r.getBody()))
                .collect(Collectors.toSet());

        assertEquals(expectedBodies.size(), zippedBodies.size());
        assertTrue(zippedBodies.containsAll(expectedBodies));
    }

    @Test
    public void contentTypeTest() throws Exception {

        final String CONTENT_TYPE = "test/contentTypeTest";

        final Set<String> zippedcontentTypes = new ArchiveRecordExtractor()
                .contentType(CONTENT_TYPE)
                .recordsFrom(zipfile.toPath())
                .map(r -> new String(r.getRecordInfo().getContentType()))
                .collect(Collectors.toSet());

        assertEquals(1, zippedcontentTypes.size());
        assertEquals(CONTENT_TYPE, zippedcontentTypes.iterator().next());
    }

    @Test
    public void uniqueRecordIdTest() throws Exception {
        final Set<String> zippedRecordIDs = new ArchiveRecordExtractor()
                .recordsFrom(zipfile.toPath())
                .map(r -> new String(r.getRecordInfo().getId().toString()))
                .collect(Collectors.toSet());

        assertEquals(3, zippedRecordIDs.size());

    }
}
