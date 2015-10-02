
package info.rmapproject.loader.integration;

import info.rmapproject.loader.camel.ContextFactory;

import java.io.File;
import java.nio.file.Paths;

import javax.inject.Inject;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.ConfigurationManager;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.exam.util.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.replaceConfigurationFile;

/**
 * Verify that XSL transform routes can be successfully run in karaf.
 * 
 * @author apb18
 */
@RunWith(PaxExam.class)
public class XslTransformIT
        extends CamelTestSupport {

    Logger log = LoggerFactory.getLogger(XslTransformIT.class);

    private static final String IMPL_TRANSFORM_XSL =
            "info.rmapproject.loader.transform.xsl.impl.XSLTransform";

    @Inject
    @Filter("(&(loader.role=transformer)(loader.format=test))")
    public RoutesBuilder xslRoutes;

    @Inject
    public ContextFactory factory;

    @Configuration
    public Option[] config() {
        MavenArtifactUrlReference karafUrl =
                maven().groupId("org.apache.karaf").artifactId("apache-karaf")
                        .version(karafVersion()).type("zip");

        MavenUrlReference rmapKarafl =
                maven().groupId("info.rmapproject")
                        .artifactId("rmap-loader-karaf").versionAsInProject()
                        .classifier("features").type("xml");

        MavenUrlReference camelRepo =
                maven().groupId("org.apache.camel.karaf")
                        .artifactId("apache-camel").type("xml")
                        .classifier("features").versionAsInProject();

        try {

            return new Option[] {
                    karafDistributionConfiguration().frameworkUrl(karafUrl)
                            .unpackDirectory(new File("target", "exam"))
                            .useDeployFolder(false),
                    keepRuntimeFolder(),
                    configureConsole().ignoreLocalConsole(),

                    /* Drop in a config file to enable our XSL transform service */
                    replaceConfigurationFile(String.format("etc/%s-test.cfg",
                                                           IMPL_TRANSFORM_XSL),
                                             Paths.get(getClass()
                                                     .getResource(String
                                                             .format("/cfg/%s-test.cfg",
                                                                     IMPL_TRANSFORM_XSL))
                                                     .toURI()).toFile()),

                    features(rmapKarafl, "rmap-loader"),
                    features(camelRepo, "camel-test")};
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String karafVersion() {
        ConfigurationManager cm = new ConfigurationManager();
        String karafVersion = cm.getProperty("pax.exam.karaf.version", "4.0.0");
        return karafVersion;
    }

    @Test
    public void smokeTest() throws Exception {
        CamelContext context = factory.newContext(xslRoutes);

        context.addRoutes(new RouteBuilder() {

            @Override
            public void configure() throws Exception {
                from("direct:out").to("mock:out");
            }
        });

        context.start();

        MockEndpoint test_out = (MockEndpoint) context.getEndpoint("mock:out");
        ProducerTemplate in = context.createProducerTemplate();

        in.sendBody("direct:in",
                    getClass().getResourceAsStream("/data/input.xml"));

        test_out.setExpectedMessageCount(5);
        test_out.assertIsSatisfied();

        factory.disposeContext(context);
    }
}
