package org.opendaylight.controller.test.sal.binding.it;

import static org.ops4j.pax.exam.CoreOptions.*;

import javax.inject.Inject;

import org.junit.runner.RunWith;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.osgi.framework.BundleContext;

@RunWith(PaxExam.class)
public abstract class AbstractTest {

    public static final String CONTROLLER = "org.opendaylight.controller";
    public static final String YANGTOOLS = "org.opendaylight.yangtools";
    
    public static final String CONTROLLER_MODELS = "org.opendaylight.controller.model";
    public static final String YANGTOOLS_MODELS = "org.opendaylight.yangtools.model";
    
    
    @Inject
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
        return options(systemProperty("osgi.console").value("2401"), 
                mavenBundle("org.slf4j", "slf4j-api").versionAsInProject(), //
                mavenBundle("org.slf4j", "log4j-over-slf4j").versionAsInProject(), //
                mavenBundle("ch.qos.logback", "logback-core").versionAsInProject(), //
                mavenBundle("ch.qos.logback", "logback-classic").versionAsInProject(), //
                
                // MD-SAL Dependencies
                mavenBundle(YANGTOOLS, "concepts").versionAsInProject(),
                mavenBundle(YANGTOOLS, "yang-binding").versionAsInProject(), //
                mavenBundle(YANGTOOLS, "yang-common").versionAsInProject(), //
                
                mavenBundle(YANGTOOLS+".thirdparty", "xtend-lib-osgi").versionAsInProject(),
                mavenBundle("com.google.guava", "guava").versionAsInProject(), //
                mavenBundle("org.javassist", "javassist").versionAsInProject(),
                
                // MD SAL
                mavenBundle(CONTROLLER, "sal-binding-api").versionAsInProject(), //
                mavenBundle(CONTROLLER, "sal-binding-broker-impl").versionAsInProject(), //
                mavenBundle(CONTROLLER, "sal-common").versionAsInProject(), //
                mavenBundle(CONTROLLER, "sal-common-api").versionAsInProject(),
                mavenBundle(CONTROLLER, "sal-common-util").versionAsInProject(), //
                
                // BASE Models
                mavenBundle(YANGTOOLS_MODELS,"yang-ext").versionAsInProject(),
                mavenBundle(YANGTOOLS_MODELS,"ietf-inet-types").versionAsInProject(),
                mavenBundle(YANGTOOLS_MODELS,"ietf-yang-types").versionAsInProject(),
                mavenBundle(YANGTOOLS_MODELS,"opendaylight-l2-types").versionAsInProject(),
                mavenBundle(CONTROLLER_MODELS,"model-flow-base").versionAsInProject(),
                mavenBundle(CONTROLLER_MODELS,"model-flow-service").versionAsInProject(),
                mavenBundle(CONTROLLER_MODELS,"model-inventory").versionAsInProject(),
                junitAndMockitoBundles()
                );
    }
    
    
    public static Option junitAndMockitoBundles() {
        return new DefaultCompositeOption(
                // Repository required to load harmcrest (OSGi-fied version).
                repository("http://repository.springsource.com/maven/bundles/external").id(
                        "com.springsource.repository.bundles.external"),

                // Mockito without Hamcrest and Objenesis
                mavenBundle("org.mockito", "mockito-all", "1.9.5"),

                // Hamcrest with a version matching the range expected by Mockito
                //mavenBundle("org.hamcrest", "com.springsource.org.hamcrest.core", "1.1.0"),

                // Objenesis with a version matching the range expected by Mockito
                //wrappedBundle(mavenBundle("org.objenesis", "objenesis", "1.2"))
                 //       .exports("*;version=1.2"),

                // The default JUnit bundle also exports Hamcrest, but with an (incorrect) version of
                // 4.9 which does not match the Mockito import. When deployed after the hamcrest bundles, it gets
                // resolved correctly.
                junitBundles(),

                /*
                 * Felix has implicit boot delegation enabled by default. It conflicts with Mockito:
                 * java.lang.LinkageError: loader constraint violation in interface itable initialization:
                 * when resolving method "org.osgi.service.useradmin.User$$EnhancerByMockitoWithCGLIB$$dd2f81dc
                 * .newInstance(Lorg/mockito/cglib/proxy/Callback;)Ljava/lang/Object;" the class loader
                 * (instance of org/mockito/internal/creation/jmock/SearchingClassLoader) of the current class,
                 * org/osgi/service/useradmin/User$$EnhancerByMockitoWithCGLIB$$dd2f81dc, and the class loader
                 * (instance of org/apache/felix/framework/BundleWiringImpl$BundleClassLoaderJava5) for interface
                 * org/mockito/cglib/proxy/Factory have different Class objects for the type org/mockito/cglib/
                 * proxy/Callback used in the signature
                 *
                 * So we disable the bootdelegation. this property has no effect on the other OSGi implementation.
                 */
                frameworkProperty("felix.bootdelegation.implicit").value("false")
        );
    }
}
