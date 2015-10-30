
package info.rmapproject.loader.transform.xsl.impl;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.lib.OutputURIResolver;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.xml.ResultHandler;
import org.apache.camel.builder.xml.ResultHandlerFactory;
import org.apache.camel.builder.xml.StreamResultHandler;
import org.apache.camel.builder.xml.StringResultHandlerFactory;
import org.apache.camel.model.ProcessorDefinition;
import org.osgi.service.component.annotations.Component;

/**
 * Camel {@link Processor} that applies an XSLT 1.0 or 2.0 template, producing a
 * list of result documents.
 * <p>
 * Uses Saxon to perform transform, as it is the only free and open source
 * implementation of XSLT 2.0 in Java. The Processor accepts a {@link Message}
 * containing an XML document, and will look in the message headers for the
 * presence of the mandatory header {@link Xslt2Splitter#HEADER_XSLT_FILE_NAME}.
 * This shall contain an absolute path to an xslt file on the local filesystem.
 * </p>
 * <p>
 * Because XSLT 2.0 defines a means of transforming a single source document
 * into several result documents via <code>xsl:result-document</code>, and
 * because the Camel XSLT component adopts a strict 1:1 model used in XSLT 1.0,
 * this processor modifies the body of the incoming message and replaces it with
 * a {@link List} of Messages, each containing the original headers, and one
 * output document. In addition, the processor adds to each result message a
 * header {@link Exchange#FILE_NAME} containing the logical file name as
 * determined by <code>xsl:result-document</code>.
 * </p>
 * <p>
 * This Processor is expected to be used in conjunction with
 * {@link ProcessorDefinition#split()} in Camel, which will split the resulting
 * <code>List&lt;Message&gt;</code> into individual messages for further
 * processing by Camel route, as in
 * <code>...process(xsltSplit).split(body())...</code>, where
 * <code>xsltsplit</code> is an instance of {@link Xslt2Splitter}
 * </p>
 * 
 * @author apb18
 */
@Component(service = Processor.class, property = {"name=xslt2split"})
public class Xslt2Splitter
        implements Processor {

    private static final net.sf.saxon.s9api.Processor SAXON =
            new net.sf.saxon.s9api.Processor(false);

    private Map<String, TransformSpec> transformMap = new HashMap<>();

    public static final String HEADER_XSLT_FILE_NAME = "xslt2split.xsl_file";

    @Override
    public void process(Exchange exchange) throws Exception {
        Message msg = exchange.getIn();

        CaptureTransformer transform =
                new CaptureTransformer(getTransformer(msg),
                                       new StringResultHandlerFactory());

        List<Message> splitMessages = new LinkedList<>();

        for (Map.Entry<String, ResultHandler> result : transform
                .transform(new StreamSource(msg.getBody(InputStream.class)))
                .entrySet()) {

            Message outputDoc = msg.copy();
            outputDoc.setHeader(Exchange.FILE_NAME, result.getKey());
            result.getValue().setBody(outputDoc);
            splitMessages.add(outputDoc);
        }

        msg.setBody(splitMessages);
    }

    private XsltTransformer getTransformer(Message msg) {
        if (msg.getHeader(HEADER_XSLT_FILE_NAME) == null) {
            throw new RuntimeException("Header " + HEADER_XSLT_FILE_NAME
                    + " is not defined");
        }

        if (!transformMap.containsKey(HEADER_XSLT_FILE_NAME)) {

            transformMap.put(HEADER_XSLT_FILE_NAME,
                             new TransformSpec(msg
                                     .getHeader(HEADER_XSLT_FILE_NAME,
                                                String.class)));
        }

        XsltTransformer xslt;

        try {
            TransformSpec spec = transformMap.get(HEADER_XSLT_FILE_NAME);

            xslt = spec.template.load();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        for (Map.Entry<String, Object> header : msg.getHeaders().entrySet()) {
            xslt.setParameter(new QName(header.getKey()),
                              new XdmAtomicValue(header.getValue().toString()));
        }

        return xslt;

    }

    private class CaptureTransformer {

        private final XsltTransformer xslt;

        private final ResultHandlerFactory handler;

        public CaptureTransformer(XsltTransformer xslt,
                                  ResultHandlerFactory handler) {
            this.xslt = xslt;
            this.handler = handler;
        }

        public Map<String, ResultHandler> transform(Source body) {
            Map<String, ResultHandler> capturedResults = new HashMap<>();

            xslt.getUnderlyingController()
                    .setOutputURIResolver(new OutputCapture(handler,
                                                            capturedResults));

            OutputStream out = null;
            try {

                StreamResultHandler defaultResultHandler =
                        new StreamResultHandler();
                out =
                        ((StreamResult) defaultResultHandler.getResult())
                                .getOutputStream();

                xslt.setSource(body);
                xslt.setDestination(SAXON.newSerializer(out));

                xslt.transform();

                if (capturedResults.isEmpty()) {
                    capturedResults.put("output.xml", defaultResultHandler);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    out.close();
                } catch (Exception e) {
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

        public OutputCapture(ResultHandlerFactory factory,
                             Map<String, ResultHandler> container) {
            this.factory = factory;
            this.container = container;
        }

        @Override
        public OutputURIResolver newInstance() {
            return this;
        }

        @Override
        public Result resolve(String href, String base)
                throws TransformerException {

            try {
                ResultHandler handler = factory.createResult(null);
                container.put(href, handler);
                return handler.getResult();
            } catch (Exception e) {
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

            Path filePath = Paths.get(xsltFile);

            try {
                synchronized (transformMap) {

                    template =
                            SAXON.newXsltCompiler()
                                    .compile(new StreamSource(filePath.toFile()));

                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
