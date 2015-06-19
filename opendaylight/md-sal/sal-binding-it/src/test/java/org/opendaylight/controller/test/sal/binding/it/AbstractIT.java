/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.test.sal.binding.it;

import static org.opendaylight.controller.test.sal.binding.it.TestHelper.baseModelBundles;
import static org.opendaylight.controller.test.sal.binding.it.TestHelper.bindingAwareSalBundles;
import static org.opendaylight.controller.test.sal.binding.it.TestHelper.configMinumumBundles;
import static org.opendaylight.controller.test.sal.binding.it.TestHelper.junitAndMockitoBundles;
import static org.opendaylight.controller.test.sal.binding.it.TestHelper.mdSalCoreBundles;
import static org.opendaylight.controller.test.sal.binding.it.TestHelper.salTestModelBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import javax.inject.Inject;
import org.junit.runner.RunWith;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.util.Filter;
import org.osgi.framework.BundleContext;

@RunWith(PaxExam.class)
public abstract class AbstractIT {

    public static final String CONTROLLER = "org.opendaylight.controller";
    public static final String YANGTOOLS = "org.opendaylight.yangtools";

    public static final String CONTROLLER_MODELS = "org.opendaylight.controller.model";
    public static final String YANGTOOLS_MODELS = "org.opendaylight.yangtools.model";

    @Inject
    @Filter(timeout=120*1000)
    BindingAwareBroker broker;

    @Inject
    BundleContext bundleContext;

    public BindingAwareBroker getBroker() {
        return broker;
    }

    public void setBroker(final BindingAwareBroker broker) {
        this.broker = broker;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Configuration
    public Option[] config() {
        return options(systemProperty("osgi.console").value("2401"), mavenBundle("org.slf4j", "slf4j-api")
                .versionAsInProject(), //
                mavenBundle("org.slf4j", "log4j-over-slf4j").versionAsInProject(), //
                mavenBundle("ch.qos.logback", "logback-core").versionAsInProject(), //
                mavenBundle("ch.qos.logback", "logback-classic").versionAsInProject(),
                mavenBundle("openexi", "nagasena").versionAsInProject(),
                mavenBundle("openexi", "nagasena-rta").versionAsInProject(),
                 //
                systemProperty("osgi.bundles.defaultStartLevel").value("4"),
                systemPackages("sun.nio.ch"),

                mdSalCoreBundles(),
                configMinumumBundles(),
                bindingAwareSalBundles(),

                // BASE Models
                baseModelBundles(),
                salTestModelBundles(),

                // Set fail if unresolved bundle present
                systemProperty("pax.exam.osgi.unresolved.fail").value("true"),
                junitAndMockitoBundles());
    }

}
