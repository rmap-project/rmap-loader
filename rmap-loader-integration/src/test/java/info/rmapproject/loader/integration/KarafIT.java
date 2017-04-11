
package info.rmapproject.loader.integration;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.replaceConfigurationFile;

import java.io.File;
import java.nio.file.Paths;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.ConfigurationManager;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.rmapproject.loader.camel.ContextFactory;

/**
 * Verify that XSL transform routes can be successfully run in karaf.
 *
 * @author apb18
 */
@RunWith(PaxExam.class)
public class KarafIT {

    Logger LOG = LoggerFactory.getLogger(KarafIT.class);

    @Inject
    public ContextFactory factory;

    @Inject
    public BundleContext cxt;

    @Configuration
    public Option[] config() {
        final MavenArtifactUrlReference karafUrl =
                maven().groupId("org.apache.karaf").artifactId("apache-karaf")
                        .version(karafVersion()).type("zip");

        final MavenUrlReference rmapKaraf =
                maven().groupId("info.rmapproject")
                        .artifactId("rmap-loader-karaf").versionAsInProject()
                        .classifier("features").type("xml");

        final MavenUrlReference camelRepo =
                maven().groupId("org.apache.camel.karaf")
                        .artifactId("apache-camel").type("xml")
                        .classifier("features").versionAsInProject();

        final MavenUrlReference activemqRepo =
                maven().groupId("org.apache.activemq")
                        .artifactId("activemq-karaf").type("xml")
                        .classifier("features").versionAsInProject();

        try {

            return new Option[] {
                karafDistributionConfiguration().frameworkUrl(karafUrl)
                        .unpackDirectory(new File("target", "exam"))
                        .useDeployFolder(false),
                keepRuntimeFolder(),
                configureConsole().ignoreLocalConsole(),

                deployConfig("info.rmapproject.service.activemq.cfg"),

                /* Drop in a config file to enable our XSL transform service */
                deployConfig("info.rmapproject.loader.transform.xsl.impl.XSLTransformService-test.cfg"),

                features(rmapKaraf, "rmap-loader-core"),
                features(rmapKaraf, "rmap-loader-activemq"),
                features(camelRepo, "camel-jetty"),
                features(activemqRepo, "activemq-broker"),

            };
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    Option deployConfig(String FileName) throws Exception {
        return replaceConfigurationFile(String.format("etc/%s", FileName),
                Paths.get(getClass()
                        .getResource(String
                                .format("/cfg/%s",
                                        FileName))
                        .toURI()).toFile());
    }

    @Test
    public void smokeTest() throws Exception {
        for (final ServiceReference<Object> references : cxt.getServiceReferences(Object.class,
                null)) {
            LOG.warn("\n\nSERVICE REFERENCE\n\n");
        }

        LOG.warn("\n\nDONE\n\n");
    }

    public static String karafVersion() {
        final ConfigurationManager cm = new ConfigurationManager();
        final String karafVersion = cm.getProperty("pax.exam.karaf.version", "4.0.0");
        return karafVersion;
    }
}
