
package info.rmapproject.loader.experimental.model;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import ioinformarics.oss.jackson.module.jsonld.annotation.JsonldId;
import ioinformarics.oss.jackson.module.jsonld.annotation.JsonldProperty;
import ioinformarics.oss.jackson.module.jsonld.annotation.JsonldType;

@JsonldType("http://example.org/test#Container")
public class Container {

    @JsonldId
    public URI id;

    @JsonldProperty("http://example.org/test#name")
    private String name;

    @JsonldProperty("http://example.org/test#creator")
    private List<String> creators = new ArrayList<>();

    @JsonldProperty("http://example.org/test#items")
    private List<Item> items = new ArrayList<>();

    public URI getId() {
        return id;
    }

    public void setId(URI id) {
        this.id = id;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public List<String> getCreators() {
        return creators;
    }

    public void setCreators(List<String> creators) {
        this.creators = creators;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
