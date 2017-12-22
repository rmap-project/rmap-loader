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

package info.rmapproject.loader.impl.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.rmapproject.loader.HarvestRecord;

/**
 * @author apb@jhu.edu
 */
public class OneTimeRecordSource implements RecordSource {

    Logger LOG = LoggerFactory.getLogger(OneTimeRecordSource.class);

    private RecordExtractor extractor;

    private Consumer<HarvestRecord> processRecord;

    private Predicate<Path> filter = path -> true;

    private Path dir = null;

    @Override
    public void run() {

        if (dir == null || !Files.exists(dir)) {
            LOG.warn("Directory " + dir + " does not exist, exiting");
            return;
        }

        try {
            Files.walk(dir)
                    .filter(p -> filter.test(dir.relativize(p)))
                    .flatMap(extractor::recordsFrom)
                    .forEach(processRecord);

        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public RecordSource withExtractor(RecordExtractor extractor) {
        this.extractor = extractor;
        return this;
    }

    @Override
    public RecordSource onRecord(Consumer<HarvestRecord> sink) {
        this.processRecord = sink;
        return this;
    }

    public OneTimeRecordSource ofDirectory(String dir) {
        this.dir = new File(dir).toPath();
        return this;
    }

    public OneTimeRecordSource withFilter(Predicate<Path> filter) {
        this.filter = filter;
        return this;
    }
}
