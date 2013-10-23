package org.opendaylight.controller.test.sal.binding.it;

import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.repository;

import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.options.DefaultCompositeOption;

public class TestHelper {

    public static final String CONTROLLER = "org.opendaylight.controller";
    public static final String YANGTOOLS = "org.opendaylight.yangtools";

    public static final String CONTROLLER_MODELS = "org.opendaylight.controller.model";
    public static final String YANGTOOLS_MODELS = "org.opendaylight.yangtools.model";

    public static Option mdSalCoreBundles() {
        return new DefaultCompositeOption( //
                mavenBundle(YANGTOOLS, "concepts").versionAsInProject(), //
                mavenBundle(YANGTOOLS, "yang-binding").versionAsInProject(), //
                mavenBundle(YANGTOOLS, "yang-common").versionAsInProject(), //
                mavenBundle(CONTROLLER, "sal-common").versionAsInProject(), //
                mavenBundle(CONTROLLER, "sal-common-api").versionAsInProject(), //
                mavenBundle(CONTROLLER, "sal-common-impl").versionAsInProject(), //
                mavenBundle("com.google.guava", "guava").versionAsInProject(), //
                mavenBundle(YANGTOOLS + ".thirdparty", "xtend-lib-osgi").versionAsInProject() //
        );
    }

    public static Option bindingAwareSalBundles() {
        return new DefaultCompositeOption( //
                mavenBundle(CONTROLLER, "sal-binding-api").versionAsInProject(), //
                mavenBundle(CONTROLLER, "sal-binding-broker-impl").versionAsInProject(), //
                mavenBundle("org.javassist", "javassist").versionAsInProject(), //
                mavenBundle(CONTROLLER, "sal-common-util").versionAsInProject() //
        );

    }

    public static Option bindingIndependentSalBundles() {
        return new DefaultCompositeOption(

        );

    }

    public static Option flowCapableModelBundles() {
        return new DefaultCompositeOption( //
                mavenBundle(CONTROLLER_MODELS, "model-flow-base").versionAsInProject(), //
                mavenBundle(CONTROLLER_MODELS, "model-flow-service").versionAsInProject(), //
                mavenBundle(CONTROLLER_MODELS, "model-inventory").versionAsInProject() //
        );

    }

    public static Option baseModelBundles() {
        return new DefaultCompositeOption( //
                mavenBundle(YANGTOOLS_MODELS, "yang-ext").versionAsInProject(), //
                mavenBundle(YANGTOOLS_MODELS, "ietf-inet-types").versionAsInProject(), //
                mavenBundle(YANGTOOLS_MODELS, "ietf-yang-types").versionAsInProject(), //
                mavenBundle(YANGTOOLS_MODELS, "opendaylight-l2-types").versionAsInProject(), //
                mavenBundle(CONTROLLER_MODELS, "model-inventory").versionAsInProject());
    }

    public static Option junitAndMockitoBundles() {
        return new DefaultCompositeOption(
                // Repository required to load harmcrest (OSGi-fied version).
                repository("http://repository.springsource.com/maven/bundles/external").id(
                        "com.springsource.repository.bundles.external"),

                // Mockito
                mavenBundle("org.mockito", "mockito-all", "1.9.5"),
                junitBundles(),

                /*
                 * Felix has implicit boot delegation enabled by default. It
                 * conflicts with Mockito: java.lang.LinkageError: loader
                 * constraint violation in interface itable initialization: when
                 * resolving method
                 * "org.osgi.service.useradmin.User$$EnhancerByMockitoWithCGLIB$$dd2f81dc
                 * .
                 * newInstance(Lorg/mockito/cglib/proxy/Callback;)Ljava/lang/Object
                 * ;" the class loader (instance of
                 * org/mockito/internal/creation/jmock/SearchingClassLoader) of
                 * the current class, org/osgi/service/useradmin/
                 * User$$EnhancerByMockitoWithCGLIB$$dd2f81dc, and the class
                 * loader (instance of org/apache/felix/framework/
                 * BundleWiringImpl$BundleClassLoaderJava5) for interface
                 * org/mockito/cglib/proxy/Factory have different Class objects
                 * for the type org/mockito/cglib/ proxy/Callback used in the
                 * signature
                 * 
                 * So we disable the bootdelegation. this property has no effect
                 * on the other OSGi implementation.
                 */
                frameworkProperty("felix.bootdelegation.implicit").value("false"));
    }
}
