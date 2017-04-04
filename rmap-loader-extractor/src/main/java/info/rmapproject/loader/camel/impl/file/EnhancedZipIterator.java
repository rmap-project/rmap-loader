
package info.rmapproject.loader.camel.impl.file;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.impl.DefaultMessage;
import org.apache.camel.util.IOHelper;

import static info.rmapproject.loader.camel.impl.file.EnhancedZipDataFormat.HEADER_ZIP_ENTRY;

public class EnhancedZipIterator
        implements Iterator<Message>, Closeable {

    private final Message inputMessage;

    private ZipInputStream zipInputStream;

    private Message parent;

    public EnhancedZipIterator(Message inputMessage) {
        this.inputMessage = inputMessage;
        final InputStream inputStream = inputMessage.getBody(InputStream.class);
        if (inputStream instanceof ZipInputStream) {
            zipInputStream = (ZipInputStream) inputStream;
        } else {
            zipInputStream = new ZipInputStream(new BufferedInputStream(inputStream));
        }
        parent = null;
    }

    @Override
    public boolean hasNext() {
        try {
            if (zipInputStream == null) {
                return false;
            }
            boolean availableDataInCurrentEntry = zipInputStream.available() == 1;
            if (!availableDataInCurrentEntry) {
                // advance to the next entry.
                parent = getNextElement();
                // check if there are more data.
                availableDataInCurrentEntry = zipInputStream.available() == 1;
                // if there are not more data, close the stream.
                if (!availableDataInCurrentEntry) {
                    zipInputStream.close();
                }
            }
            return availableDataInCurrentEntry;
        } catch (final IOException exception) {
            // Just wrap the IOException as CamelRuntimeException
            throw new RuntimeCamelException(exception);
        }
    }

    @Override
    public Message next() {
        if (parent == null) {
            parent = getNextElement();
        }
        final Message answer = parent;
        parent = null;
        checkNullAnswer(answer);

        return answer;
    }

    private Message getNextElement() {
        Message answer = null;

        if (zipInputStream != null) {
            try {
                final ZipEntry current = getNextEntry();

                if (current != null) {
                    answer = new DefaultMessage();
                    answer.getHeaders().putAll(inputMessage.getHeaders());
                    answer.setHeader(HEADER_ZIP_ENTRY, current.clone());
                    answer.setHeader(Exchange.FILE_NAME, current.getName());
                    answer.setBody(new ZipInputStreamWrapper(zipInputStream));
                    return answer;
                }

            } catch (final IOException exception) {
                // Just wrap the IOException as CamelRuntimeException
                throw new RuntimeCamelException(exception);
            }
        }

        return answer;
    }

    public void checkNullAnswer(Message answer) {
        if (answer == null && zipInputStream != null) {
            IOHelper.close(zipInputStream);
            zipInputStream = null;
        }
    }

    private ZipEntry getNextEntry() throws IOException {
        ZipEntry entry;

        while ((entry = zipInputStream.getNextEntry()) != null) {
            if (!entry.isDirectory()) {
                return entry;
            }
        }

        return null;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException {
        if (zipInputStream != null) {
            zipInputStream.close();
        }
    }
}
