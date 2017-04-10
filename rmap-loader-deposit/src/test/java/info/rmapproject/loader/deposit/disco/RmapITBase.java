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

import static org.apache.http.HttpStatus.SC_CREATED;

import java.net.URI;
import java.util.function.BiConsumer;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * @author apb@jhu.edu
 */
public class RmapITBase {

    private static Server server;

    protected static URI getRmapURI() {
        return URI.create("http://localhost:" + getRmapPort() + "/discos");
    }

    private static int getRmapPort() {
        return Integer.getInteger("rmap.dynamic.test.port", 8080);
    }

    static BiConsumer<HttpServletRequest, HttpServletResponse> handler = (req, resp) -> resp.setStatus(
            SC_CREATED);

    protected static void setHandler(BiConsumer<HttpServletRequest, HttpServletResponse> handler) {
        RmapITBase.handler = handler;
    }

    @SuppressWarnings("serial")
    @BeforeClass
    public static void startFakeRmap() throws Exception {
        server = new Server(getRmapPort());
        final ServletContextHandler servletContext = new ServletContextHandler();
        servletContext.setContextPath("/");

        servletContext.addServlet(new ServletHolder(new HttpServlet() {

            @Override
            public void doPost(HttpServletRequest request, HttpServletResponse resp) {
                handler.accept(request, resp);
            }

        }), "/discos/*");

        server.setHandler(servletContext);
        server.start();

    }

    @AfterClass
    public static void stopFakeRmap() throws Exception {
        server.stop();
    }
}
