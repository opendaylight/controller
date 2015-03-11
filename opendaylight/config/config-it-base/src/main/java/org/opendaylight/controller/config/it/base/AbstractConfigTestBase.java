package org.opendaylight.controller.config.it.base;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.util.Calendar;

import javax.management.ObjectName;

import org.junit.Before;
import org.opendaylight.controller.config.api.ConfigRegistry;
import org.opendaylight.controller.config.util.ConfigRegistryJMXClient;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ObjectArrays;

public abstract class AbstractConfigTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractConfigTestBase.class);
    public static final String ORG_OPS4J_PAX_LOGGING_CFG = "etc/org.ops4j.pax.logging.cfg";
    
    /*
     * Default values for karaf distro version, type, groupId, and artifactId
     */
    private static final String KARAF_DISTRO_VERSION = "3.0.2";
    private static final String KARAF_DISTRO_TYPE = "zip";
    private static final String KARAF_DISTRO_ARTIFACTID = "apache-karaf";
    private static final String KARAF_DISTRO_GROUPID = "org.apache.karaf";

    /*
     * Property names to override defaults for karaf distro artifactId, groupId, version, and type
     */
    private static final String KARAF_DISTRO_VERSION_PROP = "karaf.distro.version";
    private static final String KARAF_DISTRO_TYPE_PROP = "karaf.distro.type";
    private static final String KARAF_DISTRO_ARTIFACTID_PROP = "karaf.distro.artifactId";
    private static final String KARAF_DISTRO_GROUPID_PROP = "karaf.distro.groupId";

    /*
     * Wait up to 10s for our configured module to come up
     */
    private static final int MODULE_TIMEOUT = 10000;

    public abstract String getModuleName();

    public abstract String getInstanceName();

    public abstract MavenUrlReference getFeatureRepo();

    public abstract String getFeatureName();

    public Option[] getLoggingOptions() {
        Option[] options = new Option[] {
                editConfigurationFilePut(ORG_OPS4J_PAX_LOGGING_CFG,
                        logConfiguration(AbstractConfigTestBase.class),
                        LogLevel.INFO.name()),
        };
        return options;
    }

    public String logConfiguration(Class<?> klazz) {
        return "log4j.logger." + klazz.getPackage().getName();
    }

    public String getKarafDistro() {
        String groupId = System.getProperty(KARAF_DISTRO_GROUPID_PROP,KARAF_DISTRO_GROUPID);
        String artifactId = System.getProperty(KARAF_DISTRO_ARTIFACTID_PROP,KARAF_DISTRO_ARTIFACTID);
        String version = System.getProperty(KARAF_DISTRO_VERSION_PROP,KARAF_DISTRO_VERSION);
        String type = System.getProperty(KARAF_DISTRO_TYPE_PROP,KARAF_DISTRO_TYPE);
        MavenArtifactUrlReference karafUrl = maven()
                .groupId(groupId)
                .artifactId(artifactId)
                .version(version)
                .type(type);
        return karafUrl.getURL();
    }

    @Configuration
    public Option[] config() {
        Option[] options = new Option[] {
        		when(Boolean.getBoolean("karaf.debug")).useOptions(KarafDistributionOption.debugConfiguration("5005", true)),
                karafDistributionConfiguration()
                    .frameworkUrl(getKarafDistro())
                    .unpackDirectory(new File("target/exam"))
                    .useDeployFolder(false),
                keepRuntimeFolder(),
                features(getFeatureRepo() , getFeatureName()),
        };
        options = ObjectArrays.concat(options, getLoggingOptions(),Option.class);
        options = ObjectArrays.concat(options, getExtraBundleOptions(),Option.class);
        //options = ObjectArrays.concat(options, getKarafEditConfigurationOptions(),Option.class);
        logOptions("Options: {}",options);
        return options;
    }

    protected Option[] getExtraBundleOptions() {
		Option[] options = new Option[] {
			mavenBundle()
				.groupId("org.opendaylight.controller")
				.artifactId("config-it-base")
				.versionAsInProject()
		};
		LOG.info("Adding extra bundles: {}", options);
		
		return options;
	}
    
    protected Option[] getKarafEditConfigurationOptions() {
    	Option[] options = new Option[] {
    		editConfigurationFilePut("etc/org.ops4j.pax.url.mvn.cfg", "org.ops4j.pax.url.mvn.localRepository", "${user.home}/.m2/repository/")
    	};
    	return options;
    }

	@Before
    public void setup() throws Exception {
        LOG.info("Module: {} Instance: {} attempting to configure.",
                getModuleName(),getInstanceName());
        Calendar start = Calendar.getInstance();
        for(int i = 0;i<MODULE_TIMEOUT;i++) {
            try {
            	ConfigRegistry configRegistryClient = new ConfigRegistryJMXClient(ManagementFactory
                        .getPlatformMBeanServer());
                ObjectName objectName = configRegistryClient.lookupConfigBean(getModuleName(), getInstanceName());
                LOG.info("Module: {} Instance: {} ObjectName: {}.",
                        getModuleName(),getInstanceName(),objectName);
                Thread.sleep(1);
            } catch (Exception e) {
                if(i<MODULE_TIMEOUT) {
                    continue;
                } else {
                    throw e;
                }
            }
        }
        Calendar stop = Calendar.getInstance();
        LOG.info("Module: {} Instance: {} configured after {} ms",
                getModuleName(),getInstanceName(),
                stop.getTimeInMillis() - start.getTimeInMillis());
    }
	
	protected void logOptions(String message, Option[] options) {
		for (Option option:options) {
			LOG.info(message,option);
		}
	}

}
