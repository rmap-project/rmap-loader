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

import static info.rmapproject.loader.util.ActiveMQConfig.buildConnectionFactory;
import static info.rmapproject.loader.util.ConfigProperties.JMS_QUEUE_DEST;
import static info.rmapproject.loader.util.ConfigUtil.string;
import static info.rmapproject.loader.util.LogUtil.adjustLogLevels;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import info.rmapproject.loader.jms.HarvestRecordWriter;
import info.rmapproject.loader.jms.JmsClient;
import info.rmapproject.loader.util.CloseableConnectionFactory;

/**
 * @author apb@jhu.edu
 */
public class Main {

    static final PathMatcher pathFilter = p -> true;

    public static void main(final String[] args) throws Exception {
        adjustLogLevels();

        final List<Path> cmdLinePaths = commandLineFiles(args);

        try (CloseableConnectionFactory factory = buildConnectionFactory();
                JmsClient client = new JmsClient(factory)) {

            final HarvestRecordWriter writer = new HarvestRecordWriter(client);

            final String queue = string(JMS_QUEUE_DEST, "rmap.harvest.xml.zip");

            new OneTimeRecordSource()
                    .ofDirectory(fromCmdLineOrEnv(cmdLinePaths))
                    .withFiles(cmdLinePaths.stream().filter(Files::isRegularFile).collect(toList()))
                    .withFilter(REGEX)
                    .withExtractor(extractor()
                            .contentType(string("content.type", "application/xml"))
                            .onDone(RENAME_TO_DONE))
                    .onRecord(r -> writer.write(queue, r))
                    .run();
        }
    }

    // If there is exactly one path specified and it's a directory, use it.
    // Otherwise, look to system properties or environment variables
    private static String fromCmdLineOrEnv(List<Path> candidates) {
        if (candidates.size() == 1 && Files.isDirectory(candidates.get(0))) {
            return candidates.get(0).toString();
        } else {
            return string("dir", null);
        }
    }

    private static List<Path> commandLineFiles(String[] args) {

        return Arrays.asList(args).stream()
                .map(File::new)
                .filter(File::exists)
                .map(File::toPath)
                .collect(toList());
    }

    private static Consumer<Path> RENAME_TO_DONE = path -> {
        final File file = path.toFile();
        file.renameTo(new File(file.getAbsolutePath() + ".done"));
    };

    private static Predicate<Path> REGEX = path -> {
        return pathFilter.matches(path) &&
                !path.getFileName().toString().endsWith(".done");
    };

    private static RecordExtractor extractor() {
        return new ArchiveRecordExtractor();
    }
}
