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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import info.rmapproject.loader.HarvestRecord;
import info.rmapproject.loader.impl.file.OneTimeRecordSource;
import info.rmapproject.loader.impl.file.RecordExtractor;

/**
 * @author apb@jhu.edu
 */
public class OneTimeRecordSourceTest {

    final Path zipfile = new File(getClass().getResource("/fileDriver/data.zip").getFile()).toPath();

    final File dir = new File(getClass().getResource("/fileDriver/data.zip").getFile()).getParentFile();

    @Test
    public void filterTest() {

        final HarvestRecord RECORD = new HarvestRecord();

        final RecordExtractor extractor = mock(RecordExtractor.class);
        when(extractor.recordsFrom(eq(zipfile))).thenReturn(Arrays.asList(RECORD).stream());

        final List<HarvestRecord> encounteredRecords = new ArrayList<>();

        final PathMatcher pathFilter = FileSystems.getDefault().getPathMatcher("glob:*.zip");

        new OneTimeRecordSource()
                .ofDirectory(dir.toString())
                .withFilter(pathFilter::matches)
                .withExtractor(extractor)
                .onRecord(r -> encounteredRecords.add(r))
                .run();

        assertEquals(1, encounteredRecords.size());
        assertEquals(RECORD, encounteredRecords.get(0));
    }
}
