
package info.rmapproject.loader.transform.xsl.impl;

import java.util.Map;

import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

/**
 * Short and sweet: runs the specified XSLT transform.
 * <p>
 * Note: If using with osgi and the loader framework, it is necessary to include
 * paramaters loader.domain and loader.format to translate messages from a
 * specific format to domain object model.
 * </p>
 * 
 * @author apb18
 */
@Component(service = RoutesBuilder.class, configurationPolicy = ConfigurationPolicy.REQUIRE, property = {"loader.role=transformer"})
public class XSLTransform
        extends RouteBuilder {

    /**
     * Absolute or relative path to XSLT file.
     * <p>
     * If the are any &lt;xsl:include&gt; directives, they are resolved relative
     * to the directory of file specified in {@link #CONFIG_PARAM_XSLT_FILE}.
     * That is to say, with xslt file parameter /path/to/my.xslt, an xsl include
     * of "other.xslt" will be assumed to be at /path/to/other.xslt"
     * </p>
     */
    public static final String CONFIG_PARAM_XSLT_FILE = "xslt.file";

    private String xslt_file;

    /* TODO: Provenance, when we get that sorted out */
    @Activate
    public void start(Map<String, String> config) {
        this.xslt_file = config.get(CONFIG_PARAM_XSLT_FILE);
    }

    @Override
    public void configure() throws Exception {
        from("direct:in").to("xslt:file:" + xslt_file).to("direct:out");
    }

}
