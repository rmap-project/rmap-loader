/*
 * Copyright 2013 Johns Hopkins University
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

package info.rmapproject.loader.osgi.impl;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.camel.core.osgi.OsgiNamingHelper;
import org.apache.camel.spi.CamelContextNameStrategy;

import org.osgi.framework.BundleContext;

import static org.apache.camel.core.osgi.OsgiCamelContextPublisher.CONTEXT_NAME_PROPERTY;

public class ExplicitOsgiCamelContextNameStrategy
        implements CamelContextNameStrategy {

    private static final AtomicInteger CONTEXT_COUNTER = new AtomicInteger(0);

    private final BundleContext context;

    private final String prefix;

    private volatile String name;

    public ExplicitOsgiCamelContextNameStrategy(BundleContext context, String name) {
        this.prefix = name;
        this.context = context;
    }

    @Override
    public String getName() {
        if (name == null) {
            name = getNextName();
        }
        return name;
    }

    @Override
    public synchronized String getNextName() {

        return OsgiNamingHelper.findFreeCamelContextName(context,
                                                         prefix,
                                                         CONTEXT_NAME_PROPERTY,
                                                         CONTEXT_COUNTER,
                                                         false);
    }

    @Override
    public boolean isFixedName() {
        return false;
    }

}
