package org.opendaylight.controller.config.it.base;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;

import com.google.common.collect.ObjectArrays;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.Calendar;

import javax.management.InstanceNotFoundException;

import org.junit.Before;
import org.opendaylight.controller.config.api.ConfigRegistry;
import org.opendaylight.controller.config.util.ConfigRegistryJMXClient;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractConfigTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractConfigTestBase.class);
    public static final String ORG_OPS4J_PAX_LOGGING_CFG = "etc/org.ops4j.pax.logging.cfg";

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

    public MavenArtifactUrlReference getKarafDistro() {
        MavenArtifactUrlReference karafUrl = maven()
                .groupId("org.opendaylight.controller")
                .artifactId("opendaylight-karaf-empty")
                .version("1.5.0-SNAPSHOT")
                .type("zip");
        return karafUrl;
    }

    @Configuration
    public Option[] config() {
        Option[] options = new Option[] {
                // KarafDistributionOption.debugConfiguration("5005", true),
                karafDistributionConfiguration()
                    .frameworkUrl(getKarafDistro())
                    .unpackDirectory(new File("target/exam"))
                    .useDeployFolder(false),
                keepRuntimeFolder(),
                features(getFeatureRepo() , getFeatureName()),
        };
        options = ObjectArrays.concat(options, getLoggingOptions(),Option.class);
        return options;
    }

    @Before
    public void setup() throws Exception {
        LOG.info("Module: {} Instance: {} attempting to configure.",
                getModuleName(),getInstanceName());
        Calendar start = Calendar.getInstance();
        ConfigRegistry configRegistryClient = new ConfigRegistryJMXClient(ManagementFactory
                .getPlatformMBeanServer());
        for(int i = 0;i<MODULE_TIMEOUT;i++) {
            try {
                configRegistryClient.lookupConfigBean(getModuleName(), getInstanceName());
                Thread.sleep(1);
            } catch (InstanceNotFoundException e) {
                if(i<MODULE_TIMEOUT) {
                    continue;
                } else {
                    throw e;
                }
            } catch (InterruptedException e) {
                LOG.error("Exception: ",e);
            }
        }
        Calendar stop = Calendar.getInstance();
        LOG.info("Module: {} Instance: {} configured after {} ms",
                getModuleName(),getInstanceName(),
                stop.getTimeInMillis() - start.getTimeInMillis());
    }

}
