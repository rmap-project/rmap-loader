
package info.rmapproject.loader.camel.impl.file;

import java.io.InputStream;
import java.io.OutputStream;

import org.apache.camel.Exchange;
import org.apache.camel.spi.DataFormat;

public class EnhancedZipDataFormat
        implements DataFormat {

    public static final String HEADER_ZIP_ENTRY = "zip.entry";

    @Override
    public void marshal(Exchange exchange, Object graph, OutputStream stream) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object unmarshal(Exchange exchange, InputStream stream) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

}
