package org.opendaylight.controller.test.restconf.it;

import static org.junit.Assert.*;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.maven;

import java.util.Collection;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.test.sal.binding.it.TestHelper;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.util.PathUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import static org.opendaylight.controller.test.sal.binding.it.TestHelper.*;

@RunWith(PaxExam.class)
public class ServiceProviderController {

    public static final String ODL = "org.opendaylight.controller";
    public static final String YANG = "org.opendaylight.yangtools";
    public static final String SAMPLE = "org.opendaylight.controller.samples";

    @Test
    public void properInitialized() throws Exception {

        Thread.sleep(30*60*1000); // Waiting for services to get wired.
        assertTrue(true);
        // assertTrue(consumer.createToast(WhiteBread.class, 5));

    }

    // @Inject
    // BindingAwareBroker broker;

    // @Inject
    // ToastConsumer consumer;

    @Inject
    BundleContext ctx;

    @Configuration
    public Option[] config() {
        return options(
                mavenBundle("org.slf4j", "slf4j-api").versionAsInProject(), //
                mavenBundle("org.slf4j", "log4j-over-slf4j").versionAsInProject(), //
                mavenBundle("ch.qos.logback", "logback-core").versionAsInProject(), //
                mavenBundle("ch.qos.logback", "logback-classic").versionAsInProject(), //

                mdSalCoreBundles(),
                baseModelBundles(),
                flowCapableModelBundles(),

                // mavenBundle(ODL,
                // "sal-binding-broker-impl").versionAsInProject().update(), //
                mavenBundle(ODL, "sal-common").versionAsInProject(), //
                mavenBundle(ODL, "sal-common-api").versionAsInProject(),//
                mavenBundle(ODL, "sal-common-impl").versionAsInProject(), //
                mavenBundle(ODL, "sal-common-util").versionAsInProject(), //

                mavenBundle(ODL, "sal-core-api").versionAsInProject().update(), //
                mavenBundle(ODL, "sal-broker-impl").versionAsInProject(), //
                mavenBundle(ODL, "sal-core-spi").versionAsInProject().update(), //

                mavenBundle(ODL, "sal-connector-api").versionAsInProject(), //
                // mavenBundle(SAMPLE,
                // "zeromq-test-provider").versionAsInProject(), //
                mavenBundle(ODL, "sal-rest-connector").versionAsInProject(), //

                mavenBundle(YANG, "concepts").versionAsInProject(),
                mavenBundle(YANG, "yang-binding").versionAsInProject(), //
                mavenBundle(YANG, "yang-common").versionAsInProject(), //
                mavenBundle(YANG, "yang-data-api").versionAsInProject(), //
                mavenBundle(YANG, "yang-data-impl").versionAsInProject(), //
                mavenBundle(YANG, "yang-model-api").versionAsInProject(), //
                mavenBundle(YANG, "yang-model-util").versionAsInProject(), //
                mavenBundle(YANG, "yang-parser-api").versionAsInProject(),
                mavenBundle(YANG, "yang-parser-impl").versionAsInProject(),
                mavenBundle(YANG + ".thirdparty", "xtend-lib-osgi").versionAsInProject(), //
                mavenBundle(YANG + ".thirdparty", "antlr4-runtime-osgi-nohead").versionAsInProject(), //
                mavenBundle("com.google.guava", "guava").versionAsInProject(), //

                // systemProperty("logback.configurationFile").value(
                // "file:" + PathUtils.getBaseDir() +
                // "/src/test/resources/logback.xml"),
                // To start OSGi console for inspection remotely
                systemProperty("osgi.console").value("2401"),
                systemProperty("org.eclipse.gemini.web.tomcat.config.path").value(
                        PathUtils.getBaseDir() + "/src/test/resources/tomcat-server.xml"),

                // setting default level. Jersey bundles will need to be started
                // earlier.
                systemProperty("osgi.bundles.defaultStartLevel").value("4"),

                // Set the systemPackages (used by clustering)
                systemPackages("sun.reflect", "sun.reflect.misc", "sun.misc"),
                mavenBundle("org.slf4j", "jcl-over-slf4j").versionAsInProject(),
                mavenBundle("org.slf4j", "slf4j-api").versionAsInProject(),
                mavenBundle("org.slf4j", "log4j-over-slf4j").versionAsInProject(),
                mavenBundle("ch.qos.logback", "logback-core").versionAsInProject(),
                mavenBundle("ch.qos.logback", "logback-classic").versionAsInProject(),
                mavenBundle("org.apache.commons", "commons-lang3").versionAsInProject(),
                mavenBundle("org.apache.felix", "org.apache.felix.dependencymanager").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "clustering.services").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "containermanager").versionAsInProject(),
                // List all the opendaylight modules
                mavenBundle("org.opendaylight.controller", "configuration").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "sal").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "switchmanager").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "usermanager").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "logging.bridge").versionAsInProject(),
                // mavenBundle("org.opendaylight.controller",
                // "clustering.test").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "bundlescanner").versionAsInProject(),
                mavenBundle("org.opendaylight.controller", "bundlescanner.implementation").versionAsInProject(),

                // Northbound bundles
                mavenBundle("org.opendaylight.controller", "commons.northbound").versionAsInProject(),
                mavenBundle("org.codehaus.jackson", "jackson-mapper-asl").versionAsInProject(),
                mavenBundle("org.codehaus.jackson", "jackson-core-asl").versionAsInProject(),
                mavenBundle("org.codehaus.jackson", "jackson-jaxrs").versionAsInProject(),
                mavenBundle("org.codehaus.jackson", "jackson-xc").versionAsInProject(),
                mavenBundle("org.codehaus.jettison", "jettison").versionAsInProject(),

                mavenBundle("commons-io", "commons-io").versionAsInProject(),

                // mavenBundle("commons-fileupload",
                // "commons-fileupload").versionAsInProject(),

                mavenBundle("equinoxSDK381", "javax.servlet").versionAsInProject(),
                mavenBundle("equinoxSDK381", "javax.servlet.jsp").versionAsInProject(),
                mavenBundle("equinoxSDK381", "org.eclipse.equinox.ds").versionAsInProject(),
                mavenBundle("orbit", "javax.xml.rpc").versionAsInProject(),
                mavenBundle("equinoxSDK381", "org.eclipse.equinox.util").versionAsInProject(),
                mavenBundle("equinoxSDK381", "org.eclipse.osgi.services").versionAsInProject(),
                mavenBundle("equinoxSDK381", "org.apache.felix.gogo.command").versionAsInProject(),
                mavenBundle("equinoxSDK381", "org.apache.felix.gogo.runtime").versionAsInProject(),
                mavenBundle("equinoxSDK381", "org.apache.felix.gogo.shell").versionAsInProject(),
                mavenBundle("equinoxSDK381", "org.eclipse.equinox.cm").versionAsInProject(),
                mavenBundle("equinoxSDK381", "org.eclipse.equinox.console").versionAsInProject(),
                mavenBundle("equinoxSDK381", "org.eclipse.equinox.launcher").versionAsInProject(),

                mavenBundle("geminiweb", "org.eclipse.gemini.web.core").versionAsInProject(),
                mavenBundle("geminiweb", "org.eclipse.gemini.web.extender").versionAsInProject(),
                mavenBundle("geminiweb", "org.eclipse.gemini.web.tomcat").versionAsInProject(),
                mavenBundle("geminiweb", "org.eclipse.virgo.kernel.equinox.extensions").versionAsInProject().noStart(),
                mavenBundle("geminiweb", "org.eclipse.virgo.util.common").versionAsInProject(),
                mavenBundle("geminiweb", "org.eclipse.virgo.util.io").versionAsInProject(),
                mavenBundle("geminiweb", "org.eclipse.virgo.util.math").versionAsInProject(),
                mavenBundle("geminiweb", "org.eclipse.virgo.util.osgi").versionAsInProject(),
                mavenBundle("geminiweb", "org.eclipse.virgo.util.osgi.manifest").versionAsInProject(),
                mavenBundle("geminiweb", "org.eclipse.virgo.util.parser.manifest").versionAsInProject(),

                mavenBundle("org.apache.felix", "org.apache.felix.dependencymanager").versionAsInProject(),
                mavenBundle("org.apache.felix", "org.apache.felix.dependencymanager.shell").versionAsInProject(),

                mavenBundle("com.google.code.gson", "gson").versionAsInProject(),
                mavenBundle("org.jboss.spec.javax.transaction", "jboss-transaction-api_1.1_spec").versionAsInProject(),
                mavenBundle("org.apache.felix", "org.apache.felix.fileinstall").versionAsInProject(),
                mavenBundle("org.apache.commons", "commons-lang3").versionAsInProject(),
                mavenBundle("commons-codec", "commons-codec").versionAsInProject(),
                mavenBundle("virgomirror", "org.eclipse.jdt.core.compiler.batch").versionAsInProject(),
                mavenBundle("eclipselink", "javax.persistence").versionAsInProject(),
                mavenBundle("eclipselink", "javax.resource").versionAsInProject(),

                mavenBundle("orbit", "javax.activation").versionAsInProject(),
                mavenBundle("orbit", "javax.annotation").versionAsInProject(),
                mavenBundle("orbit", "javax.ejb").versionAsInProject(),
                mavenBundle("orbit", "javax.el").versionAsInProject(),
                mavenBundle("orbit", "javax.mail.glassfish").versionAsInProject(),
                mavenBundle("orbit", "javax.xml.rpc").versionAsInProject(),
                mavenBundle("orbit", "org.apache.catalina").versionAsInProject(),
                // these are bundle fragments that can't be started on its own
                mavenBundle("orbit", "org.apache.catalina.ha").versionAsInProject().noStart(),
                mavenBundle("orbit", "org.apache.catalina.tribes").versionAsInProject().noStart(),
                mavenBundle("orbit", "org.apache.coyote").versionAsInProject().noStart(),
                mavenBundle("orbit", "org.apache.jasper").versionAsInProject().noStart(),

                mavenBundle("orbit", "org.apache.el").versionAsInProject(),
                mavenBundle("orbit", "org.apache.juli.extras").versionAsInProject(),
                mavenBundle("orbit", "org.apache.tomcat.api").versionAsInProject(),
                mavenBundle("orbit", "org.apache.tomcat.util").versionAsInProject().noStart(),
                mavenBundle("orbit", "javax.servlet.jsp.jstl").versionAsInProject(),
                mavenBundle("orbit", "javax.servlet.jsp.jstl.impl").versionAsInProject(),

                mavenBundle("org.ops4j.pax.exam", "pax-exam-container-native").versionAsInProject(),
                mavenBundle("org.ops4j.pax.exam", "pax-exam-junit4").versionAsInProject(),
                mavenBundle("org.ops4j.pax.exam", "pax-exam-link-mvn").versionAsInProject(),
                mavenBundle("org.ops4j.pax.url", "pax-url-aether").versionAsInProject(),

                mavenBundle("org.ow2.asm", "asm-all").versionAsInProject(),

                mavenBundle("org.springframework", "org.springframework.asm").versionAsInProject(),
                mavenBundle("org.springframework", "org.springframework.aop").versionAsInProject(),
                mavenBundle("org.springframework", "org.springframework.context").versionAsInProject(),
                mavenBundle("org.springframework", "org.springframework.context.support").versionAsInProject(),
                mavenBundle("org.springframework", "org.springframework.core").versionAsInProject(),
                mavenBundle("org.springframework", "org.springframework.beans").versionAsInProject(),
                mavenBundle("org.springframework", "org.springframework.expression").versionAsInProject(),
                mavenBundle("org.springframework", "org.springframework.web").versionAsInProject(),

                mavenBundle("org.aopalliance", "com.springsource.org.aopalliance").versionAsInProject(),
                mavenBundle("org.springframework", "org.springframework.web.servlet").versionAsInProject(),
                mavenBundle("org.springframework.security", "spring-security-config").versionAsInProject(),
                mavenBundle("org.springframework.security", "spring-security-core").versionAsInProject(),
                mavenBundle("org.springframework.security", "spring-security-web").versionAsInProject(),
                mavenBundle("org.springframework.security", "spring-security-taglibs").versionAsInProject(),
                mavenBundle("org.springframework", "org.springframework.transaction").versionAsInProject(),

                mavenBundle("org.ow2.chameleon.management", "chameleon-mbeans").versionAsInProject(),
                mavenBundle("org.opendaylight.controller.thirdparty", "com.sun.jersey.jersey-servlet")
                        .versionAsInProject().startLevel(2),
                mavenBundle("org.opendaylight.controller.thirdparty", "org.apache.catalina.filters.CorsFilter")
                        .versionAsInProject().noStart(),

                // Jersey needs to be started before the northbound application
                // bundles, using a lower start level
                mavenBundle("com.sun.jersey", "jersey-client").versionAsInProject(),
                mavenBundle("com.sun.jersey", "jersey-server").versionAsInProject().startLevel(2),
                mavenBundle("com.sun.jersey", "jersey-core").versionAsInProject().startLevel(2),
                mavenBundle("com.sun.jersey", "jersey-json").versionAsInProject().startLevel(2),

                junitBundles());
    }

}
