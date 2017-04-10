
package info.rmapproject.loader.experimental.model;

import java.net.URI;
import java.util.Date;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import info.rmapproject.loader.model.HarvestInfo;
import info.rmapproject.loader.model.RecordInfo;

public class ContainerTest {

    @Test
    public void serializationTest() throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        final RecordInfo record = new RecordInfo();
        final HarvestInfo harvest = new HarvestInfo();
        harvest.setDate(new Date(2345));
        harvest.setId(URI.create("http://exampe.org/harvest-id"));
        harvest.setSrc(URI.create("http://example.org/oai"));

        record.setDate(new Date(1234));
        record.setId(URI.create("http://example.org/my_id"));
        record.setSrc(URI.create("file://this"));
        record.setHarvestInfo(harvest);

        mapper.writeValue(System.out, record);
    }
}
