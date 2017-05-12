/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.it.base;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import com.google.common.base.Stopwatch;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.management.ObjectName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.internal.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.opendaylight.controller.config.api.ConfigRegistry;
import org.opendaylight.controller.config.util.ConfigRegistryJMXClient;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractConfigTestBase {
    private static final String MAVEN_REPO_LOCAL = "maven.repo.local";
    private static final String ORG_OPS4J_PAX_URL_MVN_LOCAL_REPOSITORY = "org.ops4j.pax.url.mvn.localRepository";
    private static final String ETC_ORG_OPS4J_PAX_URL_MVN_CFG = "etc/org.ops4j.pax.url.mvn.cfg";
    private static final String ETC_ORG_OPS4J_PAX_LOGGING_CFG = "etc/org.ops4j.pax.logging.cfg";

    private static final String PAX_EXAM_UNPACK_DIRECTORY = "target/exam";
    private static final String KARAF_DEBUG_PORT = "5005";
    private static final String KARAF_DEBUG_PROP = "karaf.debug";
    private static final String KEEP_UNPACK_DIRECTORY_PROP = "karaf.keep.unpack";
    private static final Logger LOG = LoggerFactory.getLogger(AbstractConfigTestBase.class);
    public static final String ORG_OPS4J_PAX_LOGGING_CFG = "etc/org.ops4j.pax.logging.cfg";

    /*
     * Default values for karaf distro type, groupId, and artifactId
     */
    private static final String KARAF_DISTRO_TYPE = "zip";
    private static final String KARAF_DISTRO_ARTIFACTID = "opendaylight-karaf-empty";
    private static final String KARAF_DISTRO_GROUPID = "org.opendaylight.odlparent";

    /*
     * Property names to override defaults for karaf distro artifactId, groupId, version, and type
     */
    private static final String KARAF_DISTRO_VERSION_PROP = "karaf.distro.version";
    private static final String KARAF_DISTRO_TYPE_PROP = "karaf.distro.type";
    private static final String KARAF_DISTRO_ARTIFACTID_PROP = "karaf.distro.artifactId";
    private static final String KARAF_DISTRO_GROUPID_PROP = "karaf.distro.groupId";

    /**
     * Property file used to store the Karaf distribution version
     */
    private static final String PROPERTIES_FILENAME = "abstractconfigtestbase.properties";

    /*
     * Wait up to 10s for our configured module to come up
     */
    private static final int MODULE_TIMEOUT_MILLIS = 60000;

    /**
     * This method need only be overridden if using the config system.
     *
     * @return the config module name
     */
    @Deprecated
    public String getModuleName() {
        return null;
    }

    /**
     * This method need only be overridden if using the config system.
     *
     * @return the config module instance name
     */
    @Deprecated
    public String getInstanceName() {
        return null;
    }

    public abstract MavenUrlReference getFeatureRepo();

    public abstract String getFeatureName();

    public Option getLoggingOption() {
        Option option = editConfigurationFilePut(ORG_OPS4J_PAX_LOGGING_CFG,
                        logConfiguration(AbstractConfigTestBase.class),
                        LogLevel.INFO.name());
        return option;
    }

    /**
     * Override this method to provide more options to config
     *
     * @return An array of additional config options
     */
    protected Option[] getAdditionalOptions() {
        return null;
    }

    public String logConfiguration(final Class<?> klazz) {
        return "log4j.logger." + klazz.getPackage().getName();
    }

    public String getKarafDistro() {
        String groupId = System.getProperty(KARAF_DISTRO_GROUPID_PROP,KARAF_DISTRO_GROUPID);
        String artifactId = System.getProperty(KARAF_DISTRO_ARTIFACTID_PROP,KARAF_DISTRO_ARTIFACTID);
        String version = System.getProperty(KARAF_DISTRO_VERSION_PROP);
        String type = System.getProperty(KARAF_DISTRO_TYPE_PROP,KARAF_DISTRO_TYPE);
        if (version == null) {
            // We use a properties file to retrieve ${karaf.version}, instead of .versionAsInProject()
            // This avoids forcing all users to depend on Karaf in their POMs
            Properties abstractConfigTestBaseProps = new Properties();
            try (InputStream abstractConfigTestBaseInputStream = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream(PROPERTIES_FILENAME)) {
                abstractConfigTestBaseProps.load(abstractConfigTestBaseInputStream);
            } catch (final IOException e) {
                LOG.error("Unable to load {} to determine the Karaf version", PROPERTIES_FILENAME, e);
            }
            version = abstractConfigTestBaseProps.getProperty(KARAF_DISTRO_VERSION_PROP);
        }
        MavenArtifactUrlReference karafUrl = maven()
                .groupId(groupId)
                .artifactId(artifactId)
                .version(version)
                .type(type);
        return karafUrl.getURL();
    }

    protected Option mvnLocalRepoOption() {
        String mvnRepoLocal = System.getProperty(MAVEN_REPO_LOCAL, "");
        LOG.info("mvnLocalRepo \"{}\"", mvnRepoLocal);
        return editConfigurationFilePut(ETC_ORG_OPS4J_PAX_URL_MVN_CFG, ORG_OPS4J_PAX_URL_MVN_LOCAL_REPOSITORY,
                mvnRepoLocal);
    }

    @Configuration
    public Option[] config() {
        Option[] options = new Option[] {
                when(Boolean.getBoolean(KARAF_DEBUG_PROP))
                        .useOptions(KarafDistributionOption.debugConfiguration(KARAF_DEBUG_PORT, true)),
                karafDistributionConfiguration().frameworkUrl(getKarafDistro())
                        .unpackDirectory(new File(PAX_EXAM_UNPACK_DIRECTORY))
                        .useDeployFolder(false),
                when(Boolean.getBoolean(KEEP_UNPACK_DIRECTORY_PROP)).useOptions(keepRuntimeFolder()),
                features(getFeatureRepo(), getFeatureName()),
                mavenBundle("org.apache.aries.quiesce", "org.apache.aries.quiesce.api", "1.0.0"),
                getLoggingOption(),
                mvnLocalRepoOption(),
                editConfigurationFilePut(ETC_ORG_OPS4J_PAX_LOGGING_CFG, "log4j.rootLogger", "INFO, stdout, osgi:*")};
        return OptionUtils.combine(options, getAdditionalOptions());
    }

    @Before
    public void setup() throws Exception {
        String moduleName = getModuleName();
        String instanceName = getInstanceName();
        if(moduleName == null || instanceName == null) {
            return;
        }

        LOG.info("Module: {} Instance: {} attempting to configure.",
                moduleName, instanceName);
        Stopwatch stopWatch = Stopwatch.createStarted();
        ObjectName objectName = null;
        for(int i = 0;i<MODULE_TIMEOUT_MILLIS;i++) {
            try {
                ConfigRegistry configRegistryClient = new ConfigRegistryJMXClient(ManagementFactory
                        .getPlatformMBeanServer());
                objectName = configRegistryClient.lookupConfigBean(moduleName, instanceName);
                LOG.info("Module: {} Instance: {} ObjectName: {}.",
                        moduleName,instanceName,objectName);
                break;
            } catch (final Exception e) {
                if(i<MODULE_TIMEOUT_MILLIS) {
                    Thread.sleep(1);
                    continue;
                } else {
                    throw e;
                }
            }
        }
        if(objectName != null) {
            LOG.info("Module: {} Instance: {} configured after {} ms",
                moduleName,instanceName,
                stopWatch.elapsed(TimeUnit.MILLISECONDS));
        } else {
            throw new RuntimeException("NOT FOUND Module: " +moduleName + " Instance: " + instanceName +
                    " configured after " + stopWatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
        }
    }

    @Rule
    public TestRule watcher = new TestWatcher() {

        @Override
        protected void starting(final Description description) {
            LOG.info("TestWatcher: Starting test: {}", description.getDisplayName());
        }

        @Override
        protected void finished(final Description description) {
            LOG.info("TestWatcher: Finished test: {}", description.getDisplayName());
        }

        @Override
        protected void succeeded(final Description description) {
            LOG.info("TestWatcher: Test succeeded: {}", description.getDisplayName());
        }

        @Override
        protected void failed(final Throwable ex, final Description description) {
            LOG.info("TestWatcher: Test failed: {}", description.getDisplayName(), ex);
        }

        @Override
        protected void skipped(final AssumptionViolatedException ex, final Description description) {
            LOG.info("TestWatcher: Test skipped: {} ", description.getDisplayName(), ex);
        }
    };

}
