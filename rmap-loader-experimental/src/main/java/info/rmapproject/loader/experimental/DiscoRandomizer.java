
package info.rmapproject.loader.experimental;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * Returns a disco containing random values based on a template.
 * <p>
 * Occurrences of {@link #URI_REPLACE_TOKEN} and {@link #LITERAL_REPLACE_TOKEN}
 * will be replaced with a random URI or string value. If the uri replacement
 * template has a number in it matching {@link #URI_REF_REPLACE_TOKEN}, then all
 * occurrences of exact matching templates will have the same value. (For
 * example, "<code>__URI_1__, __URI__, __URI_1__</code>" may evaluate to "
 * <code>http://example.org/ONE, http://example.org/TWO, http://example.org/ONE"</code>
 * " The template is treated as just a text string, to the extent that it is rdf
 * in a particular format is a decision relegated to the template author.
 * </p>
 * 
 * @author apb18@cornell.edu
 */
public class DiscoRandomizer {

    private final String template;

    public static final String URI_REPLACE_TOKEN = "__URI__";

    public static final String URI_REF_REPLACE_TOKEN = "__URI_([0-9]*)__";

    public static final String LITERAL_REPLACE_TOKEN = "__LITERAL__";

    private static final String DEFAULT_DISCO_LOC = "/templates/disco.xml";

    private static final Pattern replace = Pattern.compile(String
            .format("%s|%s|%s",
                    URI_REPLACE_TOKEN,
                    LITERAL_REPLACE_TOKEN,
                    URI_REF_REPLACE_TOKEN));

    public DiscoRandomizer() {
        template = load(null);
    }

    public DiscoRandomizer(String loc) {
        template = load(loc);
    }

    private String load(String loc) {
        try {
            if (loc == null) {
                return IOUtils.toString(this.getClass()
                        .getResourceAsStream(DEFAULT_DISCO_LOC));
            } else {
                return FileUtils.readFileToString(new File(loc));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getRandomDisco() {
        return evaluate(template);
    }

    private static String evaluate(String template) {

        StringBuffer evaluated = new StringBuffer();

        Matcher matcher = replace.matcher(template);

        Map<String, String> uriRefs = new HashMap<String, String>();

        while (matcher.find()) {
            matcher.appendReplacement(evaluated, valueFor(matcher, uriRefs));
        }

        matcher.appendTail(evaluated);

        return evaluated.toString();
    }

    static String valueFor(Matcher match, Map<String, String> uriRefs) {
        if (match.group().equals(URI_REPLACE_TOKEN)) {
            return "http://example.org/test/" + UUID.randomUUID().toString();
        } else if (match.group().equals(LITERAL_REPLACE_TOKEN)) {
            return UUID.randomUUID().toString();
        } else if (match.groupCount() == 1) {
            String id = match.group(1);
            /* URI ref */
            if (!uriRefs.containsKey(id)) {
                uriRefs.put(id, "http://example.org/test/"
                        + UUID.randomUUID().toString());
            }

            return uriRefs.get(id);
        }

        else {
            /* Should never happen */
            return match.group();
        }
    }
}
