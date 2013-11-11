package org.opendaylight.controller.sample.toaster.it;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sample.toaster.provider.api.ToastConsumer;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.WhiteBread;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.BundleContext;

import javax.inject.Inject;

import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import org.ops4j.pax.exam.util.PathUtils;

@RunWith(PaxExam.class)
public class ToasterTest {

    public static final String ODL = "org.opendaylight.controller";
    public static final String YANG = "org.opendaylight.yangtools";
    public static final String CONTROLLER = "org.opendaylight.controller";
    public static final String YANGTOOLS = "org.opendaylight.yangtools";
    
    
    public static final String SAMPLE = "org.opendaylight.controller.samples";

    @Test
    public void properInitialized() throws Exception {

        Thread.sleep(500); // Waiting for services to get wired.

        assertTrue(consumer.createToast(WhiteBread.class, 5));

    }

    @Inject
    BindingAwareBroker broker;

    @Inject
    ToastConsumer consumer;

    @Inject
    BundleContext ctx;

    @Configuration
    public Option[] config() {
        return options(systemProperty("osgi.console").value("2401"), 
                systemProperty("logback.configurationFile").value(
                    "file:" + PathUtils.getBaseDir()
                    + "/src/test/resources/logback.xml"),
                mavenBundle("org.slf4j", "slf4j-api").versionAsInProject(), //
                mavenBundle("org.slf4j", "log4j-over-slf4j").versionAsInProject(), //
                mavenBundle("ch.qos.logback", "logback-core").versionAsInProject(), //
                mavenBundle("ch.qos.logback", "logback-classic").versionAsInProject(), //
                
                mavenBundle(ODL, "sal-common").versionAsInProject(), //
                mavenBundle(ODL, "sal-common-api").versionAsInProject(),//
                mavenBundle(ODL, "sal-common-impl").versionAsInProject(), //
                mavenBundle(ODL, "sal-common-util").versionAsInProject(), //
                
                mavenBundle(ODL, "config-api").versionAsInProject(), //
                mavenBundle(ODL, "config-manager").versionAsInProject(), //
                mavenBundle("commons-io", "commons-io").versionAsInProject(),
                mavenBundle("org.apache.commons", "commons-lang3").versionAsInProject(),
                
                mavenBundle(CONTROLLER, "sal-binding-api").versionAsInProject(), //
                mavenBundle(CONTROLLER, "sal-binding-config").versionAsInProject(),
                mavenBundle(CONTROLLER, "sal-binding-broker-impl").versionAsInProject(), //
                mavenBundle("org.javassist", "javassist").versionAsInProject(), //
                mavenBundle(CONTROLLER, "sal-common-util").versionAsInProject(), //
        
                mavenBundle(YANGTOOLS, "yang-data-api").versionAsInProject(), //
                mavenBundle(YANGTOOLS, "yang-data-impl").versionAsInProject(), //
                mavenBundle(YANGTOOLS, "yang-model-api").versionAsInProject(), //
                mavenBundle(YANGTOOLS, "yang-model-util").versionAsInProject(), //
                mavenBundle(YANGTOOLS, "yang-parser-api").versionAsInProject(),
                mavenBundle(YANGTOOLS, "yang-parser-impl").versionAsInProject(),
                
                
                mavenBundle(YANGTOOLS, "binding-generator-spi").versionAsInProject(), //
                mavenBundle(YANGTOOLS, "binding-model-api").versionAsInProject(), //
                mavenBundle(YANGTOOLS, "binding-generator-util").versionAsInProject(),
                mavenBundle(YANGTOOLS, "yang-parser-impl").versionAsInProject(),
                mavenBundle(YANGTOOLS, "binding-type-provider").versionAsInProject(),
                mavenBundle(YANGTOOLS, "binding-generator-api").versionAsInProject(),
                mavenBundle(YANGTOOLS, "binding-generator-spi").versionAsInProject(),
                mavenBundle(YANGTOOLS, "binding-generator-impl").versionAsInProject(),
                
                
                mavenBundle(CONTROLLER, "sal-core-api").versionAsInProject().update(), //
                mavenBundle(CONTROLLER, "sal-broker-impl").versionAsInProject(), //
                mavenBundle(CONTROLLER, "sal-core-spi").versionAsInProject().update(), //
                
                mavenBundle(YANGTOOLS + ".thirdparty", "antlr4-runtime-osgi-nohead").versionAsInProject(), //
                
                mavenBundle(SAMPLE, "sample-toaster").versionAsInProject(), //
                mavenBundle(SAMPLE, "sample-toaster-consumer").versionAsInProject(), //
                mavenBundle(SAMPLE, "sample-toaster-provider").versionAsInProject(), //
                mavenBundle(YANG, "concepts").versionAsInProject(),
                mavenBundle(YANG, "yang-binding").versionAsInProject(), //
                mavenBundle(YANG, "yang-common").versionAsInProject(), //
                mavenBundle(YANG+".thirdparty", "xtend-lib-osgi").versionAsInProject(),
                mavenBundle("com.google.guava", "guava").versionAsInProject(), //
                mavenBundle("org.javassist", "javassist").versionAsInProject(),
                junitBundles()
                );
    }

}
