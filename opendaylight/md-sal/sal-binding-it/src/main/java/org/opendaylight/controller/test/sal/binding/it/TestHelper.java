/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.test.sal.binding.it;

import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.util.PathUtils;

/**
 * @deprecated Use config-it and/or mdsal-it instead.
 */
@Deprecated
public class TestHelper {

    public static final String CONTROLLER = "org.opendaylight.controller";
    public static final String MDSAL = "org.opendaylight.mdsal";
    public static final String YANGTOOLS = "org.opendaylight.yangtools";

    public static final String CONTROLLER_MODELS = "org.opendaylight.controller.model";
    public static final String MDSAL_MODELS = "org.opendaylight.mdsal.model";

    public static Option mdSalCoreBundles() {
        return new DefaultCompositeOption( //
                mavenBundle(YANGTOOLS, "concepts").versionAsInProject(), // //
                mavenBundle(YANGTOOLS, "util").versionAsInProject(), // //
                mavenBundle(MDSAL, "yang-binding").versionAsInProject(), // //
                mavenBundle(YANGTOOLS, "yang-common").versionAsInProject(), // //
                mavenBundle(YANGTOOLS, "object-cache-api").versionAsInProject(), // //
                mavenBundle(YANGTOOLS, "object-cache-guava").versionAsInProject(), // //
                mavenBundle(CONTROLLER, "sal-common-api").versionAsInProject(), // //
                mavenBundle(CONTROLLER, "sal-common-impl").versionAsInProject(), // //

                mavenBundle("org.apache.commons", "commons-lang3").versionAsInProject(), //
                mavenBundle("com.google.guava", "guava").versionAsInProject(),
                mavenBundle("com.github.romix", "java-concurrent-hash-trie-map").versionAsInProject()
        );
    }

    public static Option configMinumumBundles() {
        return new DefaultCompositeOption(
                mavenBundle(CONTROLLER, "config-api").versionAsInProject(), // //
                bindingAwareSalBundles(),
                mavenBundle("commons-codec", "commons-codec").versionAsInProject(),

                systemPackages("sun.nio.ch", "sun.misc"),

                mavenBundle(CONTROLLER, "config-manager").versionAsInProject(), // //
                mavenBundle(CONTROLLER, "config-util").versionAsInProject(), // //
                mavenBundle("commons-io", "commons-io").versionAsInProject(), //
                mavenBundle(CONTROLLER, "config-manager-facade-xml").versionAsInProject(), //
                mavenBundle(CONTROLLER, "yang-jmx-generator").versionAsInProject(), //
                mavenBundle(CONTROLLER, "logback-config").versionAsInProject(), //
                mavenBundle(CONTROLLER, "config-persister-api").versionAsInProject(), //

                mavenBundle(CONTROLLER, "config-persister-impl").versionAsInProject(), //

                mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.xerces", "2.11.0_1"),
                mavenBundle("org.eclipse.birt.runtime.3_7_1", "org.apache.xml.resolver", "1.2.0"),

                mavenBundle(CONTROLLER, "config-persister-file-xml-adapter").versionAsInProject().noStart(),
                mavenBundle("org.eclipse.persistence", "org.eclipse.persistence.moxy").versionAsInProject(),
                mavenBundle("org.eclipse.persistence", "org.eclipse.persistence.core").versionAsInProject());

    }

    public static Option bindingAwareSalBundles() {
        return new DefaultCompositeOption( //
                mdSalCoreBundles(),

                mavenBundle("org.javassist", "javassist").versionAsInProject(), // //

                mavenBundle(YANGTOOLS, "yang-data-api").versionAsInProject(), // //
                mavenBundle(YANGTOOLS, "yang-data-util").versionAsInProject(), // //
                mavenBundle(YANGTOOLS, "yang-data-impl").versionAsInProject(), // //
                mavenBundle(YANGTOOLS, "yang-model-api").versionAsInProject(), // //
                mavenBundle(YANGTOOLS, "yang-model-util").versionAsInProject(), // //
                mavenBundle(YANGTOOLS, "yang-parser-api").versionAsInProject(), //
                mavenBundle(YANGTOOLS, "yang-parser-impl").versionAsInProject(), //
                mavenBundle(MDSAL, "mdsal-binding-generator-api").versionAsInProject(), //
                mavenBundle(MDSAL, "mdsal-binding-generator-util").versionAsInProject(), //
                mavenBundle(MDSAL, "mdsal-binding-generator-impl").versionAsInProject(),
                mavenBundle(MDSAL, "mdsal-binding-dom-codec").versionAsInProject(),
                mavenBundle("org.antlr", "antlr4-runtime").versionAsInProject(), // //

                mavenBundle(CONTROLLER, "sal-binding-util").versionAsInProject(), //
                mavenBundle(CONTROLLER, "sal-common-util").versionAsInProject(), // //


                mavenBundle(CONTROLLER, "sal-core-api").versionAsInProject().update(), //
                mavenBundle(CONTROLLER, "sal-binding-api").versionAsInProject(), // //

                mavenBundle("com.lmax", "disruptor").versionAsInProject(), //
                mavenBundle(CONTROLLER, "sal-broker-impl").versionAsInProject(), // //
                mavenBundle(CONTROLLER, "sal-dom-config").versionAsInProject(), // //

                mavenBundle(CONTROLLER, "sal-inmemory-datastore").versionAsInProject(), //
                mavenBundle(CONTROLLER, "sal-dom-broker-config").versionAsInProject(), // //

                mavenBundle(CONTROLLER, "sal-core-spi").versionAsInProject().update(), //

                mavenBundle(CONTROLLER, "sal-binding-broker-impl").versionAsInProject(), // //
                mavenBundle(CONTROLLER, "sal-binding-config").versionAsInProject(), //

                systemProperty("netconf.config.persister.active").value("1"), //
                systemProperty("netconf.config.persister.1.storageAdapterClass").value(
                        "org.opendaylight.controller.config.persist.storage.file.xml.XmlFileStorageAdapter"), //
                systemProperty("netconf.config.persister.1.properties.fileStorage")
                        .value(PathUtils.getBaseDir() + "/src/test/resources/controller.xml"), //
                systemProperty("netconf.config.persister.1.properties.numberOfBackups").value("1") //

        );

    }

    public static Option bindingIndependentSalBundles() {
        return new DefaultCompositeOption(

        );

    }

    public static Option protocolFrameworkBundles() {
        return new DefaultCompositeOption(
            mavenBundle("io.netty", "netty-common").versionAsInProject(), //
            mavenBundle("io.netty", "netty-buffer").versionAsInProject(), //
            mavenBundle("io.netty", "netty-handler").versionAsInProject(), //
            mavenBundle("io.netty", "netty-codec").versionAsInProject(), //
            mavenBundle("io.netty", "netty-transport").versionAsInProject(), //
            mavenBundle(CONTROLLER, "netty-config-api").versionAsInProject(), //
            mavenBundle(CONTROLLER, "protocol-framework").versionAsInProject()
        );

    }

    public static Option flowCapableModelBundles() {
        return new DefaultCompositeOption( //
                mavenBundle(CONTROLLER_MODELS, "model-inventory").versionAsInProject() //
        );

    }

    /**
     * @return option containing models for testing purposes
     */
    public static Option salTestModelBundles() {
        return new DefaultCompositeOption( //
                mavenBundle(CONTROLLER, "sal-test-model").versionAsInProject()
        );

    }

    public static Option baseModelBundles() {
        return new DefaultCompositeOption( //
                mavenBundle(MDSAL+".model", "yang-ext").versionAsInProject(),
                mavenBundle(MDSAL_MODELS, "ietf-type-util").versionAsInProject(),
                mavenBundle(MDSAL_MODELS, "ietf-inet-types").versionAsInProject(),
                mavenBundle(MDSAL_MODELS, "ietf-yang-types").versionAsInProject(),
                mavenBundle(MDSAL_MODELS, "opendaylight-l2-types").versionAsInProject()
                );
    }

    public static Option junitAndMockitoBundles() {
        return new DefaultCompositeOption(
        // Repository required to load harmcrest (OSGi-fied version).
        // Mockito
                mavenBundle("org.mockito", "mockito-core", "1.10.19"),
                mavenBundle("org.objenesis", "objenesis", "2.2"),
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
