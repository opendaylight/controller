package org.opendaylight.controller.test.sal.binding.it;

import static org.ops4j.pax.exam.CoreOptions.*;
import static org.opendaylight.controller.test.sal.binding.it.TestHelper.*;

import javax.inject.Inject;

import org.junit.runner.RunWith;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.util.Filter;
import org.ops4j.pax.exam.util.PathUtils;
import org.osgi.framework.BundleContext;

@RunWith(PaxExam.class)
public abstract class AbstractTest {

    public static final String CONTROLLER = "org.opendaylight.controller";
    public static final String YANGTOOLS = "org.opendaylight.yangtools";

    public static final String CONTROLLER_MODELS = "org.opendaylight.controller.model";
    public static final String YANGTOOLS_MODELS = "org.opendaylight.yangtools.model";

    @Inject
    @Filter(timeout=60*1000)
    BindingAwareBroker broker;
    
    @Inject
    BundleContext bundleContext;

    public BindingAwareBroker getBroker() {
        return broker;
    }

    public void setBroker(BindingAwareBroker broker) {
        this.broker = broker;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Configuration
    public Option[] config() {
        return options(systemProperty("osgi.console").value("2401"), mavenBundle("org.slf4j", "slf4j-api")
                .versionAsInProject(), //
                systemProperty("logback.configurationFile").value(
                        "file:" + PathUtils.getBaseDir()
                                + "/src/test/resources/logback.xml"),
                mavenBundle("org.slf4j", "log4j-over-slf4j").versionAsInProject(), //
                mavenBundle("ch.qos.logback", "logback-core").versionAsInProject(), //
                mavenBundle("ch.qos.logback", "logback-classic").versionAsInProject(), //
                systemProperty("osgi.bundles.defaultStartLevel").value("4"),

                
                
                mdSalCoreBundles(),

                bindingAwareSalBundles(),
                configMinumumBundles(),
                // BASE Models
                baseModelBundles(), flowCapableModelBundles(), junitAndMockitoBundles());
    }
    
}
