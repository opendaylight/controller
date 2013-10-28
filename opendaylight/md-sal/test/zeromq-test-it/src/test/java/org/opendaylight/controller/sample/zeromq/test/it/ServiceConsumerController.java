package org.opendaylight.controller.sample.zeromq.test.it;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.BundleContext;

import javax.inject.Inject;

import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.*;

@RunWith(PaxExam.class)
public class ServiceConsumerController {

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
                systemProperty("pub.port").value("5557"),
                systemProperty("sub.port").value("5556"),
                systemProperty("rpc.port").value("5555"),
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
                mavenBundle(SAMPLE, "zeromq-test-consumer").versionAsInProject(), //
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
