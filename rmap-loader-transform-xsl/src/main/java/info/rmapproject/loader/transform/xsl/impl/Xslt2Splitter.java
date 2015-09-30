
package info.rmapproject.loader.transform.xsl.impl;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.Controller;
import net.sf.saxon.TransformerFactoryImpl;
import net.sf.saxon.lib.OutputURIResolver;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.xml.ResultHandler;
import org.apache.camel.builder.xml.ResultHandlerFactory;
import org.apache.camel.builder.xml.StringResultHandlerFactory;
import org.osgi.service.component.annotations.Component;

@Component(service = Processor.class, property = {"name=xslt2split"})
public class Xslt2Splitter
        implements Processor {

    private TransformerFactory factory;

    private Map<String, TransformSpec> transformMap = new HashMap<>();

    public static final String HEADER_XSLT_FILE_NAME = "xslt2split.xsl_file";

    public Xslt2Splitter() {
        factory = new TransformerFactoryImpl();
    }

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

    private Transformer getTransformer(Message msg) {
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

        Transformer xslt;
        
        try {
            TransformSpec spec = transformMap.get(HEADER_XSLT_FILE_NAME);

            xslt = spec.template.newTransformer();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        msg.getHeaders().forEach(xslt::setParameter);

        return xslt;

    }

    private class CaptureTransformer {

        private final Transformer xslt;

        private final ResultHandlerFactory handler;

        public CaptureTransformer(Transformer xslt, ResultHandlerFactory handler) {
            this.xslt = xslt;
            this.handler = handler;
        }

        public Map<String, ResultHandler> transform(Source body) {
            Map<String, ResultHandler> capturedResults = new HashMap<>();

            ((Controller) xslt)
                    .setOutputURIResolver(new OutputCapture(handler,
                                                            capturedResults));

            try {
                ResultHandler defaultResultHandler = handler.createResult(null);
                xslt.transform(body, defaultResultHandler.getResult());
                if (capturedResults.isEmpty()) {
                    capturedResults.put("output.xml", defaultResultHandler);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
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

        private final Templates template;

        public TransformSpec(String xsltFile) {

            Path filePath = Paths.get(xsltFile);

            try {
                synchronized (transformMap) {
                    template =
                            factory.newTemplates(new StreamSource(filePath
                                    .toFile()));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
