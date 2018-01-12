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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.rmapproject.loader.HarvestRecord;
import info.rmapproject.loader.model.HarvestInfo;
import info.rmapproject.loader.model.RecordInfo;

/**
 * @author apb@jhu.edu
 */
public class ArchiveRecordExtractor implements RecordExtractor {

    static final Logger LOG = LoggerFactory.getLogger(ArchiveRecordExtractor.class);

    private Consumer<Path> doneAction = path -> {
    };

    private String contentType = "application/octet-stream";

    @Override
    public RecordExtractor onDone(Consumer<Path> action) {
        this.doneAction = action;
        return this;
    }

    @SuppressWarnings("resource")
    @Override
    public Stream<HarvestRecord> recordsFrom(Path file) {

        LOG.info("Extracting from file archive " + file);

        try {
            final HarvestInfo harvest = new HarvestInfo();
            harvest.setDate(new Date(Files.getLastModifiedTime(file).toMillis()));
            harvest.setSrc(URI.create("file:" + file.getFileName().toString()));
            harvest.setId(URI.create(harvest.getSrc().toString() + "@" + new Date().getTime()));

            final ArchiveInputStream is = archiveStream(Files.newInputStream(file));

            final Iterable<HarvestRecord> records = () -> new Iterator<HarvestRecord>() {

                ArchiveEntry entry;

                @Override
                public boolean hasNext() {
                    try {
                        entry = is.getNextEntry();

                        // Skip over directories
                        while (entry != null && entry.isDirectory()) {
                            entry = is.getNextEntry();
                        }

                        final boolean hasNext = entry != null;
                        if (!hasNext) {
                            LOG.info("Done extracting from " + file);
                            is.close();
                            doneAction.accept(file);
                        }
                        return hasNext;
                    } catch (final Exception e) {
                        throw new RuntimeException("Error reading from archive", e);
                    }
                }

                @Override
                public HarvestRecord next() {
                    final RecordInfo info = new RecordInfo();
                    info.setContentType(contentType);
                    info.setDate(entry.getLastModifiedDate());
                    info.setId(URI.create("file:" + entry.getName()));
                    info.setSrc(URI.create(harvest.getSrc().toString() + "#" + entry.getName()));
                    info.setHarvestInfo(harvest);

                    final HarvestRecord record = new HarvestRecord();
                    record.setRecordInfo(info);

                    try {
                        LOG.debug("entry: " + entry);
                        LOG.debug("size: " + entry.getSize());
                        if (entry.getSize() > -1) {
                            record.setBody(IOUtils.toByteArray(is, entry.getSize()));
                        } else {
                            record.setBody(IOUtils.toByteArray(is));
                        }
                    } catch (final Exception e) {

                        try {
                            is.close();
                        } catch (final Exception x) {
                        }

                        throw new RuntimeException("Could not read from archive", e);
                    }

                    return record;
                }
            };

            return StreamSupport.stream(records.spliterator(), false);

        } catch (final Exception e) {
            throw new RuntimeException("Error reading " + file, e);
        }
    }

    private static InputStream buffered(final InputStream in) {
        if (!in.markSupported()) {
            return new BufferedInputStream(in);
        }
        return in;
    }

    private static InputStream decompress(final InputStream in) {
        try {
            return new CompressorStreamFactory().createCompressorInputStream(buffered(in));
        } catch (final CompressorException e) {
            return in;
        }
    }

    @SuppressWarnings("resource")
    private static ArchiveInputStream archiveStream(final InputStream in) throws ArchiveException {
        final ArchiveStreamFactory af = new ArchiveStreamFactory();
        final InputStream buffered = buffered(in);
        try {
            return af.createArchiveInputStream(buffered);
        } catch (final ArchiveException e) {
            try {
                buffered.reset();
                return af.createArchiveInputStream(buffered(decompress(buffered)));
            } catch (final IOException x) {
                throw new RuntimeException(x);
            }
        }
    }

    @Override
    public RecordExtractor contentType(String type) {
        this.contentType = type;
        return this;
    }
}
