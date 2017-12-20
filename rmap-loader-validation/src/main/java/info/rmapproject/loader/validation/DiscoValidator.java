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

	static final String RMAP_NS = "http://purl.org/ontology/rmap#";

	static final String ORE_NS = "http://www.openarchives.org/ore/terms/";

	static final String RDF_NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

	public static void validate(InputStream rdf, Format format) {
		final Model model = ModelFactory.createDefaultModel();
		model.read(rdf, "", format.toString());

		final List<Statement> discoResources =
				cut(model,
						new SimpleSelector(null, model.getProperty(RDF_NS + "type"), model.getResource(RMAP_NS +
								"DiSCO")))
				.listStatements().toList();

		if (discoResources.size() != 1) {
			throw new ValidationException("Wrong number of DiSCO resources.  Should be 1, is " + discoResources
					.size());
		}

		final Model discoTriples = cut(model, new SimpleSelector(discoResources.get(0).getSubject(), null,
				(RDFNode) null));

		/* Any number of creators, so we don't care */
		cut(discoTriples, new SimpleSelector(null, discoTriples.getProperty(DCTERMS_NS + "creator"), (RDFNode) null));

		/* Zero or one descriptions */
		if (1 < cut(discoTriples,
				new SimpleSelector(null, discoTriples.getProperty(DCTERMS_NS + "description"), (RDFNode) null))
				.listStatements().toList().size()) {
			throw new ValidationException("More than one description detected");
		}

		/* The rest need to be aggregated resources */
		final List<Resource> aggregatedResources =
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
			final StringWriter writer = new StringWriter();
			discoTriples.write(writer, "N-TRIPLE");
			throw new ValidationException("DiSCO has extra triples " + writer.toString());
		}

		/* Remove all triples with our aggregated resources as subject */
		for (final Resource aggregated : aggregatedResources) {
			removeTree(aggregated, model);
		}

		/* Now verify that there are none left */
		if (!model.isEmpty()) {
			final StringWriter writer = new StringWriter();
			model.write(writer, "N-TRIPLE");
			throw new ValidationException("Unconnected triples found" + writer.toString());
		}
	}

	/**
	 * Compares two sets of RDF passed in to determine whether they are different, and therefore an update is required.
	 * TODO: doesn't currently work for BNODES that aren't DiSCO ID
	 * @param rmapRdf
	 * @param newRdf
	 * @param format
	 * @return true if different
	 */
	public static boolean different(InputStream rmapRdf, InputStream newRdf, Format format) {
		final Model rmapModel = ModelFactory.createDefaultModel();
		rmapModel.read(rmapRdf, "", format.toString());

		final Model newModel = ModelFactory.createDefaultModel();
		newModel.read(newRdf, "", format.toString());

		Selector idSelector = new SimpleSelector(null, rmapModel.getProperty(RDF_NS + "type"), rmapModel.getResource(RMAP_NS + "DiSCO"));

		Resource rmapDiscoUri = rmapModel.listStatements(idSelector).toList().get(0).getSubject();
		Selector discoPropsSelector = new SimpleSelector(rmapDiscoUri, null, (RDFNode) null);
		List<Statement> rmapDiscoStmts = rmapModel.listStatements(discoPropsSelector).toList();

		Resource newDiscoUri = newModel.listStatements(idSelector).toList().get(0).getSubject();

		Model newStmts = newModel.difference(rmapModel);

		for (Statement stmt : rmapDiscoStmts){
			newStmts = remove(newStmts, new SimpleSelector(newDiscoUri, stmt.getPredicate(), stmt.getObject()));
			newStmts = remove(newStmts, new SimpleSelector(rmapDiscoUri, stmt.getPredicate(), stmt.getObject()));        	
		}

		/*
		if (newStmts.size()>0) {
			//still some differences
			for (Statement stmt : newStmts.listStatements().toList()){
				if (stmt.getSubject() instanceof AnonId) {
				//check for non-bnode version        			
				}
			}        	
		}
		 */

		if (newStmts.size()>0){
			return true;
		} else {
			return false;
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

		@Override
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
	 * Remove and return a subset of a Model with the given Selector.
	 *
	 * @param from Model that will have statements removed
	 * @param selector Selector for matching statements to remove
	 * @return a Model containing all the extracted triples.
	 */
	public static Model cut(Model from, Selector selector) {

		final Model excised = ModelFactory.createDefaultModel();
		final List<Statement> toRemove = from.listStatements(selector).toList();

		excised.add(toRemove);
		from.remove(toRemove);

		return excised;
	}

	/**
	 * Remove a subset of a Model with the given Selector and return the rest.
	 *
	 * @param from Model that will have statements removed
	 * @param selector Selector for matching statements to remove
	 * @return a Model containing all the extracted triples.
	 */
	public static Model remove(Model from, Selector selector) {
		final List<Statement> toRemove = from.listStatements(selector).toList();
		from.remove(toRemove);
		return from;
	}

}
