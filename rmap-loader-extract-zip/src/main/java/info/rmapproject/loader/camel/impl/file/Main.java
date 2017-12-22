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

package info.rmapproject.loader.camel.impl.file;

import static info.rmapproject.loader.util.CamelConfig.buildCamelContext;
import static info.rmapproject.loader.util.ConfigProperties.JMS_QUEUE_DEST;
import static info.rmapproject.loader.util.ConfigUtil.string;
import static info.rmapproject.loader.util.LogUtil.adjustLogLevels;

import org.apache.camel.CamelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author apb@jhu.edu
 */
public class Main {

    static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        adjustLogLevels();
        final CamelContext cxt = buildCamelContext();

        final ZipFileHarvestService zipHarvest = new ZipFileHarvestService();

        zipHarvest.setOutputContentType(string("content.type", "application/xml"));
        zipHarvest.setDestUri("msg:queue:" + string(JMS_QUEUE_DEST, "rmap.harvest.xml.zip"));

        String fileName = string("file", null);
        String directory = string("dir", null);
        if (directory != null) {
            fileName = "&fileName=" + fileName;
        }

        if (fileName == null && directory == null) {
            LOG.warn("Please specify  'file', 'dir', or both");
            return;
        } else if (directory == null) {
            directory = ".";
            LOG.info("Consuming files named {} in {}", fileName, directory);
        } else {
            LOG.info("Consuming files from directory {}", directory);
        }

        zipHarvest.setSrcUri("file:" + directory + "?recursive=false&move=.done" + fileName);

        zipHarvest.addRoutesToCamelContext(cxt);

        Thread.currentThread().join();
    }
}
