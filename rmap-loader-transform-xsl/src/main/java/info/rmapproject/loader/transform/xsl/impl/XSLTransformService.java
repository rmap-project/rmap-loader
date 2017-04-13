
package info.rmapproject.loader.transform.xsl.impl;

import static info.rmapproject.loader.transform.xsl.impl.Xslt2Splitter.HEADER_XSLT_FILE_NAME;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;

import info.rmapproject.loader.model.RecordInfo;

/**
 * Short and sweet: runs the specified XSLT transform.
 * <p>
 * If the transform produces multiple output files, each one WILL HAVE THE SAME {@link RecordInfo} header values; so
 * some later step/transform will have to set the record IDs as approperiate - likely from some value present in the
 * transformed XML.
 * </p>
 *
 * @author apb18
 */
public class XSLTransformService extends RouteBuilder {

    public static final String PROP_HARVEST_RECORD_ID = "rmap.harvest.record.id";

    private final Processor xsltSplit = new Xslt2Splitter();

    /**
     * Absolute or relative path to XSLT file.
     * <p>
     * If the are any &lt;xsl:include&gt; directives, they are resolved relative to the directory of file specified in
     * {@link #CONFIG_PARAM_XSLT_FILE}. That is to say, with xslt file parameter /path/to/my.xslt, an xsl include of
     * "other.xslt" will be assumed to be at /path/to/other.xslt"
     * </p>
     */
    public static final String CONFIG_PARAM_XSLT_FILE = "xslt.file";

    private String xslt_file;

    private String src;

    private String dest;

    private String contentType;

    public void setSrcUri(String src) {
        this.src = src;
    }

    public void setDestUri(String dest) {
        this.dest = dest;
    }

    public void setOutputContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setXsltFile(String fileName) {
        this.xslt_file = fileName;
    }

    @Override
    public void configure() throws Exception {

        from(src)
                .id("xslt-split")
                .setHeader(HEADER_XSLT_FILE_NAME, constant(xslt_file))
                .process(xsltSplit).split(body())
                .process(e -> {
                    if (contentType != null) {
                        e.getIn().setHeader(Exchange.CONTENT_TYPE, contentType);
                    }
                })
                .to(dest);
    }
}
