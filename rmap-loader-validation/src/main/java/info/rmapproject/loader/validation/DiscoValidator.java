
package info.rmapproject.loader.validation;

import java.io.InputStream;
import java.io.StringWriter;

import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Selector;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;

public class DiscoValidator {

    static final String DCTERMS_NS = "http://purl.org/dc/terms/";

    static final String RMAP_NS = "http://rmap-project.org/rmap/terms/";
    
    static final String ORE_NS = "http://www.openarchives.org/ore/terms/";

    static final String RDF_NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    public static void validate(InputStream rdf, Format format) {
        Model model = ModelFactory.createDefaultModel();
        model.read(rdf, "", format.toString());

        List<Statement> discoResources =
                cut(model,
                    new SimpleSelector(null, model.getProperty(RDF_NS + "type"), model.getResource(RMAP_NS + "DiSCO")))
                            .listStatements().toList();

        if (discoResources.size() != 1)
            throw new ValidationException("Wrong number of DiSCO resources.  Should be 1, is " + discoResources.size());

        Model discoTriples = cut(model, new SimpleSelector(discoResources.get(0).getSubject(), null, (RDFNode) null));

        /* Any number of creators, so we don't care */
        cut(discoTriples, new SimpleSelector(null, discoTriples.getProperty(DCTERMS_NS + "creator"), (RDFNode) null));

        /* Zero or one descriptions */
        if (1 < cut(discoTriples,
                    new SimpleSelector(null, discoTriples.getProperty(DCTERMS_NS + "description"), (RDFNode) null))
                            .listStatements().toList().size()) {
            throw new ValidationException("More than one description detected");
        }

        /* The rest need to be aggregated resources */
        List<Resource> aggregatedResources =
                cut(discoTriples,
                    new SimpleSelector(null, discoTriples.getProperty(ORE_NS + "aggregates"), (RDFNode) null))
                            .listObjects().mapWith(n -> n.asResource()).filterKeep(r -> {
                                if (r.isAnon()) {
                                    throw new ValidationException("Cannot aggregate blank nodes");
                                }
                                return true;
                            }).toList();

        /* Make sure DiSCO resource triples have been exhausted */
        if (!discoTriples.isEmpty()) {
            StringWriter writer = new StringWriter();
            discoTriples.write(writer, "N-TRIPLE");
            throw new ValidationException("DiSCO has extra triples " + writer.toString());
        }

        /* Remove all triples with our aggregated resources as subject */
        for (Resource aggregated : aggregatedResources) {
            removeTree(aggregated, model);
        }

        /* Now verify that there are none left */
        if (!model.isEmpty()) {
            StringWriter writer = new StringWriter();
            model.write(writer, "N-TRIPLE");
            throw new ValidationException("Unconnected triples found" + writer.toString());
        }
    }

    static void removeTree(Resource resource, Model model) {
        cut(model, new SimpleSelector(resource, null, (RDFNode) null)).listObjects().filterKeep(o -> o.isResource())
                .mapWith(RDFNode::asResource).forEachRemaining(r -> removeTree(r, model));
    }

    public enum Format {
        TURTLE("TTL"), RDF_XML("RDF/XML");

        final String jena_name;

        Format(String name) {
            jena_name = name;
        }

        public String toString() {
            return jena_name;
        }
    }

    @SuppressWarnings("serial")
    public static class ValidationException
            extends RuntimeException {

        public ValidationException(String msg) {
            super(msg);
        }
    }

    /**
     * Remove a subset of a Model with the given Selector.
     * 
     * @param from
     *        Model that will have statements removed
     * @param selector
     *        Selector for matching statements to remove
     * @return a Model containing all the extracted triples.
     */
    public static Model cut(Model from, Selector selector) {

        Model excised = ModelFactory.createDefaultModel();
        List<Statement> toRemove = from.listStatements(selector).toList();

        excised.add(toRemove);
        from.remove(toRemove);

        return excised;
    }

}
