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

package info.rmapproject.loader.transform.xsl.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.xml.ResultHandler;
import org.apache.camel.builder.xml.ResultHandlerFactory;
import org.apache.camel.builder.xml.StreamResultHandler;
import org.apache.camel.builder.xml.StringResultHandlerFactory;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.xml.resolver.tools.CatalogResolver;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import net.sf.saxon.lib.OutputURIResolver;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;

/**
 * Camel {@link Processor} that applies an XSLT 1.0 or 2.0 template, producing a list of result documents.
 * <p>
 * Uses Saxon to perform transform, as it is the only free and open source implementation of XSLT 2.0 in Java. The
 * Processor accepts a {@link Message} containing an XML document, and will look in the message headers for the
 * presence of the mandatory header {@link Xslt2Splitter#HEADER_XSLT_FILE_NAME}. This shall contain an absolute path
 * to an xslt file on the local filesystem.
 * </p>
 * <p>
 * Because XSLT 2.0 defines a means of transforming a single source document into several result documents via
 * <code>xsl:result-document</code>, and because the Camel XSLT component adopts a strict 1:1 model used in XSLT 1.0,
 * this processor modifies the body of the incoming message and replaces it with a {@link List} of Messages, each
 * containing the original headers, and one output document. In addition, the processor adds to each result message a
 * header {@link Exchange#FILE_NAME} containing the logical file name as determined by
 * <code>xsl:result-document</code>.
 * </p>
 * <p>
 * This Processor is expected to be used in conjunction with {@link ProcessorDefinition#split()} in Camel, which will
 * split the resulting <code>List&lt;Message&gt;</code> into individual messages for further processing by Camel
 * route, as in <code>...process(xsltSplit).split(body())...</code>, where <code>xsltsplit</code> is an instance of
 * {@link Xslt2Splitter}
 * </p>
 *
 * @author apb18
 */
public class Xslt2Splitter
        implements Processor {

    private static final net.sf.saxon.s9api.Processor SAXON = new net.sf.saxon.s9api.Processor(false);

    private final Map<String, TransformSpec> transformMap = new HashMap<>();

    public static final String HEADER_XSLT_FILE_NAME = "xslt2split.xsl_file";

    @Override
    public void process(Exchange exchange) throws Exception {
        final Message msg = exchange.getIn();

        final CaptureTransformer transform = new CaptureTransformer(getTransformer(msg),
                new StringResultHandlerFactory());

        final List<Message> splitMessages = new LinkedList<>();

        final SAXParser p = SAXParserFactory.newInstance().newSAXParser();
        final XMLReader reader = p.getXMLReader();
        reader.setEntityResolver(new EntityResolver() {

            CatalogResolver delegate = new CatalogResolver();

            @Override
            public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                return delegate.resolveEntity(publicId, systemId);
            }
        });

        for (final Map.Entry<String, ResultHandler> result : transform
                .transform(new SAXSource(reader, new InputSource(msg.getBody(InputStream.class)))).entrySet()) {

            final Message outputDoc = msg.copy();
            outputDoc.setHeader(Exchange.FILE_NAME, result.getKey());
            result.getValue().setBody(outputDoc);
            splitMessages.add(outputDoc);
        }

        msg.setBody(splitMessages);
    }

    private XsltTransformer getTransformer(Message msg) {
        if (msg.getHeader(HEADER_XSLT_FILE_NAME) == null) {
            throw new RuntimeException("Header " + HEADER_XSLT_FILE_NAME + " is not defined");
        }

        if (!transformMap.containsKey(HEADER_XSLT_FILE_NAME)) {

            transformMap.put(HEADER_XSLT_FILE_NAME,
                    new TransformSpec(msg.getHeader(HEADER_XSLT_FILE_NAME, String.class)));
        }

        XsltTransformer xslt;

        try {
            final TransformSpec spec = transformMap.get(HEADER_XSLT_FILE_NAME);

            xslt = spec.template.load();

        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        msg.getHeaders().entrySet().stream()
                .filter(h -> h.getValue() != null)
                .forEach(h -> xslt.setParameter(
                        new QName(h.getKey()), new XdmAtomicValue(h.getValue().toString())));

        return xslt;

    }

    private class CaptureTransformer {

        private final XsltTransformer xslt;

        private final ResultHandlerFactory handler;

        public CaptureTransformer(XsltTransformer xslt, ResultHandlerFactory handler) {
            this.xslt = xslt;
            this.handler = handler;
        }

        public Map<String, ResultHandler> transform(Source body) {
            final Map<String, ResultHandler> capturedResults = new HashMap<>();

            xslt.getUnderlyingController().setErrorListener(new ErrorListener() {

                @Override
                public void warning(TransformerException exception) throws TransformerException {
                    exception.printStackTrace();
                }

                @Override
                public void fatalError(TransformerException exception) throws TransformerException {
                    exception.printStackTrace();
                }

                @Override
                public void error(TransformerException exception) throws TransformerException {
                    exception.printStackTrace();
                }
            });
            xslt.getUnderlyingController().setURIResolver(new URIResolver() {

                CatalogResolver resolver = new CatalogResolver();

                @Override
                public Source resolve(String href, String base) throws TransformerException {

                    return resolver.resolve(href, base);
                }
            });
            xslt.getUnderlyingController().setOutputURIResolver(new OutputCapture(handler, capturedResults));

            OutputStream out = null;
            try {

                final StreamResultHandler defaultResultHandler = new StreamResultHandler();
                out = ((StreamResult) defaultResultHandler.getResult()).getOutputStream();

                xslt.setSource(body);
                xslt.setDestination(SAXON.newSerializer(out));

                xslt.transform();

                if (capturedResults.isEmpty()) {
                    capturedResults.put("output.xml", defaultResultHandler);
                }
            } catch (final Exception e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    out.close();
                } catch (final Exception e) {
                    /* Nothing */
                }
            }

            return capturedResults;

        }
    }

    private class OutputCapture
            implements OutputURIResolver {

        private final ResultHandlerFactory factory;

        private final Map<String, ResultHandler> container;

        public OutputCapture(ResultHandlerFactory factory, Map<String, ResultHandler> container) {
            this.factory = factory;
            this.container = container;
        }

        @Override
        public OutputURIResolver newInstance() {
            return this;
        }

        @Override
        public Result resolve(String href, String base) throws TransformerException {

            try {
                final ResultHandler handler = factory.createResult(null);
                container.put(href, handler);
                return handler.getResult();
            } catch (final Exception e) {
                throw new RuntimeException();
            }
        }

        @Override
        public void close(Result result) throws TransformerException {
            /* Nothing */
        }

    }

    private class TransformSpec {

        private final XsltExecutable template;

        public TransformSpec(String xsltFile) {

            final Path filePath = Paths.get(xsltFile);

            try {
                synchronized (transformMap) {

                    template = SAXON.newXsltCompiler().compile(new StreamSource(filePath.toFile()));

                }
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
