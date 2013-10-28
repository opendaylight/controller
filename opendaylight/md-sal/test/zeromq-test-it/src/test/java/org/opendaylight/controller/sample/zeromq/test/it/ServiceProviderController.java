package org.opendaylight.controller.sample.zeromq.test.it;

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
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

@RunWith(PaxExam.class)
public class ServiceProviderController {

    public static final String ODL = "org.opendaylight.controller";
    public static final String YANG = "org.opendaylight.yangtools";
    public static final String SAMPLE = "org.opendaylight.controller.samples";

    @Test
    public void properInitialized() throws Exception {

        Thread.sleep(30000); // Waiting for services to get wired.
        assertTrue(true);
        //assertTrue(consumer.createToast(WhiteBread.class, 5));

    }

//    @Inject
//    BindingAwareBroker broker;

//    @Inject
//    ToastConsumer consumer;

    @Inject
    BundleContext ctx;

    @Configuration
    public Option[] config() {
        return options(systemProperty("osgi.console").value("2401"),
                systemProperty("pub.port").value("5556"),
                systemProperty("sub.port").value("5557"),
                systemProperty("rpc.port").value("5554"),
                systemProperty("pub.ip").value("localhost"),
                mavenBundle("org.slf4j", "slf4j-api").versionAsInProject(), //
                mavenBundle("org.slf4j", "log4j-over-slf4j").versionAsInProject(), //
                mavenBundle("ch.qos.logback", "logback-core").versionAsInProject(), //
                mavenBundle("ch.qos.logback", "logback-classic").versionAsInProject(), //
               
                //mavenBundle(ODL, "sal-binding-broker-impl").versionAsInProject().update(), //
                mavenBundle(ODL, "sal-common").versionAsInProject(), //
                mavenBundle(ODL, "sal-common-api").versionAsInProject(),//
                mavenBundle(ODL, "sal-common-impl").versionAsInProject(), //
                mavenBundle(ODL, "sal-common-util").versionAsInProject(), //
                mavenBundle(ODL, "sal-core-api").versionAsInProject().update(), //
                mavenBundle(ODL, "sal-broker-impl").versionAsInProject(), //
                mavenBundle(ODL, "sal-core-spi").versionAsInProject().update(), //
                mavenBundle(ODL, "sal-connector-api").versionAsInProject(), //
                mavenBundle(SAMPLE, "zeromq-test-provider").versionAsInProject(), //
                mavenBundle(ODL, "sal-zeromq-connector").versionAsInProject(), //
                mavenBundle(YANG, "concepts").versionAsInProject(),
                mavenBundle(YANG, "yang-binding").versionAsInProject(), //
                mavenBundle(YANG, "yang-common").versionAsInProject(), //
                mavenBundle(YANG, "yang-data-api").versionAsInProject(), //
                mavenBundle(YANG, "yang-model-api").versionAsInProject(), //
                mavenBundle(YANG+".thirdparty", "xtend-lib-osgi").versionAsInProject(), //
                mavenBundle("com.google.guava", "guava").versionAsInProject(), //
                mavenBundle("org.jeromq", "jeromq").versionAsInProject(),
                junitBundles()
                );
    }

}
