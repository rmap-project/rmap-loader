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
package info.rmapproject.loader.camel;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.util.ObjectHelper;

/** Provides workaround for Context Component bug CAMEL-9200.
 * 
 * See https://issues.apache.org/jira/browse/CAMEL-9200
 * 
 */
public class ContextHelper {
    public static CamelContext fix(final CamelContext cxt) {

        return (CamelContext) Proxy.newProxyInstance(CamelContext.class
                .getClassLoader(), new Class[] {CamelContext.class,
                ModelCamelContext.class}, new InvocationHandler() {

            @SuppressWarnings("unchecked")
            @Override
            public Object invoke(Object proxy, Method method, Object[] args)
                    throws Throwable {

                if (method.getName().equals("getEndpointMap")) {
                    final Map<String, Endpoint> resultMap = new HashMap<>();

                    for (Map.Entry<String, Endpoint> original : ((Map<String, Endpoint>) method
                            .invoke(cxt, args)).entrySet()) {
                        resultMap.put(original.getKey(),
                                      wrap(original.getValue()));
                    }

                    return resultMap;

                }

                if (method.getName().equals("getEndpoint")) {
                    final Endpoint delegate =
                            (Endpoint) method.invoke(cxt, args);

                    return wrap(delegate);
                }
                return method.invoke(cxt, args);
            }
        });
    }

    private static Endpoint wrap(Endpoint delegate) {
        if (delegate == null) {
            return null;
        }

        /*
         * For some unknown reason, this is necessary for the unit test to work,
         * otherwise it complains that it cannot inject a MockEndpoint via
         * @EndpointInject
         */
        if (delegate instanceof MockEndpoint) {
            return delegate;
        }

        return (Endpoint) Proxy.newProxyInstance(Endpoint.class
                                                         .getClassLoader(),
                                                 new Class[] {Endpoint.class},
                                                 new InvocationHandler() {

                                                     @Override
                                                     public Object invoke(Object proxy,
                                                                          Method method,
                                                                          Object[] args)
                                                             throws Throwable {

                                                         if (method
                                                                 .getName()
                                                                 .equals("equals")
                                                                 && args.length == 1) {

                                                             if (args[0] instanceof DefaultEndpoint) {
                                                                 DefaultEndpoint that =
                                                                         (DefaultEndpoint) args[0];
                                                                 return ObjectHelper
                                                                         .equal(delegate.getEndpointUri(),
                                                                                that.getEndpointUri())
                                                                         && ObjectHelper
                                                                                 .equal(delegate.getCamelContext()
                                                                                                .getName(),
                                                                                        that.getCamelContext()
                                                                                                .getName());
                                                             }
                                                             return false;

                                                         }

                                                         return method
                                                                 .invoke(delegate,
                                                                         args);

                                                     }

                                                 });
    }
}
