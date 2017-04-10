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

package info.rmapproject.loader.camel.impl;

import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;

import info.rmapproject.loader.camel.ContextFactory;

/**
 * @author apb@jhu.edu
 */
public class SuppliedContextFactory implements ContextFactory {

    private CamelContext cxt;

    public void setCamelContext(CamelContext cxt) {
        this.cxt = cxt;
    }

    public SuppliedContextFactory() {

    }

    public SuppliedContextFactory(CamelContext cxt) {
        this.cxt = cxt;
    }

    @Override
    public CamelContext newContext(RoutesBuilder routes, String id) {
        try {
            if (routes != null) {
                routes.addRoutesToCamelContext(cxt);
            }
            return cxt;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void disposeContext(CamelContext context) {
        try {
            context.stop();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}
