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

package info.rmapproject.loader.camel.impl.oai;

import static info.rmapproject.loader.util.CamelConfig.buildCamelContext;
import static info.rmapproject.loader.util.ConfigUtil.string;
import static info.rmapproject.loader.util.LogUtil.adjustLogLevels;

import org.apache.camel.CamelContext;

import info.rmapproject.loader.util.ConfigProperties;

/**
 * @author apb@jhu.edu
 */
public class Main implements ConfigProperties {

    public static void main(String[] args) throws Exception {

        adjustLogLevels();

        final CamelContext cxt = buildCamelContext();

        final OneTimeOaiHarvest oai = new OneTimeOaiHarvest();
        oai.setDestUri(string(JMS_QUEUE_SRC, "rmap.harvest.oai_dc"));
        oai.setOaiBaseUri(string("oai.baseURL", null));
        oai.setOaiSet(string("oai.setSpec", null));
        oai.setOaiFromDate(string("oai.from", null));
        oai.setOaiUntilDate(string("oai.until", null));
        oai.setMetadataPrefix(string("oai.metadataPrefix", "oai_dc"));
        oai.setOutputContentType(string("content.type", "application/xml"));
        oai.setOaiDriver(new OAIDriver());

        oai.addRoutesToCamelContext(cxt);
    }

}
