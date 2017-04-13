
package info.rmapproject.loader.camel.impl.file;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ZipInputStreamWrapper
        extends BufferedInputStream {

    public ZipInputStreamWrapper(InputStream in, int size) {
        super(in, size);
    }

    public ZipInputStreamWrapper(InputStream in) {
        super(in);
    }

    @Override
    public void close() throws IOException {
        InputStream input = in;
        try {
            in = null;
            super.close();
        } finally {
            in = input;
        }
    }

}
