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
import static org.apache.http.HttpHeaders.AUTHORIZATION;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.function.Consumer;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.rmapproject.loader.HarvestRecord;
import info.rmapproject.loader.HarvestRecordStatus;
import info.rmapproject.loader.HarvestRegistry;
import info.rmapproject.loader.model.RecordInfo;

/**
 * POSTs the contents of harvest records to RMap.
 * <p>
 * Checks the harvest registry based on the provided harvest into within the record. If the record is already up to
 * date (based on matching record id and date), the record is skipped. If the record exists but the provided date is
 * greater than the date in the registry, the latest RMap DiSCO corresponding to the record ID will be updated. If the
 * record does not have a corresponding disco, it is presumed to be new. A new DiSCO will be created. Finally, the
 * registry will be updated if applicable.
 *
 * @author apb@jhu.edu
 */
public class DiscoDepositConsumer implements Consumer<HarvestRecord> {

    private static final Logger LOG = LoggerFactory.getLogger(DiscoDepositConsumer.class);

    private CloseableHttpClient client;

    private URI rmapDiscoEndpoint;

    private String authToken;

    private HarvestRegistry harvestRegistry;

    public void setHarvestRegistry(HarvestRegistry registry) {
        this.harvestRegistry = registry;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public void setRmapDiscoEndpoint(URI endpoint) {
        this.rmapDiscoEndpoint = endpoint;
    }

    public DiscoDepositConsumer() {

        try {
            client = HttpClientBuilder.create()
                    .setConnectionManager(new PoolingHttpClientConnectionManager(
                            RegistryBuilder
                                    .<ConnectionSocketFactory> create()
                                    .register("http", PlainConnectionSocketFactory.getSocketFactory())
                                    .register("https", new SSLConnectionSocketFactory(
                                            SSLContexts.custom()
                                                    .loadTrustMaterial(new TrustSelfSignedStrategy()).build(),
                                            new NoopHostnameVerifier()))
                                    .build()))
                    .setRedirectStrategy(new DefaultRedirectStrategy())
                    .build();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void accept(HarvestRecord record) {

        final HarvestRecordStatus status = harvestRegistry.getStatus(record.getRecordInfo());

        if (status.isLatest()) {
            if (record.getRecordInfo() != null) {
                LOG.info("Skipping record {}, as it is already up to date", record.getRecordInfo().getId());
            }
            return;
        }

        URI uri = rmapDiscoEndpoint;

        if (status.recordExists()) {
            uri = getUpdateURI(status.latest());
        }

        if (uri == null) {
            throw new RuntimeException("DiSCO URI is null!");
        }

        final HttpPost post = new HttpPost(uri);
        if (authToken != null) {
            post.setHeader(AUTHORIZATION, "Basic: " + Base64.encodeBase64String(authToken.getBytes(UTF_8)));
        }
        post.setHeader(CONTENT_TYPE, contentType(record));
        post.setEntity(new ByteArrayEntity(record.getBody()));

        try (CloseableHttpResponse response = client.execute(post)) {

            if (response.getStatusLine().getStatusCode() == 201) {
                harvestRegistry.register(
                        record.getRecordInfo(),
                        URI.create(response.getFirstHeader("Location")
                                .getValue()));
                EntityUtils.consume(response.getEntity());
            } else {
                throw new RuntimeException(String.format("Unexpected status code %s; '%s'", response.getStatusLine()
                        .getStatusCode(), IOUtils.toString(response.getEntity().getContent(), UTF_8)));
            }

        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private URI getUpdateURI(URI disco) {
        String path = rmapDiscoEndpoint.toString();
        if (!path.endsWith("/")) {
            path += "/";
        }

        try {
            return URI.create(path + URLEncoder.encode(disco.toString(), UTF_8.toString()));
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private String contentType(HarvestRecord record) {
        final RecordInfo recordInfo = record.getRecordInfo();
        if (recordInfo != null && recordInfo.getContentType() != null) {
            return recordInfo.getContentType();
        }

        return "application/vnd.rmap-project.disco+rdf+xml";
    }
}
