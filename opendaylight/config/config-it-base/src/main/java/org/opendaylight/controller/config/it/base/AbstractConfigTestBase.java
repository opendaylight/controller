/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.it.base;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;

import com.google.common.base.Stopwatch;

import java.io.File;
import java.lang.management.ManagementFactory;
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
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractConfigTestBase {

    private static final String PAX_EXAM_UNPACK_DIRECTORY = "target/exam";
    private static final String KARAF_DEBUG_PORT = "5005";
    private static final String KARAF_DEBUG_PROP = "karaf.debug";
    private static final String KEEP_UNPACK_DIRECTORY_PROP = "karaf.keep.unpack";
    private static final Logger LOG = LoggerFactory.getLogger(AbstractConfigTestBase.class);
    public static final String ORG_OPS4J_PAX_LOGGING_CFG = "etc/org.ops4j.pax.logging.cfg";

    /*
     * Default values for karaf distro version, type, groupId, and artifactId
     */
    private static final String KARAF_DISTRO_VERSION = "3.0.4";
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
    private static final int MODULE_TIMEOUT_MILLIS = 60000;

    public abstract String getModuleName();

    public abstract String getInstanceName();

    public abstract MavenUrlReference getFeatureRepo();

    public abstract String getFeatureName();

    public Option getLoggingOption() {
        Option option = editConfigurationFilePut(ORG_OPS4J_PAX_LOGGING_CFG,
                        logConfiguration(AbstractConfigTestBase.class),
                        LogLevel.INFO.name());
        return option;
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
                when(Boolean.getBoolean(KARAF_DEBUG_PROP))
                        .useOptions(KarafDistributionOption.debugConfiguration(KARAF_DEBUG_PORT, true)),
                karafDistributionConfiguration().frameworkUrl(getKarafDistro())
                        .unpackDirectory(new File(PAX_EXAM_UNPACK_DIRECTORY))
                        .useDeployFolder(false),
                when(Boolean.getBoolean(KEEP_UNPACK_DIRECTORY_PROP)).useOptions(keepRuntimeFolder()),
                features(getFeatureRepo(), getFeatureName()),
                getLoggingOption()};
        return options;
    }

    @Before
    public void setup() throws Exception {
        LOG.info("Module: {} Instance: {} attempting to configure.",
                getModuleName(),getInstanceName());
        Stopwatch stopWatch = Stopwatch.createStarted();
        ObjectName objectName = null;
        for(int i = 0;i<MODULE_TIMEOUT_MILLIS;i++) {
            try {
                ConfigRegistry configRegistryClient = new ConfigRegistryJMXClient(ManagementFactory
                        .getPlatformMBeanServer());
                objectName = configRegistryClient.lookupConfigBean(getModuleName(), getInstanceName());
                LOG.info("Module: {} Instance: {} ObjectName: {}.",
                        getModuleName(),getInstanceName(),objectName);
                break;
            } catch (Exception e) {
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
                getModuleName(),getInstanceName(),
                stopWatch.elapsed(TimeUnit.MILLISECONDS));
        } else {
            throw new RuntimeException("NOT FOUND Module: " +getModuleName() + " Instance: " + getInstanceName() +
                    " configured after " + stopWatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
        }
    }

    @Rule
    public TestRule watcher = new TestWatcher() {

        @Override
        protected void starting(Description description) {
            LOG.info("TestWatcher: Starting test: {}", description.getDisplayName());
        }

        @Override
        protected void finished(Description description) {
            LOG.info("TestWatcher: Finished test: {}", description.getDisplayName());
        }

        @Override
        protected void succeeded(Description description) {
            LOG.info("TestWatcher: Test succeeded: {}", description.getDisplayName());
        }

        @Override
        protected void failed(Throwable ex, Description description) {
            LOG.info("TestWatcher: Test failed: {}", description.getDisplayName(), ex);
        }

        @Override
        protected void skipped(AssumptionViolatedException ex, Description description) {
            LOG.info("TestWatcher: Test skipped: {} ", description.getDisplayName(), ex);
        }
    };

}
