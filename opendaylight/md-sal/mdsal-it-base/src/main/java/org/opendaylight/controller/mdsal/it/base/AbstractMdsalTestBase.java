/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.mdsal.it.base;

import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;

import java.io.File;
import javax.inject.Inject;
import org.junit.Before;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.exam.options.extra.VMOption;
import org.ops4j.pax.exam.util.Filter;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMdsalTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractMdsalTestBase.class);
    private static final String MAVEN_REPO_LOCAL = "maven.repo.local";
    private static final String ETC_ORG_OPS4J_PAX_URL_MVN_CFG = "etc/org.ops4j.pax.url.mvn.cfg";
    private static final String ETC_ORG_OPS4J_PAX_LOGGING_CFG = "etc/org.ops4j.pax.logging.cfg";

    private static final String PAX_EXAM_UNPACK_DIRECTORY = "target/exam";
    private static final String KARAF_DEBUG_PORT = "5005";
    private static final String KARAF_DEBUG_PROP = "karaf.debug";
    private static final String KEEP_UNPACK_DIRECTORY_PROP = "karaf.keep.unpack";

    /*
     * Default values for karaf distro type, groupId, and artifactId
     */
    private static final String KARAF_DISTRO_TYPE = "zip";
    private static final String KARAF_DISTRO_ARTIFACTID = "opendaylight-karaf-empty";
    private static final String KARAF_DISTRO_GROUPID = "org.opendaylight.odlparent";

    /*
     * Property names to override defaults for karaf distro artifactId, groupId,
     * version, and type
     */
    private static final String KARAF_DISTRO_TYPE_PROP = "karaf.distro.type";
    private static final String KARAF_DISTRO_ARTIFACTID_PROP = "karaf.distro.artifactId";
    private static final String KARAF_DISTRO_GROUPID_PROP = "karaf.distro.groupId";

    public static final String ORG_OPS4J_PAX_LOGGING_CFG = "etc/org.ops4j.pax.logging.cfg";

    @Inject @Filter(timeout = 60000)
    private BundleContext context;

    public abstract MavenUrlReference getFeatureRepo();

    public abstract String getFeatureName();

    @Before
    public void setup() throws Exception {
    }

    public Option getLoggingOption() {
        Option option = editConfigurationFilePut(ORG_OPS4J_PAX_LOGGING_CFG,
                "log4j2.logger.mdsal-it-base.name",
                AbstractMdsalTestBase.class.getPackage().getName());
        option = composite(option, editConfigurationFilePut(ORG_OPS4J_PAX_LOGGING_CFG,
                "log4j2.logger.mdsal-it-base.level",
                LogLevel.INFO.name()));
        return option;
    }

    /**
     * Override this method to provide more options to config.
     *
     * @return An array of additional config options
     */
    protected Option[] getAdditionalOptions() {
        return new Option[0];
    }

    /**
     * Returns a Log4J logging configuration property name for the given class's package name of the form
     * "log4j.logger.package_name".
     *
     * @deprecated The karaf logging provider is now Log4J2 so logging configurations must conform to the Log4J2 style.
     *     This method is kept for compilation backwards compatibility but will be removed in a future release.
     */
    @Deprecated
    public String logConfiguration(final Class<?> klazz) {
        return "log4j.logger." + klazz.getPackage().getName();
    }

    public String getKarafDistro() {
        String groupId = System.getProperty(KARAF_DISTRO_GROUPID_PROP, KARAF_DISTRO_GROUPID);
        String artifactId = System.getProperty(KARAF_DISTRO_ARTIFACTID_PROP, KARAF_DISTRO_ARTIFACTID);
        String type = System.getProperty(KARAF_DISTRO_TYPE_PROP, KARAF_DISTRO_TYPE);

        return maven().groupId(groupId).artifactId(artifactId).versionAsInProject().type(type).getURL();
    }

    protected Option mvnLocalRepoOption() {
        String mvnRepoLocal = System.getProperty(MAVEN_REPO_LOCAL, "");
        LOG.info("mvnLocalRepo \"{}\"", mvnRepoLocal);
        return editConfigurationFilePut(ETC_ORG_OPS4J_PAX_URL_MVN_CFG,
            "org.ops4j.pax.url.mvn.localRepository", mvnRepoLocal);
    }

    @Configuration
    public Option[] config() {
        Option[] options = new Option[] {
                when(Boolean.getBoolean(KARAF_DEBUG_PROP))
                        .useOptions(KarafDistributionOption.debugConfiguration(KARAF_DEBUG_PORT, true)),
                karafDistributionConfiguration().frameworkUrl(getKarafDistro())
                        .unpackDirectory(new File(PAX_EXAM_UNPACK_DIRECTORY)).useDeployFolder(false),
                when(Boolean.getBoolean(KEEP_UNPACK_DIRECTORY_PROP)).useOptions(keepRuntimeFolder()),
                features(getFeatureRepo(), getFeatureName()),
                mvnLocalRepoOption(),

                // Make sure karaf's default repository is consulted before anything else
                editConfigurationFilePut(ETC_ORG_OPS4J_PAX_URL_MVN_CFG, "org.ops4j.pax.url.mvn.defaultRepositories",
                        "file:${karaf.home}/${karaf.default.repository}@id=system.repository"),

                configureConsole().ignoreLocalConsole().ignoreRemoteShell(),
                editConfigurationFilePut(ETC_ORG_OPS4J_PAX_LOGGING_CFG, "log4j2.rootLogger.level", "INFO") };

        final String karafVersion = MavenUtils.getArtifactVersion("org.apache.karaf.features",
                "org.apache.karaf.features.core");
        options = OptionUtils.combine(options, new VMOption[] {
            new VMOption("--add-reads=java.xml=java.logging"),
            new VMOption("--add-exports=java.base/org.apache.karaf.specs.locator=java.xml,ALL-UNNAMED"),
            new VMOption("--patch-module"),
            new VMOption("java.base=lib/endorsed/org.apache.karaf.specs.locator-" + karafVersion + ".jar"),
            new VMOption("--patch-module"),
            new VMOption("java.xml=lib/endorsed/org.apache.karaf.specs.java.xml-" + karafVersion + ".jar"),
            new VMOption("--add-opens"),
            new VMOption("java.base/java.security=ALL-UNNAMED"),
            new VMOption("--add-opens"),
            new VMOption("java.base/java.net=ALL-UNNAMED"),
            new VMOption("--add-opens"),
            new VMOption("java.base/java.lang=ALL-UNNAMED"),
            new VMOption("--add-opens"),
            new VMOption("java.base/java.util=ALL-UNNAMED"),
            new VMOption("--add-opens"),
            new VMOption("java.naming/javax.naming.spi=ALL-UNNAMED"),
            new VMOption("--add-opens"),
            new VMOption("java.rmi/sun.rmi.transport.tcp=ALL-UNNAMED"),
            new VMOption("--add-exports=java.base/sun.net.www.protocol.http=ALL-UNNAMED"),
            new VMOption("--add-exports=java.base/sun.net.www.protocol.https=ALL-UNNAMED"),
            new VMOption("--add-exports=java.base/sun.net.www.protocol.jar=ALL-UNNAMED"),
            new VMOption("--add-exports=jdk.naming.rmi/com.sun.jndi.url.rmi=ALL-UNNAMED"),
            new VMOption("-classpath"),
            new VMOption("lib/jdk9plus/*" + File.pathSeparator + "lib/boot/*")
        });

        return OptionUtils.combine(options, getAdditionalOptions());
    }
}
