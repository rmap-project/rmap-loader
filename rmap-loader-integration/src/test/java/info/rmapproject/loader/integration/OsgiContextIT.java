
package info.rmapproject.loader.integration;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import info.rmapproject.loader.camel.ContextFactory;

import java.io.File;

import javax.inject.Inject;

import org.apache.camel.CamelContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.ConfigurationManager;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.options.MavenUrlReference;

@RunWith(PaxExam.class)
public class OsgiContextIT {

    @Inject
    private ContextFactory factory;

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
                            .useDeployFolder(false), keepRuntimeFolder(),
                    configureConsole().ignoreLocalConsole(),

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
    public void registryTest() {

        String FIRST_ID = "one";
        String SECOND_ID = "two";

        CamelContext first = factory.newContext(FIRST_ID);
    }
}
