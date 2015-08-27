
package info.rmapproject.loader.experimental.model;

import java.net.URI;
import java.util.Date;

import info.rmapproject.loader.model.RecordInfo;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ContainerTest {

    @Test
    public void serializationTest() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        RecordInfo record = new RecordInfo();

        record.setFormat("nsdl_dc");
        record.setId(URI.create("http://example.org/my_id"));
        record.setSrc(URI.create("file://this"));
        record.setRetrieveDate(new Date());

        mapper.writeValue(System.out, record);
    }
}
