package org.opendaylight.controller.usermanager.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.clustering.services.IClusterServices;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.util.PathUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.controller.usermanager.*;

import org.opendaylight.controller.sal.authorization.AuthResultEnum;
import org.opendaylight.controller.sal.authorization.UserLevel;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.controller.usermanager.internal.*;

@RunWith(PaxExam.class)
public class UserManagerIntegrationTest {
    private Logger log = LoggerFactory
            .getLogger(UserManagerIntegrationTest.class);
    // get the OSGI bundle context
    @Inject
    private BundleContext bc;

    private IUserManager manager = null;

    // Configure the OSGi container
    @Configuration
    public Option[] config() {
        return options(
                //
                //
                systemProperty("logback.configurationFile").value(
                        "file:" + PathUtils.getBaseDir()
                                + "/src/test/resources/logback.xml"),
                // To start OSGi console for inspection remotely
                systemProperty("osgi.console").value("2401"),
                systemProperty("org.eclipse.gemini.web.tomcat.config.path")
                        .value(PathUtils.getBaseDir()
                                + "/src/test/resources/tomcat-server.xml"),

                // setting default level. Jersey bundles will need to be started
                // earlier.
                systemProperty("osgi.bundles.defaultStartLevel").value("4"),

                // Set the systemPackages (used by clustering)
                systemPackages("sun.reflect", "sun.reflect.misc", "sun.misc"),
                mavenBundle("javax.servlet", "servlet-api", "2.5"),

                mavenBundle("org.slf4j", "jcl-over-slf4j", "1.7.2"),
                mavenBundle("org.slf4j", "slf4j-api", "1.7.2"),
                mavenBundle("org.slf4j", "log4j-over-slf4j", "1.7.2"),
                mavenBundle("ch.qos.logback", "logback-core", "1.0.9"),
                mavenBundle("ch.qos.logback", "logback-classic", "1.0.9"),
                mavenBundle("org.apache.commons", "commons-lang3", "3.1"),
                mavenBundle("org.apache.felix",
                        "org.apache.felix.dependencymanager", "3.1.0"),

                // the plugin stub to get data for the tests
                mavenBundle("org.opendaylight.controller",
                        "protocol_plugins.stub", "0.4.0-SNAPSHOT"),

                // List all the opendaylight modules
                mavenBundle("org.opendaylight.controller", "security",
                        "0.4.0-SNAPSHOT").noStart(),
                mavenBundle("org.opendaylight.controller", "sal",
                        "0.5.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "sal.implementation", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "statisticsmanager",
                        "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "statisticsmanager.implementation", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "containermanager",
                        "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "containermanager.implementation", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "forwardingrulesmanager", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "forwardingrulesmanager.implementation",
                        "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "arphandler",
                        "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "clustering.services", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "clustering.services-implementation", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "switchmanager",
                        "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "switchmanager.implementation", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "configuration",
                        "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "configuration.implementation", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "hosttracker",
                        "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "hosttracker.implementation", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "arphandler",
                        "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "routing.dijkstra_implementation", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "topologymanager",
                        "0.4.0-SNAPSHOT"),

                mavenBundle("org.opendaylight.controller", "usermanager",
                        "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "usermanager.implementation", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "logging.bridge",
                        "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller", "clustering.test",
                        "0.4.0-SNAPSHOT"),

                mavenBundle("org.opendaylight.controller",
                        "forwarding.staticrouting", "0.4.0-SNAPSHOT"),

                // Northbound bundles
                mavenBundle("org.opendaylight.controller",
                        "commons.northbound", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "forwarding.staticrouting.northbound", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "statistics.northbound", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "topology.northbound", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "hosttracker.northbound", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "switchmanager.northbound", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "flowprogrammer.northbound", "0.4.0-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller",
                        "subnets.northbound", "0.4.0-SNAPSHOT"),

                mavenBundle("org.codehaus.jackson", "jackson-mapper-asl",
                        "1.9.8"),
                mavenBundle("org.codehaus.jackson", "jackson-core-asl", "1.9.8"),
                mavenBundle("org.codehaus.jackson", "jackson-jaxrs", "1.9.8"),
                mavenBundle("org.codehaus.jettison", "jettison", "1.3.3"),

                mavenBundle("commons-io", "commons-io", "2.3"),

                mavenBundle("commons-fileupload", "commons-fileupload", "1.2.2"),

                mavenBundle("equinoxSDK381", "javax.servlet",
                        "3.0.0.v201112011016"),
                mavenBundle("equinoxSDK381", "javax.servlet.jsp",
                        "2.2.0.v201112011158"),
                mavenBundle("equinoxSDK381", "org.eclipse.equinox.ds",
                        "1.4.0.v20120522-1841"),
                mavenBundle("orbit", "javax.xml.rpc", "1.1.0.v201005080400"),
                mavenBundle("equinoxSDK381", "org.eclipse.equinox.util",
                        "1.0.400.v20120522-2049"),
                mavenBundle("equinoxSDK381", "org.eclipse.osgi.services",
                        "3.3.100.v20120522-1822"),
                mavenBundle("equinoxSDK381", "org.apache.felix.gogo.command",
                        "0.8.0.v201108120515"),
                mavenBundle("equinoxSDK381", "org.apache.felix.gogo.runtime",
                        "0.8.0.v201108120515"),
                mavenBundle("equinoxSDK381", "org.apache.felix.gogo.shell",
                        "0.8.0.v201110170705"),
                mavenBundle("equinoxSDK381", "org.eclipse.equinox.cm",
                        "1.0.400.v20120522-1841"),
                mavenBundle("equinoxSDK381", "org.eclipse.equinox.console",
                        "1.0.0.v20120522-1841"),
                mavenBundle("equinoxSDK381", "org.eclipse.equinox.launcher",
                        "1.3.0.v20120522-1813"),

                mavenBundle("geminiweb", "org.eclipse.gemini.web.core",
                        "2.2.0.RELEASE"),
                mavenBundle("geminiweb", "org.eclipse.gemini.web.extender",
                        "2.2.0.RELEASE"),
                mavenBundle("geminiweb", "org.eclipse.gemini.web.tomcat",
                        "2.2.0.RELEASE"),
                mavenBundle("geminiweb",
                        "org.eclipse.virgo.kernel.equinox.extensions",
                        "3.6.0.RELEASE").noStart(),
                mavenBundle("geminiweb", "org.eclipse.virgo.util.common",
                        "3.6.0.RELEASE"),
                mavenBundle("geminiweb", "org.eclipse.virgo.util.io",
                        "3.6.0.RELEASE"),
                mavenBundle("geminiweb", "org.eclipse.virgo.util.math",
                        "3.6.0.RELEASE"),
                mavenBundle("geminiweb", "org.eclipse.virgo.util.osgi",
                        "3.6.0.RELEASE"),
                mavenBundle("geminiweb",
                        "org.eclipse.virgo.util.osgi.manifest", "3.6.0.RELEASE"),
                mavenBundle("geminiweb",
                        "org.eclipse.virgo.util.parser.manifest",
                        "3.6.0.RELEASE"),

                mavenBundle("org.apache.felix",
                        "org.apache.felix.dependencymanager", "3.1.0"),
                mavenBundle("org.apache.felix",
                        "org.apache.felix.dependencymanager.shell", "3.0.1"),

                mavenBundle("com.google.code.gson", "gson", "2.1"),
                mavenBundle("org.jboss.spec.javax.transaction",
                        "jboss-transaction-api_1.1_spec", "1.0.1.Final"),
                mavenBundle("org.apache.felix", "org.apache.felix.fileinstall",
                        "3.1.6"),
                mavenBundle("org.apache.commons", "commons-lang3", "3.1"),
                mavenBundle("commons-codec", "commons-codec"),
                mavenBundle("virgomirror",
                        "org.eclipse.jdt.core.compiler.batch",
                        "3.8.0.I20120518-2145"),
                mavenBundle("eclipselink", "javax.persistence",
                        "2.0.4.v201112161009"),

                mavenBundle("orbit", "javax.activation", "1.1.0.v201211130549"),
                mavenBundle("orbit", "javax.annotation", "1.1.0.v201209060031"),
                mavenBundle("orbit", "javax.ejb", "3.1.1.v201204261316"),
                mavenBundle("orbit", "javax.el", "2.2.0.v201108011116"),
                mavenBundle("orbit", "javax.mail.glassfish",
                        "1.4.1.v201108011116"),
                mavenBundle("orbit", "javax.xml.rpc", "1.1.0.v201005080400"),
                mavenBundle("orbit", "org.apache.catalina",
                        "7.0.32.v201211201336"),
                // these are bundle fragments that can't be started on its own
                mavenBundle("orbit", "org.apache.catalina.ha",
                        "7.0.32.v201211201952").noStart(),
                mavenBundle("orbit", "org.apache.catalina.tribes",
                        "7.0.32.v201211201952").noStart(),
                mavenBundle("orbit", "org.apache.coyote",
                        "7.0.32.v201211201952").noStart(),
                mavenBundle("orbit", "org.apache.jasper",
                        "7.0.32.v201211201952").noStart(),

                mavenBundle("orbit", "org.apache.el", "7.0.32.v201211081135"),
                mavenBundle("orbit", "org.apache.juli.extras",
                        "7.0.32.v201211081135"),
                mavenBundle("orbit", "org.apache.tomcat.api",
                        "7.0.32.v201211081135"),
                mavenBundle("orbit", "org.apache.tomcat.util",
                        "7.0.32.v201211201952").noStart(),
                mavenBundle("orbit", "javax.servlet.jsp.jstl",
                        "1.2.0.v201105211821"),
                mavenBundle("orbit", "javax.servlet.jsp.jstl.impl",
                        "1.2.0.v201210211230"),

                mavenBundle("org.ops4j.pax.exam", "pax-exam-container-native"),
                mavenBundle("org.ops4j.pax.exam", "pax-exam-junit4"),
                mavenBundle("org.ops4j.pax.exam", "pax-exam-link-mvn"),
                mavenBundle("org.ops4j.pax.url", "pax-url-aether"),

                mavenBundle("org.springframework", "org.springframework.asm",
                        "3.1.3.RELEASE"),
                mavenBundle("org.springframework", "org.springframework.aop",
                        "3.1.3.RELEASE"),
                mavenBundle("org.springframework",
                        "org.springframework.context", "3.1.3.RELEASE"),
                mavenBundle("org.springframework",
                        "org.springframework.context.support", "3.1.3.RELEASE"),
                mavenBundle("org.springframework", "org.springframework.core",
                        "3.1.3.RELEASE"),
                mavenBundle("org.springframework", "org.springframework.beans",
                        "3.1.3.RELEASE"),
                mavenBundle("org.springframework",
                        "org.springframework.expression", "3.1.3.RELEASE"),
                mavenBundle("org.springframework", "org.springframework.web",
                        "3.1.3.RELEASE"),

                mavenBundle("org.aopalliance",
                        "com.springsource.org.aopalliance", "1.0.0"),
                mavenBundle("org.springframework",
                        "org.springframework.web.servlet", "3.1.3.RELEASE"),
                mavenBundle("org.springframework.security",
                        "spring-security-config", "3.1.3.RELEASE"),
                mavenBundle("org.springframework.security",
                        "spring-security-core", "3.1.3.RELEASE"),
                mavenBundle("org.springframework.security",
                        "spring-security-web", "3.1.3.RELEASE"),
                mavenBundle("org.springframework.security",
                        "spring-security-taglibs", "3.1.3.RELEASE"),
                mavenBundle("org.springframework",
                        "org.springframework.transaction", "3.1.3.RELEASE"),

                mavenBundle("org.ow2.chameleon.management", "chameleon-mbeans",
                        "1.0.0"),
                mavenBundle("org.opendaylight.controller.thirdparty",
                        "net.sf.jung2", "2.0.1-SNAPSHOT"),
                mavenBundle("org.opendaylight.controller.thirdparty",
                        "com.sun.jersey.jersey-servlet", "1.17-SNAPSHOT"),

                // Jersey needs to be started before the northbound application
                // bundles, using a lower start level
                mavenBundle("com.sun.jersey", "jersey-client", "1.17"),
                mavenBundle("com.sun.jersey", "jersey-server", "1.17")
                        .startLevel(2),
                mavenBundle("com.sun.jersey", "jersey-core", "1.17")
                        .startLevel(2),
                mavenBundle("com.sun.jersey", "jersey-json", "1.17")
                        .startLevel(2), junitBundles());
    }

    private String stateToString(int state) {
        switch (state) {
        case Bundle.ACTIVE:
            return "ACTIVE";
        case Bundle.INSTALLED:
            return "INSTALLED";
        case Bundle.RESOLVED:
            return "RESOLVED";
        case Bundle.UNINSTALLED:
            return "UNINSTALLED";
        default:
            return "Not CONVERTED";
        }
    }

    @Before
    public void areWeReady() {
        assertNotNull(bc);
        boolean debugit = false;
        Bundle b[] = bc.getBundles();
        for (int i = 0; i < b.length; i++) {
            int state = b[i].getState();
            if (state != Bundle.ACTIVE && state != Bundle.RESOLVED) {
                log.debug("Bundle:" + b[i].getSymbolicName() + " state:"
                        + stateToString(state));
                debugit = true;
            }
        }
        if (debugit) {
            log.debug("Do some debugging because some bundle is "
                    + "unresolved");
        }

        // Assert if true, if false we are good to go!
        assertFalse(debugit);

        ServiceReference r = bc.getServiceReference(IUserManager.class
                .getName());
        if (r != null) {
            this.manager = (IUserManager) bc.getService(r);
        }
        // If StatisticsManager is null, cannot run tests.
        assertNotNull(this.manager);

    }

    /**
     * Test method for
     * {@link org.opendaylight.controller.usermanager.internal.UserManagerImpl#changeLocalUserPassword(java.lang.String, java.lang.String, java.lang.String)}
     * .
     */
    @Test
    public void testChangeLocalUserPassword() {
        // fail("Not yet implemented");
        try {
            List<String> roles = new ArrayList<String>(1);
            roles.add(UserLevel.SYSTEMADMIN.toString());
            UserConfig uc = new UserConfig("administrator", "admin", roles);
            this.manager.addLocalUser(uc);
            Status changepwd = this.manager.changeLocalUserPassword(
                    uc.getUser(), uc.getPassword(), "admin1");
            log.info("Code::" + changepwd.getCode());
            log.info("desc::" + changepwd.getDescription());
            Assert.assertTrue(changepwd.getCode() == StatusCode.SUCCESS);
            // Assert.assertFalse("user does not exist", changepwd.getCode() ==
            // StatusCode.NOTFOUND);
        } catch (Exception e) {
            // Got an unexpected exception
            Assert.assertTrue(false);
        }

    }

}